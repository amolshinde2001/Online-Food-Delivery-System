package com.ofds.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ofds.order.configuration.FeignClientConfig;

import java.util.Map;

@FeignClient(name = "notification-service", configuration = FeignClientConfig.class)
public interface NotificationClient {
    @PostMapping("/notify/email")
    Map<String, Object> sendEmail(@RequestBody Map<String, Object> body);
}
