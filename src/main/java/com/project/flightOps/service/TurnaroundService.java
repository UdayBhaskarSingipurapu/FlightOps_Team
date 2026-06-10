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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        Flight flight = flightService.findById(request.getFlightId());

        if (planRepository.existsByFlight(flight)) {
            throw new ConflictException("A turnaround plan already exists for flight: "
                    + flight.getFlightNumber());
        }

        User supervisor = userRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found"));

        TurnaroundPlan plan = new TurnaroundPlan();
        plan.setFlight(flight);
        plan.setTargetTurnaroundMinutes(request.getTargetTurnaroundMinutes());
        plan.setSupervisor(supervisor);
        plan.setStatus(TurnaroundStatus.Active);

        TurnaroundPlan savedPlan = planRepository.save(plan);

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

        // Notify ramp officers
        userRepository.findByRole(Role.RampOfficer).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "Turnaround plan created for flight " + flight.getFlightNumber(),
                        NotificationCategory.Turnaround));

        return toResponse(savedPlan, milestones);
    }

    public List<TurnaroundPlanResponse> getActive() {
        return planRepository.findByStatusOrderByStatusAsc(TurnaroundStatus.Active)
                .stream().map(p -> toResponse(p,
                        milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(p)))
                .toList();
    }

    public List<TurnaroundPlanResponse> getAll() {
        return planRepository.findAll().stream()
                .map(p -> toResponse(p,
                        milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(p)))
                .toList();
    }

    public TurnaroundPlanResponse getById(String planId) {
        TurnaroundPlan plan = findPlanById(planId);
        return toResponse(plan, milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(plan));
    }

    public TurnaroundPlanResponse getByFlight(String flightId) {
        TurnaroundPlan plan = planRepository.findByFlight_FlightId(flightId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No turnaround plan found for flight: " + flightId));
        return toResponse(plan, milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(plan));
    }

    @Transactional
    public TurnaroundPlanResponse completePlan(String planId) {
        TurnaroundPlan plan = findPlanById(planId);
        if (plan.getStatus() == TurnaroundStatus.Completed) {
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
            plan.setActualTurnaroundMinutes(
                    (int) ChronoUnit.MINUTES.between(chocksOnActual, pushbackActual));
        }
        plan.setStatus(TurnaroundStatus.Completed);
        TurnaroundPlan saved = planRepository.save(plan);

        // Notify coordinator
        notificationService.sendNotification(
                plan.getSupervisor().getUserId(),
                "Turnaround for flight " + plan.getFlight().getFlightNumber() + " completed",
                NotificationCategory.Turnaround);

        return toResponse(saved, milestones);
    }

    // Milestone operations
    public List<TurnaroundMilestoneResponse> getMilestonesByPlan(String planId) {
        return milestoneRepository.findByTurnaroundPlan_PlanIdOrderByPlannedTimeAsc(planId)
                .stream().map(this::toMilestoneResponse).toList();
    }

    @Transactional
    public TurnaroundMilestoneResponse completeMilestone(String milestoneId,
            MilestoneCompleteRequest request, String completedByUserId) {
        TurnaroundMilestone milestone = findMilestoneById(milestoneId);

        if (milestone.getStatus() == MilestoneStatus.Completed) {
            throw new BadRequestException("Milestone is already completed");
        }

        User completedBy = userRepository.findById(completedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        milestone.setActualTime(request.getActualTime());
        milestone.setCompletedBy(completedBy);

        // Determine if delayed
        boolean delayed = request.getActualTime().isAfter(milestone.getPlannedTime());
        milestone.setStatus(delayed ? MilestoneStatus.Delayed : MilestoneStatus.Completed);

        TurnaroundMilestone saved = milestoneRepository.save(milestone);

        if (delayed) {
            long delayMinutes = ChronoUnit.MINUTES.between(
                    milestone.getPlannedTime(), request.getActualTime());
            // Notify supervisor of SLA breach
            TurnaroundPlan plan = milestone.getTurnaroundPlan();
            notificationService.sendNotification(
                    plan.getSupervisor().getUserId(),
                    "SLA breach: " + milestone.getMilestoneType() + " for flight "
                            + plan.getFlight().getFlightNumber()
                            + " is " + delayMinutes + " min late",
                    NotificationCategory.Turnaround);
            // Update plan status to Delayed
            plan.setStatus(TurnaroundStatus.Delayed);
            planRepository.save(plan);
        }

        return toMilestoneResponse(saved);
    }

    public List<TurnaroundMilestoneResponse> getDelayedMilestones() {
        return milestoneRepository.findByStatusOrderByPlannedTimeAsc(MilestoneStatus.Delayed)
                .stream().map(this::toMilestoneResponse).toList();
    }

    public TurnaroundMilestoneResponse getMilestoneById(String milestoneId) {
        return toMilestoneResponse(findMilestoneById(milestoneId));
    }

    // Scheduled job: every 2 minutes, check for overdue pending milestones and fire alerts
    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void checkOverdueMilestones() {
        List<TurnaroundMilestone> overdue =
                milestoneRepository.findOverdueMilestones(LocalDateTime.now());
        overdue.forEach(milestone -> {
            TurnaroundPlan plan = milestone.getTurnaroundPlan();
            long minutesLate = ChronoUnit.MINUTES.between(
                    milestone.getPlannedTime(), LocalDateTime.now());
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
                .orElseThrow(() -> new ResourceNotFoundException("Turnaround plan not found: " + id));
    }

    private TurnaroundMilestone findMilestoneById(String id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found: " + id));
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
