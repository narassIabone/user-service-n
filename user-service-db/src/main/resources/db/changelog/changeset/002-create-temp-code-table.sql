CREATE TABLE temp_code (
                           id UUID PRIMARY KEY,
                           user_id UUID NOT NULL,
                           code VARCHAR(6) NOT NULL,
                           CONSTRAINT fk_user FOREIGN KEY(user_id) REFERENCES "user"(id) ON DELETE CASCADE
);