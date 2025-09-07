package com.ofds.restaurant.web;

import com.ofds.restaurant.dto.ApiResponse;
import com.ofds.restaurant.model.MenuItem;
import com.ofds.restaurant.model.Restaurant;
import com.ofds.restaurant.repo.MenuItemRepository;
import com.ofds.restaurant.repo.RestaurantRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {
    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;

    public RestaurantController(RestaurantRepository restaurants, MenuItemRepository menuItems) {
        this.restaurants = restaurants;
        this.menuItems = menuItems;
    }

    @GetMapping
    public List<Restaurant> all() { return restaurants.findAll(); }

    @PostMapping
    public ApiResponse create(@RequestBody Restaurant r) {
        return restaurants.findByNameAndAddress(r.getName(), r.getAddress())
                .map(existing -> new ApiResponse(false, "Restaurant with same name and address already exists!"))
                .orElseGet(() -> {
                    Restaurant saved = restaurants.save(r);
                    return new ApiResponse(true, "Restaurant created successfully!", saved);
                });
    }

    @GetMapping("/{id}/menu")
    public List<MenuItem> menu(@PathVariable Long id) { return menuItems.findByRestaurantId(id); }

    @PostMapping("/{id}/menu")
    public ApiResponse addItems(@PathVariable Long id, @RequestBody List<MenuItem> items) {
        if (!restaurants.existsById(id)) {
            return new ApiResponse(false, "Restaurant with id " + id + " not found!");
        }

        List<MenuItem> toSave = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        for (MenuItem item : items) {
            item.setRestaurantId(id);

            // Duplicate in request payload itself
            if (!seenNames.add(item.getName())) {
                skipped.add(item.getName());
                continue;
            }

            // Duplicate in DB
            if (menuItems.existsByRestaurantIdAndName(id, item.getName())) {
                skipped.add(item.getName());
            } else {
                toSave.add(item);
            }
        }

        List<MenuItem> savedItems = toSave.isEmpty() ? List.of() : menuItems.saveAll(toSave);

        if (!skipped.isEmpty()) {
            return new ApiResponse(
                    true,
                    "Some items skipped because they already exist: " + String.join(", ", skipped),
                    savedItems
            );
        }

        return new ApiResponse(true, "All items saved successfully!", savedItems);
    }


}
