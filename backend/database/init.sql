-- MindHaven MySQL 8 database bootstrap.
-- Run this file once with a MySQL account that can create databases.
-- Business tables are managed by Flyway under src/main/resources/db/migration.

CREATE DATABASE IF NOT EXISTS mind_haven
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE mind_haven;
