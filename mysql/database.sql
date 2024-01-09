CREATE SCHEMA IF NOT EXISTS webauthn_basswood;
USE webauthn_basswood;

CREATE TABLE `webauthn_basswood`.`webauthn_user` (
  `userHandle` VARCHAR(256) NOT NULL,
  `displayName` VARCHAR(128) NULL,
  PRIMARY KEY (`userHandle`)
  )ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `webauthn_username` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `username` varchar(128) NOT NULL,
  `userHandle` varchar(256) NOT NULL,
  KEY `fk_user_handle_idx` (`userHandle`),
  CONSTRAINT `fk_user_handle` FOREIGN KEY (`userHandle`) REFERENCES `webauthn_user` (`userHandle`) ON delete CASCADE ON update CASCADE,
  UNIQUE KEY `uniq_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `webauthn_relying_party` (
  `id` varchar(128) NOT NULL,
  `name` varchar(128) NOT NULL,
  `attestation` varchar(32) NULL,
  `authenticatorAttachment` varchar(32) NOT NULL,
  `residentKey` varchar(32) NULL,
  `userVerification` varchar(32) NULL,
  `allowOriginPort` tinyint NOT NULL default 0,
  `allowOriginSubdomain` tinyint NOT NULL default 0,
  `timeout` bigint NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_relying_party_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `webauthn_relying_party_origins` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `rpId` varchar(128) NOT NULL,
  `origin` varchar(256) NOT NULL,
  KEY `fk_rp_id_idx` (`rpId`),
  CONSTRAINT `fk_rp_id` FOREIGN KEY (`rpId`) REFERENCES `webauthn_relying_party` (`id`) ON delete CASCADE ON update CASCADE,
  UNIQUE KEY `uniq_origin` (`origin`),
  UNIQUE KEY `uniq_rpId_origin` (`rpId`,`origin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `webauthn_registered_credential` (
  `credentialId` varchar(256) NOT NULL,
  `type` varchar(32) NOT NULL DEFAULT 'public-key',
  `userHandle` VARCHAR(256) NOT NULL,
  `publicKeyCose` VARCHAR(1024) NOT NULL,
  `clientDataJSON` VARCHAR(1024) NOT NULL,
  `attestationObject` VARCHAR(1024) NOT NULL,
  `authenticatorAttachment` varchar(32) NULL,
  `signatureCount` bigint NOT NULL DEFAULT 0,
  `discoverable` tinyint NOT NULL default 1,
  PRIMARY KEY (`credentialId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `webauthn_authenticator_transport` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `transport` varchar(64) NOT NULL,
  `credentialId` varchar(256) NOT NULL,
  KEY `fk_credential_id_idx` (`credentialId`),
  CONSTRAINT `fk_credential_id` FOREIGN KEY (`credentialId`) REFERENCES `webauthn_registered_credential` (`credentialId`) ON delete CASCADE ON update CASCADE,
  UNIQUE KEY `uniq_credential_id_transport` (`credentialId`,`transport`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

