package com.pet.walkthroughserver.modules._shared.infra.email;

public interface EmailService {
    void sendMail(String to, String subject, String... content);
}
