-- RBAC seed data (modules, roles, permissions, users, role-permissions)
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

INSERT INTO roles (code, display_name, name, description, system_role, arabic_name, arabic_description, color)
VALUES
('OPERATIONS_STAFF', 'Operations Staff', 'Operations Staff', 'Operations Staff role', FALSE, 'موظف العمليات', NULL, NULL),
('FINANCE_OFFICER', 'Finance Officer', 'Finance Officer', 'Finance Officer role', FALSE, 'مسؤول المالية', NULL, NULL),
('VIEWER', 'Viewer', 'Viewer', 'Read-only viewer role', FALSE, 'عارض', NULL, NULL),
('CUSTOMS_MANAGER', 'Customs Manager', 'Customs Manager', 'Customs management role', FALSE, 'مدير الجمارك', NULL, NULL),
('SUPER_ADMIN', 'Super Admin', 'Super Admin', 'Full system access', TRUE, 'مشرف عام', NULL, NULL);

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

-- Reset and re-seed role_permissions to match the RBAC matrix:
-- Super Admin      -> VIEW, CREATE, EDIT, DELETE, EXPORT (all modules)
-- Customs Manager  -> VIEW, CREATE, EDIT, EXPORT       (all modules)
-- Finance Officer  -> VIEW, EXPORT                     (all modules)
-- Operations Staff -> VIEW                             (all modules)
-- Viewer           -> VIEW                             (all modules)

-- 1) Delete existing role-permission mappings for the five roles
DELETE FROM role_permissions rp
USING roles r
WHERE rp.role_id = r.id
  AND r.code IN (
      'SUPER_ADMIN',
      'CUSTOMS_MANAGER',
      'FINANCE_OFFICER',
      'OPERATIONS_STAFF',
      'VIEWER'
  );

-- 2) SUPER_ADMIN -> all actions on all modules
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
JOIN modules m ON m.id = p.module_id
WHERE r.code = 'SUPER_ADMIN'
  AND m.code IN (
      'AP_MANAGEMENT',
      'AR_MANAGEMENT',
      'B2B_KEYWORDS_MANAGER',
      'CUSTOMS',
      'DOCUMENTS_LIST',
      'ENTITY_SETTINGS',
      'GLOBAL_SETTINGS',
      'REPORTS'
  )
  AND p.code IN (
      m.code || '_VIEW',
      m.code || '_CREATE',
      m.code || '_EDIT',
      m.code || '_DELETE',
      m.code || '_EXPORT'
  )
ON CONFLICT DO NOTHING;

-- 3) CUSTOMS_MANAGER -> VIEW, CREATE, EDIT, EXPORT (no DELETE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
JOIN modules m ON m.id = p.module_id
WHERE r.code = 'CUSTOMS_MANAGER'
  AND m.code IN (
      'AP_MANAGEMENT',
      'AR_MANAGEMENT',
      'B2B_KEYWORDS_MANAGER',
      'CUSTOMS',
      'DOCUMENTS_LIST',
      'ENTITY_SETTINGS',
      'GLOBAL_SETTINGS',
      'REPORTS'
  )
  AND p.code IN (
      m.code || '_VIEW',
      m.code || '_CREATE',
      m.code || '_EDIT',
      m.code || '_EXPORT'
  )
ON CONFLICT DO NOTHING;

-- 4) FINANCE_OFFICER -> VIEW, EXPORT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
JOIN modules m ON m.id = p.module_id
WHERE r.code = 'FINANCE_OFFICER'
  AND m.code IN (
      'AP_MANAGEMENT',
      'AR_MANAGEMENT',
      'B2B_KEYWORDS_MANAGER',
      'CUSTOMS',
      'DOCUMENTS_LIST',
      'ENTITY_SETTINGS',
      'GLOBAL_SETTINGS',
      'REPORTS'
  )
  AND p.code IN (
      m.code || '_VIEW',
      m.code || '_EXPORT'
  )
ON CONFLICT DO NOTHING;

-- 5) OPERATIONS_STAFF -> VIEW only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
JOIN modules m ON m.id = p.module_id
WHERE r.code = 'OPERATIONS_STAFF'
  AND m.code IN (
      'AP_MANAGEMENT',
      'AR_MANAGEMENT',
      'B2B_KEYWORDS_MANAGER',
      'CUSTOMS',
      'DOCUMENTS_LIST',
      'ENTITY_SETTINGS',
      'GLOBAL_SETTINGS',
      'REPORTS'
  )
  AND p.code = m.code || '_VIEW'
ON CONFLICT DO NOTHING;

-- 6) VIEWER -> VIEW only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
JOIN modules m ON m.id = p.module_id
WHERE r.code = 'VIEWER'
  AND m.code IN (
      'AP_MANAGEMENT',
      'AR_MANAGEMENT',
      'B2B_KEYWORDS_MANAGER',
      'CUSTOMS',
      'DOCUMENTS_LIST',
      'ENTITY_SETTINGS',
      'GLOBAL_SETTINGS',
      'REPORTS'
  )
  AND p.code = m.code || '_VIEW'
ON CONFLICT DO NOTHING;
