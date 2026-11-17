import re
from datetime import datetime
import json
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class Credentials(BaseModel):
    username: str = Field(min_length=3, max_length=32)
    password: str = Field(min_length=1, max_length=128)

    @field_validator("username")
    @classmethod
    def normalize_username(cls, value: str) -> str:
        value = value.strip()
        if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{2,31}", value):
            raise ValueError("Use 3-32 letters, numbers, dots, underscores, or hyphens.")
        return value


class Registration(Credentials):
    @field_validator("password")
    @classmethod
    def require_strong_password(cls, value: str) -> str:
        if len(value) < 12 or not any(char.isalpha() for char in value):
            raise ValueError("Use at least 12 characters with letters, numbers, and a symbol.")
        if not any(char.isdigit() for char in value) or not any(not char.isalnum() for char in value):
            raise ValueError("Use at least 12 characters with letters, numbers, and a symbol.")
        return value


class AccountCreated(BaseModel):
    id: UUID
    username: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    expires_in: int
    username: str
    user_id: UUID


class CurrentAccount(BaseModel):
    id: UUID
    username: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class AccountDataUpdate(BaseModel):
    data: dict = Field(default_factory=dict)

    @model_validator(mode="after")
    def validate_data(self):
        forbidden_names = {"password", "password_hash", "pin", "otp", "token", "secret", "credential"}

        def validate_value(value):
            if isinstance(value, dict):
                for key, child in value.items():
                    normalized_key = re.sub(r"[^a-z]", "", str(key).lower())
                    if any(forbidden in normalized_key for forbidden in forbidden_names):
                        raise ValueError("Authentication secrets must not be stored in account data.")
                    validate_value(child)
            elif isinstance(value, list):
                for child in value:
                    validate_value(child)

        validate_value(self.data)
        if len(json.dumps(self.data, separators=(",", ":"), ensure_ascii=True)) > 1_000_000:
            raise ValueError("Account data cannot exceed 1 MB.")
        return self


class AccountDataResponse(BaseModel):
    data: dict
    updated_at: datetime | None = None
