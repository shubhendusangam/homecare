-- =====================================================================
-- HomeCare Seed Data (dev profile)
-- =====================================================================
-- NOTE: Tables are auto-created by Hibernate (ddl-auto: create-drop).
-- This file only inserts seed data.
-- Password for all users = BCrypt(12) of 'Admin@123'
-- Generated via: new BCryptPasswordEncoder(12).encode("Admin@123")
-- =====================================================================

-- -------------------------------------------------------------------
-- ADMIN USER
-- -------------------------------------------------------------------
INSERT INTO users (id, email, phone, password_hash, role, name, active, email_verified, phone_verified, created_at, updated_at, created_by) VALUES
('a0000000-0000-0000-0000-000000000001', 'admin@homecare.in', '+919999900000',
 '$2a$12$4EDFwRi.UqcrFW7U3ukua.b1XTmR.Y7stTJO0mQ5fF60LY5a7d.IK',
 'ADMIN', 'Super Admin', true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

-- -------------------------------------------------------------------
-- CUSTOMERS
-- -------------------------------------------------------------------
INSERT INTO users (id, email, phone, password_hash, role, name, active, email_verified, phone_verified, created_at, updated_at, created_by) VALUES
('c1000000-0000-0000-0000-000000000001', 'rahul@example.com', '+919876500001',
 '$2a$12$4EDFwRi.UqcrFW7U3ukua.b1XTmR.Y7stTJO0mQ5fF60LY5a7d.IK',
 'CUSTOMER', 'Rahul Sharma', true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

INSERT INTO users (id, email, phone, password_hash, role, name, active, email_verified, phone_verified, created_at, updated_at, created_by) VALUES
('c2000000-0000-0000-0000-000000000002', 'priya@example.com', '+919876500002',
 '$2a$12$4EDFwRi.UqcrFW7U3ukua.b1XTmR.Y7stTJO0mQ5fF60LY5a7d.IK',
 'CUSTOMER', 'Priya Patel', true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

INSERT INTO users (id, email, phone, password_hash, role, name, active, email_verified, phone_verified, created_at, updated_at, created_by) VALUES
('c3000000-0000-0000-0000-000000000003', 'amit@example.com', '+919876500003',
 '$2a$12$4EDFwRi.UqcrFW7U3ukua.b1XTmR.Y7stTJO0mQ5fF60LY5a7d.IK',
 'CUSTOMER', 'Amit Kumar', true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

-- Customer profiles
INSERT INTO customer_profiles (id, user_id, city, state, pincode, latitude, longitude, preferred_language, created_at, updated_at, created_by) VALUES
('d1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', 'Mumbai', 'Maharashtra', '400001', 19.0760, 72.8777, 'en', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO customer_profiles (id, user_id, city, state, pincode, latitude, longitude, preferred_language, created_at, updated_at, created_by) VALUES
('d2000000-0000-0000-0000-000000000002', 'c2000000-0000-0000-0000-000000000002', 'Delhi', 'Delhi', '110001', 28.6139, 77.2090, 'en', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO customer_profiles (id, user_id, city, state, pincode, latitude, longitude, preferred_language, created_at, updated_at, created_by) VALUES
('d3000000-0000-0000-0000-000000000003', 'c3000000-0000-0000-0000-000000000003', 'Bangalore', 'Karnataka', '560001', 12.9716, 77.5946, 'en', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

-- -------------------------------------------------------------------
-- HELPERS
-- -------------------------------------------------------------------
INSERT INTO users (id, email, phone, password_hash, role, name, active, email_verified, phone_verified, created_at, updated_at, created_by) VALUES
('de000000-0000-0000-0000-000000000001', 'deepa@example.com', '+919876500010',
 '$2a$12$4EDFwRi.UqcrFW7U3ukua.b1XTmR.Y7stTJO0mQ5fF60LY5a7d.IK',
 'HELPER', 'Deepa Verma', true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

INSERT INTO users (id, email, phone, password_hash, role, name, active, email_verified, phone_verified, created_at, updated_at, created_by) VALUES
('de000000-0000-0000-0000-000000000002', 'suresh@example.com', '+919876500011',
 '$2a$12$4EDFwRi.UqcrFW7U3ukua.b1XTmR.Y7stTJO0mQ5fF60LY5a7d.IK',
 'HELPER', 'Suresh Yadav', true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

INSERT INTO users (id, email, phone, password_hash, role, name, active, email_verified, phone_verified, created_at, updated_at, created_by) VALUES
('de000000-0000-0000-0000-000000000003', 'meena@example.com', '+919876500012',
 '$2a$12$4EDFwRi.UqcrFW7U3ukua.b1XTmR.Y7stTJO0mQ5fF60LY5a7d.IK',
 'HELPER', 'Meena Kumari', true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

INSERT INTO users (id, email, phone, password_hash, role, name, active, email_verified, phone_verified, created_at, updated_at, created_by) VALUES
('de000000-0000-0000-0000-000000000004', 'ravi@example.com', '+919876500013',
 '$2a$12$4EDFwRi.UqcrFW7U3ukua.b1XTmR.Y7stTJO0mQ5fF60LY5a7d.IK',
 'HELPER', 'Ravi Singh', true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

INSERT INTO users (id, email, phone, password_hash, role, name, active, email_verified, phone_verified, created_at, updated_at, created_by) VALUES
('de000000-0000-0000-0000-000000000005', 'anita@example.com', '+919876500014',
 '$2a$12$4EDFwRi.UqcrFW7U3ukua.b1XTmR.Y7stTJO0mQ5fF60LY5a7d.IK',
 'HELPER', 'Anita Das', true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

-- Helper profiles
INSERT INTO helper_profiles (id, user_id, city, pincode, latitude, longitude, available, background_verified, rating, total_jobs_completed, status, created_at, updated_at, created_by) VALUES
('e1000000-0000-0000-0000-000000000001', 'de000000-0000-0000-0000-000000000001', 'Mumbai', '400001', 19.0821, 72.8815, false, true, 4.5, 120, 'OFFLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO helper_profiles (id, user_id, city, pincode, latitude, longitude, available, background_verified, rating, total_jobs_completed, status, created_at, updated_at, created_by) VALUES
('e2000000-0000-0000-0000-000000000002', 'de000000-0000-0000-0000-000000000002', 'Mumbai', '400002', 19.0544, 72.8402, false, true, 4.2, 85, 'OFFLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO helper_profiles (id, user_id, city, pincode, latitude, longitude, available, background_verified, rating, total_jobs_completed, status, created_at, updated_at, created_by) VALUES
('e3000000-0000-0000-0000-000000000003', 'de000000-0000-0000-0000-000000000003', 'Delhi', '110001', 28.6200, 77.2100, false, false, 4.8, 200, 'OFFLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO helper_profiles (id, user_id, city, pincode, latitude, longitude, available, background_verified, rating, total_jobs_completed, status, created_at, updated_at, created_by) VALUES
('e4000000-0000-0000-0000-000000000004', 'de000000-0000-0000-0000-000000000004', 'Bangalore', '560001', 12.9750, 77.5900, false, true, 3.9, 50, 'OFFLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO helper_profiles (id, user_id, city, pincode, latitude, longitude, available, background_verified, rating, total_jobs_completed, status, created_at, updated_at, created_by) VALUES
('e5000000-0000-0000-0000-000000000005', 'de000000-0000-0000-0000-000000000005', 'Bangalore', '560002', 12.9800, 77.5950, false, false, 4.6, 150, 'OFFLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

-- Helper skills (using helper_profile_id, not user_id)
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES ('e1000000-0000-0000-0000-000000000001', 'CLEANING');
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES ('e1000000-0000-0000-0000-000000000001', 'COOKING');
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES ('e2000000-0000-0000-0000-000000000002', 'COOKING');
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES ('e3000000-0000-0000-0000-000000000003', 'BABYSITTING');
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES ('e3000000-0000-0000-0000-000000000003', 'ELDERLY_HELP');
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES ('e4000000-0000-0000-0000-000000000004', 'CLEANING');
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES ('e4000000-0000-0000-0000-000000000004', 'ELDERLY_HELP');
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES ('e5000000-0000-0000-0000-000000000005', 'BABYSITTING');
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES ('e5000000-0000-0000-0000-000000000005', 'COOKING');

-- -------------------------------------------------------------------
-- WALLETS
-- -------------------------------------------------------------------
INSERT INTO wallets (id, user_id, balance, held_amount, currency, created_at, updated_at, created_by) VALUES
('f1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', 5000.00, 0.00, 'INR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO wallets (id, user_id, balance, held_amount, currency, created_at, updated_at, created_by) VALUES
('f2000000-0000-0000-0000-000000000002', 'c2000000-0000-0000-0000-000000000002', 2500.00, 0.00, 'INR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO wallets (id, user_id, balance, held_amount, currency, created_at, updated_at, created_by) VALUES
('f3000000-0000-0000-0000-000000000003', 'c3000000-0000-0000-0000-000000000003', 1000.00, 0.00, 'INR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO wallets (id, user_id, balance, held_amount, currency, created_at, updated_at, created_by) VALUES
('f4000000-0000-0000-0000-000000000004', 'de000000-0000-0000-0000-000000000001', 3200.00, 0.00, 'INR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');
INSERT INTO wallets (id, user_id, balance, held_amount, currency, created_at, updated_at, created_by) VALUES
('f5000000-0000-0000-0000-000000000005', 'de000000-0000-0000-0000-000000000002', 1800.00, 0.00, 'INR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system');

