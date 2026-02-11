CREATE TABLE "user" (
                        id UUID PRIMARY KEY,
                        username VARCHAR(100) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        email VARCHAR(200) NOT NULL UNIQUE,
                        role VARCHAR(10) NOT NULL,
                        is_active BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMPTZ NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_user_username ON "user"(username);