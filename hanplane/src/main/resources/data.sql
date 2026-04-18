INSERT INTO users (email, password, role) VALUES ('user1@test.com', '$2a$10$I2oKfOUj1lWeBwMn4JOxPuhjD4ruqeJ9Oj7LH0y4hP/RTPmmbP.Ji', 'USER');
INSERT INTO users (email, password, role) VALUES ('user2@test.com', '$2a$10$I2oKfOUj1lWeBwMn4JOxPuhjD4ruqeJ9Oj7LH0y4hP/RTPmmbP.Ji', 'USER');
INSERT INTO users (email, password, role) VALUES ('user3@test.com', '$2a$10$I2oKfOUj1lWeBwMn4JOxPuhjD4ruqeJ9Oj7LH0y4hP/RTPmmbP.Ji', 'ADMIN');

INSERT INTO coupon (name, discount_rate, total_quantity, issued_quantity, expired_at)
VALUES ('신규가입 10% 할인쿠폰', 10, 100, 0, '2027-12-31 23:59:59');