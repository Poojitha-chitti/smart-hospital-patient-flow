package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Visit;
import com.hospital.smart_hospital.model.WorkflowEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowEventRepository extends JpaRepository<WorkflowEvent, Integer> {

    @Query("""
        SELECT
            w.stage AS stage,
            AVG(TIMESTAMPDIFF(MINUTE, w.queue_entry_time, w.service_start_time)) AS averageWaitingTime,
            AVG(TIMESTAMPDIFF(MINUTE, w.service_start_time, w.service_end_time)) AS averageServiceTime
        FROM WorkflowEvent w
        GROUP BY w.stage
        ORDER BY averageWaitingTime DESC
        """)
    List<BottleneckProjection> findBottleneckAnalysis();

    @Query("""
        SELECT w.stage,
               AVG(TIMESTAMPDIFF(MINUTE,
                   w.queue_entry_time,
                   w.service_start_time))
        FROM WorkflowEvent w
        GROUP BY w.stage
        ORDER BY AVG(TIMESTAMPDIFF(MINUTE,
                   w.queue_entry_time,
                   w.service_start_time)) DESC
        """)
    List<Object[]> findStageWaitingPatterns();

    @Query("""
        SELECT HOUR(v.arrival_time), COUNT(v.visit_id)
        FROM Visit v
        GROUP BY HOUR(v.arrival_time)
        ORDER BY COUNT(v.visit_id) DESC
        """)
    List<Object[]> findArrivalHourPatterns();

    @Query("""
        SELECT COUNT(w.event_id)
        FROM WorkflowEvent w
        WHERE TIMESTAMPDIFF(MINUTE,
              w.queue_entry_time,
              w.service_start_time) >= 20
        """)
    Long countHighWaitingEvents();

    @Query("""
        SELECT w
        FROM WorkflowEvent w
        ORDER BY w.event_id ASC
        """)
    List<WorkflowEvent> findFirstWorkflowEvents(Pageable pageable);

    @Query("SELECT COUNT(w) FROM WorkflowEvent w")
    long countTotalWorkflowEvents();

    @Query("""
        SELECT COUNT(w)
        FROM WorkflowEvent w
        WHERE w.stage = :stage
        """)
    long countWorkflowEventsByStage(@Param("stage") String stage);
    @Query("""
    SELECT w
    FROM WorkflowEvent w
    WHERE w.visit_id = :visitId
      AND w.stage = :stage
    """)
WorkflowEvent findByVisitIdAndStage(
        @Param("visitId") Integer visitId,
        @Param("stage") String stage
);
}