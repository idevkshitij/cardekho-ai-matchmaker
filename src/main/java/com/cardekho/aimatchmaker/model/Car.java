package com.cardekho.aimatchmaker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cars")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    private String variant;

    @Column(name = "price_lakhs")
    private Double priceLakhs; // Numerical price in Lakhs for filtering (e.g. 12.5)

    @Column(name = "price_display")
    private String priceDisplay; // Human readable display price (e.g. "₹ 12.5 Lakhs")

    @Column(name = "fuel_type")
    private String fuelType; // Petrol, Diesel, CNG, Electric

    private String transmission; // Manual, Automatic

    private Double mileage; // km/l or km/full charge

    @Column(name = "safety_rating")
    private Integer safetyRating; // GNCAP Safety Rating (1-5 stars)

    @Column(name = "seating_capacity")
    private Integer seatingCapacity; // 5, 7, etc.

    @Column(name = "body_type")
    private String bodyType; // SUV, Sedan, Hatchback, MUV

    @Column(length = 1000)
    private String specifications; // Engine capacity, horsepower, boot space, etc.

    @Lob
    @Column(name = "user_reviews", columnDefinition = "TEXT")
    private String userReviews; // Aggregated reviews or comments for AI extraction of pros/cons
}
