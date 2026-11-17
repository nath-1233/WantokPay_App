from datetime import datetime, timedelta, timezone
from uuid import UUID

import jwt
from pwdlib import PasswordHash

from app.config import get_settings


password_hash = PasswordHash.recommended()
_dummy_password_hash = password_hash.hash("not-a-real-user-password")


def hash_password(password: str) -> str:
    return password_hash.hash(password)


def verify_password(password: str, stored_hash: str | None) -> bool:
    if stored_hash is None:
        password_hash.verify(password, _dummy_password_hash)
        return False
    return password_hash.verify(password, stored_hash)


def create_access_token(user_id: UUID) -> tuple[str, int]:
    settings = get_settings()
    expires_in = settings.access_token_expire_minutes * 60
    now = datetime.now(timezone.utc)
    token = jwt.encode(
        {
            "sub": str(user_id),
            "iss": "wantokpay-api",
            "iat": now,
            "exp": now + timedelta(seconds=expires_in),
        },
        settings.secret_key.get_secret_value(),
        algorithm="HS256",
    )
    return token, expires_in


def decode_access_token(token: str) -> UUID:
    settings = get_settings()
    payload = jwt.decode(
        token,
        settings.secret_key.get_secret_value(),
        algorithms=["HS256"],
        issuer="wantokpay-api",
        options={"require": ["sub", "iss", "iat", "exp"]},
    )
    return UUID(payload["sub"])
