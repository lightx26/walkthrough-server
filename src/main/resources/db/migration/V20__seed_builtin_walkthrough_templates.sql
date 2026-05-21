-- Built-in templates with fixed UUIDs so they are stable across environments.

INSERT INTO walkthrough_templates (id, user_id, name, description, pr_type, is_builtin)
VALUES ('11111111-0000-0000-0000-000000000001', NULL, 'New Feature',
        'For shipping a full vertical slice: data model, business logic, API, and tests.',
        'FEATURE', TRUE),
       ('11111111-0000-0000-0000-000000000002', NULL, 'Refactor',
        'Walking reviewers through structural changes while highlighting what behaviour is preserved.',
        'REFACTOR', TRUE),
       ('11111111-0000-0000-0000-000000000003', NULL, 'Bug Fix',
        'A narrative from root cause to fix to regression coverage.',
        'BUGFIX', TRUE),
       ('11111111-0000-0000-0000-000000000004', NULL, 'Hotfix',
        'Minimal context for an emergency fix — fast to review, easy to verify.',
        'HOTFIX', TRUE),
       ('11111111-0000-0000-0000-000000000005', NULL, 'Database Migration',
        'Guides reviewers through schema changes, migration safety, and rollback.',
        'MIGRATION', TRUE);

-- New Feature chapters
INSERT INTO walkthrough_template_chapters (id, template_id, title, description, sort_order)
VALUES ('11111111-1000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001',
        'Data model', 'Schema changes, new entities, migrations', 0),
       ('11111111-1000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001',
        'Business logic', 'Service layer, core logic implementation', 1),
       ('11111111-1000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000001',
        'API layer', 'Controllers, endpoints, request/response DTOs', 2),
       ('11111111-1000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000001',
        'Tests', 'Unit tests and integration tests for the new feature', 3);

-- Refactor chapters
INSERT INTO walkthrough_template_chapters (id, template_id, title, description, sort_order)
VALUES ('11111111-2000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000002',
        'Before (context)', 'Files showing the original structure being replaced', 0),
       ('11111111-2000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000002',
        'Shared abstractions', 'New base classes, interfaces, or utilities introduced', 1),
       ('11111111-2000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000002',
        'Migrated components', 'Files updated to use the new structure', 2),
       ('11111111-2000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000002',
        'Tests', 'Updated or new tests validating the refactored behavior', 3);

-- Bug Fix chapters
INSERT INTO walkthrough_template_chapters (id, template_id, title, description, sort_order)
VALUES ('11111111-3000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000003',
        'Reproducing context', 'Files that illustrate where and why the bug occurred', 0),
       ('11111111-3000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000003',
        'The fix', 'Core change that resolves the issue', 1),
       ('11111111-3000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000003',
        'Guard rails', 'Validation, error handling, or defensive code added', 2),
       ('11111111-3000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000003',
        'Tests', 'Test cases that would have caught this bug', 3);

-- Hotfix chapters
INSERT INTO walkthrough_template_chapters (id, template_id, title, description, sort_order)
VALUES ('11111111-4000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000004',
        'The fix', 'The minimal change that resolves the critical issue', 0),
       ('11111111-4000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000004',
        'Impact check', 'Related files reviewed to confirm no regressions', 1);

-- Database Migration chapters
INSERT INTO walkthrough_template_chapters (id, template_id, title, description, sort_order)
VALUES ('11111111-5000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000005',
        'Migration script', 'The schema change itself', 0),
       ('11111111-5000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000005',
        'Model updates', 'Entity or ORM model changes reflecting the new schema', 1),
       ('11111111-5000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000005',
        'Service / query updates', 'Business logic updated to work with the new schema', 2),
       ('11111111-5000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000005',
        'Rollback plan', 'Down migration or rollback strategy', 3);
