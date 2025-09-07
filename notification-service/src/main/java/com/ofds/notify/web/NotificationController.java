package com.ofds.notify.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notify")
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping("/email")
    public Map<String, Object> email(@RequestBody Map<String, Object> body) {
        // Log-only for demo; replace with JavaMailSender for real email
        log.info("Sending email: {}", body);
        return Map.of("status", "QUEUED");
    }
}
