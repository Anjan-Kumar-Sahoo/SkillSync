-- ============================================================
--  SkillSync - PostgreSQL Database Initialization
--  Creates all databases and schemas required by microservices
--  This runs ONLY on first container start (empty volume)
-- ============================================================

-- ======================== DATABASES ==========================
CREATE DATABASE skillsync_auth;
CREATE DATABASE skillsync_user;
CREATE DATABASE skillsync_skill;
CREATE DATABASE skillsync_session;
CREATE DATABASE skillsync_notification;
CREATE DATABASE skillsync_payment;

-- ======================== SCHEMAS ============================
-- Each service uses its own schema within its database
-- user-service now hosts users, mentors, and groups schemas
-- session-service now hosts sessions and reviews schemas
-- payment-service hosts payments schema

\c skillsync_auth
CREATE SCHEMA IF NOT EXISTS auth;

\c skillsync_user
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS mentors;
CREATE SCHEMA IF NOT EXISTS groups;

\c skillsync_skill
CREATE SCHEMA IF NOT EXISTS skills;

\c skillsync_session
CREATE SCHEMA IF NOT EXISTS sessions;
CREATE SCHEMA IF NOT EXISTS reviews;

\c skillsync_notification
CREATE SCHEMA IF NOT EXISTS notifications;

\c skillsync_payment
CREATE SCHEMA IF NOT EXISTS payments;

