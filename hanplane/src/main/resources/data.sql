INSERT INTO users (email, password, role, name) VALUES ('user1@test.com', '$2a$10$I2oKfOUj1lWeBwMn4JOxPuhjD4ruqeJ9Oj7LH0y4hP/RTPmmbP.Ji', 'USER', '테스트1');
INSERT INTO users (email, password, role, name) VALUES ('user2@test.com', '$2a$10$I2oKfOUj1lWeBwMn4JOxPuhjD4ruqeJ9Oj7LH0y4hP/RTPmmbP.Ji', 'USER', '테스트2');
INSERT INTO users (email, password, role, name) VALUES ('user3@test.com', '$2a$10$I2oKfOUj1lWeBwMn4JOxPuhjD4ruqeJ9Oj7LH0y4hP/RTPmmbP.Ji', 'ADMIN', '테스트3');

INSERT INTO coupon (name, discount_rate, total_quantity, issued_quantity, expired_at)
VALUES ('신규가입 10% 할인쿠폰', 10, 100, 1, '2027-12-31 23:59:59');

INSERT INTO coupon (name, discount_rate, total_quantity, issued_quantity, expired_at)
VALUES ('여름 할인쿠폰', 20, 100, 1, '2027-12-31 23:59:59');

INSERT INTO coupon (name, discount_rate, total_quantity, issued_quantity, expired_at)
VALUES ('가을 할인쿠폰', 30, 100, 1, '2027-12-31 23:59:59');

insert into user_coupon
(coupon_id, issued_at, user_id , status)
values
(1, '2026-12-31 23:59:59', 1, 'UNUSED');

insert into user_coupon
(coupon_id, issued_at, user_id , status)
values
(2, '2026-12-31 23:59:59', 1, 'UNUSED');

insert into user_coupon
(coupon_id, issued_at, user_id , status)
values
(3, '2026-12-31 23:59:59', 1, 'UNUSED');