package com.cardekho.aimatchmaker.component;

import com.cardekho.aimatchmaker.model.Car;
import com.cardekho.aimatchmaker.repository.CarRepository;
import com.cardekho.aimatchmaker.service.ExcelService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ExcelService excelService;

    @Override
    public void run(String... args) throws Exception {
        if (carRepository.count() == 0) {
            System.out.println(">>> Database is empty. Seeding sample car dataset...");
            
            // Check if resources directory exists, write file if it does
            String resourcePath = "src/main/resources/sample-cars.xlsx";
            File resourceFile = new File(resourcePath);
            
            byte[] excelBytes;
            
            // Generate Excel in memory
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                generateSampleExcel(bos);
                excelBytes = bos.toByteArray();
            }
            
            // Write to disk in source resources if directory exists (for development / git tracking)
            try {
                File parentDir = resourceFile.getParentFile();
                if (parentDir != null && parentDir.exists() && !resourceFile.exists()) {
                    try (FileOutputStream fos = new FileOutputStream(resourceFile)) {
                        fos.write(excelBytes);
                    }
                    System.out.println(">>> Saved physical sample Excel template to: " + resourceFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println(">>> Could not write sample Excel to disk (this is normal in packaged environments): " + e.getMessage());
            }

            // Load and Seed the Database
            try (InputStream is = new ByteArrayInputStream(excelBytes)) {
                List<Car> seededCars = excelService.importExcel(is);
                System.out.println(">>> [SUCCESS] Database successfully seeded with " + seededCars.size() + " cars.");
            } catch (Exception e) {
                System.err.println(">>> Failed to parse sample Excel data: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> Database already contains " + carRepository.count() + " cars. Skipping seeding.");
        }
    }

    private void generateSampleExcel(OutputStream outputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Cars");
        
        // Define Headers
        String[] headers = {
            "Make", "Model", "Variant", "Price (Lakhs)", "Price Display", 
            "Fuel Type", "Transmission", "Mileage (km/l)", "Safety Rating", 
            "Seating Capacity", "Body Type", "Specifications", "User Reviews"
        };
        
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            // Style header slightly
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }
        
        // Sample Cars Data
        Object[][] data = {
            {
                "Tata", "Nexon", "XZ+ (S)", 10.5, "₹ 10.50 Lakhs", 
                "Petrol", "Manual", 17.5, 5, 5, "SUV", 
                "1.2L Turbocharged Revotron, 118 bhp, 170 Nm, 350L Boot Space", 
                "Excellent build quality and very safe. The 5-star rating gives peace of mind. Ground clearance is amazing for bad roads. However, the clutch is a bit heavy and the infotainment screen response is sluggish."
            },
            {
                "Tata", "Nexon EV", "Empowered+", 14.8, "₹ 14.80 Lakhs", 
                "Electric", "Automatic", 325.0, 5, 5, "SUV", 
                "Permanent Magnet Synchronous Motor, 127 bhp, 215 Nm, 350L Boot", 
                "Extremely silent cabin, zero emissions, and pocket-friendly running costs. Safety is top-notch. Regenerative braking works well. Cons: Highway range drops to 240km, charging infrastructure in rural areas is poor."
            },
            {
                "Maruti Suzuki", "Swift", "ZXI+", 8.4, "₹ 8.40 Lakhs", 
                "Petrol", "Manual", 22.4, 2, 5, "Hatchback", 
                "1.2L DualJet K-Series, 89 bhp, 113 Nm, 268L Boot Space", 
                "Incredible fuel economy, I get 20+ km/l in city traffic. Fun to drive, light clutch, and very low maintenance cost. Highly reliable. Cons: Build quality feels very light/flimsy, safety rating is poor (2 stars), and cabin gets noisy at high speeds."
            },
            {
                "Maruti Suzuki", "Swift AMT", "ZXI+ AMT", 8.9, "₹ 8.90 Lakhs", 
                "Petrol", "Automatic", 22.4, 2, 5, "Hatchback", 
                "1.2L DualJet K-Series, 89 bhp, 113 Nm, 268L Boot Space", 
                "AMT automatic is highly convenient for daily bumper-to-bumper city commuting. Mileage remains superb. Parts are cheap and easy to service. Cons: Shift lag is noticeable when accelerating hard, highway safety is a major concern."
            },
            {
                "Hyundai", "Creta", "SX (O)", 16.8, "₹ 16.80 Lakhs", 
                "Petrol", "Automatic", 16.8, 3, 5, "SUV", 
                "1.5L MPi Petrol, 113 bhp, 144 Nm, IVT Automatic, 433L Boot", 
                "Very premium interiors, panoramic sunroof is a crowd pleaser. Extremely smooth IVT gearbox. High comfort levels for long drives. Cons: Average safety rating (3 stars), body roll on curves, mileage is low in heavy traffic (10-11 kmpl)."
            },
            {
                "Hyundai", "Creta Diesel", "SX (O) CRDi", 17.5, "₹ 17.50 Lakhs", 
                "Diesel", "Manual", 18.0, 3, 5, "SUV", 
                "1.5L U2 CRDi Diesel, 113 bhp, 250 Nm, 6-Speed Manual, 433L Boot", 
                "Diesel torque is fantastic, making highway overtaking effortless. Great mileage on long tours. Excellent seats and feature-loaded. Cons: Safety is mediocre at 3 stars, diesel engine maintenance is higher, clutch has slight travel."
            },
            {
                "Mahindra", "Thar", "LX 4-Seater", 14.5, "₹ 14.50 Lakhs", 
                "Diesel", "Manual", 12.0, 4, 4, "SUV", 
                "2.2L mHawk Diesel, 4WD, 130 bhp, 300 Nm, 4-Star Safety", 
                "Stunning road presence, can drive through literally any terrain (mud, sand, rocks). 4-star safety is reassuring. Cons: Bouncy ride on normal roads, 3-door entry makes rear seat access painful for family, zero boot space with rear seats up."
            },
            {
                "Honda", "City", "ZX", 14.9, "₹ 14.90 Lakhs", 
                "Petrol", "Manual", 18.4, 5, 5, "Sedan", 
                "1.5L i-VTEC DOHC, 119 bhp, 145 Nm, 6-Speed Manual, 506L Boot", 
                "The i-VTEC engine is a masterpiece, revs cleanly and is super fun to drive. Backseat legroom is class-leading, huge boot. Premium executive feel. Cons: Low ground clearance causes scraping on tall speed bumps, sound insulation could be better."
            },
            {
                "Mahindra", "XUV700", "AX7 L 7-Str", 24.2, "₹ 24.20 Lakhs", 
                "Diesel", "Automatic", 13.5, 5, 7, "SUV", 
                "2.2L mHawk Diesel, 182 bhp, 450 Nm, 6-Speed Torque Converter, ADAS", 
                "Extremely powerful engine, feels like a luxury rocket. ADAS safety features work well. GNCAP 5-star rating. Very spacious for large families. Cons: Big size makes it hard to park in cities, fuel efficiency is low, long waiting period."
            },
            {
                "Maruti Suzuki", "Brezza", "ZXI+", 12.5, "₹ 12.50 Lakhs", 
                "Petrol", "Automatic", 19.8, 4, 5, "SUV", 
                "1.5L K15C Smart Hybrid, 102 bhp, 137 Nm, 6-Speed TC Automatic", 
                "Solid build quality, 4-star safety rating. The 6-speed torque converter automatic is butter smooth. Smart Hybrid system helps in traffic. Spacious cabin. Cons: Engine feels weak/underpowered on steep hills, cabin plastics look basic."
            },
            {
                "Maruti Suzuki", "Ertiga CNG", "VXI CNG", 11.5, "₹ 11.50 Lakhs", 
                "CNG", "Manual", 26.1, 3, 7, "MUV", 
                "1.5L K15C Engine, CNG mode: 87 bhp, 121.5 Nm, 7-Seater utility", 
                "Perfect budget 7-seater for families. CNG makes it incredibly cheap to run (under ₹4 per km). Soft suspension is comfortable. Cons: Luggage space is zero with 3rd row occupied, CNG queues are long, performance is sluggish under full load."
            },
            {
                "Toyota", "Fortuner", "Sigma 4", 39.5, "₹ 39.50 Lakhs", 
                "Diesel", "Automatic", 10.0, 5, 7, "SUV", 
                "2.8L GD Turbo Diesel, 201 bhp, 500 Nm, 4WD, GNCAP 5-Star", 
                "Indestructible reliability, resale value is unmatched. Massive road authority, everyone gives way. 4WD is extremely capable. Cons: Ride is very stiff and bumpy at low speeds, overpriced, interior dashboard looks cheap for a 40 Lakh car."
            }
        };
        
        int rowNum = 1;
        for (Object[] carData : data) {
            Row row = sheet.createRow(rowNum++);
            for (int colNum = 0; colNum < carData.length; colNum++) {
                Cell cell = row.createCell(colNum);
                if (carData[colNum] instanceof String) {
                    cell.setCellValue((String) carData[colNum]);
                } else if (carData[colNum] instanceof Double) {
                    cell.setCellValue((Double) carData[colNum]);
                } else if (carData[colNum] instanceof Integer) {
                    cell.setCellValue((Integer) carData[colNum]);
                }
            }
        }
        
        // Auto-fit columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        workbook.write(outputStream);
        workbook.close();
    }
}
