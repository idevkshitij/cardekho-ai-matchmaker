package com.cardekho.aimatchmaker.repository;

import com.cardekho.aimatchmaker.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    
    // Find cars within a certain budget range
    List<Car> findByPriceLakhsBetween(Double minPrice, Double maxPrice);
    
    // Find cars below a certain budget
    List<Car> findByPriceLakhsLessThanEqual(Double maxPrice);
    
    // Find cars by fuel type
    List<Car> findByFuelTypeIgnoreCase(String fuelType);
    
    // Find cars by transmission
    List<Car> findByTransmissionIgnoreCase(String transmission);
}
