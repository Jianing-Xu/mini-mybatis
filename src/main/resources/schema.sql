DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL
);

INSERT INTO users (id, username, email) VALUES (1, 'alice', 'alice@example.com');
INSERT INTO users (id, username, email) VALUES (2, 'bob', 'bob@example.com');
INSERT INTO users (id, username, email) VALUES (3, 'carol', 'carol@example.com');
