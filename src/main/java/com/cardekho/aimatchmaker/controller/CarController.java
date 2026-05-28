package com.cardekho.aimatchmaker.controller;

import com.cardekho.aimatchmaker.model.Car;
import com.cardekho.aimatchmaker.repository.CarRepository;
import com.cardekho.aimatchmaker.service.ExcelService;
import com.cardekho.aimatchmaker.service.MatchmakerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cars")
@CrossOrigin(origins = "*")
@Tag(name = "Car Matchmaker API", description = "Endpoints for managing cars and AI matchmaking")
public class CarController {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private MatchmakerService matchmakerService;

    @GetMapping
    @Operation(summary = "Retrieve all cars currently in the database")
    public ResponseEntity<List<Car>> getAllCars() {
        return ResponseEntity.ok(carRepository.findAll());
    }

    @DeleteMapping
    @Operation(summary = "Clear all cars from the database")
    public ResponseEntity<?> clearDatabase() {
        carRepository.deleteAll();
        return ResponseEntity.ok(Map.of("message", "Database successfully cleared."));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Delete specific cars by their IDs")
    public ResponseEntity<?> deleteCarsBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No IDs provided for deletion."));
        }
        carRepository.deleteAllById(ids);
        return ResponseEntity.ok(Map.of("message", "Successfully deleted " + ids.size() + " cars."));
    }


    @PostMapping("/upload")
    @Operation(summary = "Upload an Excel (.xlsx) file to import a dataset of cars")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty."));
        }
        try {
            List<Car> importedCars = excelService.importExcel(file.getInputStream());
            return ResponseEntity.ok(Map.of(
                "message", "Successfully imported " + importedCars.size() + " cars.",
                "count", importedCars.size()
            ));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to parse Excel: " + e.getMessage()));
        }
    }

    @PostMapping("/match")
    @Operation(summary = "Send a buyer profile to match against database and get personalized AI recommendations")
    public ResponseEntity<?> matchmake(
            @RequestBody Map<String, Object> buyerProfile,
            @RequestHeader(value = "X-Groq-Api-Key", required = false) String tempApiKey) {
        try {
            String jsonResult = matchmakerService.matchmake(buyerProfile, tempApiKey);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(jsonResult);
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Matchmaking failed: " + e.getMessage()));
        }
    }

    @Autowired
    private org.springframework.core.env.Environment env;

    @GetMapping("/status")
    @Operation(summary = "Check database record count and Groq key setup status")
    public ResponseEntity<?> getStatus(@RequestHeader(value = "X-Groq-Api-Key", required = false) String tempApiKey) {
        long carCount = carRepository.count();
        String apiKey = env.getProperty("spring.ai.openai.api-key");
        boolean isKeySet = (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("dummy-key")) || (tempApiKey != null && !tempApiKey.isEmpty());
        
        return ResponseEntity.ok(Map.of(
            "carCount", carCount,
            "isAiKeyConfigured", isKeySet,
            "configuredModel", "llama-3.3-70b-versatile",
            "message", isKeySet ? "AI Matchmaker is fully operational." : "AI key is missing. Matchmaker will not work."
        ));
    }
}
