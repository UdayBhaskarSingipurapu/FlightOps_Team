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
import org.springframework.stereotype.Service;

import java.util.List;

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
        if (equipmentRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new ConflictException("Equipment with registration "
                    + request.getRegistrationNumber() + " already exists");
        }
        GroundEquipment equipment = new GroundEquipment();
        equipment.setType(request.getType());
        equipment.setRegistrationNumber(request.getRegistrationNumber());
        equipment.setCurrentLocation(request.getCurrentLocation());
        equipment.setStatus(EquipmentStatus.Available);
        return toEquipmentResponse(equipmentRepository.save(equipment));
    }

    public List<GroundEquipmentResponse> getAllEquipment() {
        return equipmentRepository.findAll().stream().map(this::toEquipmentResponse).toList();
    }

    public GroundEquipmentResponse getEquipmentById(String id) {
        return toEquipmentResponse(findEquipmentById(id));
    }

    public List<GroundEquipmentResponse> getAvailableEquipment() {
        return equipmentRepository.findByStatus(EquipmentStatus.Available)
                .stream().map(this::toEquipmentResponse).toList();
    }

    @Transactional
    public GroundEquipmentResponse updateEquipmentStatus(String id, EquipmentStatusRequest request) {
        GroundEquipment equipment = findEquipmentById(id);
        equipment.setStatus(request.getStatus());
        return toEquipmentResponse(equipmentRepository.save(equipment));
    }

    // ─── Allocations ────────────────────────────────────────────────────────────

    @Transactional
    public EquipmentAllocationResponse allocate(EquipmentAllocationRequest request, String userId) {
        GroundEquipment equipment = findEquipmentById(request.getEquipmentId());

        // Guard: equipment must be Available
        if (equipment.getStatus() != EquipmentStatus.Available) {
            throw new ConflictException("Equipment " + equipment.getRegistrationNumber()
                    + " is not available (current status: " + equipment.getStatus() + ")");
        }

        // Guard: prevent double-allocation (atomic check via DB constraint)
        boolean alreadyAllocated = allocationRepository
                .existsByEquipmentAndStatus(equipment, AllocationStatus.Allocated);
        if (alreadyAllocated) {
            throw new ConflictException("Equipment " + equipment.getRegistrationNumber()
                    + " is already allocated to a flight");
        }

        Flight flight = flightService.findById(request.getFlightId());
        User allocatedBy = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

        return toAllocationResponse(allocationRepository.save(allocation));
    }

    public List<EquipmentAllocationResponse> getAllocationsByFlight(String flightId) {
        return allocationRepository.findByFlight_FlightId(flightId)
                .stream().map(this::toAllocationResponse).toList();
    }

    public List<EquipmentAllocationResponse> getAllActiveAllocations() {
        return allocationRepository.findByStatusOrderByAllocationTimeDesc(AllocationStatus.Allocated)
                .stream().map(this::toAllocationResponse).toList();
    }

    @Transactional
    public EquipmentAllocationResponse release(String allocationId) {
        EquipmentAllocation allocation = findAllocationById(allocationId);
        if (allocation.getStatus() == AllocationStatus.Released) {
            throw new BadRequestException("Equipment is already released");
        }
        allocation.setStatus(AllocationStatus.Released);

        // Free the equipment
        GroundEquipment equipment = allocation.getEquipment();
        equipment.setStatus(EquipmentStatus.Available);
        equipmentRepository.save(equipment);

        return toAllocationResponse(allocationRepository.save(allocation));
    }

    // ─── Maintenance ────────────────────────────────────────────────────────────

    @Transactional
    public EquipmentMaintenanceResponse reportMaintenance(EquipmentMaintenanceRequest request,
            String reportedByUserId) {
        GroundEquipment equipment = findEquipmentById(request.getEquipmentId());
        User reportedBy = userRepository.findById(reportedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
        userRepository.findByRole(Role.GroundSupervisor).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "Equipment fault reported: " + equipment.getRegistrationNumber()
                                + " — " + request.getIssue(),
                        NotificationCategory.Equipment));

        return toMaintenanceResponse(maintenanceRepository.save(maintenance));
    }

    public List<EquipmentMaintenanceResponse> getAllMaintenance() {
        return maintenanceRepository.findAllByOrderByReportedDateDesc()
                .stream().map(this::toMaintenanceResponse).toList();
    }

    @Transactional
    public EquipmentMaintenanceResponse resolveMaintenence(String maintenanceId) {
        EquipmentMaintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Maintenance record not found: " + maintenanceId));

        maintenance.setStatus(MaintenanceStatus.ReturnedToService);

        // Return equipment to Available
        GroundEquipment equipment = maintenance.getEquipment();
        equipment.setStatus(EquipmentStatus.Available);
        equipmentRepository.save(equipment);

        return toMaintenanceResponse(maintenanceRepository.save(maintenance));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private GroundEquipment findEquipmentById(String id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found: " + id));
    }

    private EquipmentAllocation findAllocationById(String id) {
        return allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));
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
