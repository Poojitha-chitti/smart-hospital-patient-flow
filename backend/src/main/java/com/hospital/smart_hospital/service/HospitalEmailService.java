package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HospitalEmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // Keeps track of stages for which the current persistent alert
    // has already been sent.
    private final Set<String> alertedStages = new HashSet<>();

    public HospitalEmailService(
            JavaMailSender mailSender,
            UserRepository userRepository) {

        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    public void sendBottleneckAlert(
            String stage,
            double waitingTime,
            double baseline,
            double pressure,
            String evidence) {

        if (stage == null || stage.isBlank()) {
            return;
        }

        String normalizedStage = normalizeStage(stage);

        // Prevent the same persistent bottleneck from sending
        // an email repeatedly whenever the dashboard is refreshed.
        if (alertedStages.contains(normalizedStage)) {
            return;
        }

        List<User> recipients = getRecipients(normalizedStage);

        if (recipients.isEmpty()) {
            return;
        }

        String subject =
                "Smart Hospital - Bottleneck Alert: " + normalizedStage;

        String body =
                "Smart Hospital System - Bottleneck Alert\n\n" +
                "A persistent bottleneck has been detected.\n\n" +
                "Stage: " + normalizedStage + "\n" +
                String.format("Current Waiting Time: %.2f minutes\n", waitingTime) +
                String.format("Baseline Waiting Time: %.2f minutes\n", baseline) +
                String.format("Pressure: %.2fx\n", pressure) +
                "\nEvidence:\n" +
                evidence +
                "\n\n" +
                "Please review the patient flow and available resources for this stage.\n\n" +
                "This is an automated notification from Smart Hospital System.";

        boolean sentToAtLeastOne = false;

        for (User user : recipients) {

            if (user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }

            try {
                sendEmail(user.getEmail(), subject, body);
                sentToAtLeastOne = true;

            } catch (Exception e) {
                System.err.println(
                        "Could not send bottleneck alert to "
                                + user.getEmail()
                                + ": "
                                + e.getMessage()
                );
            }
        }

        if (sentToAtLeastOne) {
            alertedStages.add(normalizedStage);
        }
    }

    public void resetAlert(String stage) {

        if (stage == null || stage.isBlank()) {
            return;
        }

        alertedStages.remove(normalizeStage(stage));
    }

    private List<User> getRecipients(String stage) {

        List<User> admins =
                userRepository.findUsersByRole("ADMIN");

        String workArea = mapStageToWorkArea(stage);

        List<User> staff =
                userRepository.findUsersByRoleAndWorkArea(
                        "STAFF",
                        workArea
                );

        // Combine ADMIN + relevant STAFF.
        Set<Integer> userIds = new HashSet<>();
        List<User> recipients = new java.util.ArrayList<>();

        for (User user : admins) {
            if (user.getEmail() != null &&
                    !user.getEmail().isBlank() &&
                    userIds.add(user.getUser_id())) {

                recipients.add(user);
            }
        }

        for (User user : staff) {
            if (user.getEmail() != null &&
                    !user.getEmail().isBlank() &&
                    userIds.add(user.getUser_id())) {

                recipients.add(user);
            }
        }

        return recipients;
    }

    private String mapStageToWorkArea(String stage) {

    return switch (stage.toUpperCase()) {

        case "OP", "CONSULTATION", "OP CONSULTATION" -> "Op";

        case "PHARMACY" ->
                "Pharmacy";

        case "DIAGNOSTICS" ->
                "Diagnostics";

        default ->
                stage;
    };
}

    private String normalizeStage(String stage) {

        String value = stage.trim().toUpperCase();

        return switch (value) {

            case "OP", "CONSULTATION" ->
                    "OP CONSULTATION";

            case "PHARMACY" ->
                    "PHARMACY";

            case "DIAGNOSTICS" ->
                    "DIAGNOSTICS";

            case "TRIAGE" ->
                    "TRIAGE";

            case "EMERGENCY_TREATMENT" ->
                    "EMERGENCY TREATMENT";

            default ->
                    value;
        };
    }

    private void sendEmail(
            String recipient,
            String subject,
            String body)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(
                senderEmail,
                "Smart Hospital System"
        );

        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(body);

        mailSender.send(message);
    }
}