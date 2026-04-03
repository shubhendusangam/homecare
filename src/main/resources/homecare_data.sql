-- ============================================================
-- HomeCare Test Data
-- Generated for: https://github.com/shubhendusangam/homecare
-- Password for ALL users: Test@1234
-- ============================================================

-- Clear auto-seeded service configs (ServiceConfigCache @PostConstruct seeds defaults)
DELETE FROM service_config;

-- Service configuration
INSERT INTO service_config (id, service_type, name, base_price, per_hour_price, icon, active, created_at, updated_at, created_by) VALUES
  ('0a7c9a42-a9e8-4776-a324-b9da8ac0fb89', 'CLEANING',     'Cleaning',     299.00, 149.00, 'cleaning',    TRUE, '2026-02-27 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('d9e8ebc6-791e-4ba4-b384-b76f463e2a08', 'COOKING',      'Cooking',      399.00, 199.00, 'cooking',     TRUE, '2026-02-27 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('32fbe5c3-8baa-4df3-8679-a35c0b8d138e', 'BABYSITTING',  'Babysitting',  499.00, 249.00, 'babysitting', TRUE, '2026-02-27 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('2dea92cf-bb27-4519-8e4c-38c02d037999', 'ELDERLY_HELP', 'Elderly Help', 449.00, 199.00, 'elderly',     TRUE, '2026-02-27 17:43:40', '2026-03-28 17:43:40', 'system');

-- Admin user
INSERT INTO users (id, email, phone, password_hash, role, name, avatar_url, email_verified, phone_verified, active, last_login_at, created_at, updated_at, created_by) VALUES
  ('f610771b-aa4a-4390-9539-b03884b531b5', 'admin@homecare.in', '9000000000', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'ADMIN', 'HomeCare Admin', NULL, TRUE, TRUE, TRUE, '2026-03-29 17:43:40', '2026-01-28 17:43:40', '2026-03-29 17:43:40', 'system');

INSERT INTO refresh_tokens (id, token_hash, user_id, expires_at, revoked, created_at) VALUES
  ('06cac806-ed60-46d7-884a-c6a622aef9da', 'e68badcf86df5752d46855dea493661ce5b185aa540edb4c9173da843addbd18', 'f610771b-aa4a-4390-9539-b03884b531b5', '2026-04-28 17:43:40', FALSE, '2026-03-29 17:43:40');

-- Customer users
INSERT INTO users (id, email, phone, password_hash, role, name, avatar_url, email_verified, phone_verified, active, last_login_at, created_at, updated_at, created_by) VALUES
  ('9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', 'priya.sharma@gmail.com', '9876543210', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'CUSTOMER', 'Priya Sharma', NULL, TRUE, TRUE, TRUE, '2026-03-29 17:43:40', '2026-02-27 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('c7de7343-2d0b-44f3-a673-c1ee65ebb287', 'rahul.mehta@outlook.com', '9876543211', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'CUSTOMER', 'Rahul Mehta', NULL, TRUE, TRUE, TRUE, '2026-03-28 17:43:40', '2026-02-26 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', 'anjali.singh@gmail.com', '9876543212', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'CUSTOMER', 'Anjali Singh', NULL, TRUE, TRUE, TRUE, '2026-03-27 17:43:40', '2026-02-25 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('557286f6-6a25-46c6-ac7f-4f474cd0e139', 'vikram.nair@yahoo.com', '9876543213', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'CUSTOMER', 'Vikram Nair', NULL, TRUE, TRUE, TRUE, '2026-03-26 17:43:40', '2026-02-24 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('f233bbd2-a362-481a-ba93-60216c305efc', 'sunita.joshi@gmail.com', '9876543214', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'CUSTOMER', 'Sunita Joshi', NULL, TRUE, TRUE, TRUE, '2026-03-25 17:43:40', '2026-02-23 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('b3fa1828-319c-4fba-8d9c-1cd53dc824f7', 'amit.desai@outlook.com', '9876543215', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'CUSTOMER', 'Amit Desai', NULL, TRUE, TRUE, TRUE, '2026-03-24 17:43:40', '2026-02-22 17:43:40', '2026-03-24 17:43:40', 'system');

-- Customer profiles
INSERT INTO customer_profiles (id, user_id, address_line, city, state, pincode, latitude, longitude, preferred_language, created_at, updated_at, created_by) VALUES
  ('e73e7167-2e12-4ef4-9dde-4077d4cba4df', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', 'Koregaon Park, Pune', 'Pune', 'Maharashtra', '411000', 18.5362, 73.8947, 'en', '2026-02-27 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('09668f54-83ea-4b0d-8359-bba3a6726fa9', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', 'Baner, Pune', 'Pune', 'Maharashtra', '411001', 18.559, 73.7868, 'en', '2026-02-26 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('0749b707-fe2a-432f-9d17-c45d58da99b9', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', 'Viman Nagar, Pune', 'Pune', 'Maharashtra', '411002', 18.5679, 73.9143, 'en', '2026-02-25 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('969a2b37-2d49-4964-820a-200c2f739406', '557286f6-6a25-46c6-ac7f-4f474cd0e139', 'Aundh, Pune', 'Pune', 'Maharashtra', '411003', 18.5592, 73.8083, 'en', '2026-02-24 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('34a9a004-c794-490d-b916-3a8e82367689', 'f233bbd2-a362-481a-ba93-60216c305efc', 'Kothrud, Pune', 'Pune', 'Maharashtra', '411004', 18.5074, 73.8077, 'en', '2026-02-23 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('633aa3bf-57c8-4277-84d3-a11a28c0de58', 'b3fa1828-319c-4fba-8d9c-1cd53dc824f7', 'Wakad, Pune', 'Pune', 'Maharashtra', '411005', 18.5986, 73.76, 'en', '2026-02-22 17:43:40', '2026-03-24 17:43:40', 'system');

-- Customer refresh tokens
INSERT INTO refresh_tokens (id, token_hash, user_id, expires_at, revoked, created_at) VALUES
  ('79ce96e5-72ae-4998-b649-8fafd1880e3d', '1af094805fe8cba11bccc3eece3db8d51b431a6eaee0c3b02c060507d5aef9e9', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', '2026-04-28 17:43:40', FALSE, '2026-03-29 17:43:40'),
  ('1bcd9ee6-ec47-492f-a225-22b2ae4d11c4', '3c15346b502df69bcda59b71f68fc1309cc364585a27036798c9519eeb05794a', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', '2026-04-28 17:43:40', FALSE, '2026-03-28 17:43:40'),
  ('781f04e7-7cc5-47b5-8ddd-24e82e3bd97e', 'f776ee76b7c3d2883ed1ace42a6de8e750f6beee5095a66a250a09d07f8f6281', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', '2026-04-28 17:43:40', FALSE, '2026-03-27 17:43:40'),
  ('4e043260-7335-495c-9eaa-91de7b31ecea', '196731d324a59ccde6ff2a09469dfe15f638282e265855e52fd75b550d22674b', '557286f6-6a25-46c6-ac7f-4f474cd0e139', '2026-04-28 17:43:40', FALSE, '2026-03-26 17:43:40'),
  ('c432bbcd-9799-4d8d-8d5c-1a739720e682', '7da168e41c632898af035add9d35d26eccb7bb5cac727cd89ba0207132f23a1a', 'f233bbd2-a362-481a-ba93-60216c305efc', '2026-04-28 17:43:40', FALSE, '2026-03-25 17:43:40'),
  ('66bbd121-88f9-4a47-ab89-cc3e5146ea68', '0ff6b8cd7763b250093fea432e79ebfafd96ccc9329b41feeb3ee52acd06fa5e', 'b3fa1828-319c-4fba-8d9c-1cd53dc824f7', '2026-04-28 17:43:40', FALSE, '2026-03-24 17:43:40');

-- Customer wallets (varied balances)
-- NOTE: balance = total in wallet (including held portion); available = balance − heldAmount
INSERT INTO wallets (id, user_id, balance, held_amount, currency, created_at, updated_at, created_by) VALUES
  ('46720816-ad75-4558-a1ea-f09aae0ac60c', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', 1254.0, 597.0, 'INR', '2026-02-27 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('cee162d7-9be0-410a-991b-484c5e2ea8d1', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', 4203.0, 1046.0, 'INR', '2026-02-26 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('23bb4c83-db9e-439b-a00b-8c6439ef264e', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', 1505.0, 997.0, 'INR', '2026-02-25 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('3ba2c0cc-de4e-4ff6-9d7d-71e3ffc39827', '557286f6-6a25-46c6-ac7f-4f474cd0e139', 153.0, 0.0, 'INR', '2026-02-24 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('56a69861-a53e-46b5-8b78-0103a42aa04d', 'f233bbd2-a362-481a-ba93-60216c305efc', 9105.0, 0.0, 'INR', '2026-02-23 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('41026e9d-f000-42e6-a547-9ae220efb63f', 'b3fa1828-319c-4fba-8d9c-1cd53dc824f7', 2004.0, 0.0, 'INR', '2026-02-22 17:43:40', '2026-03-27 17:43:40', 'system');

-- Helper users
INSERT INTO users (id, email, phone, password_hash, role, name, avatar_url, email_verified, phone_verified, active, last_login_at, created_at, updated_at, created_by) VALUES
  ('5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'meera.k@helper.in', '8765432100', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'HELPER', 'Meera Kamble', NULL, TRUE, TRUE, TRUE, '2026-03-29 17:43:40', '2026-02-12 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('aaacb3b3-c7aa-45bc-a7ab-503c223cda35', 'rajan.p@helper.in', '8765432101', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'HELPER', 'Rajan Patil', NULL, TRUE, TRUE, TRUE, '2026-03-28 17:43:40', '2026-02-11 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'savita.s@helper.in', '8765432102', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'HELPER', 'Savita Shinde', NULL, TRUE, TRUE, TRUE, '2026-03-27 17:43:40', '2026-02-10 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('f78a5172-4fb2-41e6-a22e-3828cac6046c', 'deepak.m@helper.in', '8765432103', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'HELPER', 'Deepak More', NULL, TRUE, TRUE, TRUE, '2026-03-26 17:43:40', '2026-02-09 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('d194f10d-5a3d-47b2-adcb-9b24c26d06cc', 'kavita.j@helper.in', '8765432104', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'HELPER', 'Kavita Jadhav', NULL, TRUE, TRUE, TRUE, '2026-03-25 17:43:40', '2026-02-08 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('b4c038f2-fcc3-48d0-8668-9d4211f70572', 'suresh.w@helper.in', '8765432105', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'HELPER', 'Suresh Waghmare', NULL, TRUE, TRUE, TRUE, '2026-03-24 17:43:40', '2026-02-07 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('67ea7aaf-2293-4262-bbd0-ff55c467784a', 'lata.p@helper.in', '8765432106', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'HELPER', 'Lata Pawar', NULL, TRUE, TRUE, TRUE, '2026-03-23 17:43:40', '2026-02-06 17:43:40', '2026-03-23 17:43:40', 'system'),
  ('2a202cf9-66b7-4ba4-92a5-dbd14e64a21c', 'ganesh.b@helper.in', '8765432107', '$2a$10$s2N3nyaWmeePeeB2VzGeGuKZrFoFmAkVTvnnfo4FWAxb.eka1Izre', 'HELPER', 'Ganesh Bhosale', NULL, TRUE, TRUE, TRUE, '2026-03-22 17:43:40', '2026-02-05 17:43:40', '2026-03-22 17:43:40', 'system');

-- Helper profiles
INSERT INTO helper_profiles (id, user_id, latitude, longitude, city, pincode, available, background_verified, id_proof_url, rating, total_jobs_completed, status, created_at, updated_at, created_by) VALUES
  ('af9d1f80-dfcd-4001-ac9d-86aefa8de497', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 18.538, 73.88, 'Pune', '411000', TRUE, TRUE, 'https://storage.homecare.in/id-proofs/helper-1.jpg', 4.8, 124, 'ON_JOB', '2026-02-12 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('c2c5e48a-eb5c-4393-b4c2-55b79822a4fc', 'aaacb3b3-c7aa-45bc-a7ab-503c223cda35', 18.56, 73.79, 'Pune', '411001', TRUE, TRUE, 'https://storage.homecare.in/id-proofs/helper-2.jpg', 4.5, 89, 'ONLINE', '2026-02-11 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('0d010ca2-ab5e-48a5-a04f-aee3cec133c5', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 18.57, 73.91, 'Pune', '411002', TRUE, TRUE, 'https://storage.homecare.in/id-proofs/helper-3.jpg', 4.9, 201, 'ON_JOB', '2026-02-10 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('d694de0e-1167-417a-8c9e-3c56c0d572c6', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 18.51, 73.81, 'Pune', '411003', TRUE, TRUE, 'https://storage.homecare.in/id-proofs/helper-4.jpg', 4.7, 67, 'ON_JOB', '2026-02-09 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('98be0f80-2b3c-4131-942a-66940aecbcb8', 'd194f10d-5a3d-47b2-adcb-9b24c26d06cc', 18.599, 73.77, 'Pune', '411004', FALSE, TRUE, 'https://storage.homecare.in/id-proofs/helper-5.jpg', 4.3, 45, 'OFFLINE', '2026-02-08 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('95c9df99-1d9c-403b-a7df-9d002782955e', 'b4c038f2-fcc3-48d0-8668-9d4211f70572', 18.54, 73.83, 'Pune', '411005', TRUE, TRUE, 'https://storage.homecare.in/id-proofs/helper-6.jpg', 4.6, 33, 'ONLINE', '2026-02-07 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('4fab8eb3-e0d3-45e6-b3ef-8c2f20c92852', '67ea7aaf-2293-4262-bbd0-ff55c467784a', 18.52, 73.86, 'Pune', '411006', TRUE, FALSE, NULL, 4.1, 12, 'ONLINE', '2026-02-06 17:43:40', '2026-03-23 17:43:40', 'system'),
  ('9c233468-97ad-4722-aa45-b57730b2ca3b', '2a202cf9-66b7-4ba4-92a5-dbd14e64a21c', 18.58, 73.89, 'Pune', '411007', FALSE, FALSE, NULL, 0.0, 0, 'OFFLINE', '2026-02-05 17:43:40', '2026-03-22 17:43:40', 'system');

-- Helper skills (element collection table)
INSERT INTO helper_profile_skills (helper_profile_id, skill) VALUES
  ('af9d1f80-dfcd-4001-ac9d-86aefa8de497', 'CLEANING'),
  ('af9d1f80-dfcd-4001-ac9d-86aefa8de497', 'COOKING'),
  ('c2c5e48a-eb5c-4393-b4c2-55b79822a4fc', 'CLEANING'),
  ('0d010ca2-ab5e-48a5-a04f-aee3cec133c5', 'COOKING'),
  ('0d010ca2-ab5e-48a5-a04f-aee3cec133c5', 'ELDERLY_HELP'),
  ('d694de0e-1167-417a-8c9e-3c56c0d572c6', 'BABYSITTING'),
  ('d694de0e-1167-417a-8c9e-3c56c0d572c6', 'ELDERLY_HELP'),
  ('98be0f80-2b3c-4131-942a-66940aecbcb8', 'CLEANING'),
  ('98be0f80-2b3c-4131-942a-66940aecbcb8', 'BABYSITTING'),
  ('95c9df99-1d9c-403b-a7df-9d002782955e', 'ELDERLY_HELP'),
  ('4fab8eb3-e0d3-45e6-b3ef-8c2f20c92852', 'COOKING'),
  ('9c233468-97ad-4722-aa45-b57730b2ca3b', 'CLEANING'),
  ('9c233468-97ad-4722-aa45-b57730b2ca3b', 'COOKING'),
  ('9c233468-97ad-4722-aa45-b57730b2ca3b', 'BABYSITTING');

-- Helper refresh tokens
INSERT INTO refresh_tokens (id, token_hash, user_id, expires_at, revoked, created_at) VALUES
  ('89bc6273-ff79-4af6-91f9-eccb3f07c407', '9252cd6f76946b3998ca022bc1ccbd227ed56379c5513de575442ed04409f942', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', '2026-04-28 17:43:40', FALSE, '2026-03-29 17:43:40'),
  ('e36ad9c7-8c27-4bea-bea8-152bbf43070a', '64b3c6f4542b75b0177d395bba0a2458bb8e6a012cb2da544e10dbb12459b42a', 'aaacb3b3-c7aa-45bc-a7ab-503c223cda35', '2026-04-28 17:43:40', FALSE, '2026-03-28 17:43:40'),
  ('dd82cfea-5028-4338-9380-542243cefb1b', '1ae84aa6d4cb2fdc291ffb5e92ea38c3d1dc0f2dd2a526fb8b16adbd35f340b4', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', '2026-04-28 17:43:40', FALSE, '2026-03-27 17:43:40'),
  ('92981879-5383-47cb-b469-b72a50106bfb', 'e54831b9579dcf655a2b6da419eafaabb75d61c240d4829badbd2f7bd08e7396', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', '2026-04-28 17:43:40', FALSE, '2026-03-26 17:43:40'),
  ('79917232-dd46-4b9e-98d0-fb9bdf471916', 'de3157d2fab02bec5cd36c53f210c5dc382b08728b2a4d84590810512478261d', 'd194f10d-5a3d-47b2-adcb-9b24c26d06cc', '2026-04-28 17:43:40', FALSE, '2026-03-25 17:43:40'),
  ('9ddfb71f-3188-42b8-9ab1-d1fa7d4d5c24', 'c9d19ebb489ca203c03fd60956190d5c2761248aee5b20809c60ec93e8168f5c', 'b4c038f2-fcc3-48d0-8668-9d4211f70572', '2026-04-28 17:43:40', FALSE, '2026-03-24 17:43:40'),
  ('fbb88cf3-751e-43c9-8e3e-a59dc4ad376f', 'aab601f939de509146d1a89f69d221ab7afe468e566a2ab6de91f1b5314f9673', '67ea7aaf-2293-4262-bbd0-ff55c467784a', '2026-04-28 17:43:40', FALSE, '2026-03-23 17:43:40'),
  ('37ad7e79-e5ce-4f35-a4c4-c49d6402717c', 'dd546f43466ccc1a4949a3e36324858e6a9317cf32fc34b8f38ca1153aec5a85', '2a202cf9-66b7-4ba4-92a5-dbd14e64a21c', '2026-04-28 17:43:40', FALSE, '2026-03-22 17:43:40');

-- Helper wallets (earnings)
INSERT INTO wallets (id, user_id, balance, held_amount, currency, created_at, updated_at, created_by) VALUES
  ('d3de09ce-b918-4925-af14-807dd667e7b0', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 8450.0, 0.00, 'INR', '2026-02-12 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('749d0f0e-36f3-4111-a8be-7ef39e0b0d6d', 'aaacb3b3-c7aa-45bc-a7ab-503c223cda35', 5200.0, 0.00, 'INR', '2026-02-11 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('7862935f-115b-4d2f-9010-99d78fcd54e0', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 14890.0, 0.00, 'INR', '2026-02-10 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('9a21b90c-0712-42fb-9d33-130d78bc1511', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 3100.0, 0.00, 'INR', '2026-02-09 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('4e9ed2a3-ce25-46f3-a608-9c65cc4dad85', 'd194f10d-5a3d-47b2-adcb-9b24c26d06cc', 2800.0, 0.00, 'INR', '2026-02-08 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('0d0146ef-8dd6-4d8d-8839-3cce959993c6', 'b4c038f2-fcc3-48d0-8668-9d4211f70572', 1900.0, 0.00, 'INR', '2026-02-07 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('a1799911-463d-4843-8646-fc233679699a', '67ea7aaf-2293-4262-bbd0-ff55c467784a', 600.0, 0.00, 'INR', '2026-02-06 17:43:40', '2026-03-23 17:43:40', 'system'),
  ('4a796e4f-8e8d-4095-9cf8-a7f6c6e76b4c', '2a202cf9-66b7-4ba4-92a5-dbd14e64a21c', 0.0, 0.00, 'INR', '2026-02-05 17:43:40', '2026-03-22 17:43:40', 'system');

-- ============================================================
-- Bookings — covering all statuses and service types
-- ============================================================

INSERT INTO bookings (id, customer_id, helper_id, service_type, booking_type, status, scheduled_at, accepted_at, started_at, completed_at, address_line, latitude, longitude, duration_hours, special_instructions, base_price, total_price, payment_status, payment_reference, rating, review_text, created_at, updated_at, created_by) VALUES
  ('d7a4e420-525e-4b65-ab93-8d6299ffcb57', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'CLEANING', 'IMMEDIATE', 'COMPLETED', NULL, '2026-03-24 16:43:40', '2026-03-24 17:43:40', '2026-03-25 17:43:40', '12 Koregaon Park, Pune', 18.5362, 73.8947, 3, 'Please bring your own cleaning supplies.', 299.0, 746.0, 'PAID', 'TXN10000', 5, 'Meera was fantastic! The house is spotless. Will definitely book again.', '2026-03-23 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('254a3963-f546-49ba-bf60-ff353be4576e', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'COOKING', 'IMMEDIATE', 'COMPLETED', NULL, '2026-03-21 16:43:40', '2026-03-21 17:43:40', '2026-03-22 17:43:40', '45 Baner Road, Pune', 18.559, 73.7868, 2, 'Vegetarian food only. No onion or garlic.', 399.0, 797.0, 'PAID', 'TXN10001', 4, 'Savita cooked delicious food. Very hygienic and professional.', '2026-03-20 17:43:40', '2026-03-22 17:43:40', 'system'),
  ('ed90ba9a-0144-4153-91de-f3fd8780ba9a', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 'BABYSITTING', 'SCHEDULED', 'COMPLETED', '2026-03-16 17:43:40', '2026-03-17 16:43:40', '2026-03-17 17:43:40', '2026-03-18 17:43:40', '78 Viman Nagar, Pune', 18.5679, 73.9143, 4, 'Two children aged 3 and 6. Both are well-behaved.', 499.0, 1495.0, 'PAID', 'TXN10002', 5, 'Deepak was great with the kids. They loved him! Very trustworthy.', '2026-03-16 17:43:40', '2026-03-18 17:43:40', 'system'),
  ('6ee48ec0-8edb-46cf-8054-6ef0950e34c2', '557286f6-6a25-46c6-ac7f-4f474cd0e139', 'b4c038f2-fcc3-48d0-8668-9d4211f70572', 'ELDERLY_HELP', 'IMMEDIATE', 'COMPLETED', NULL, '2026-03-26 16:43:40', '2026-03-26 17:43:40', '2026-03-27 17:43:40', '23 Aundh, Pune', 18.5592, 73.8083, 2, 'Elderly patient — needs help with mobility and medicines.', 449.0, 847.0, 'PAID', 'TXN10003', 4, 'Suresh took good care of my father. Patient and attentive.', '2026-03-25 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('e136657c-cd9b-4192-ad80-e40d40d217c4', 'f233bbd2-a362-481a-ba93-60216c305efc', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'CLEANING', 'SCHEDULED', 'COMPLETED', '2026-03-13 17:43:40', '2026-03-14 16:43:40', '2026-03-14 17:43:40', '2026-03-15 17:43:40', '66 Kothrud, Pune', 18.5074, 73.8077, 4, NULL, 299.0, 895.0, 'PAID', 'TXN10004', 5, 'Excellent work! Very thorough and efficient cleaning.', '2026-03-13 17:43:40', '2026-03-15 17:43:40', 'system'),
  ('6fa28ead-18d9-4abc-8548-195a5dc9fd70', 'b3fa1828-319c-4fba-8d9c-1cd53dc824f7', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'COOKING', 'IMMEDIATE', 'COMPLETED', NULL, '2026-03-27 16:43:40', '2026-03-27 17:43:40', '2026-03-28 17:43:40', '90 Wakad Road, Pune', 18.5986, 73.76, 3, 'Please arrive by 10 AM.', 399.0, 996.0, 'PAID', 'TXN10005', 3, 'Food was decent but arrived 20 minutes late. Average experience.', '2026-03-26 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('df44ee16-a98f-46ad-89d2-5982f44f66f0', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'CLEANING', 'IMMEDIATE', 'IN_PROGRESS', NULL, '2026-03-29 14:58:40', '2026-03-29 16:43:40', NULL, '12 Koregaon Park, Pune', 18.5362, 73.8947, 2, 'Main door code is 4521.', 299.0, 597.0, 'HELD', NULL, 0, NULL, '2026-03-28 17:43:40', '2026-03-29 16:43:40', 'system'),
  ('0c6210eb-0590-4fa0-b9ce-baf736628d97', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'ELDERLY_HELP', 'IMMEDIATE', 'HELPER_EN_ROUTE', NULL, '2026-03-29 15:43:40', NULL, NULL, '45 Baner Road, Pune', 18.559, 73.7868, 3, 'Patient needs wheelchair assistance.', 449.0, 1046.0, 'HELD', NULL, 0, NULL, '2026-03-28 17:43:40', '2026-03-29 17:28:40', 'system'),
  ('737a7b81-9a24-4a30-9f1a-7a256c7362e1', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 'BABYSITTING', 'IMMEDIATE', 'ASSIGNED', NULL, '2026-03-29 15:43:40', NULL, NULL, '78 Viman Nagar, Pune', 18.5679, 73.9143, 2, NULL, 499.0, 997.0, 'HELD', NULL, 0, NULL, '2026-03-28 17:43:40', '2026-03-29 15:43:40', 'system'),
  ('09bf7744-22b3-430f-981b-b2ad013d7822', '557286f6-6a25-46c6-ac7f-4f474cd0e139', NULL, 'COOKING', 'IMMEDIATE', 'PENDING_ASSIGNMENT', NULL, NULL, NULL, NULL, '23 Aundh, Pune', 18.5592, 73.8083, 2, 'First floor, flat 201.', 399.0, 797.0, 'PENDING', NULL, 0, NULL, '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('29b28f43-cd9f-4dab-ad04-a10cbefeaab9', 'f233bbd2-a362-481a-ba93-60216c305efc', 'aaacb3b3-c7aa-45bc-a7ab-503c223cda35', 'CLEANING', 'SCHEDULED', 'CANCELLED', '2026-03-26 17:43:40', '2026-03-25 16:43:40', NULL, NULL, '66 Kothrud, Pune', 18.5074, 73.8077, 2, 'Please call before arriving.', 299.0, 597.0, 'REFUNDED', NULL, 0, NULL, '2026-03-24 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('152608d6-e88c-41b8-914f-1674576181fb', 'b3fa1828-319c-4fba-8d9c-1cd53dc824f7', NULL, 'ELDERLY_HELP', 'SCHEDULED', 'PENDING_ASSIGNMENT', '2026-03-31 17:43:40', NULL, NULL, NULL, '90 Wakad Road, Pune', 18.5986, 73.76, 4, 'Evening shift preferred.', 449.0, 1245.0, 'PENDING', NULL, 0, NULL, '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system');

-- Booking status history (audit trail)
INSERT INTO booking_status_history (id, booking_id, from_status, to_status, changed_by, reason, changed_at, created_at, updated_at, created_by) VALUES
  -- d7a4e420 (COMPLETED): Priya→Meera CLEANING
  ('0a276acf-1259-4827-8ba7-2facdc0c2636', 'd7a4e420-525e-4b65-ab93-8d6299ffcb57', NULL, 'PENDING_ASSIGNMENT', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', 'Booking created', '2026-03-23 17:43:40', '2026-03-23 17:43:40', '2026-03-23 17:43:40', 'system'),
  ('45cad489-f314-4fc9-9f98-290295c24066', 'd7a4e420-525e-4b65-ab93-8d6299ffcb57', 'PENDING_ASSIGNMENT', 'ASSIGNED', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Helper accepted', '2026-03-24 16:43:40', '2026-03-24 16:43:40', '2026-03-24 16:43:40', 'system'),
  ('a18956a3-84a3-466d-a3b7-2508ca98f084', 'd7a4e420-525e-4b65-ab93-8d6299ffcb57', 'ASSIGNED', 'HELPER_EN_ROUTE', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Helper started travel', '2026-03-24 17:13:40', '2026-03-24 17:13:40', '2026-03-24 17:13:40', 'system'),
  ('7c1f9b35-4b45-4a8a-8ba4-441a428f514e', 'd7a4e420-525e-4b65-ab93-8d6299ffcb57', 'HELPER_EN_ROUTE', 'IN_PROGRESS', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Job started', '2026-03-24 17:43:40', '2026-03-24 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('c1a1b2c3-d4e5-4f67-8901-aabbccddeef1', 'd7a4e420-525e-4b65-ab93-8d6299ffcb57', 'IN_PROGRESS', 'COMPLETED', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Job completed', '2026-03-25 17:43:40', '2026-03-25 17:43:40', '2026-03-25 17:43:40', 'system'),
  -- 254a3963 (COMPLETED): Rahul→Savita COOKING
  ('b158f555-051b-4c2e-bfa0-5ed17525e25e', '254a3963-f546-49ba-bf60-ff353be4576e', NULL, 'PENDING_ASSIGNMENT', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', 'Booking created', '2026-03-20 17:43:40', '2026-03-20 17:43:40', '2026-03-20 17:43:40', 'system'),
  ('3e77e70d-26f0-4bbb-98ac-60afac4c2bae', '254a3963-f546-49ba-bf60-ff353be4576e', 'PENDING_ASSIGNMENT', 'ASSIGNED', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Helper accepted', '2026-03-21 16:43:40', '2026-03-21 16:43:40', '2026-03-21 16:43:40', 'system'),
  ('af3d7e85-e5f5-4f56-b04f-ab123fc50cbd', '254a3963-f546-49ba-bf60-ff353be4576e', 'ASSIGNED', 'HELPER_EN_ROUTE', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Helper started travel', '2026-03-21 17:13:40', '2026-03-21 17:13:40', '2026-03-21 17:13:40', 'system'),
  ('b98c4dc3-4b4f-4596-bd3d-f7dc0924df41', '254a3963-f546-49ba-bf60-ff353be4576e', 'HELPER_EN_ROUTE', 'IN_PROGRESS', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Job started', '2026-03-21 17:43:40', '2026-03-21 17:43:40', '2026-03-21 17:43:40', 'system'),
  ('c2b2c3d4-e5f6-4a78-9012-bbccddeeff22', '254a3963-f546-49ba-bf60-ff353be4576e', 'IN_PROGRESS', 'COMPLETED', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Job completed', '2026-03-22 17:43:40', '2026-03-22 17:43:40', '2026-03-22 17:43:40', 'system'),
  -- ed90ba9a (COMPLETED): Anjali→Deepak BABYSITTING
  ('d3c3d4e5-f6a7-4b89-0123-ccddeeff0033', 'ed90ba9a-0144-4153-91de-f3fd8780ba9a', NULL, 'PENDING_ASSIGNMENT', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', 'Booking created', '2026-03-16 17:43:40', '2026-03-16 17:43:40', '2026-03-16 17:43:40', 'system'),
  ('e4d4e5f6-a7b8-4c90-1234-ddeeff001144', 'ed90ba9a-0144-4153-91de-f3fd8780ba9a', 'PENDING_ASSIGNMENT', 'ASSIGNED', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 'Helper accepted', '2026-03-17 16:43:40', '2026-03-17 16:43:40', '2026-03-17 16:43:40', 'system'),
  ('f5e5f6a7-b8c9-4d01-2345-eeff00112255', 'ed90ba9a-0144-4153-91de-f3fd8780ba9a', 'ASSIGNED', 'HELPER_EN_ROUTE', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 'Helper started travel', '2026-03-17 17:13:40', '2026-03-17 17:13:40', '2026-03-17 17:13:40', 'system'),
  ('a6f6a7b8-c9d0-4e12-3456-ff00112233a6', 'ed90ba9a-0144-4153-91de-f3fd8780ba9a', 'HELPER_EN_ROUTE', 'IN_PROGRESS', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 'Job started', '2026-03-17 17:43:40', '2026-03-17 17:43:40', '2026-03-17 17:43:40', 'system'),
  ('b7a7b8c9-d0e1-4f23-4567-001122334477', 'ed90ba9a-0144-4153-91de-f3fd8780ba9a', 'IN_PROGRESS', 'COMPLETED', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 'Job completed', '2026-03-18 17:43:40', '2026-03-18 17:43:40', '2026-03-18 17:43:40', 'system'),
  -- 6ee48ec0 (COMPLETED): Vikram→Suresh ELDERLY_HELP
  ('c8b8c9d0-e1f2-4a34-5678-112233445588', '6ee48ec0-8edb-46cf-8054-6ef0950e34c2', NULL, 'PENDING_ASSIGNMENT', '557286f6-6a25-46c6-ac7f-4f474cd0e139', 'Booking created', '2026-03-25 17:43:40', '2026-03-25 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('d9c9d0e1-f2a3-4b45-6789-223344556699', '6ee48ec0-8edb-46cf-8054-6ef0950e34c2', 'PENDING_ASSIGNMENT', 'ASSIGNED', 'b4c038f2-fcc3-48d0-8668-9d4211f70572', 'Helper accepted', '2026-03-26 16:43:40', '2026-03-26 16:43:40', '2026-03-26 16:43:40', 'system'),
  ('ead0e1f2-a3b4-4c56-7890-334455667700', '6ee48ec0-8edb-46cf-8054-6ef0950e34c2', 'ASSIGNED', 'HELPER_EN_ROUTE', 'b4c038f2-fcc3-48d0-8668-9d4211f70572', 'Helper started travel', '2026-03-26 17:13:40', '2026-03-26 17:13:40', '2026-03-26 17:13:40', 'system'),
  ('fbe1f2a3-b4c5-4d67-8901-445566778811', '6ee48ec0-8edb-46cf-8054-6ef0950e34c2', 'HELPER_EN_ROUTE', 'IN_PROGRESS', 'b4c038f2-fcc3-48d0-8668-9d4211f70572', 'Job started', '2026-03-26 17:43:40', '2026-03-26 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('acf2a3b4-c5d6-4e78-9012-556677889922', '6ee48ec0-8edb-46cf-8054-6ef0950e34c2', 'IN_PROGRESS', 'COMPLETED', 'b4c038f2-fcc3-48d0-8668-9d4211f70572', 'Job completed', '2026-03-27 17:43:40', '2026-03-27 17:43:40', '2026-03-27 17:43:40', 'system'),
  -- e136657c (COMPLETED): Sunita→Meera CLEANING
  ('bda3b4c5-d6e7-4f89-0123-667788990033', 'e136657c-cd9b-4192-ad80-e40d40d217c4', NULL, 'PENDING_ASSIGNMENT', 'f233bbd2-a362-481a-ba93-60216c305efc', 'Booking created', '2026-03-13 17:43:40', '2026-03-13 17:43:40', '2026-03-13 17:43:40', 'system'),
  ('ceb4c5d6-e7f8-4a90-1234-778899001144', 'e136657c-cd9b-4192-ad80-e40d40d217c4', 'PENDING_ASSIGNMENT', 'ASSIGNED', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Helper accepted', '2026-03-14 16:43:40', '2026-03-14 16:43:40', '2026-03-14 16:43:40', 'system'),
  ('dfc5d6e7-f8a9-4b01-2345-889900112255', 'e136657c-cd9b-4192-ad80-e40d40d217c4', 'ASSIGNED', 'HELPER_EN_ROUTE', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Helper started travel', '2026-03-14 17:13:40', '2026-03-14 17:13:40', '2026-03-14 17:13:40', 'system'),
  ('e0d6e7f8-a9b0-4c12-3456-990011223366', 'e136657c-cd9b-4192-ad80-e40d40d217c4', 'HELPER_EN_ROUTE', 'IN_PROGRESS', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Job started', '2026-03-14 17:43:40', '2026-03-14 17:43:40', '2026-03-14 17:43:40', 'system'),
  ('f1e7f8a9-b0c1-4d23-4567-001122334477', 'e136657c-cd9b-4192-ad80-e40d40d217c4', 'IN_PROGRESS', 'COMPLETED', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Job completed', '2026-03-15 17:43:40', '2026-03-15 17:43:40', '2026-03-15 17:43:40', 'system'),
  -- 6fa28ead (COMPLETED): Amit→Savita COOKING
  ('a2f8a9b0-c1d2-4e34-5678-112233445588', '6fa28ead-18d9-4abc-8548-195a5dc9fd70', NULL, 'PENDING_ASSIGNMENT', 'b3fa1828-319c-4fba-8d9c-1cd53dc824f7', 'Booking created', '2026-03-26 17:43:40', '2026-03-26 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('b3a9b0c1-d2e3-4f45-6789-223344556699', '6fa28ead-18d9-4abc-8548-195a5dc9fd70', 'PENDING_ASSIGNMENT', 'ASSIGNED', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Helper accepted', '2026-03-27 16:43:40', '2026-03-27 16:43:40', '2026-03-27 16:43:40', 'system'),
  ('c4b0c1d2-e3f4-4a56-7890-334455667700', '6fa28ead-18d9-4abc-8548-195a5dc9fd70', 'ASSIGNED', 'HELPER_EN_ROUTE', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Helper started travel', '2026-03-27 17:13:40', '2026-03-27 17:13:40', '2026-03-27 17:13:40', 'system'),
  ('d5c1d2e3-f4a5-4b67-8901-445566778811', '6fa28ead-18d9-4abc-8548-195a5dc9fd70', 'HELPER_EN_ROUTE', 'IN_PROGRESS', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Job started', '2026-03-27 17:43:40', '2026-03-27 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('e6d2e3f4-a5b6-4c78-9012-556677889922', '6fa28ead-18d9-4abc-8548-195a5dc9fd70', 'IN_PROGRESS', 'COMPLETED', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Job completed', '2026-03-28 17:43:40', '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system'),
  -- df44ee16 (IN_PROGRESS): Priya→Meera CLEANING
  ('14f209f2-0243-4ffd-bc37-9766de944a98', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', NULL, 'PENDING_ASSIGNMENT', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', 'Booking created', '2026-03-28 17:43:40', '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('2e52a933-aa22-466d-b2b8-52992a46f340', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', 'PENDING_ASSIGNMENT', 'ASSIGNED', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Helper accepted', '2026-03-29 14:58:40', '2026-03-29 14:58:40', '2026-03-29 14:58:40', 'system'),
  ('7a821f95-2dec-430c-bc4f-dafee8703e89', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', 'ASSIGNED', 'HELPER_EN_ROUTE', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Helper started travel', '2026-03-29 15:43:40', '2026-03-29 15:43:40', '2026-03-29 15:43:40', 'system'),
  ('877ce27f-0396-468c-8e9e-91be5904b105', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', 'HELPER_EN_ROUTE', 'IN_PROGRESS', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'Job started', '2026-03-29 16:43:40', '2026-03-29 16:43:40', '2026-03-29 16:43:40', 'system'),
  -- 0c6210eb (HELPER_EN_ROUTE): Rahul→Savita ELDERLY_HELP
  ('8665aecb-4065-4dc5-bbc0-0a2ead9f7237', '0c6210eb-0590-4fa0-b9ce-baf736628d97', NULL, 'PENDING_ASSIGNMENT', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', 'Booking created', '2026-03-28 17:43:40', '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('e630a526-9fa8-47ca-8d4b-8a46e41db39e', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'PENDING_ASSIGNMENT', 'ASSIGNED', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Helper accepted', '2026-03-29 15:43:40', '2026-03-29 15:43:40', '2026-03-29 15:43:40', 'system'),
  ('466fdab5-1d19-4bc7-bed5-d061960a2e9a', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'ASSIGNED', 'HELPER_EN_ROUTE', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'Helper started travel', '2026-03-29 17:28:40', '2026-03-29 17:28:40', '2026-03-29 17:28:40', 'system'),
  -- 737a7b81 (ASSIGNED): Anjali→Deepak BABYSITTING
  ('f7e3f4a5-b6c7-4d89-0123-667788990033', '737a7b81-9a24-4a30-9f1a-7a256c7362e1', NULL, 'PENDING_ASSIGNMENT', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', 'Booking created', '2026-03-28 17:43:40', '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('a8f4a5b6-c7d8-4e90-1234-778899001144', '737a7b81-9a24-4a30-9f1a-7a256c7362e1', 'PENDING_ASSIGNMENT', 'ASSIGNED', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 'Helper accepted', '2026-03-29 15:43:40', '2026-03-29 15:43:40', '2026-03-29 15:43:40', 'system'),
  -- 09bf7744 (PENDING_ASSIGNMENT): Vikram COOKING
  ('b9a5b6c7-d8e9-4f01-2345-889900112255', '09bf7744-22b3-430f-981b-b2ad013d7822', NULL, 'PENDING_ASSIGNMENT', '557286f6-6a25-46c6-ac7f-4f474cd0e139', 'Booking created', '2026-03-28 17:43:40', '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system'),
  -- 29b28f43 (CANCELLED): Sunita→Rajan CLEANING
  ('7b3d6968-9907-4ec3-b020-4a6b9da0e741', '29b28f43-cd9f-4dab-ad04-a10cbefeaab9', NULL, 'PENDING_ASSIGNMENT', 'f233bbd2-a362-481a-ba93-60216c305efc', 'Booking created', '2026-03-24 17:43:40', '2026-03-24 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('80c91d31-1d8a-400c-8477-7f83c138d227', '29b28f43-cd9f-4dab-ad04-a10cbefeaab9', 'PENDING_ASSIGNMENT', 'ASSIGNED', 'aaacb3b3-c7aa-45bc-a7ab-503c223cda35', 'Helper accepted', '2026-03-25 16:43:40', '2026-03-25 16:43:40', '2026-03-25 16:43:40', 'system'),
  ('471b6043-8733-416f-8b3c-b604d24c3532', '29b28f43-cd9f-4dab-ad04-a10cbefeaab9', 'ASSIGNED', 'CANCELLED', 'f233bbd2-a362-481a-ba93-60216c305efc', 'Customer cancelled — change of plans', '2026-03-25 17:43:40', '2026-03-25 17:43:40', '2026-03-25 17:43:40', 'system'),
  -- 152608d6 (PENDING_ASSIGNMENT): Amit ELDERLY_HELP
  ('cab6c7d8-e9f0-4a12-3456-990011223366', '152608d6-e88c-41b8-914f-1674576181fb', NULL, 'PENDING_ASSIGNMENT', 'b3fa1828-319c-4fba-8d9c-1cd53dc824f7', 'Booking created', '2026-03-28 17:43:40', '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system');

-- ============================================================
-- Wallet transactions — top-ups, booking payments, earnings, refunds
-- ============================================================

INSERT INTO wallet_transactions (id, wallet_id, booking_id, type, status, amount, description, external_reference, processed_at, created_at, updated_at, created_by) VALUES
  -- Priya's wallet (46720816): topup 2000, debit 746, hold 597 → balance 657
  ('2f20af82-c157-4c9d-9877-51c52f8b273f', '46720816-ad75-4558-a1ea-f09aae0ac60c', NULL, 'CREDIT_TOPUP', 'SUCCESS', 2000.0, 'Wallet top-up via Razorpay', 'pay_mock_001', '2026-03-09 17:43:40', '2026-03-09 17:43:40', '2026-03-09 17:43:40', 'system'),
  ('4134377f-824a-45b8-83ca-1be29904decf', '46720816-ad75-4558-a1ea-f09aae0ac60c', 'd7a4e420-525e-4b65-ab93-8d6299ffcb57', 'DEBIT_BOOKING', 'SUCCESS', 746.0, 'Cleaning service payment', NULL, '2026-03-24 17:43:40', '2026-03-24 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('0e2fa19e-6150-4ce4-ba06-0bc31c6ad889', '46720816-ad75-4558-a1ea-f09aae0ac60c', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', 'DEBIT_BOOKING', 'PENDING', 597.0, 'Cleaning service (held)', NULL, NULL, '2026-03-29 14:43:40', '2026-03-29 14:43:40', 'system'),
  -- Rahul's wallet (cee162d7): topup 5000, debit 797, hold 1046 → balance 3157
  ('1755753a-8d4b-4cab-9140-06258e2fa764', 'cee162d7-9be0-410a-991b-484c5e2ea8d1', NULL, 'CREDIT_TOPUP', 'SUCCESS', 5000.0, 'Wallet top-up via Razorpay', 'pay_mock_002', '2026-02-27 17:43:40', '2026-02-27 17:43:40', '2026-02-27 17:43:40', 'system'),
  ('47f5cc43-f03f-453c-8297-a865bdc41be1', 'cee162d7-9be0-410a-991b-484c5e2ea8d1', '254a3963-f546-49ba-bf60-ff353be4576e', 'DEBIT_BOOKING', 'SUCCESS', 797.0, 'Cooking service payment', NULL, '2026-03-21 17:43:40', '2026-03-21 17:43:40', '2026-03-21 17:43:40', 'system'),
  ('5639b88b-0899-45ee-9611-e4ebd23fcab7', 'cee162d7-9be0-410a-991b-484c5e2ea8d1', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'DEBIT_BOOKING', 'PENDING', 1046.0, 'Elderly help (held)', NULL, NULL, '2026-03-29 16:43:40', '2026-03-29 16:43:40', 'system'),
  -- Anjali's wallet (23bb4c83): topup 3000, debit 1495, hold 997 → balance 508
  ('aa239026-a104-4252-8465-bc8013ae74b8', '23bb4c83-db9e-439b-a00b-8c6439ef264e', NULL, 'CREDIT_TOPUP', 'SUCCESS', 3000.0, 'Wallet top-up via Razorpay', 'pay_mock_003', '2026-03-04 17:43:40', '2026-03-04 17:43:40', '2026-03-04 17:43:40', 'system'),
  ('9e900128-8120-49e3-b3e5-bd2f34379360', '23bb4c83-db9e-439b-a00b-8c6439ef264e', 'ed90ba9a-0144-4153-91de-f3fd8780ba9a', 'DEBIT_BOOKING', 'SUCCESS', 1495.0, 'Babysitting service payment', NULL, '2026-03-17 17:43:40', '2026-03-17 17:43:40', '2026-03-17 17:43:40', 'system'),
  ('b57e179a-606a-497d-8062-81a322e72b6a', '23bb4c83-db9e-439b-a00b-8c6439ef264e', '737a7b81-9a24-4a30-9f1a-7a256c7362e1', 'DEBIT_BOOKING', 'PENDING', 997.0, 'Babysitting (held)', NULL, NULL, '2026-03-29 15:43:40', '2026-03-29 15:43:40', 'system'),
  -- Vikram's wallet (3ba2c0cc): topup 1000, debit 847 → balance 153
  ('9f9754fe-072f-4cf6-9dda-0237936fd7c0', '3ba2c0cc-de4e-4ff6-9d7d-71e3ffc39827', NULL, 'CREDIT_TOPUP', 'SUCCESS', 1000.0, 'Wallet top-up via Razorpay', 'pay_mock_004', '2026-03-19 17:43:40', '2026-03-19 17:43:40', '2026-03-19 17:43:40', 'system'),
  ('2de2c36f-8417-4b1c-9bad-e2e1e6c820f5', '3ba2c0cc-de4e-4ff6-9d7d-71e3ffc39827', '6ee48ec0-8edb-46cf-8054-6ef0950e34c2', 'DEBIT_BOOKING', 'SUCCESS', 847.0, 'Elderly help payment', NULL, '2026-03-26 17:43:40', '2026-03-26 17:43:40', '2026-03-26 17:43:40', 'system'),
  -- Sunita's wallet (56a69861): topup 10000, debit 895, debit 597, refund 597 → balance 9105
  ('daaa4a7f-43c5-4f95-ad68-037ca4a4d94a', '56a69861-a53e-46b5-8b78-0103a42aa04d', NULL, 'CREDIT_TOPUP', 'SUCCESS', 10000.0, 'Wallet top-up via Razorpay', 'pay_mock_007', '2026-03-09 17:43:40', '2026-03-09 17:43:40', '2026-03-09 17:43:40', 'system'),
  ('61c393ea-be87-4949-bff7-3bb6bb94370e', '56a69861-a53e-46b5-8b78-0103a42aa04d', 'e136657c-cd9b-4192-ad80-e40d40d217c4', 'DEBIT_BOOKING', 'SUCCESS', 895.0, 'Cleaning service payment', NULL, '2026-03-14 17:43:40', '2026-03-14 17:43:40', '2026-03-14 17:43:40', 'system'),
  ('5b92b5de-8961-4c92-baab-a2c94ae9947e', '56a69861-a53e-46b5-8b78-0103a42aa04d', '29b28f43-cd9f-4dab-ad04-a10cbefeaab9', 'DEBIT_BOOKING', 'FAILED', 597.0, 'Cleaning service (cancelled)', NULL, NULL, '2026-03-24 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('5b30dcd2-555c-45cd-802b-4af86608d45e', '56a69861-a53e-46b5-8b78-0103a42aa04d', '29b28f43-cd9f-4dab-ad04-a10cbefeaab9', 'REFUND', 'SUCCESS', 597.0, 'Full refund — customer cancel', 'rfnd_001', '2026-03-25 17:43:40', '2026-03-25 17:43:40', '2026-03-25 17:43:40', 'system'),
  -- Amit's wallet (41026e9d): topup 3000, debit 996 → balance 2004
  ('2a2d791a-2b4a-4929-9fd1-40f5c4880a5b', '41026e9d-f000-42e6-a547-9ae220efb63f', NULL, 'CREDIT_TOPUP', 'SUCCESS', 3000.0, 'Wallet top-up via Razorpay', 'pay_mock_008', '2026-03-14 17:43:40', '2026-03-14 17:43:40', '2026-03-14 17:43:40', 'system'),
  ('683f0cd7-27c6-40cc-b6cf-162df30cd906', '41026e9d-f000-42e6-a547-9ae220efb63f', '6fa28ead-18d9-4abc-8548-195a5dc9fd70', 'DEBIT_BOOKING', 'SUCCESS', 996.0, 'Cooking service payment', NULL, '2026-03-27 17:43:40', '2026-03-27 17:43:40', '2026-03-27 17:43:40', 'system'),
  -- Helper earnings
  ('f4e835ce-240c-4e5d-9f27-5f151f82c4fd', 'd3de09ce-b918-4925-af14-807dd667e7b0', 'd7a4e420-525e-4b65-ab93-8d6299ffcb57', 'CREDIT_EARNING', 'SUCCESS', 634.1, 'Earnings: Cleaning (85%)', NULL, '2026-03-24 17:43:40', '2026-03-24 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('266bda58-476a-4495-a7a1-0611ea2310ab', 'd3de09ce-b918-4925-af14-807dd667e7b0', 'e136657c-cd9b-4192-ad80-e40d40d217c4', 'CREDIT_EARNING', 'SUCCESS', 760.75, 'Earnings: Cleaning (85%)', NULL, '2026-03-14 17:43:40', '2026-03-14 17:43:40', '2026-03-14 17:43:40', 'system'),
  ('51739887-4fb9-4f47-b8fa-8b862c4bfd65', '7862935f-115b-4d2f-9010-99d78fcd54e0', '254a3963-f546-49ba-bf60-ff353be4576e', 'CREDIT_EARNING', 'SUCCESS', 677.45, 'Earnings: Cooking (85%)', NULL, '2026-03-21 17:43:40', '2026-03-21 17:43:40', '2026-03-21 17:43:40', 'system'),
  ('553a076a-bb95-4aff-826e-ebbfa6d504e6', '7862935f-115b-4d2f-9010-99d78fcd54e0', '6fa28ead-18d9-4abc-8548-195a5dc9fd70', 'CREDIT_EARNING', 'SUCCESS', 846.6, 'Earnings: Cooking (85%)', NULL, '2026-03-27 17:43:40', '2026-03-27 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('fb38f687-6a57-46a0-a905-4ea57091af77', '9a21b90c-0712-42fb-9d33-130d78bc1511', 'ed90ba9a-0144-4153-91de-f3fd8780ba9a', 'CREDIT_EARNING', 'SUCCESS', 1270.75, 'Earnings: Babysitting (85%)', NULL, '2026-03-17 17:43:40', '2026-03-17 17:43:40', '2026-03-17 17:43:40', 'system'),
  ('55611c8d-d1fe-4674-8044-f7d32d2c4602', '0d0146ef-8dd6-4d8d-8839-3cce959993c6', '6ee48ec0-8edb-46cf-8054-6ef0950e34c2', 'CREDIT_EARNING', 'SUCCESS', 719.95, 'Earnings: Elderly Help (85%)', NULL, '2026-03-26 17:43:40', '2026-03-26 17:43:40', '2026-03-26 17:43:40', 'system');

-- Payment orders (Razorpay)
INSERT INTO payment_orders (id, user_id, booking_id, razorpay_order_id, razorpay_payment_id, razorpay_signature, amount, status, failure_reason, created_at, updated_at, created_by) VALUES
  ('83a17637-32f6-4649-bd12-0792c1c20258', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', NULL, 'order_mock_001', 'pay_mock_001', 'sig_mock_001', 2000.0, 'PAID', NULL, '2026-03-19 17:43:40', '2026-03-19 17:43:40', 'system'),
  ('c469d4e4-3bc0-4267-8fc7-64093981b0a1', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', NULL, 'order_mock_002', 'pay_mock_002', 'sig_mock_002', 5000.0, 'PAID', NULL, '2026-03-19 17:43:40', '2026-03-19 17:43:40', 'system'),
  ('0cda1a8f-53be-4403-861e-6d4a5af2f0a8', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', NULL, 'order_mock_003', 'pay_mock_003', 'sig_mock_003', 3000.0, 'PAID', NULL, '2026-03-19 17:43:40', '2026-03-19 17:43:40', 'system'),
  ('f847e47a-12b1-4cfc-af0e-71fe890e3c45', '557286f6-6a25-46c6-ac7f-4f474cd0e139', NULL, 'order_mock_004', 'pay_mock_004', 'sig_mock_004', 1000.0, 'PAID', NULL, '2026-03-19 17:43:40', '2026-03-19 17:43:40', 'system'),
  ('64e49996-70fa-4b4b-8285-517a44c7c113', 'f233bbd2-a362-481a-ba93-60216c305efc', NULL, 'order_mock_007', 'pay_mock_007', 'sig_mock_007', 10000.0, 'PAID', NULL, '2026-03-19 17:43:40', '2026-03-19 17:43:40', 'system'),
  ('53b5a26b-2ff9-4e49-bfef-b78794cb0abd', 'b3fa1828-319c-4fba-8d9c-1cd53dc824f7', NULL, 'order_mock_008', 'pay_mock_008', 'sig_mock_008', 3000.0, 'PAID', NULL, '2026-03-19 17:43:40', '2026-03-19 17:43:40', 'system'),
  ('5f6d5606-4635-4b38-8be1-8a7655508be9', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', NULL, 'order_mock_009', NULL, NULL, 500.0, 'FAILED', 'Payment declined by bank', '2026-03-19 17:43:40', '2026-03-19 17:43:40', 'system');

-- Reviews (one per completed booking)
INSERT INTO reviews (id, booking_id, customer_id, helper_id, rating, comment, published, flagged, service_type, created_at, updated_at, created_by) VALUES
  ('3830f6d6-4f7f-4bef-986c-c5de11b1c6dc', 'd7a4e420-525e-4b65-ab93-8d6299ffcb57', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 5, 'Meera was fantastic! The house is spotless. Will definitely book again.', TRUE, FALSE, 'CLEANING', '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('377a9545-1dd7-4e64-835a-c1717ebf0cda', '254a3963-f546-49ba-bf60-ff353be4576e', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 4, 'Savita cooked delicious food. Very hygienic and professional.', TRUE, FALSE, 'COOKING', '2026-03-27 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('ca91168a-a2ad-4528-8ae5-2c7b204d145c', 'ed90ba9a-0144-4153-91de-f3fd8780ba9a', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 5, 'Deepak was great with the kids. They loved him! Very trustworthy.', TRUE, FALSE, 'BABYSITTING', '2026-03-26 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('6868a496-d658-40d0-9bcd-0f346da69505', '6ee48ec0-8edb-46cf-8054-6ef0950e34c2', '557286f6-6a25-46c6-ac7f-4f474cd0e139', 'b4c038f2-fcc3-48d0-8668-9d4211f70572', 4, 'Suresh took good care of my father. Patient and attentive.', TRUE, FALSE, 'ELDERLY_HELP', '2026-03-25 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('0c6f1c55-e6df-473d-9edc-2d7b195ef17b', 'e136657c-cd9b-4192-ad80-e40d40d217c4', 'f233bbd2-a362-481a-ba93-60216c305efc', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 5, 'Excellent work! Very thorough and efficient cleaning.', TRUE, FALSE, 'CLEANING', '2026-03-24 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('9a545c83-3a3a-483b-b79c-7ebf35a03db3', '6fa28ead-18d9-4abc-8548-195a5dc9fd70', 'b3fa1828-319c-4fba-8d9c-1cd53dc824f7', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 3, 'Food was decent but arrived 20 minutes late. Average experience.', TRUE, TRUE, 'COOKING', '2026-03-23 17:43:40', '2026-03-23 17:43:40', 'system');

-- Notifications (in-app)
INSERT INTO notifications (id, user_id, type, title, body, action_url, is_read, read_at, metadata, email_failed, sms_failed, created_at, updated_at, created_by) VALUES
  ('1eaea6d1-e5d5-45e9-ba9a-b70d993149db', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', 'BOOKING_CONFIRMED', 'Booking confirmed', 'Your cleaning booking has been confirmed.', '/customer/history', TRUE, '2026-03-24 17:43:40', '{"bookingId":"d7a4e420-525e-4b65-ab93-8d6299ffcb57"}', FALSE, FALSE, '2026-03-23 17:43:40', '2026-03-23 17:43:40', 'system'),
  ('517d0bb8-7f07-408a-9392-d58d47a04a5b', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', 'BOOKING_ASSIGNED', 'Helper assigned', 'Meera Kamble will clean your home today.', '/customer/track', TRUE, '2026-03-24 17:43:40', '{"bookingId":"d7a4e420-525e-4b65-ab93-8d6299ffcb57"}', FALSE, FALSE, '2026-03-24 16:43:40', '2026-03-24 16:43:40', 'system'),
  ('94ca5467-2d60-4b75-8e55-f8c5b181a9f7', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', 'BOOKING_COMPLETED', 'Booking completed', 'Your cleaning session is complete. Please rate Meera.', '/customer/history', TRUE, '2026-03-24 17:43:40', '{"bookingId":"d7a4e420-525e-4b65-ab93-8d6299ffcb57"}', FALSE, FALSE, '2026-03-24 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('e6f21f40-aebb-4aab-acff-4cacf9a08f26', '9ce6cf6a-31d6-4189-97e8-75b4c7465dd8', 'BOOKING_ASSIGNED', 'Helper on the way!', 'Meera Kamble is heading to your location.', '/customer/track', FALSE, NULL, '{"bookingId":"df44ee16-a98f-46ad-89d2-5982f44f66f0"}', FALSE, FALSE, '2026-03-29 15:43:40', '2026-03-29 15:43:40', 'system'),
  ('1729841b-4a8f-45d9-b9d9-32ed17e2ddb8', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', 'BOOKING_CONFIRMED', 'Booking confirmed', 'Your cooking booking is confirmed.', '/customer/history', TRUE, '2026-03-21 17:43:40', '{"bookingId":"254a3963-f546-49ba-bf60-ff353be4576e"}', FALSE, FALSE, '2026-03-20 17:43:40', '2026-03-20 17:43:40', 'system'),
  ('6c1e68eb-6730-413b-9cbc-f974c0f042f8', 'c7de7343-2d0b-44f3-a673-c1ee65ebb287', 'HELPER_EN_ROUTE', 'Helper on the way!', 'Savita Shinde is heading to your location. ETA: 12 min.', '/customer/track', FALSE, NULL, '{"bookingId":"0c6210eb-0590-4fa0-b9ce-baf736628d97"}', FALSE, FALSE, '2026-03-29 17:28:40', '2026-03-29 17:28:40', 'system'),
  ('0e1d585d-f926-49f3-8eb5-5a95985ab1b2', '81cc22b3-6fca-4018-8ad7-cf1d314c3ddb', 'BOOKING_CONFIRMED', 'Booking confirmed', 'Your babysitting booking is confirmed for tomorrow.', '/customer/history', TRUE, '2026-03-17 17:43:40', '{"bookingId":"ed90ba9a-0144-4153-91de-f3fd8780ba9a"}', FALSE, FALSE, '2026-03-16 17:43:40', '2026-03-16 17:43:40', 'system'),
  ('cf1e3acb-0f9a-4910-b908-49f6a74d5670', '557286f6-6a25-46c6-ac7f-4f474cd0e139', 'PAYMENT_SUCCESS', 'Payment successful', '₹847.00 paid for elderly help service.', '/customer/wallet', TRUE, '2026-03-26 17:43:40', '{"amount":"847.00"}', FALSE, FALSE, '2026-03-26 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('2384c70a-4c24-4019-9b90-e354728de81a', 'f233bbd2-a362-481a-ba93-60216c305efc', 'BOOKING_CANCELLED', 'Booking cancelled', 'Your booking has been cancelled. Refund of ₹597 initiated.', '/customer/history', TRUE, '2026-03-25 17:43:40', '{"bookingId":"29b28f43-cd9f-4dab-ad04-a10cbefeaab9"}', FALSE, FALSE, '2026-03-25 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('059b72dd-81f3-474d-850d-d4298f6663b2', 'f233bbd2-a362-481a-ba93-60216c305efc', 'PAYMENT_REFUND', 'Refund processed', '₹597.00 has been refunded to your wallet.', '/customer/wallet', TRUE, '2026-03-25 17:43:40', '{"amount":"597.00"}', FALSE, FALSE, '2026-03-25 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('d3fa3d37-bc67-46f6-af07-5862cd4fb4cb', '557286f6-6a25-46c6-ac7f-4f474cd0e139', 'WALLET_LOW', 'Low wallet balance', 'Your wallet balance is below ₹100. Add money to continue booking.', '/customer/wallet', FALSE, NULL, '{}', FALSE, FALSE, '2026-03-29 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('f63051d4-e7ce-4207-86bc-26b3b913f0f3', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'BOOKING_ASSIGNED', 'New booking request', 'New cleaning job in Koregaon Park. ₹597.00.', '/helper/bookings', TRUE, '2026-03-29 15:43:40', '{"bookingId":"df44ee16-a98f-46ad-89d2-5982f44f66f0"}', FALSE, FALSE, '2026-03-29 14:43:40', '2026-03-29 14:43:40', 'system'),
  ('36da85b1-07fc-40ca-9196-5682d85fd8a5', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 'BOOKING_COMPLETED', 'Job completed!', 'Great job! You earned ₹634.10 for today''s cleaning.', '/helper/earnings', TRUE, '2026-03-24 17:43:40', '{"earning":"634.10"}', FALSE, FALSE, '2026-03-24 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('eba0e517-d3c1-42a0-b94c-a7f68bfcfd12', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 'BOOKING_ASSIGNED', 'New booking request', 'New elderly help job in Baner. ₹1046.00.', '/helper/bookings', FALSE, NULL, '{"bookingId":"0c6210eb-0590-4fa0-b9ce-baf736628d97"}', FALSE, FALSE, '2026-03-29 16:43:40', '2026-03-29 16:43:40', 'system'),
  ('b69e2eb1-3ec5-4396-9173-979b5a822f42', 'f78a5172-4fb2-41e6-a22e-3828cac6046c', 'BOOKING_ASSIGNED', 'New booking request', 'New babysitting job in Viman Nagar. ₹997.00.', '/helper/bookings', FALSE, NULL, '{"bookingId":"737a7b81-9a24-4a30-9f1a-7a256c7362e1"}', FALSE, FALSE, '2026-03-29 15:43:40', '2026-03-29 15:43:40', 'system'),
  ('4cb1f119-3fd7-49a2-b0d0-eae98e9acf03', 'd194f10d-5a3d-47b2-adcb-9b24c26d06cc', 'SYSTEM_ALERT', 'You are now offline', 'You have been marked offline due to inactivity.', '/helper/home', TRUE, '2026-03-27 17:43:40', '{}', FALSE, FALSE, '2026-03-27 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('2c16a346-fc70-4a5c-a210-8911ac01a94f', 'f610771b-aa4a-4390-9539-b03884b531b5', 'SYSTEM_ALERT', 'Daily report ready', 'Today''s report: 3 bookings, ₹2,440 revenue.', '/admin/dashboard', TRUE, '2026-03-28 17:43:40', '{}', FALSE, FALSE, '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system'),
  ('497de171-853e-49d3-944d-43aef7f108f8', 'f610771b-aa4a-4390-9539-b03884b531b5', 'SYSTEM_ALERT', 'Helper pending verification', 'Lata Pawar and Ganesh Bhosale need background check.', '/admin/helpers', FALSE, NULL, '{}', FALSE, FALSE, '2026-03-27 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('7200fc0b-6e80-453a-b8c7-030599ccdec7', 'f610771b-aa4a-4390-9539-b03884b531b5', 'SYSTEM_ALERT', 'Review flagged', 'A review has been flagged for moderation.', '/admin/reviews', FALSE, NULL, '{"reviewId":"9a545c83-3a3a-483b-b79c-7ebf35a03db3"}', FALSE, FALSE, '2026-03-29 17:43:40', '2026-03-29 17:43:40', 'system'),
  ('d9f4302a-1254-403b-af45-67353c9f7cc2', 'f610771b-aa4a-4390-9539-b03884b531b5', 'SYSTEM_ALERT', 'New booking — no helper', 'Booking in Aundh has no helper assigned yet.', '/admin/bookings', FALSE, NULL, '{"bookingId":"09bf7744-22b3-430f-981b-b2ad013d7822"}', FALSE, FALSE, '2026-03-29 17:43:40', '2026-03-29 17:43:40', 'system');

-- Location history for active and completed bookings
INSERT INTO location_history (id, booking_id, helper_id, latitude, longitude, accuracy, heading, speed_kmh, recorded_at, created_at, updated_at, created_by) VALUES
  ('1f584d2a-1c66-4594-9997-3fbd7c092939', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 18.538, 73.88, 0.0, 0.0, 0.0, '2026-03-29 15:43:40', '2026-03-29 15:43:40', '2026-03-29 15:43:40', 'system'),
  ('962ba67e-5889-454d-8584-c2cd6c47bdbb', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 18.5375, 73.882, 0.0, 0.0, 0.0, '2026-03-29 15:48:40', '2026-03-29 15:48:40', '2026-03-29 15:48:40', 'system'),
  ('55d81339-a60c-4487-9840-8b925c1bf0f8', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 18.537, 73.8845, 0.0, 0.0, 0.0, '2026-03-29 15:53:40', '2026-03-29 15:53:40', '2026-03-29 15:53:40', 'system'),
  ('839fa4c4-f27e-49af-995e-27ba3236009c', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 18.5368, 73.887, 0.0, 0.0, 0.0, '2026-03-29 15:58:40', '2026-03-29 15:58:40', '2026-03-29 15:58:40', 'system'),
  ('597b2a09-9304-4d19-a5bc-ecc949f6393c', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 18.5365, 73.8895, 0.0, 0.0, 0.0, '2026-03-29 16:03:40', '2026-03-29 16:03:40', '2026-03-29 16:03:40', 'system'),
  ('b8fef196-e9eb-41c6-99dd-25214a440a49', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 18.5363, 73.892, 0.0, 0.0, 0.0, '2026-03-29 16:08:40', '2026-03-29 16:08:40', '2026-03-29 16:08:40', 'system'),
  ('aafe63e4-aba2-4ebf-90c0-d767b32ee89c', 'df44ee16-a98f-46ad-89d2-5982f44f66f0', '5af2788d-da2a-4df2-b5ae-f4a8d4d05831', 18.5362, 73.894, 0.0, 0.0, 0.0, '2026-03-29 16:13:40', '2026-03-29 16:13:40', '2026-03-29 16:13:40', 'system'),
  ('ab6f95f3-c837-4940-b4ba-4a501de8a3c7', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 18.57, 73.91, 0.0, 0.0, 0.0, '2026-03-29 17:28:40', '2026-03-29 17:28:40', '2026-03-29 17:28:40', 'system'),
  ('133b6057-84dc-4a1e-b9fa-eb1f5f9c79d0', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 18.568, 73.905, 0.0, 0.0, 0.0, '2026-03-29 17:31:40', '2026-03-29 17:31:40', '2026-03-29 17:31:40', 'system'),
  ('386c0fef-3416-486b-bfe7-a8452e1e90df', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 18.566, 73.901, 0.0, 0.0, 0.0, '2026-03-29 17:34:40', '2026-03-29 17:34:40', '2026-03-29 17:34:40', 'system'),
  ('e24b50f9-2d8f-4c6b-b6a9-bcd7ca14edd3', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 18.564, 73.898, 0.0, 0.0, 0.0, '2026-03-29 17:37:40', '2026-03-29 17:37:40', '2026-03-29 17:37:40', 'system'),
  ('e58029cc-0a1e-41f9-88a2-40b2f77a88a7', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 18.562, 73.895, 0.0, 0.0, 0.0, '2026-03-29 17:40:40', '2026-03-29 17:40:40', '2026-03-29 17:40:40', 'system'),
  ('d46d01c7-6f7b-4eee-a567-18e586be1954', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 18.561, 73.893, 0.0, 0.0, 0.0, '2026-03-29 17:42:40', '2026-03-29 17:42:40', '2026-03-29 17:42:40', 'system'),
  ('92481f7f-0a7e-4b6e-bbbb-0c7469cd91d8', '0c6210eb-0590-4fa0-b9ce-baf736628d97', 'eca5c12a-d8db-43fd-b190-35fee2eac9b3', 18.56, 73.891, 0.0, 0.0, 0.0, '2026-03-29 17:43:40', '2026-03-29 17:43:40', '2026-03-29 17:43:40', 'system');

-- Daily reports (last 7 days)
INSERT INTO daily_reports (id, report_date, total_bookings, completed_bookings, cancelled_bookings, new_customers, new_helpers, total_revenue, platform_fee, avg_helper_rating, created_at, updated_at, created_by) VALUES
  ('07f6ef7e-2dea-4753-9007-65d773b86192', '2026-03-22', 2, 1, 0, 0, 0, 1895.0, 284.25, 4.7, '2026-03-22 17:43:40', '2026-03-22 17:43:40', 'system'),
  ('221db242-5fb7-4f5b-b750-3b671657b64e', '2026-03-23', 3, 2, 1, 0, 0, 2692.0, 403.8, 4.6, '2026-03-23 17:43:40', '2026-03-23 17:43:40', 'system'),
  ('e947e520-37df-4dfe-88d8-c5eba57e2ef6', '2026-03-24', 4, 3, 0, 1, 0, 1641.0, 246.15, 4.8, '2026-03-24 17:43:40', '2026-03-24 17:43:40', 'system'),
  ('73e5649d-8b42-4043-bf30-daadb0bc8e50', '2026-03-25', 3, 2, 0, 0, 0, 3490.0, 523.5, 4.9, '2026-03-25 17:43:40', '2026-03-25 17:43:40', 'system'),
  ('4b2fc29b-5022-4d70-bca5-88a9f0f3b54f', '2026-03-26', 5, 3, 1, 0, 0, 4237.0, 635.55, 4.7, '2026-03-26 17:43:40', '2026-03-26 17:43:40', 'system'),
  ('0001322f-3f4d-4d23-8cef-20d9da6f191e', '2026-03-27', 4, 3, 0, 0, 0, 3996.0, 599.4, 4.5, '2026-03-27 17:43:40', '2026-03-27 17:43:40', 'system'),
  ('5a1d79aa-8a50-4c33-9c9f-0a6d2e8d524b', '2026-03-28', 6, 5, 0, 1, 0, 5942.0, 891.3, 4.8, '2026-03-28 17:43:40', '2026-03-28 17:43:40', 'system');

-- ============================================================
-- TEST DATA SUMMARY
-- ============================================================
--
-- USERS (all password: Test@1234)
-- Admin:    admin@homecare.in
--
-- Customers:
--   Priya Sharma         priya.sharma@gmail.com              wallet: ₹1,254.00  (held: ₹597.00)
--   Rahul Mehta          rahul.mehta@outlook.com             wallet: ₹4,203.00  (held: ₹1,046.00)
--   Anjali Singh         anjali.singh@gmail.com              wallet: ₹1,505.00  (held: ₹997.00)
--   Vikram Nair          vikram.nair@yahoo.com               wallet: ₹153.00
--   Sunita Joshi         sunita.joshi@gmail.com              wallet: ₹9,105.00
--   Amit Desai           amit.desai@outlook.com              wallet: ₹2,004.00
--
-- Helpers:
--   Meera Kamble         meera.k@helper.in                   ON_JOB   rating:4.8 verified
--   Rajan Patil          rajan.p@helper.in                   ONLINE   rating:4.5 verified
--   Savita Shinde        savita.s@helper.in                  ON_JOB   rating:4.9 verified
--   Deepak More          deepak.m@helper.in                  ON_JOB   rating:4.7 verified
--   Kavita Jadhav        kavita.j@helper.in                  OFFLINE  rating:4.3 verified
--   Suresh Waghmare      suresh.w@helper.in                  ONLINE   rating:4.6 verified
--   Lata Pawar           lata.p@helper.in                    ONLINE   rating:4.1 UNVERIFIED
--   Ganesh Bhosale       ganesh.b@helper.in                  OFFLINE  rating:0.0 UNVERIFIED
--
-- BOOKINGS (12 total):
--   COMPLETED: 6
--   IN_PROGRESS: 1
--   HELPER_EN_ROUTE: 1
--   ASSIGNED: 1
--   PENDING_ASSIGNMENT: 2
--   CANCELLED: 1
--
-- SERVICE COVERAGE:
--   CLEANING: 4 bookings
--   COOKING: 3 bookings
--   BABYSITTING: 2 bookings
--   ELDERLY_HELP: 3 bookings
--
-- REVIEWS: 6 (5 published, 1 flagged)
-- NOTIFICATIONS: 20 (mix of read/unread, all channels)
-- LOCATION HISTORY: 14 pings across 2 active bookings
-- DAILY REPORTS: 7 days of analytics
-- ============================================================