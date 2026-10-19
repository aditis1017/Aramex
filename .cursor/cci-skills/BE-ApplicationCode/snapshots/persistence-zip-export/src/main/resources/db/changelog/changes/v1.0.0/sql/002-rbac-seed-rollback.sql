-- Restore previous (005) role-permission seed behavior

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

-- SUPER_ADMIN -> ALL
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

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
WHERE r.code = 'FINANCE_OFFICER'
ON CONFLICT DO NOTHING;

-- OPERATIONS_STAFF -> CUSTOMS + DOCUMENTS
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON (
        p.code LIKE 'CUSTOMS_%'
        OR p.code LIKE 'DOCUMENTS_LIST_%'
    )
WHERE r.code = 'OPERATIONS_STAFF'
ON CONFLICT DO NOTHING;

-- CUSTOMS_MANAGER -> CUSTOMS + REPORT VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON (
        p.code LIKE 'CUSTOMS_%'
        OR p.code = 'REPORTS_VIEW'
    )
WHERE r.code = 'CUSTOMS_MANAGER'
ON CONFLICT DO NOTHING;

-- ALL non-super roles -> ALL VIEW permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.code LIKE '%_VIEW'
WHERE r.code <> 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

DELETE FROM user_permissions up
USING users u
WHERE up.user_id = u.id
  AND u.username = 'finance_user';

DELETE FROM users
WHERE username IN ('admin', 'finance_user');

DELETE FROM permissions
WHERE code LIKE 'CUSTOMS_%'
   OR code LIKE 'DOCUMENTS_LIST_%'
   OR code LIKE 'REPORTS_%'
   OR code LIKE 'AR_MANAGEMENT_%'
   OR code LIKE 'AP_MANAGEMENT_%'
   OR code LIKE 'GLOBAL_SETTINGS_%'
   OR code LIKE 'ENTITY_SETTINGS_%'
   OR code LIKE 'B2B_KEYWORDS_MANAGER_%';

DELETE FROM roles
WHERE code IN (
    'OPERATIONS_STAFF',
    'FINANCE_OFFICER',
    'VIEWER',
    'CUSTOMS_MANAGER',
    'SUPER_ADMIN'
);

DELETE FROM modules
WHERE code IN (
    'CUSTOMS',
    'DOCUMENTS_LIST',
    'REPORTS',
    'AR_MANAGEMENT',
    'AP_MANAGEMENT',
    'GLOBAL_SETTINGS',
    'ENTITY_SETTINGS',
    'B2B_KEYWORDS_MANAGER'
);
