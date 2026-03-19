-- ============================================================
--  SkillSync - PostgreSQL Database Initialization
--  Creates all databases and schemas required by microservices
--  This runs ONLY on first container start (empty volume)
-- ============================================================

-- ======================== DATABASES ==========================
CREATE DATABASE skillsync_auth;
CREATE DATABASE skillsync_user;
CREATE DATABASE skillsync_mentor;
CREATE DATABASE skillsync_skill;
CREATE DATABASE skillsync_session;
CREATE DATABASE skillsync_group;
CREATE DATABASE skillsync_review;
CREATE DATABASE skillsync_notification;

-- ======================== SCHEMAS ============================
-- Each service uses its own schema within its database

\c skillsync_auth
CREATE SCHEMA IF NOT EXISTS auth;

\c skillsync_user
CREATE SCHEMA IF NOT EXISTS users;

\c skillsync_mentor
CREATE SCHEMA IF NOT EXISTS mentors;

\c skillsync_skill
CREATE SCHEMA IF NOT EXISTS skills;

\c skillsync_session
CREATE SCHEMA IF NOT EXISTS sessions;

\c skillsync_group
CREATE SCHEMA IF NOT EXISTS groups;

\c skillsync_review
CREATE SCHEMA IF NOT EXISTS reviews;

\c skillsync_notification
CREATE SCHEMA IF NOT EXISTS notifications;
