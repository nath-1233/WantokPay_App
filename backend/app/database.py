from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker
from sqlalchemy.pool import StaticPool

from app.config import get_settings


class Base(DeclarativeBase):
    pass


settings = get_settings()
engine_options = {
    "pool_pre_ping": True,
    "connect_args": {"check_same_thread": False} if settings.database_url.startswith("sqlite") else {},
}
if settings.database_url.startswith("postgresql+psycopg://"):
    engine_options.update(pool_size=1, max_overflow=0, pool_recycle=300)
if settings.database_url in ("sqlite://", "sqlite:///:memory:"):
    engine_options["poolclass"] = StaticPool

engine = create_engine(
    settings.database_url,
    **engine_options,
)
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)
