package com.ofds.order.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofds.order.client.NotificationClient;
import com.ofds.order.client.PaymentClient;
import com.ofds.order.client.RestaurantClient;
import com.ofds.order.dto.MenuItemDto;
import com.ofds.order.dto.OrderItemDto;
import com.ofds.order.dto.OrderResponseDto;
import com.ofds.order.dto.PlaceOrderRequestDto;
import com.ofds.order.model.OrderEntity;
import com.ofds.order.repo.OrderRepository;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
	
    private final OrderRepository repo;
    private final PaymentClient payment;
    private final RestaurantClient restaurantClient;
    private final NotificationClient notify;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderController(OrderRepository repo, PaymentClient payment, NotificationClient notify, RestaurantClient restaurantClient) {
        this.repo = repo;
        this.payment = payment;
		this.restaurantClient = restaurantClient;
        this.notify = notify;
    }

    @GetMapping("/allorders")
    public List<OrderEntity> all() { return repo.findAll(); }
    
    @GetMapping("/{id}")
    public OrderEntity getOrder(@PathVariable("id") Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @PostMapping("/placeorder")
    public OrderResponseDto place(@Valid @RequestBody PlaceOrderRequestDto body) throws Exception {
        if (body.getRestaurantId() == null || body.getItems() == null || body.getItems().isEmpty()) {
            throw new IllegalArgumentException("Missing required order fields.");
        }

        Long restaurantId = body.getRestaurantId();
        List<OrderItemDto> requestedItems = body.getItems();

        //Fetch menu from restaurant service
        List<MenuItemDto> menu;
        try {
            menu = restaurantClient.getMenu(restaurantId);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        if (menu.isEmpty()) {
            throw new IllegalArgumentException("No menu found for restaurant " + restaurantId);
        }

        //Validate & calculate total
        double total = 0.0;
        StringBuilder invalidItems = new StringBuilder();
        for (OrderItemDto reqItem : requestedItems) {
            String itemName = reqItem.getName();
            int qty = reqItem.getQty();

            MenuItemDto menuItem = menu.stream()
                    .filter(m -> m.getName().equalsIgnoreCase(itemName))
                    .findFirst()
                    .orElse(null);

            if (menuItem == null) {
                invalidItems.append(itemName).append(", ");
                continue;
            }
            total += menuItem.getPrice() * qty;
        }

        if (invalidItems.length() > 0) {
            throw new IllegalArgumentException("These items are not availabe in restaurant: " + invalidItems);
        }

        //Create order
        OrderEntity o = new OrderEntity();
        o.setRestaurantId(restaurantId);
        o.setItems(mapper.writeValueAsString(requestedItems));
        o.setTotalAmount(total);
        o.setStatus("CREATED");
        o.setEmail(body.getEmail());
        repo.save(o);

        //Call Payment Service
        Map<String, Object> payResp;
        try {
            payResp = payment.pay(Map.of(
                "amount", o.getTotalAmount(),
                "mode", body.getMode()
            ));
        } catch (Exception e) {
            o.setStatus("PAYMENT_FAILED");
            repo.save(o);
            throw new RuntimeException("Payment service error: " + e.getMessage());
        }

        //Payment handling
        if ("SUCCESS".equalsIgnoreCase(String.valueOf(payResp.get("status")))) {
            o.setPaymentTxnId(String.valueOf(payResp.get("transactionId")));
            o.setStatus("PAID");
            repo.save(o);

            try {
                notify.sendEmail(Map.of(
                    "to", body.getEmail(),
                    "subject", "Order Confirmed",
                    "body", "Your order #" + o.getId() + " is confirmed. Txn: " + o.getPaymentTxnId()
                ));
            } catch (Exception e) {
                System.err.println("Notification failed: " + e.getMessage());
            }
        } else {
            o.setStatus("PAYMENT_FAILED");
            repo.save(o);
        }

        return new OrderResponseDto(o.getId(), o.getStatus(), o.getPaymentTxnId(), o.getTotalAmount());
    }
    
    @PutMapping("/cancel/{id}")
    public OrderResponseDto cancelOrder(@PathVariable("id") Long id) throws Exception {
        OrderEntity order = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("Cannot cancel an already paid order.");
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("Order is already cancelled.");
        }

        order.setStatus("CANCELLED");
        repo.save(order);

        //notify customer
        try {
            notify.sendEmail(Map.of(
                    "to", order.getEmail(), // you can store customer email in order
                    "subject", "Order Cancelled",
                    "body", "Your order #" + order.getId() + " has been cancelled."
            ));
        } catch (Exception e) {
            System.err.println("Notification failed: " + e.getMessage());
        }

        return new OrderResponseDto(order.getId(), order.getStatus(), order.getPaymentTxnId(), order.getTotalAmount());
    }
    
}
