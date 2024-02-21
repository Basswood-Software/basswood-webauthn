-- User to access from any ip or host
CREATE USER IF NOT EXISTS 'basswood'@'%' IDENTIFIED BY 'basswood';
GRANT ALL PRIVILEGES ON webauthn_basswood.* TO 'basswood'@'%';

-- User to access from localhost
CREATE USER IF NOT EXISTS 'basswood'@'localhost' IDENTIFIED BY 'basswood';
GRANT ALL PRIVILEGES ON webauthn_basswood.* TO 'basswood'@'localhost';
