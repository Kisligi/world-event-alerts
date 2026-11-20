-- Demo users for the user switcher (there is no login, see docs/decisions.md ADR-004).
-- example.com addresses are reserved for documentation and never deliver real mail.

INSERT INTO app_user (name, email) VALUES
    ('Alice Analyst',   'alice@example.com'),
    ('Bob Trader',      'bob@example.com'),
    ('Carol Responder', 'carol@example.com');
