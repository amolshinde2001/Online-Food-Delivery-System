package com.ofds.payment.web;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

	@PostMapping("/pay")
	public Map<String, Object> pay(@RequestBody Map<String, Object> body) {
	    if (!body.containsKey("amount")) {
	        return Map.of("status", "FAILURE", "message", "Amount is required");
	    }

	    String mode = String.valueOf(body.getOrDefault("mode", "CARD"));
	    if (!List.of("CARD", "UPI", "NETBANKING").contains(mode)) {
	        return Map.of("status", "FAILURE", "message", "Unsupported payment mode: " + mode);
	    }

	    return Map.of(
	        "status", "SUCCESS",
	        "transactionId", "TXN-" + System.currentTimeMillis(),
	        "amount", body.get("amount"),
	        "mode", mode
	    );
	}

}
