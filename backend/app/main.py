from contextlib import asynccontextmanager
from datetime import datetime, timezone
from uuid import UUID

import jwt
from fastapi import Depends, FastAPI, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address
from sqlalchemy import func, select, text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.auth import create_access_token, decode_access_token, hash_password, verify_password
from app.config import get_settings
from app.database import Base, SessionLocal, engine
from app.models import AccountData, User
from app.schemas import (
    AccountCreated,
    AccountDataResponse,
    AccountDataUpdate,
    Credentials,
    CurrentAccount,
    LoginResponse,
    Registration,
)

settings = get_settings()
limiter = Limiter(
    key_func=get_remote_address,
    storage_uri=settings.rate_limit_storage_uri,
)


@asynccontextmanager
async def lifespan(_: FastAPI):
    Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(title="WantokPay Authentication API", version="1.0.0", lifespan=lifespan)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)
bearer_scheme = HTTPBearer(auto_error=False)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@app.get("/health")
def health(db: Session = Depends(get_db)):
    db.execute(text("SELECT 1"))
    return {"status": "ok", "database": "connected"}


@app.post("/auth/register", response_model=AccountCreated, status_code=status.HTTP_201_CREATED)
@limiter.limit("10/hour")
def register(request: Request, payload: Registration, db: Session = Depends(get_db)):
    existing_user = db.scalar(
        select(User.id).where(func.lower(User.username) == payload.username.lower())
    )
    if existing_user is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Username is unavailable.")

    user = User(username=payload.username, password_hash=hash_password(payload.password))
    db.add(user)
    try:
        db.commit()
        db.refresh(user)
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Username is unavailable.")
    return user


@app.post("/auth/login", response_model=LoginResponse)
@limiter.limit("5/minute")
def login(request: Request, payload: Credentials, db: Session = Depends(get_db)):
    user = db.scalar(
        select(User).where(func.lower(User.username) == payload.username.lower())
    )
    password_is_valid = verify_password(
        payload.password, user.password_hash if user is not None else None
    )
    if user is None or not password_is_valid or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    access_token, expires_in = create_access_token(user.id)
    return LoginResponse(
        access_token=access_token,
        expires_in=expires_in,
        username=user.username,
        user_id=user.id,
    )


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    unauthorized = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or expired access token.",
        headers={"WWW-Authenticate": "Bearer"},
    )
    if credentials is None:
        raise unauthorized
    try:
        user_id: UUID = decode_access_token(credentials.credentials)
    except (jwt.InvalidTokenError, ValueError):
        raise unauthorized

    user = db.get(User, user_id)
    if user is None or not user.is_active:
        raise unauthorized
    return user


@app.get("/auth/me", response_model=CurrentAccount)
def get_me(user: User = Depends(get_current_user)):
    return user


@app.get("/auth/me/data", response_model=AccountDataResponse)
def get_account_data(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    account_data = db.get(AccountData, user.id)
    if account_data is None:
        return AccountDataResponse(data={})
    return AccountDataResponse(data=account_data.data, updated_at=account_data.updated_at)


@app.put("/auth/me/data", response_model=AccountDataResponse)
def update_account_data(
    payload: AccountDataUpdate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    account_data = db.get(AccountData, user.id)
    if account_data is None:
        account_data = AccountData(user_id=user.id, data=payload.data)
        db.add(account_data)
    else:
        account_data.data = payload.data
        account_data.updated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(account_data)
    return AccountDataResponse(data=account_data.data, updated_at=account_data.updated_at)
