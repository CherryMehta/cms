CREATE DATABASE canteen_db;
USE canteen_db;
CREATE TABLE menu ( dish_number INT PRIMARY KEY,dish_name VARCHAR(100) NOT NULL,price DECIMAL(10,2) NOT NULL,availability VARCHAR(20) DEFAULT 'Available'
);

CREATE TABLE orders (order_id INT AUTO_INCREMENT PRIMARY KEY,student_name VARCHAR(100) NOT NULL,contact_number VARCHAR(15) NOT NULL,dish_number INT,order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,order_status VARCHAR(20) DEFAULT 'Pending',FOREIGN KEY (dish_number) REFERENCES menu(dish_number)
);
ALTER TABLE orders 
ADD COLUMN payment_status VARCHAR(20) DEFAULT 'Unpaid';
SHOW TABLES;
DESCRIBE menu;
DESCRIBE orders;
INSERT INTO menu (dish_number, dish_name, price, availability)
VALUES 
(1, 'Noodles', 60.00, 'Available'),
(2, 'Veg Sandwich', 40.00, 'Available'),
(3, 'Chole Bhature', 60.00, 'Available'),
(4, 'Samosa', 15.00, 'Available'),
(5, 'Kachori', 15.00, 'Available'),(6, 'Poha', 20.00, 'Available'),(7, 'Cold Coffee', 60.00, 'Available'),(8, 'French Fries', 70.00, 'Available'),(9, 'Rajma Chawal', 90.00, ' Not Available');

SELECT * FROM menu;