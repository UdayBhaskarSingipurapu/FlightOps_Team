package com.project.flightOps.service;

import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.TurnaroundMilestone;
import com.project.flightOps.entity.TurnaroundPlan;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.*;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.TurnaroundMilestoneRepository;
import com.project.flightOps.repository.TurnaroundPlanRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.MilestoneCompleteRequest;
import com.project.flightOps.requestdto.TurnaroundPlanRequest;
import com.project.flightOps.responsedto.TurnaroundMilestoneResponse;
import com.project.flightOps.responsedto.TurnaroundPlanResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added SLF4J Annotation
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j // Enables the 'log' instance variable automatically
@Service
@RequiredArgsConstructor
public class TurnaroundService {

    private final TurnaroundPlanRepository planRepository;
    private final TurnaroundMilestoneRepository milestoneRepository;
    private final FlightService flightService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Default SLA offset in minutes from scheduled arrival for each milestone
    private static final Map<MilestoneType, Integer> SLA_OFFSETS = Map.of(
            MilestoneType.ChocksOn, 2,
            MilestoneType.DoorOpen, 7,
            MilestoneType.StairsDocked, 5,
            MilestoneType.BaggageOffload, 25,
            MilestoneType.Cleaning, 35,
            MilestoneType.Catering, 40,
            MilestoneType.Fuelling, 40,
            MilestoneType.BoardingComplete, 55,
            MilestoneType.DoorClose, 58,
            MilestoneType.PushbackClearance, 60
    );

    @Transactional
    public TurnaroundPlanResponse createPlan(TurnaroundPlanRequest request, String supervisorId) {
        log.info("Attempting to create turnaround plan for flightId: {} by supervisorId: {}", request.getFlightId(), supervisorId);
        Flight flight = flightService.findById(request.getFlightId());

        if (planRepository.existsByFlight(flight)) {
            log.warn("Conflict detected: A turnaround plan already exists for flight: {}", flight.getFlightNumber());
            throw new ConflictException("A turnaround plan already exists for flight: "
                    + flight.getFlightNumber());
        }

        User supervisor = userRepository.findById(supervisorId)
                .orElseThrow(() -> {
                    log.error("Failed to create plan: Supervisor not found with ID: {}", supervisorId);
                    return new ResourceNotFoundException("Supervisor not found");
                });

        TurnaroundPlan plan = new TurnaroundPlan();
        plan.setFlight(flight);
        plan.setTargetTurnaroundMinutes(request.getTargetTurnaroundMinutes());
        plan.setSupervisor(supervisor);
        plan.setStatus(TurnaroundStatus.Active);

        TurnaroundPlan savedPlan = planRepository.save(plan);
        log.debug("Turnaround plan entity saved with ID: {}", savedPlan.getPlanId());

        // Auto-generate all 10 milestones with planned times based on scheduled arrival
        List<TurnaroundMilestone> milestones = new ArrayList<>();
        for (MilestoneType type : MilestoneType.values()) {
            TurnaroundMilestone milestone = new TurnaroundMilestone();
            milestone.setTurnaroundPlan(savedPlan);
            milestone.setMilestoneType(type);
            milestone.setPlannedTime(
                    flight.getScheduledArrival().plusMinutes(SLA_OFFSETS.getOrDefault(type, 30)));
            milestone.setStatus(MilestoneStatus.Pending);
            milestones.add(milestone);
        }
        milestoneRepository.saveAll(milestones);
        log.info("Successfully generated {} milestones for flight: {}", milestones.size(), flight.getFlightNumber());

        // Notify ramp officers
        userRepository.findByRole(Role.RampOfficer).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "Turnaround plan created for flight " + flight.getFlightNumber(),
                        NotificationCategory.Turnaround));

        return toResponse(savedPlan, milestones);
    }

    public List<TurnaroundPlanResponse> getActive() {
        log.debug("Fetching all active turnaround plans");
        return planRepository.findByStatusOrderByStatusAsc(TurnaroundStatus.Active)
                .stream().map(p -> toResponse(p,
                        milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(p)))
                .toList();
    }

    public List<TurnaroundPlanResponse> getAll() {
        log.debug("Fetching all turnaround plans");
        return planRepository.findAll().stream()
                .map(p -> toResponse(p,
                        milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(p)))
                .toList();
    }

    public TurnaroundPlanResponse getById(String planId) {
        log.debug("Fetching turnaround plan by ID: {}", planId);
        TurnaroundPlan plan = findPlanById(planId);
        return toResponse(plan, milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(plan));
    }

    public TurnaroundPlanResponse getByFlight(String flightId) {
        log.debug("Fetching turnaround plan for flight ID: {}", flightId);
        TurnaroundPlan plan = planRepository.findByFlight_FlightId(flightId)
                .orElseThrow(() -> {
                    log.warn("Turnaround plan query failed: No plan found for flight ID: {}", flightId);
                    return new ResourceNotFoundException("No turnaround plan found for flight: " + flightId);
                });
        return toResponse(plan, milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(plan));
    }

    @Transactional
    public TurnaroundPlanResponse completePlan(String planId) {
        log.info("Attempting to complete turnaround plan ID: {}", planId);
        TurnaroundPlan plan = findPlanById(planId);

        if (plan.getStatus() == TurnaroundStatus.Completed) {
            log.warn("Bad Request: Turnaround plan {} is already in Completed state", planId);
            throw new BadRequestException("Turnaround plan is already completed");
        }

        // Compute actual turnaround minutes from ChocksOn to PushbackClearance
        List<TurnaroundMilestone> milestones =
                milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(plan);
        LocalDateTime chocksOnActual = milestones.stream()
                .filter(m -> m.getMilestoneType() == MilestoneType.ChocksOn
                        && m.getActualTime() != null)
                .map(TurnaroundMilestone::getActualTime)
                .findFirst().orElse(null);
        LocalDateTime pushbackActual = milestones.stream()
                .filter(m -> m.getMilestoneType() == MilestoneType.PushbackClearance
                        && m.getActualTime() != null)
                .map(TurnaroundMilestone::getActualTime)
                .findFirst().orElse(null);

        if (chocksOnActual != null && pushbackActual != null) {
            int calculatedMinutes = (int) ChronoUnit.MINUTES.between(chocksOnActual, pushbackActual);
            plan.setActualTurnaroundMinutes(calculatedMinutes);
            log.debug("Calculated actual turnaround time for plan {}: {} minutes", planId, calculatedMinutes);
        } else {
            log.warn("Could not calculate precise turnaround duration for plan {} because ChocksOn or PushbackClearance actual times were missing", planId);
        }

        plan.setStatus(TurnaroundStatus.Completed);
        TurnaroundPlan saved = planRepository.save(plan);
        log.info("Turnaround plan for flight {} successfully marked as COMPLETED", plan.getFlight().getFlightNumber());

        // Notify coordinator
        notificationService.sendNotification(
                plan.getSupervisor().getUserId(),
                "Turnaround for flight " + plan.getFlight().getFlightNumber() + " completed",
                NotificationCategory.Turnaround);

        return toResponse(saved, milestones);
    }

    // Milestone operations
    public List<TurnaroundMilestoneResponse> getMilestonesByPlan(String planId) {
        log.debug("Fetching milestones for plan ID: {}", planId);
        return milestoneRepository.findByTurnaroundPlan_PlanIdOrderByPlannedTimeAsc(planId)
                .stream().map(this::toMilestoneResponse).toList();
    }

    @Transactional
    public TurnaroundMilestoneResponse completeMilestone(String milestoneId,
                                                         MilestoneCompleteRequest request, String completedByUserId) {
        log.info("User {} attempting to complete milestone ID: {} at actual time: {}", completedByUserId, milestoneId, request.getActualTime());
        TurnaroundMilestone milestone = findMilestoneById(milestoneId);

        if (milestone.getStatus() == MilestoneStatus.Completed) {
            log.warn("Bad Request: Milestone ID {} is already marked as Completed", milestoneId);
            throw new BadRequestException("Milestone is already completed");
        }

        User completedBy = userRepository.findById(completedByUserId)
                .orElseThrow(() -> {
                    log.error("Milestone completion failed: User {} not found", completedByUserId);
                    return new ResourceNotFoundException("User not found");
                });

        milestone.setActualTime(request.getActualTime());
        milestone.setCompletedBy(completedBy);

        // Determine if delayed
        boolean delayed = request.getActualTime().isAfter(milestone.getPlannedTime());
        milestone.setStatus(delayed ? MilestoneStatus.Delayed : MilestoneStatus.Completed);

        TurnaroundMilestone saved = milestoneRepository.save(milestone);
        TurnaroundPlan plan = milestone.getTurnaroundPlan();

        if (delayed) {
            long delayMinutes = ChronoUnit.MINUTES.between(
                    milestone.getPlannedTime(), request.getActualTime());

            log.warn("SLA BREACH: Milestone {} for flight {} completed late by {} minutes",
                    milestone.getMilestoneType(), plan.getFlight().getFlightNumber(), delayMinutes);

            // Notify supervisor of SLA breach
            notificationService.sendNotification(
                    plan.getSupervisor().getUserId(),
                    "SLA breach: " + milestone.getMilestoneType() + " for flight "
                            + plan.getFlight().getFlightNumber()
                            + " is " + delayMinutes + " min late",
                    NotificationCategory.Turnaround);

            // Update plan status to Delayed
            plan.setStatus(TurnaroundStatus.Delayed);
            planRepository.save(plan);
        } else {
            log.info("Milestone {} for flight {} successfully completed on schedule",
                    milestone.getMilestoneType(), plan.getFlight().getFlightNumber());
        }

        return toMilestoneResponse(saved);
    }

    public List<TurnaroundMilestoneResponse> getDelayedMilestones() {
        log.debug("Fetching all currently delayed milestones");
        return milestoneRepository.findByStatusOrderByPlannedTimeAsc(MilestoneStatus.Delayed)
                .stream().map(this::toMilestoneResponse).toList();
    }

    public TurnaroundMilestoneResponse getMilestoneById(String milestoneId) {
        log.debug("Fetching milestone by ID: {}", milestoneId);
        return toMilestoneResponse(findMilestoneById(milestoneId));
    }

    // Scheduled job: every 2 minutes, check for overdue pending milestones and fire alerts
    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void checkOverdueMilestones() {
        LocalDateTime now = LocalDateTime.now();
        log.trace("Scheduled check for overdue milestones executing at {}", now);

        List<TurnaroundMilestone> overdue = milestoneRepository.findOverdueMilestones(now);

        if (!overdue.isEmpty()) {
            log.warn("Scheduled job detected {} overdue pending milestones", overdue.size());
        }

        overdue.forEach(milestone -> {
            TurnaroundPlan plan = milestone.getTurnaroundPlan();
            long minutesLate = ChronoUnit.MINUTES.between(milestone.getPlannedTime(), now);

            log.warn("ALERT: Milestone {} for flight {} is overdue by {} minutes!",
                    milestone.getMilestoneType(), plan.getFlight().getFlightNumber(), minutesLate);

            notificationService.sendNotification(
                    plan.getSupervisor().getUserId(),
                    "OVERDUE: " + milestone.getMilestoneType() + " for flight "
                            + plan.getFlight().getFlightNumber()
                            + " is " + minutesLate + " min overdue",
                    NotificationCategory.Turnaround);
        });
    }

    private TurnaroundPlan findPlanById(String id) {
        return planRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Entity look up failed: Turnaround plan not found for ID: {}", id);
                    return new ResourceNotFoundException("Turnaround plan not found: " + id);
                });
    }

    private TurnaroundMilestone findMilestoneById(String id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Entity look up failed: Milestone not found for ID: {}", id);
                    return new ResourceNotFoundException("Milestone not found: " + id);
                });
    }

    private TurnaroundPlanResponse toResponse(TurnaroundPlan p, List<TurnaroundMilestone> milestones) {
        return TurnaroundPlanResponse.builder()
                .planId(p.getPlanId())
                .flightId(p.getFlight().getFlightId())
                .flightNumber(p.getFlight().getFlightNumber())
                .stand(p.getFlight().getStand())
                .targetTurnaroundMinutes(p.getTargetTurnaroundMinutes())
                .actualTurnaroundMinutes(p.getActualTurnaroundMinutes())
                .supervisorId(p.getSupervisor() != null ? p.getSupervisor().getUserId() : null)
                .supervisorName(p.getSupervisor() != null ? p.getSupervisor().getName() : null)
                .status(p.getStatus())
                .milestones(milestones.stream().map(this::toMilestoneResponse).toList())
                .build();
    }

    private TurnaroundMilestoneResponse toMilestoneResponse(TurnaroundMilestone m) {
        boolean delayed = m.getActualTime() != null
                && m.getActualTime().isAfter(m.getPlannedTime());
        Long delayMinutes = delayed
                ? ChronoUnit.MINUTES.between(m.getPlannedTime(), m.getActualTime())
                : null;

        return TurnaroundMilestoneResponse.builder()
                .milestoneId(m.getMilestoneId())
                .planId(m.getTurnaroundPlan().getPlanId())
                .milestoneType(m.getMilestoneType())
                .plannedTime(m.getPlannedTime())
                .actualTime(m.getActualTime())
                .completedById(m.getCompletedBy() != null ? m.getCompletedBy().getUserId() : null)
                .completedByName(m.getCompletedBy() != null ? m.getCompletedBy().getName() : null)
                .status(m.getStatus())
                .isDelayed(delayed)
                .delayMinutes(delayMinutes)
                .build();
    }
}