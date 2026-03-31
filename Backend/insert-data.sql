-- ============================================================
-- SkillSync - Initial Data Seed Script
-- Run this script using psql or PgAdmin to insert default users
-- and link their mentor profiles and skills accurately without 
-- modifying any java code.
-- ============================================================

-- ============================================================
-- 1. Authentication Service Data (skillsync_auth)
-- ============================================================
\c skillsync_auth

INSERT INTO auth.users (id, email, password_hash, first_name, last_name, role, is_active, is_verified, password_set, created_at, updated_at) VALUES 
(1, 'academyskillsync@gmail.com', '$2b$10$hXlA3itJtMuyuaZNYHIxneOvSUolNwgfu6gYQ8tbX.p8iaMFNMT3G', 'Admin', 'SkillSync', 'ROLE_ADMIN', true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'hiimissav@gmail.com', '$2b$10$TkeUC6pPGs7Cct2v9uvZTer8axVSQ/HANP4C1kQlYL73vbZ02NIu6', 'Anjali', 'Viswakarma', 'ROLE_LEARNER', true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'uadayasribasawoju@gmail.com', '$2b$10$vuQXoFW65Yr3z779.1MQU.P9JMKV7JKRExYpFqOxyuZv970yn2w7G', 'Udaya', 'Sri', 'ROLE_LEARNER', true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'aksahoo1097@gmail.com', '$2b$10$XI41VOdDZvfTdy.q/56sl.H5eAR5wUytB6dOiC0xcOhcRov1D/AA.', 'Anjan Kumar', 'Sahoo', 'ROLE_MENTOR', true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'anshulkumar94122@gmail.com', '$2b$10$lH9t8wjk2X2JaKhK98DKseiv4yKGdQG0abHv1wCxgTAQ0921wf67e', 'Anshul', 'Kumar', 'ROLE_MENTOR', true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'rangabattulla45@gmail.com', '$2b$10$cFHFbit/37OM9Dy//2CTKeJztL3xG/Wmq2F3K0hou4Dls4zbARbnC', 'Ranga', 'Raju', 'ROLE_MENTOR', true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Safely advance the internal PostgreSQL ID sequence so subsequent app registrations don't fail
SELECT setval(pg_get_serial_sequence('auth.users', 'id'), coalesce(max(id),0) + 1, false) FROM auth.users;


-- ============================================================
-- 2. Profile & Mentor Data (skillsync_user)
-- ============================================================
\c skillsync_user

INSERT INTO users.profiles (id, user_id, first_name, last_name, profile_complete_pct, created_at, updated_at) VALUES
(1, 1, 'Admin', 'SkillSync', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, 'Anjali', 'Viswakarma', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 3, 'Udaya', 'Sri', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 4, 'Anjan Kumar', 'Sahoo', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 5, 'Anshul', 'Kumar', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 6, 'Ranga', 'Raju', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('users.profiles', 'id'), coalesce(max(id),0) + 1, false) FROM users.profiles;

INSERT INTO mentors.mentor_profiles (id, user_id, bio, experience_years, hourly_rate, avg_rating, total_reviews, total_sessions, status, created_at, updated_at) VALUES
(1, 4, 'Communication + JAVA FS DEVELOPER', 5, 50.00, 5.0, 0, 0, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 5, 'Core Java + Kotlin + Adv. Java Expert', 5, 50.00, 5.0, 0, 0, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 6, 'Core Java + Ai/ML Specialist', 5, 50.00, 5.0, 0, 0, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('mentors.mentor_profiles', 'id'), coalesce(max(id),0) + 1, false) FROM mentors.mentor_profiles;


-- ============================================================
-- 3. Skill Data (skillsync_skill)
-- ============================================================
\c skillsync_skill

INSERT INTO skills.skills (id, name, category, description, is_active, created_at) VALUES 
(1, 'Java', 'Backend', 'Core and advanced Java development', true, CURRENT_TIMESTAMP),
(2, 'Kotlin', 'Backend', 'Modern Kotlin application development', true, CURRENT_TIMESTAMP),
(3, 'AI/ML', 'Data Science', 'Artificial Intelligence and Machine Learning', true, CURRENT_TIMESTAMP),
(4, 'Communication', 'Soft Skills', 'Effective tech communication', true, CURRENT_TIMESTAMP),
(5, 'Full Stack', 'Architecture', 'End to end system development', true, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('skills.skills', 'id'), coalesce(max(id),0) + 1, false) FROM skills.skills;


-- ============================================================
-- 4. Assign Skills back to Mentors (skillsync_user)
-- ============================================================
\c skillsync_user

-- Assign skills to Anjan (Mentor 1) -> Java, Communication, Full Stack
-- Assign skills to Anshul (Mentor 2) -> Java, Kotlin
-- Assign skills to Ranga (Mentor 3) -> Java, AI/ML
INSERT INTO mentors.mentor_skills (id, mentor_id, skill_id) VALUES
(1, 1, 1),
(2, 1, 4),
(3, 1, 5),
(4, 2, 1),
(5, 2, 2),
(6, 3, 1),
(7, 3, 3)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('mentors.mentor_skills', 'id'), coalesce(max(id),0) + 1, false) FROM mentors.mentor_skills;
