import openpyxl

def generate_test_excel():
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Cars"
    
    # Headers matching the expected format
    headers = ["Make", "Model", "Variant", "Price (Lakhs)", "Fuel Type", "Transmission", "Mileage", "Safety Rating", "Specifications", "User Reviews"]
    ws.append(headers)
    
    # Fresh test data
    data = [
        ["Hyundai", "Creta", "SX Opt", 18.5, "Petrol", "Automatic", 16.5, 4, "1.5L Engine, Panoramic Sunroof, Bose Sound", "Very comfortable but slightly overpriced."],
        ["Kia", "Seltos", "GTX Plus", 19.0, "Diesel", "Automatic", 18.0, 4, "1.5L CRDi, HUD, 360 Camera", "Sporty and feature-rich, ride is a bit stiff."],
        ["Mahindra", "Thar", "LX 4-Str", 16.0, "Diesel", "Manual", 14.0, 4, "4x4, Hard Top, Touchscreen", "Unmatched road presence, impractical for family."],
        ["Toyota", "Innova Crysta", "ZX", 25.5, "Diesel", "Automatic", 13.0, 5, "2.4L Diesel, Captain Seats", "Incredibly reliable, unmatched comfort."],
        ["Volkswagen", "Virtus", "GT Plus", 18.0, "Petrol", "Automatic", 18.0, 5, "1.5L TSI, DSG, 10 inch display", "Amazing to drive, pure enthusiast car."],
        ["Skoda", "Slavia", "Style", 17.5, "Petrol", "Manual", 19.0, 5, "1.5L TSI, Ventilated Seats", "Great ground clearance, AC is weak."],
        ["Nissan", "Magnite", "XV Premium", 10.5, "Petrol", "Automatic", 19.5, 4, "1.0L Turbo CVT, 360 Camera", "Value for money, interior feels cheap."]
    ]
    
    for row in data:
        ws.append(row)
        
    wb.save("test-inventory.xlsx")
    print("test-inventory.xlsx created successfully!")

if __name__ == "__main__":
    generate_test_excel()
