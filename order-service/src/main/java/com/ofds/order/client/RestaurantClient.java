package com.ofds.order.client;

import com.ofds.order.configuration.FeignClientConfig;
import com.ofds.order.dto.MenuItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "restaurant-service", configuration = FeignClientConfig.class) // service name registered in discovery (Eureka)
public interface RestaurantClient {

    @GetMapping("/restaurants/{id}/menu")
    List<MenuItemDto> getMenu(@PathVariable("id") Long restaurantId);
}
