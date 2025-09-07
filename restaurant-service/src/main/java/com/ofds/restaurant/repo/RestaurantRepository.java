package com.ofds.restaurant.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ofds.restaurant.model.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByNameAndAddress(String name, String address);
}
