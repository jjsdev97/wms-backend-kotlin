-- 인증용 사용자 계정. "user"는 PostgreSQL 예약어라 테이블명은 app_user 사용.
CREATE TABLE app_user (
    id            BIGSERIAL     PRIMARY KEY,
    username      VARCHAR(50)   NOT NULL UNIQUE,
    password_hash VARCHAR(100)  NOT NULL,            -- BCrypt 해시
    role          VARCHAR(20)   NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_app_user_role CHECK (role IN ('USER', 'ADMIN'))
);
