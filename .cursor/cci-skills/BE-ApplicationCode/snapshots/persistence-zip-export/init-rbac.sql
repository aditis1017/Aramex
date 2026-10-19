-- DEPRECATED: Use Liquibase migrations in src/main/resources/db/changelog instead.
-- This script is retained for manual reference and local resets only.
--
-- RBAC reference schema + seed data for custom-charges (PostgreSQL).
-- Aligns with JPA entities: Module, Permission, Role, User, RolePermission, UserPermission.

-- =========================================
-- DROP TABLES (OPTIONAL)
-- =========================================

DROP TABLE IF EXISTS user_permissions CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS modules CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- =========================================
-- MODULES
-- =========================================

CREATE TABLE modules (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================
-- PERMISSIONS
-- =========================================

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(150) NOT NULL UNIQUE,
    module_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_permissions_module
        FOREIGN KEY (module_id)
        REFERENCES modules(id)
);

-- =========================================
-- ROLES
-- =========================================

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(4000),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    arabic_name VARCHAR(255),
    arabic_description VARCHAR(2000),
    color VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Existing databases created before these columns were added can run:
-- ALTER TABLE roles ADD COLUMN IF NOT EXISTS name VARCHAR(255);
-- UPDATE roles SET name = display_name WHERE name IS NULL OR TRIM(name) = '';
-- ALTER TABLE roles ALTER COLUMN name SET NOT NULL;
-- ALTER TABLE roles ADD COLUMN IF NOT EXISTS description VARCHAR(4000);
-- ALTER TABLE roles ADD COLUMN IF NOT EXISTS system_role BOOLEAN NOT NULL DEFAULT FALSE;
-- ALTER TABLE roles ADD COLUMN IF NOT EXISTS arabic_description VARCHAR(2000);
-- ALTER TABLE roles ADD COLUMN IF NOT EXISTS color VARCHAR(64);

-- =========================================
-- ROLE PERMISSIONS
-- =========================================

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE
);

-- =========================================
-- USERS
-- =========================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,

    role_id BIGINT NOT NULL,

    groups VARCHAR(1000),

    active BOOLEAN DEFAULT TRUE,

    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
);

-- =========================================
-- USER PERMISSION OVERRIDES
-- =========================================

CREATE TABLE user_permissions (
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    allowed BOOLEAN NOT NULL,

    PRIMARY KEY (user_id, permission_id),

    CONSTRAINT fk_user_permissions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE
);

-- =========================================
-- INSERT MODULES
-- =========================================

INSERT INTO modules (code, display_name)
VALUES
('CUSTOMS', 'Customs'),
('DOCUMENTS_LIST', 'Documents List'),
('REPORTS', 'Reports'),
('AR_MANAGEMENT', 'AR Management'),
('AP_MANAGEMENT', 'AP Management'),
('GLOBAL_SETTINGS', 'Global Settings'),
('ENTITY_SETTINGS', 'Entity Settings'),
('B2B_KEYWORDS_MANAGER', 'B2B Keywords Manager');

-- =========================================
-- INSERT ROLES
-- =========================================

INSERT INTO roles (code, display_name, name, description, system_role, arabic_name, arabic_description, color)
VALUES
('OPERATIONS_STAFF', 'Operations Staff', 'Operations Staff', 'Operations Staff role', FALSE, 'موظف العمليات', NULL, NULL),
('FINANCE_OFFICER', 'Finance Officer', 'Finance Officer', 'Finance Officer role', FALSE, 'مسؤول المالية', NULL, NULL),
('VIEWER', 'Viewer', 'Viewer', 'Read-only viewer role', FALSE, 'عارض', NULL, NULL),
('CUSTOMS_MANAGER', 'Customs Manager', 'Customs Manager', 'Customs management role', FALSE, 'مدير الجمارك', NULL, NULL),
('SUPER_ADMIN', 'Super Admin', 'Super Admin', 'Full system access', TRUE, 'مشرف عام', NULL, NULL);

-- =========================================
-- INSERT PERMISSIONS
-- =========================================

INSERT INTO permissions (code, module_id)
SELECT permission_code, m.id
FROM (
    VALUES
    ('CUSTOMS_VIEW', 'CUSTOMS'),
    ('CUSTOMS_CREATE', 'CUSTOMS'),
    ('CUSTOMS_EDIT', 'CUSTOMS'),
    ('CUSTOMS_DELETE', 'CUSTOMS'),
    ('CUSTOMS_EXPORT', 'CUSTOMS'),

    ('DOCUMENTS_LIST_VIEW', 'DOCUMENTS_LIST'),
    ('DOCUMENTS_LIST_CREATE', 'DOCUMENTS_LIST'),
    ('DOCUMENTS_LIST_EDIT', 'DOCUMENTS_LIST'),
    ('DOCUMENTS_LIST_DELETE', 'DOCUMENTS_LIST'),
    ('DOCUMENTS_LIST_EXPORT', 'DOCUMENTS_LIST'),

    ('REPORTS_VIEW', 'REPORTS'),
    ('REPORTS_CREATE', 'REPORTS'),
    ('REPORTS_EDIT', 'REPORTS'),
    ('REPORTS_DELETE', 'REPORTS'),
    ('REPORTS_EXPORT', 'REPORTS'),

    ('AR_MANAGEMENT_VIEW', 'AR_MANAGEMENT'),
    ('AR_MANAGEMENT_CREATE', 'AR_MANAGEMENT'),
    ('AR_MANAGEMENT_EDIT', 'AR_MANAGEMENT'),
    ('AR_MANAGEMENT_DELETE', 'AR_MANAGEMENT'),
    ('AR_MANAGEMENT_EXPORT', 'AR_MANAGEMENT'),

    ('AP_MANAGEMENT_VIEW', 'AP_MANAGEMENT'),
    ('AP_MANAGEMENT_CREATE', 'AP_MANAGEMENT'),
    ('AP_MANAGEMENT_EDIT', 'AP_MANAGEMENT'),
    ('AP_MANAGEMENT_DELETE', 'AP_MANAGEMENT'),
    ('AP_MANAGEMENT_EXPORT', 'AP_MANAGEMENT'),

    ('GLOBAL_SETTINGS_VIEW', 'GLOBAL_SETTINGS'),
    ('GLOBAL_SETTINGS_CREATE', 'GLOBAL_SETTINGS'),
    ('GLOBAL_SETTINGS_EDIT', 'GLOBAL_SETTINGS'),
    ('GLOBAL_SETTINGS_DELETE', 'GLOBAL_SETTINGS'),
    ('GLOBAL_SETTINGS_EXPORT', 'GLOBAL_SETTINGS'),

    ('ENTITY_SETTINGS_VIEW', 'ENTITY_SETTINGS'),
    ('ENTITY_SETTINGS_CREATE', 'ENTITY_SETTINGS'),
    ('ENTITY_SETTINGS_EDIT', 'ENTITY_SETTINGS'),
    ('ENTITY_SETTINGS_DELETE', 'ENTITY_SETTINGS'),
    ('ENTITY_SETTINGS_EXPORT', 'ENTITY_SETTINGS'),

    ('B2B_KEYWORDS_MANAGER_VIEW', 'B2B_KEYWORDS_MANAGER'),
    ('B2B_KEYWORDS_MANAGER_CREATE', 'B2B_KEYWORDS_MANAGER'),
    ('B2B_KEYWORDS_MANAGER_EDIT', 'B2B_KEYWORDS_MANAGER'),
    ('B2B_KEYWORDS_MANAGER_DELETE', 'B2B_KEYWORDS_MANAGER'),
    ('B2B_KEYWORDS_MANAGER_EXPORT', 'B2B_KEYWORDS_MANAGER')

) AS perm(permission_code, module_code)
JOIN modules m
    ON m.code = perm.module_code;

-- =========================================
-- ROLE PERMISSIONS
-- =========================================

-- SUPER_ADMIN -> ALL
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN';

-- VIEWER -> VIEW ONLY
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.code LIKE '%_VIEW'
WHERE r.code = 'VIEWER';

-- FINANCE_OFFICER -> AR/AP/REPORTS
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON (
        p.code LIKE 'AR_MANAGEMENT_%'
        OR p.code LIKE 'AP_MANAGEMENT_%'
        OR p.code LIKE 'REPORTS_%'
    )
WHERE r.code = 'FINANCE_OFFICER';

-- OPERATIONS_STAFF -> CUSTOMS + DOCUMENTS
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON (
        p.code LIKE 'CUSTOMS_%'
        OR p.code LIKE 'DOCUMENTS_LIST_%'
    )
WHERE r.code = 'OPERATIONS_STAFF';

-- CUSTOMS_MANAGER -> CUSTOMS + REPORT VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON (
        p.code LIKE 'CUSTOMS_%'
        OR p.code = 'REPORTS_VIEW'
    )
WHERE r.code = 'CUSTOMS_MANAGER';

-- =========================================
-- SAMPLE USERS
-- =========================================

INSERT INTO users (
    username,
    email,
    password,
    role_id,
    groups,
    created_by
)
SELECT
    'admin',
    'admin@example.com',
    '$2a$10$encryptedpassword',
    r.id,
    'ADMIN,OPS',
    'SYSTEM'
FROM roles r
WHERE r.code = 'SUPER_ADMIN';

INSERT INTO users (
    username,
    email,
    password,
    role_id,
    groups,
    created_by
)
SELECT
    'finance_user',
    'finance@example.com',
    '$2a$10$encryptedpassword',
    r.id,
    'FINANCE',
    'SYSTEM'
FROM roles r
WHERE r.code = 'FINANCE_OFFICER';

-- =========================================
-- USER PERMISSION OVERRIDES
-- =========================================

-- finance_user gets CUSTOMS_VIEW additionally

INSERT INTO user_permissions (
    user_id,
    permission_id,
    allowed
)
SELECT
    u.id,
    p.id,
    TRUE
FROM users u
JOIN permissions p
    ON p.code = 'CUSTOMS_VIEW'
WHERE u.username = 'finance_user';

-- =========================================
-- INDEXES (RECOMMENDED)
-- =========================================

CREATE INDEX idx_users_role_id
    ON users(role_id);

CREATE INDEX idx_permissions_module_id
    ON permissions(module_id);

CREATE INDEX idx_role_permissions_role_id
    ON role_permissions(role_id);

CREATE INDEX idx_user_permissions_user_id
    ON user_permissions(user_id);

-- =========================================
-- INCREMENTAL: ENTITY_SETTINGS + B2B_KEYWORDS_MANAGER (existing DBs)
-- Run when tables already exist; skip on fresh init-rbac.sql run above.
-- =========================================
--
-- INSERT INTO modules (code, display_name)
-- VALUES
-- ('ENTITY_SETTINGS', 'Entity Settings'),
-- ('B2B_KEYWORDS_MANAGER', 'B2B Keywords Manager')
-- ON CONFLICT (code) DO NOTHING;
--
-- INSERT INTO permissions (code, module_id)
-- SELECT permission_code, m.id
-- FROM (
--     VALUES
--     ('ENTITY_SETTINGS_VIEW', 'ENTITY_SETTINGS'),
--     ('ENTITY_SETTINGS_CREATE', 'ENTITY_SETTINGS'),
--     ('ENTITY_SETTINGS_EDIT', 'ENTITY_SETTINGS'),
--     ('ENTITY_SETTINGS_DELETE', 'ENTITY_SETTINGS'),
--     ('ENTITY_SETTINGS_EXPORT', 'ENTITY_SETTINGS'),
--     ('B2B_KEYWORDS_MANAGER_VIEW', 'B2B_KEYWORDS_MANAGER'),
--     ('B2B_KEYWORDS_MANAGER_CREATE', 'B2B_KEYWORDS_MANAGER'),
--     ('B2B_KEYWORDS_MANAGER_EDIT', 'B2B_KEYWORDS_MANAGER'),
--     ('B2B_KEYWORDS_MANAGER_DELETE', 'B2B_KEYWORDS_MANAGER'),
--     ('B2B_KEYWORDS_MANAGER_EXPORT', 'B2B_KEYWORDS_MANAGER')
-- ) AS perm(permission_code, module_code)
-- JOIN modules m ON m.code = perm.module_code
-- ON CONFLICT (code) DO NOTHING;
--
-- INSERT INTO role_permissions (role_id, permission_id)
-- SELECT r.id, p.id
-- FROM roles r
-- CROSS JOIN permissions p
-- WHERE r.code = 'SUPER_ADMIN'
--   AND (p.code LIKE 'ENTITY_SETTINGS_%' OR p.code LIKE 'B2B_KEYWORDS_MANAGER_%')
-- ON CONFLICT DO NOTHING;
