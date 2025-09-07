package com.ofds.restaurant.repo;

import com.ofds.restaurant.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    //Existing
    List<MenuItem> findByRestaurantId(Long restaurantId);

    //New methods for duplicate check
    boolean existsByRestaurantIdAndName(Long restaurantId, String name);
    
}
