import os
import itertools
import sys
from pathlib import Path

os.environ.setdefault("DATABASE_URL", "sqlite://")
os.environ.setdefault(
    "SECRET_KEY",
    "test-only-signing-key-that-must-never-be-used-in-production",
)
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import pytest
from fastapi.testclient import TestClient

from app.database import Base, engine
from app.main import app

_client_ips = itertools.count(10)


@pytest.fixture
def client():
    Base.metadata.drop_all(bind=engine)
    with TestClient(app, client=(f"127.0.0.{next(_client_ips)}", 8000)) as test_client:
        yield test_client
    Base.metadata.drop_all(bind=engine)
