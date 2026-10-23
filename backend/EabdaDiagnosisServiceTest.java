package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.model.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EabdaDiagnosisServiceTest {

    private final EabdaDiagnosisService service =
            new EabdaDiagnosisService();

    private List<WorkflowEvent> createEvents(
            double baselineWait,
            double abnormalWait) {

        List<WorkflowEvent> events = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusDays(9);

        // Seven baseline days
        for (int day = 0; day < 7; day++) {
            events.add(createEvent(
                    startDate.plusDays(day),
                    baselineWait
            ));
        }

        // Three subsequent days
        for (int day = 7; day < 10; day++) {
            events.add(createEvent(
                    startDate.plusDays(day),
                    abnormalWait
            ));
        }

        return events;
    }

    private WorkflowEvent createEvent(
            LocalDate date,
            double waitingMinutes) {

        WorkflowEvent event = new WorkflowEvent();

        LocalDateTime queueTime = date.atTime(9, 0);

        event.setStage("OP");
        event.setQueue_entry_time(queueTime);
        event.setService_start_time(
                queueTime.plusSeconds(
                        Math.round(waitingMinutes * 60)
                )
        );

        return event;
    }

    @Test
    void stableWaitingTimesShouldBeNormal() {

        List<WorkflowEvent> events =
                createEvents(10.0, 10.0);

        EabdaDiagnosisService.Diagnosis result =
                service.diagnose("OP", events);

        assertEquals("NORMAL", result.state());

        System.out.println(
                "NORMAL TEST PASSED: " + result
        );
    }

    @Test
    void sustainedAbnormalWaitingTimesShouldBePersistent() {

        List<WorkflowEvent> events =
                createEvents(10.0, 30.0);

        EabdaDiagnosisService.Diagnosis result =
                service.diagnose("OP", events);

        assertEquals("PERSISTENT", result.state());
        assertEquals(3, result.persistence());

        System.out.println(
                "PERSISTENT TEST PASSED: " + result
        );
    }
}