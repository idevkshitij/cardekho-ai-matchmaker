package com.cardekho.aimatchmaker.service;

import com.cardekho.aimatchmaker.model.Car;
import com.cardekho.aimatchmaker.repository.CarRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class ExcelService {

    @Autowired
    private CarRepository carRepository;

    /**
     * Imports cars from an Excel input stream, parsing columns flexibly.
     */
    public List<Car> importExcel(InputStream inputStream) throws Exception {
        Workbook workbook = WorkbookFactory.create(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();
        
        List<Car> cars = new ArrayList<>();
        
        if (!rows.hasNext()) {
            workbook.close();
            throw new IllegalArgumentException("Excel sheet is empty");
        }
        
        Row headerRow = rows.next();
        Map<String, Integer> headerMap = getHeaderMap(headerRow);
        
        while (rows.hasNext()) {
            Row currentRow = rows.next();
            if (isRowEmpty(currentRow)) {
                continue;
            }
            
            Car car = new Car();
            
            car.setMake(getCellValueAsString(currentRow, headerMap, "make", "brand", "company"));
            car.setModel(getCellValueAsString(currentRow, headerMap, "model", "name"));
            car.setVariant(getCellValueAsString(currentRow, headerMap, "variant", "trim", "version"));
            
            Double price = getCellValueAsDouble(currentRow, headerMap, "price", "price_lakhs", "price (lakhs)", "budget");
            car.setPriceLakhs(Optional.ofNullable(price).orElse(0.0));
            
            String priceDisp = getCellValueAsString(currentRow, headerMap, "price_display", "price display", "price_str");
            car.setPriceDisplay(Optional.ofNullable(priceDisp).filter(s -> !s.isEmpty()).orElseGet(() -> "₹ " + car.getPriceLakhs() + " Lakhs"));
            
            car.setFuelType(getCellValueAsString(currentRow, headerMap, "fuel_type", "fuel type", "fuel"));
            car.setTransmission(getCellValueAsString(currentRow, headerMap, "transmission", "gearbox"));
            
            Double mileage = getCellValueAsDouble(currentRow, headerMap, "mileage", "mileage (km/l)", "fuel_economy");
            car.setMileage(Optional.ofNullable(mileage).orElse(0.0));
            
            Double safety = getCellValueAsDouble(currentRow, headerMap, "safety_rating", "safety rating", "safety", "stars");
            car.setSafetyRating(Optional.ofNullable(safety).map(Double::intValue).orElse(0));
            
            Double seating = getCellValueAsDouble(currentRow, headerMap, "seating_capacity", "seating capacity", "seating", "seats");
            car.setSeatingCapacity(Optional.ofNullable(seating).map(Double::intValue).orElse(5));
            
            car.setBodyType(getCellValueAsString(currentRow, headerMap, "body_type", "body type", "type", "category"));
            car.setSpecifications(getCellValueAsString(currentRow, headerMap, "specifications", "specs", "engine"));
            car.setUserReviews(getCellValueAsString(currentRow, headerMap, "user_reviews", "user reviews", "reviews", "feedback"));
            
            // Only add valid records (must have at least make and model)
            if (Optional.ofNullable(car.getMake()).filter(s -> !s.trim().isEmpty()).isPresent() &&
                Optional.ofNullable(car.getModel()).filter(s -> !s.trim().isEmpty()).isPresent()) {
                cars.add(car);
            }
        }
        
        workbook.close();
        
        if (!cars.isEmpty()) {
            carRepository.saveAll(cars);
        }
        return cars;
    }

    private Map<String, Integer> getHeaderMap(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING) {
                headerMap.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
            }
        }
        return headerMap;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Row row, Map<String, Integer> headerMap, String... possibleKeys) {
        return Optional.ofNullable(findColIndex(headerMap, possibleKeys))
                .map(row::getCell)
                .map(cell -> {
                    if (cell.getCellType() == CellType.STRING) {
                        return cell.getStringCellValue().trim();
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            return cell.getDateCellValue().toString();
                        }
                        double val = cell.getNumericCellValue();
                        if (val == (long) val) {
                            return String.valueOf((long) val);
                        }
                        return String.valueOf(val);
                    } else if (cell.getCellType() == CellType.BOOLEAN) {
                        return String.valueOf(cell.getBooleanCellValue());
                    }
                    return "";
                })
                .orElse("");
    }

    private Double getCellValueAsDouble(Row row, Map<String, Integer> headerMap, String... possibleKeys) {
        return Optional.ofNullable(findColIndex(headerMap, possibleKeys))
                .map(row::getCell)
                .map(cell -> {
                    if (cell.getCellType() == CellType.NUMERIC) {
                        return cell.getNumericCellValue();
                    } else if (cell.getCellType() == CellType.STRING) {
                        try {
                            String cleanVal = cell.getStringCellValue().replaceAll("[^\\d.]", "");
                            if (cleanVal.isEmpty()) return null;
                            return Double.parseDouble(cleanVal);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                    return null;
                })
                .orElse(null);
    }

    private Integer findColIndex(Map<String, Integer> headerMap, String... keys) {
        for (String key : keys) {
            Optional<Integer> index = Optional.ofNullable(headerMap.get(key.toLowerCase()));
            if (index.isPresent()) {
                return index.get();
            }
        }
        return null;
    }
}
