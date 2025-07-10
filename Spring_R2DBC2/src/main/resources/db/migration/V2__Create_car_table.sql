-- V1__Create_car_table.sql
-- 放置在 src/main/resources/db/migration/ 目錄下

-- Create car table
CREATE TABLE IF NOT EXISTS car (
    id BIGSERIAL PRIMARY KEY,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    year INTEGER NOT NULL CHECK (year > 1900 AND year <= 2100),
    color VARCHAR(30),
    price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for performance
CREATE INDEX idx_car_make_model ON car(make, model);
CREATE INDEX idx_car_year ON car(year);

-- Insert sample data
INSERT INTO car (make, model, year, color, price) VALUES
    ('Toyota', 'Camry', 2022, 'White', 25000.00),
    ('Honda', 'Civic', 2021, 'Blue', 22000.00),
    ('BMW', 'X3', 2023, 'Black', 45000.00),
    ('Mercedes', 'C-Class', 2022, 'Silver', 42000.00),
    ('Audi', 'A4', 2021, 'Red', 38000.00);