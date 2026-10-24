package com.hospital.smart_hospital.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private final JavaMailSender mailSender;

    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String email) {

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        otpStore.put(
                email.toLowerCase(),
                new OtpData(
                        otp,
                        LocalDateTime.now().plusMinutes(5)
                )
        );

        try {

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            false,
                            "UTF-8"
                    );

            // Force the exact From header and display name
            message.setFrom(
                    new InternetAddress(
                            "poojithachitti34@gmail.com",
                            "Smart Hospital System"
                    )
            );

            helper.setTo(email);

            helper.setSubject(
                    "Smart Hospital - Password Reset OTP"
            );

            helper.setText(
                    "Your Smart Hospital password reset OTP is: "
                            + otp
                            + "\n\n"
                            + "This OTP is valid for 5 minutes."
                            + "\n\n"
                            + "If you did not request a password reset, "
                            + "please ignore this email."
                            + "\n\n"
                            + "Regards,\n"
                            + "Smart Hospital System"
            );

            mailSender.send(message);

        } catch (MessagingException | UnsupportedEncodingException e) {

            otpStore.remove(email.toLowerCase());

            throw new RuntimeException(
                    "Unable to send password reset OTP",
                    e
            );
        }
    }

    public boolean verifyOtp(String email, String otp) {

        String normalizedEmail = email.toLowerCase();

        OtpData otpData = otpStore.get(normalizedEmail);

        if (otpData == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(otpData.expiryTime())) {

            otpStore.remove(normalizedEmail);

            return false;
        }

        if (!otpData.otp().equals(otp)) {
            return false;
        }

        otpStore.remove(normalizedEmail);

        return true;
    }

    private record OtpData(
            String otp,
            LocalDateTime expiryTime
    ) {
    }
}