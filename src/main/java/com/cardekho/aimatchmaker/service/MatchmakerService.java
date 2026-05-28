package com.cardekho.aimatchmaker.service;

import com.cardekho.aimatchmaker.model.Car;
import com.cardekho.aimatchmaker.repository.CarRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchmakerService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Conducts the matchmaking analysis based on the buyer's profile.
     * Returns a JSON string representing the recommended shortlist.
     */
    public String matchmake(Map<String, Object> buyerProfile, String tempApiKey) throws Exception {
        // Extract buyer details using Java's Optional with safe defaults
        Double maxPrice = Optional.ofNullable(buyerProfile.get("budget"))
                .map(this::getDoubleValue)
                .orElse(15.0);
        
        String fuelPreference = Optional.ofNullable(buyerProfile.get("fuelType"))
                .map(Object::toString)
                .filter(s -> !s.isEmpty())
                .orElse("Either");
                
        String transmissionPreference = Optional.ofNullable(buyerProfile.get("transmission"))
                .map(Object::toString)
                .filter(s -> !s.isEmpty())
                .orElse("Either");
                
        Integer seatingNeeded = Optional.ofNullable(buyerProfile.get("seatingCapacity"))
                .map(this::getIntValue)
                .orElse(5);
                
        String dailyUsage = Optional.ofNullable(buyerProfile.get("dailyUsage"))
                .map(Object::toString)
                .filter(s -> !s.isEmpty())
                .orElse("medium (20-50 km)");
                
        // Defensively handle priorities — Jackson may deserialize JSON array as List<String> or as a raw String
        String priorities;
        Object rawPriorities = buyerProfile.get("priorities");
        if (rawPriorities instanceof List<?> list) {
            priorities = list.stream().map(Object::toString).collect(Collectors.joining(", "));
        } else if (rawPriorities instanceof String s && !s.isBlank()) {
            priorities = s;
        } else {
            priorities = "Safety, Mileage";
        }

        // Fetch cars from DB
        List<Car> allCars = carRepository.findAll();
        
        // If DB is empty, return an instruction to load data
        if (allCars.isEmpty()) {
            return "{\"error\": \"No cars in the database. Please load the sample dataset first.\"}";
        }

        // Java pre-filtering to optimize prompt tokens and keep results relevant:
        final double budgetCap = Optional.ofNullable(maxPrice).map(p -> p * 1.25).orElse(1000.0);
        List<Car> filteredCars = allCars.stream()
                .filter(car -> Optional.ofNullable(car.getPriceLakhs()).map(p -> p <= budgetCap).orElse(true))
                .collect(Collectors.toList());

        // Let's pass the filtered list of candidates to Groq
        StringBuilder carCatalogBuilder = new StringBuilder();
        carCatalogBuilder.append("ID | Make | Model | Variant | Price (Lakhs) | Fuel | Transmission | Mileage (kmpl) | Safety (Stars) | Seating | Body | Specs | Reviews\n");
        for (Car car : filteredCars) {
            carCatalogBuilder.append(String.format("%d | %s | %s | %s | %.2f | %s | %s | %.1f | %d | %d | %s | %s | %s\n",
                    car.getId(),
                    car.getMake(),
                    car.getModel(),
                    Optional.ofNullable(car.getVariant()).orElse("N/A"),
                    Optional.ofNullable(car.getPriceLakhs()).orElse(0.0),
                    Optional.ofNullable(car.getFuelType()).orElse("N/A"),
                    Optional.ofNullable(car.getTransmission()).orElse("N/A"),
                    Optional.ofNullable(car.getMileage()).orElse(0.0),
                    Optional.ofNullable(car.getSafetyRating()).orElse(0),
                    Optional.ofNullable(car.getSeatingCapacity()).orElse(5),
                    Optional.ofNullable(car.getBodyType()).orElse("N/A"),
                    Optional.ofNullable(car.getSpecifications()).filter(s -> !s.isEmpty()).orElse("N/A"),
                    Optional.ofNullable(car.getUserReviews()).filter(r -> !r.isEmpty()).map(this::truncateReviews).orElse("N/A")
            ));
        }

        // Construct System Instruction
        String systemInstruction = "You are a professional, objective, and friendly Car Matchmaker AI from CarDekho. "
                + "Your task is to analyze the buyer's profile and recommend exactly the top 3 matching cars from the provided catalog. "
                + "You must only recommend cars that are present in the provided catalog. "
                + "Assign a 'matchScore' (0-100) based on how well the car fits their budget, seating, transmission, mileage needs, and top priorities. "
                + "Provide a custom 'reasoning' explaining why this car is selected, and highlight 'pros' and 'cons' specifically extracted from user reviews.\n\n"
                + "CRITICAL: You must return ONLY a JSON object and nothing else. Do not wrap the JSON in ```json markdown code blocks. "
                + "The JSON structure must match this schema exactly:\n"
                + "{\n"
                + "  \"shortlist\": [\n"
                + "    {\n"
                + "      \"carId\": 1,\n"
                + "      \"make\": \"Tata\",\n"
                + "      \"model\": \"Nexon\",\n"
                + "      \"matchScore\": 92,\n"
                + "      \"reasoning\": \"Highly recommended since you ranked Safety as #1 and have a budget of 10 Lakhs. Nexon offers a 5-star GNCAP safety rating.\",\n"
                + "      \"pros\": [\"5-star GNCAP rating\", \"Robust build quality\"],\n"
                + "      \"cons\": [\"Rear seating space is tight\", \"Slightly jerky AMT gear shifts\"]\n"
                + "    }\n"
                + "  ],\n"
                + "  \"verdict\": \"A summary paragraph explaining the recommendation choices and comparing the top picks. Detail the trade-offs (e.g. why one car is selected over another due to mileage or safety).\"\n"
                + "}";

        // Construct User Query
        String userQuery = String.format(
                "--- BUYER PROFILE ---\n"
                + "Budget: %s Lakhs INR\n"
                + "Daily Usage: %s\n"
                + "Required Seating: %d seats\n"
                + "Transmission Preference: %s\n"
                + "Fuel Preference: %s\n"
                + "Top Priorities: %s\n\n"
                + "--- AVAILABLE CAR CATALOG ---\n"
                + "%s\n\n"
                + "Please return the top 3 recommended cars matching the above profile as a JSON object.",
                maxPrice != null ? maxPrice : "Any",
                dailyUsage,
                seatingNeeded,
                transmissionPreference,
                fuelPreference,
                priorities,
                carCatalogBuilder.toString()
        );

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemInstruction),
                new UserMessage(userQuery)
        ));

        // Use custom client if temp key is provided
        ChatClient activeClient = this.chatClient;
        if (tempApiKey != null && !tempApiKey.isEmpty()) {
            OpenAiApi openAiApi = new OpenAiApi("https://api.groq.com/openai", tempApiKey);
            activeClient = new OpenAiChatClient(openAiApi, OpenAiChatOptions.builder().withModel("llama-3.3-70b-versatile").build());
        }

        String response = activeClient.call(prompt).getResult().getOutput().getContent();
        
        // Clean response if LLM accidentally wrapped it in markdown code block and wrap in Optional
        return Optional.ofNullable(response)
                .map(this::cleanJsonResponse)
                .orElse("{}");
    }

    private String cleanJsonResponse(String response) {
        return Optional.ofNullable(response)
                .map(String::trim)
                .map(s -> {
                    if (s.startsWith("```json")) {
                        s = s.substring(7);
                    } else if (s.startsWith("```")) {
                        s = s.substring(3);
                    }
                    if (s.endsWith("```")) {
                        s = s.substring(0, s.length() - 3);
                    }
                    return s.trim();
                })
                .orElse("{}");
    }

    private String truncateReviews(String reviews) {
        return Optional.ofNullable(reviews)
                .map(r -> r.length() > 200 ? r.substring(0, 197) + "..." : r)
                .orElse("N/A");
    }

    private Double getDoubleValue(Object obj) {
        return Optional.ofNullable(obj)
                .map(o -> {
                    if (o instanceof Number) {
                        return ((Number) o).doubleValue();
                    }
                    try {
                        return Double.parseDouble(o.toString().trim());
                    } catch (Exception e) {
                        return null;
                    }
                }).orElse(null);
    }

    private Integer getIntValue(Object obj) {
        return Optional.ofNullable(obj)
                .map(o -> {
                    if (o instanceof Number) {
                        return ((Number) o).intValue();
                    }
                    try {
                        return Integer.parseInt(o.toString().trim());
                    } catch (Exception e) {
                        return null;
                    }
                }).orElse(null);
    }
}
