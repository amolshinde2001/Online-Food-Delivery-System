package com.ofds.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ofds.order.configuration.FeignClientConfig;

import java.util.Map;

@FeignClient(name = "payment-service", configuration = FeignClientConfig.class)
public interface PaymentClient {
    @PostMapping("/payments/pay")
    Map<String, Object> pay(@RequestBody Map<String, Object> body);
}
