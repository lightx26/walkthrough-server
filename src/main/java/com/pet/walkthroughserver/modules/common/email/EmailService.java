package com.pet.walkthroughserver.modules.common.email;

public interface EmailService {
    void sendMail(String to, String subject, String... content);
}
