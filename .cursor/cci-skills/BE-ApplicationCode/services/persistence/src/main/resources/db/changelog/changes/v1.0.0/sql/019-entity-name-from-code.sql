-- Update-only: align entity display name with entity code.
UPDATE entity
SET name = code
WHERE name IS DISTINCT FROM code;
