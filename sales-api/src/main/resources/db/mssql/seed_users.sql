IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin')
INSERT INTO users (username, password_hash, role)
VALUES ('admin', '$2a$10$RjB4E2VzDZ2OB73n2pYWdO9/X2gd8q8pKobBfXGAwT.MelFVk8e3.', 'ADMIN');

IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'staff')
INSERT INTO users (username, password_hash, role)
VALUES ('staff', '$2a$10$aqQZI8MiXB/8gTIBej2ds.FtSZZPUYyqqGLvc41DH/qoODTwIqne2', 'STAFF');
