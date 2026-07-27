package com.project.flightOps.repository;

import com.project.flightOps.entity.TurnaroundMilestone;
import com.project.flightOps.entity.TurnaroundPlan;
import com.project.flightOps.enums.MilestoneStatus;
import com.project.flightOps.enums.TurnaroundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnaroundMilestoneRepository extends JpaRepository<TurnaroundMilestone, String> {

    List<TurnaroundMilestone> findByTurnaroundPlanOrderByPlannedTimeAsc(TurnaroundPlan plan);

    List<TurnaroundMilestone> findByTurnaroundPlan_PlanIdOrderByPlannedTimeAsc(String planId);

    // Milestones that are still Pending but past their planned time = overdue
    @Query("SELECT m FROM TurnaroundMilestone m WHERE m.status = com.project.flightOps.enums.MilestoneStatus.Pending AND m.plannedTime < :now")
    List<TurnaroundMilestone> findOverdueMilestones(LocalDateTime now);

    List<TurnaroundMilestone> findByStatusOrderByPlannedTimeAsc(MilestoneStatus status);
    // Base query method accepting a explicit date
    List<TurnaroundMilestone> findByStatusAndTurnaroundPlan_StatusInAndTurnaroundPlan_CreatedAt(
            MilestoneStatus milestoneStatus,
            List<TurnaroundStatus> planStatuses,
            LocalDate createdAt
    );

    // Default helper method using today's date automatically
    default List<TurnaroundMilestone> findByStatusAndPlanStatusesForToday(
            MilestoneStatus milestoneStatus,
            List<TurnaroundStatus> planStatuses
    ) {
        return findByStatusAndTurnaroundPlan_StatusInAndTurnaroundPlan_CreatedAt(
                milestoneStatus,
                planStatuses,
                LocalDate.now()
        );
    }
}
