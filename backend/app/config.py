from functools import lru_cache

from pydantic import AliasChoices, Field, SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_url: str = Field(
        validation_alias=AliasChoices(
            "DATABASE_URL",
            "POSTGRES_URL",
            "POSTGRES_URL_NON_POOLING",
            "NEON_DATABASE_URL",
            "database_url",
        )
    )
    secret_key: SecretStr
    access_token_expire_minutes: int = 30
    rate_limit_storage_uri: str = "memory://"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    @field_validator("database_url")
    @classmethod
    def normalize_postgres_url(cls, value: str) -> str:
        value = value.strip()
        if value.startswith("postgres://"):
            value = "postgresql://" + value.removeprefix("postgres://")
        if value.startswith("postgresql://"):
            value = "postgresql+psycopg://" + value.removeprefix("postgresql://")
        if value.startswith("postgresql+psycopg://") and "sslmode=" not in value:
            separator = "&" if "?" in value else "?"
            value = f"{value}{separator}sslmode=require"
        return value

    @field_validator("secret_key")
    @classmethod
    def require_secure_secret_key(cls, value: SecretStr) -> SecretStr:
        if len(value.get_secret_value()) < 32:
            raise ValueError("SECRET_KEY must be at least 32 characters.")
        return value


@lru_cache
def get_settings() -> Settings:
    return Settings()
