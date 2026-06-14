package com.project.flightOps.service;

import com.project.flightOps.entity.*;
import com.project.flightOps.enums.*;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.EquipmentAllocationRepository;
import com.project.flightOps.repository.EquipmentMaintenanceRepository;
import com.project.flightOps.repository.GroundEquipmentRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.EquipmentAllocationRequest;
import com.project.flightOps.requestdto.EquipmentMaintenanceRequest;
import com.project.flightOps.requestdto.EquipmentStatusRequest;
import com.project.flightOps.requestdto.GroundEquipmentRequest;
import com.project.flightOps.responsedto.EquipmentAllocationResponse;
import com.project.flightOps.responsedto.EquipmentMaintenanceResponse;
import com.project.flightOps.responsedto.GroundEquipmentResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for Logging
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j // Injects the 'log' field automatically via Lombok
@Service
@RequiredArgsConstructor
public class GseService {

    private final GroundEquipmentRepository equipmentRepository;
    private final EquipmentAllocationRepository allocationRepository;
    private final EquipmentMaintenanceRepository maintenanceRepository;
    private final FlightService flightService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ─── Equipment ──────────────────────────────────────────────────────────────

    @Transactional
    public GroundEquipmentResponse registerEquipment(GroundEquipmentRequest request) {
        log.info("Attempting to register new equipment with registration number: {}", request.getRegistrationNumber());
        if (equipmentRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            log.warn("Registration failed. Equipment already exists: {}", request.getRegistrationNumber());
            throw new ConflictException("Equipment with registration "
                    + request.getRegistrationNumber() + " already exists");
        }
        GroundEquipment equipment = new GroundEquipment();
        equipment.setType(request.getType());
        equipment.setRegistrationNumber(request.getRegistrationNumber());
        equipment.setCurrentLocation(request.getCurrentLocation());
        equipment.setStatus(EquipmentStatus.Available);

        GroundEquipment saved = equipmentRepository.save(equipment);
        log.info("Successfully registered equipment. ID: {}, Reg: {}", saved.getEquipmentId(), saved.getRegistrationNumber());
        return toEquipmentResponse(saved);
    }

    public List<GroundEquipmentResponse> getAllEquipment() {
        log.debug("Fetching all ground equipment");
        return equipmentRepository.findAll().stream().map(this::toEquipmentResponse).toList();
    }

    public GroundEquipmentResponse getEquipmentById(String id) {
        log.debug("Fetching equipment by ID: {}", id);
        return toEquipmentResponse(findEquipmentById(id));
    }

    public List<GroundEquipmentResponse> getAvailableEquipment() {
        log.debug("Fetching all available ground equipment");
        return equipmentRepository.findByStatus(EquipmentStatus.Available)
                .stream().map(this::toEquipmentResponse).toList();
    }

    @Transactional
    public GroundEquipmentResponse updateEquipmentStatus(String id, EquipmentStatusRequest request) {
        log.info("Updating status of equipment ID: {} to {}", id, request.getStatus());
        GroundEquipment equipment = findEquipmentById(id);
        EquipmentStatus oldStatus = equipment.getStatus();
        equipment.setStatus(request.getStatus());

        GroundEquipment updated = equipmentRepository.save(equipment);
        log.info("Equipment ID: {} status updated from {} to {}", id, oldStatus, updated.getStatus());
        return toEquipmentResponse(updated);
    }

    // ─── Allocations ────────────────────────────────────────────────────────────

    @Transactional
    public EquipmentAllocationResponse allocate(EquipmentAllocationRequest request, String userId) {
        log.info("Processing allocation request for Equipment ID: {} to Flight ID: {} by User: {}",
                request.getEquipmentId(), request.getFlightId(), userId);

        GroundEquipment equipment = findEquipmentById(request.getEquipmentId());

        // Guard: equipment must be Available
        if (equipment.getStatus() != EquipmentStatus.Available) {
            log.warn("Allocation rejected. Equipment {} is in state: {}", equipment.getRegistrationNumber(), equipment.getStatus());
            throw new ConflictException("Equipment " + equipment.getRegistrationNumber()
                    + " is not available (current status: " + equipment.getStatus() + ")");
        }

        // Guard: prevent double-allocation (atomic check via DB constraint)
        boolean alreadyAllocated = allocationRepository
                .existsByEquipmentAndStatus(equipment, AllocationStatus.Allocated);
        if (alreadyAllocated) {
            log.error("Conflict detected. Equipment {} is already marked as allocated in DB.", equipment.getRegistrationNumber());
            throw new ConflictException("Equipment " + equipment.getRegistrationNumber()
                    + " is already allocated to a flight");
        }

        Flight flight = flightService.findById(request.getFlightId());
        User allocatedBy = userRepository.findByEmail(userId)
                .orElseThrow(() -> {
                    log.error("Allocation failed. User ID: {} not found", userId);
                    return new ResourceNotFoundException("User not found");
                });

        EquipmentAllocation allocation = new EquipmentAllocation();
        allocation.setEquipment(equipment);
        allocation.setFlight(flight);
        allocation.setAllocatedBy(allocatedBy);
        allocation.setAllocationTime(request.getAllocationTime());
        allocation.setReleaseTime(request.getReleaseTime());
        allocation.setStatus(AllocationStatus.Allocated);

        // Mark equipment as allocated
        equipment.setStatus(EquipmentStatus.Allocated);
        equipmentRepository.save(equipment);

        EquipmentAllocation savedAllocation = allocationRepository.save(allocation);
        log.info("Successfully allocated Equipment {} to Flight {}. Allocation ID: {}",
                equipment.getRegistrationNumber(), flight.getFlightNumber(), savedAllocation.getAllocationId());

        return toAllocationResponse(savedAllocation);
    }

    public List<EquipmentAllocationResponse> getAllocationsByFlight(String flightId) {
        log.debug("Fetching allocations for flight ID: {}", flightId);
        return allocationRepository.findByFlight_FlightId(flightId)
                .stream().map(this::toAllocationResponse).toList();
    }

    public List<EquipmentAllocationResponse> getAllActiveAllocations() {
        log.debug("Fetching all active allocations");
        return allocationRepository.findByStatusOrderByAllocationTimeDesc(AllocationStatus.Allocated)
                .stream().map(this::toAllocationResponse).toList();
    }

    @Transactional
    public EquipmentAllocationResponse release(String allocationId) {
        log.info("Processing release for Allocation ID: {}", allocationId);
        EquipmentAllocation allocation = findAllocationById(allocationId);

        if (allocation.getStatus() == AllocationStatus.Released) {
            log.warn("Release aborted. Allocation ID: {} is already released", allocationId);
            throw new BadRequestException("Equipment is already released");
        }
        allocation.setStatus(AllocationStatus.Released);

        // Free the equipment
        GroundEquipment equipment = allocation.getEquipment();
        equipment.setStatus(EquipmentStatus.Available);
        equipmentRepository.save(equipment);

        EquipmentAllocation savedAllocation = allocationRepository.save(allocation);
        log.info("Successfully released Equipment {} from Allocation ID: {}", equipment.getRegistrationNumber(), allocationId);

        return toAllocationResponse(savedAllocation);
    }

    // ─── Maintenance ────────────────────────────────────────────────────────────

    @Transactional
    public EquipmentMaintenanceResponse reportMaintenance(EquipmentMaintenanceRequest request,
                                                          String reportedByUserId) {
        log.info("Reporting maintenance fault for Equipment ID: {} by User ID: {}", request.getEquipmentId(), reportedByUserId);

        GroundEquipment equipment = findEquipmentById(request.getEquipmentId());
        User reportedBy = userRepository.findByEmail(reportedByUserId)
                .orElseThrow(() -> {
                    log.error("Maintenance reporting failed. User ID: {} not found", reportedByUserId);
                    return new ResourceNotFoundException("User not found");
                });

        // Put equipment into Maintenance
        equipment.setStatus(EquipmentStatus.Maintenance);
        equipmentRepository.save(equipment);

        EquipmentMaintenance maintenance = new EquipmentMaintenance();
        maintenance.setEquipment(equipment);
        maintenance.setIssue(request.getIssue());
        maintenance.setReportedBy(reportedBy);
        maintenance.setExpectedReturnDate(request.getExpectedReturnDate());
        maintenance.setStatus(MaintenanceStatus.Reported);

        // Notify supervisors
        log.debug("Dispatching maintenance notifications to GroundSupervisors");
        userRepository.findByRole(Role.GroundSupervisor).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "Equipment fault reported: " + equipment.getRegistrationNumber()
                                + " — " + request.getIssue(),
                        NotificationCategory.Equipment));

        EquipmentMaintenance savedMaintenance = maintenanceRepository.save(maintenance);
        log.info("Maintenance logged successfully. Record ID: {} for Equipment: {}",
                savedMaintenance.getMaintenanceId(), equipment.getRegistrationNumber());

        return toMaintenanceResponse(savedMaintenance);
    }

    public List<EquipmentMaintenanceResponse> getAllMaintenance() {
        log.debug("Fetching all maintenance history records");
        return maintenanceRepository.findAllByOrderByReportedDateDesc()
                .stream().map(this::toMaintenanceResponse).toList();
    }

    @Transactional
    public EquipmentMaintenanceResponse resolveMaintenence(String maintenanceId) {
        log.info("Resolving maintenance for Record ID: {}", maintenanceId);
        EquipmentMaintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> {
                    log.error("Resolution failed. Maintenance record ID: {} not found", maintenanceId);
                    return new ResourceNotFoundException("Maintenance record not found: " + maintenanceId);
                });

        maintenance.setStatus(MaintenanceStatus.ReturnedToService);

        // Return equipment to Available
        GroundEquipment equipment = maintenance.getEquipment();
        equipment.setStatus(EquipmentStatus.Available);
        equipmentRepository.save(equipment);

        EquipmentMaintenance savedMaintenance = maintenanceRepository.save(maintenance);
        log.info("Maintenance Record ID: {} resolved. Equipment {} is now Available.",
                maintenanceId, equipment.getRegistrationNumber());

        return toMaintenanceResponse(savedMaintenance);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private GroundEquipment findEquipmentById(String id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("GroundEquipment not found with ID: {}", id);
                    return new ResourceNotFoundException("Equipment not found: " + id);
                });
    }

    private EquipmentAllocation findAllocationById(String id) {
        return allocationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("EquipmentAllocation not found with ID: {}", id);
                    return new ResourceNotFoundException("Allocation not found: " + id);
                });
    }

    private GroundEquipmentResponse toEquipmentResponse(GroundEquipment e) {
        return GroundEquipmentResponse.builder()
                .equipmentId(e.getEquipmentId())
                .type(e.getType())
                .registrationNumber(e.getRegistrationNumber())
                .currentLocation(e.getCurrentLocation())
                .status(e.getStatus())
                .build();
    }

    private EquipmentAllocationResponse toAllocationResponse(EquipmentAllocation a) {
        return EquipmentAllocationResponse.builder()
                .allocationId(a.getAllocationId())
                .equipmentId(a.getEquipment().getEquipmentId())
                .registrationNumber(a.getEquipment().getRegistrationNumber())
                .equipmentType(a.getEquipment().getType())
                .flightId(a.getFlight().getFlightId())
                .flightNumber(a.getFlight().getFlightNumber())
                .allocatedById(a.getAllocatedBy().getUserId())
                .allocatedByName(a.getAllocatedBy().getName())
                .allocationTime(a.getAllocationTime())
                .releaseTime(a.getReleaseTime())
                .status(a.getStatus())
                .build();
    }

    private EquipmentMaintenanceResponse toMaintenanceResponse(EquipmentMaintenance m) {
        return EquipmentMaintenanceResponse.builder()
                .maintenanceId(m.getMaintenanceId())
                .equipmentId(m.getEquipment().getEquipmentId())
                .registrationNumber(m.getEquipment().getRegistrationNumber())
                .issue(m.getIssue())
                .reportedById(m.getReportedBy().getUserId())
                .reportedByName(m.getReportedBy().getName())
                .reportedDate(m.getReportedDate())
                .expectedReturnDate(m.getExpectedReturnDate())
                .status(m.getStatus())
                .build();
    }
}
