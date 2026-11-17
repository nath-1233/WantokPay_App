def test_registration_and_login_issue_access_token(client):
    registration = client.post(
        "/auth/register",
        json={"username": "samuel.k", "password": "WantokPay#2026"},
    )
    assert registration.status_code == 201
    assert registration.json()["username"] == "samuel.k"
    assert "password" not in registration.json()

    login = client.post(
        "/auth/login",
        json={"username": "SAMUEL.K", "password": "WantokPay#2026"},
    )
    assert login.status_code == 200
    assert login.json()["token_type"] == "bearer"
    assert login.json()["access_token"]
    assert login.json()["user_id"] == registration.json()["id"]

    account = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {login.json()['access_token']}"},
    )
    assert account.status_code == 200
    assert account.json()["username"] == "samuel.k"


def test_wrong_password_and_unknown_username_have_same_response(client):
    client.post(
        "/auth/register",
        json={"username": "samuel.k", "password": "WantokPay#2026"},
    )

    wrong_password = client.post(
        "/auth/login",
        json={"username": "samuel.k", "password": "wrong-password"},
    )
    unknown_user = client.post(
        "/auth/login",
        json={"username": "not-a-user", "password": "wrong-password"},
    )
    assert wrong_password.status_code == 401
    assert unknown_user.status_code == 401
    assert wrong_password.json() == unknown_user.json()


def test_registration_rejects_weak_password_and_case_insensitive_duplicate(client):
    weak_password = client.post(
        "/auth/register",
        json={"username": "samuel.k", "password": "short"},
    )
    assert weak_password.status_code == 422

    client.post(
        "/auth/register",
        json={"username": "samuel.k", "password": "WantokPay#2026"},
    )
    duplicate = client.post(
        "/auth/register",
        json={"username": "SAMUEL.K", "password": "OtherPassword#2026"},
    )
    assert duplicate.status_code == 409


def test_account_endpoint_requires_a_valid_token(client):
    missing_token = client.get("/auth/me")
    invalid_token = client.get(
        "/auth/me",
        headers={"Authorization": "Bearer invalid"},
    )
    assert missing_token.status_code == 401
    assert invalid_token.status_code == 401


def test_account_data_is_private_and_persists(client):
    registration = client.post(
        "/auth/register",
        json={"username": "samuel.k", "password": "WantokPay#2026"},
    )
    login = client.post(
        "/auth/login",
        json={"username": "samuel.k", "password": "WantokPay#2026"},
    )
    token = login.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    account_data = {
        "version": 1,
        "primaryWalletBalance": 0.0,
        "transactions": [],
        "inventoryItems": [{"name": "Rice", "stockQuantity": 3}],
    }
    saved = client.put(
        "/auth/me/data",
        json={"data": account_data},
        headers=headers,
    )
    loaded = client.get("/auth/me/data", headers=headers)
    assert saved.status_code == 200
    assert saved.json()["data"] == account_data
    assert loaded.json()["data"] == account_data

    other_registration = client.post(
        "/auth/register",
        json={"username": "another.user", "password": "AnotherPass#2026"},
    )
    other_login = client.post(
        "/auth/login",
        json={"username": "another.user", "password": "AnotherPass#2026"},
    )
    other_data = client.get(
        "/auth/me/data",
        headers={"Authorization": f"Bearer {other_login.json()['access_token']}"},
    )
    assert other_registration.status_code == 201
    assert other_data.json()["data"] == {}


def test_account_data_rejects_authentication_secrets(client):
    client.post(
        "/auth/register",
        json={"username": "samuel.k", "password": "WantokPay#2026"},
    )
    login = client.post(
        "/auth/login",
        json={"username": "samuel.k", "password": "WantokPay#2026"},
    )
    response = client.put(
        "/auth/me/data",
        json={"data": {"profile": {"password": "not-allowed"}}},
        headers={"Authorization": f"Bearer {login.json()['access_token']}"},
    )
    assert response.status_code == 422


def test_neon_postgres_urls_are_normalized():
    from pydantic import SecretStr

    from app.config import Settings

    settings = Settings(
        _env_file=None,
        database_url="postgres://user:password@ep-example.neon.tech/wantokpay",
        secret_key=SecretStr("test-only-signing-key-that-must-never-be-used"),
    )
    assert settings.database_url.startswith("postgresql+psycopg://")
    assert "sslmode=require" in settings.database_url


def test_health_checks_database_connection(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "database": "connected"}
