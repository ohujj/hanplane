INSERT INTO users (email, password) VALUES ('user1@test.com', '1234');
INSERT INTO users (email, password) VALUES ('user2@test.com', '1234');
INSERT INTO users (email, password) VALUES ('user3@test.com', '1234');

INSERT INTO coupon (name, discount_rate, total_quantity, issued_quantity, expired_at)
VALUES ('신규가입 10% 할인쿠폰', 10, 100, 0, '2027-12-31 23:59:59');