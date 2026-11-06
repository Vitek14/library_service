-- File contains how should look users table. It's like migration
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    surname    VARCHAR(100)  NOT NULL,
    password   VARCHAR(255)  NOT NULL,
    role       VARCHAR(20)   NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT users_role_check
        CHECK (role IN ('USER', 'ADMIN', 'OWNER'))
);

-- maybe put here trigger to autoupdate updated_at field?
