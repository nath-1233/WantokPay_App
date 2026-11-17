# WantokPay API

This service stores account usernames and Argon2id password hashes in PostgreSQL. It provides registration, username/password sign-in, short-lived bearer tokens, and authenticated endpoints for account data. It never stores plaintext passwords.

The Android app saves its account profile, wallet display values, transaction history, bank link status and masked reference, merchants, community groups, agents, scheduled payments, inventory, invoices, bookkeeping, and disputes to a private per-account JSON record. On sign-in, the app reloads that record. Access is scoped by the signed-in account and credential fields, PINs, OTPs, and tokens are rejected from stored account data.

**This is app-data persistence, not a payment processor.** Wallet balances and transactions are still controlled by the Android prototype and are not an authoritative ledger or proof of funds. Do not use the app to send real money or store raw bank account details, card data, PINs, or OTPs.

## Deploy this API alongside the Android app on Vercel

The repository root contains `index.py` and `requirements.txt` for Vercel's FastAPI/Python runtime. Keep the Vercel project root set to this repository root; the Android Gradle project does not need to be deployed as a separate backend.

1. In the Vercel project, connect your Neon integration and add a strong, randomly generated `SECRET_KEY` (at least 32 characters) under **Settings > Environment Variables** for Production and Preview. Vercel/Neon Postgres URLs are read from `DATABASE_URL`, `POSTGRES_URL`, `POSTGRES_URL_NON_POOLING`, or `NEON_DATABASE_URL`. Vercel Postgres URLs are normalized to psycopg and TLS is required.
2. Redeploy after setting the variables. FastAPI initializes the database tables on startup.
3. Check `https://<your-vercel-domain>/health`. It should return `{"status":"ok","database":"connected"}`. Use `https://<your-vercel-domain>/docs` to inspect the API.
4. Build the Android APK with the **same deployed Vercel domain**:

   ```powershell
   .\gradlew assembleDebug "-PWANTOKPAY_API_BASE_URL=https://<your-vercel-domain>"
   ```

   The placeholder API URL in an APK cannot sign in; use your real Vercel domain.

## Run locally with Docker

1. Copy `.env.example` to `.env` and replace `POSTGRES_PASSWORD` and `SECRET_KEY`. Generate a signing key with:

   ```powershell
   python -c "import secrets; print(secrets.token_urlsafe(48))"
   ```

2. From this directory start PostgreSQL and the API:

   ```powershell
   docker compose up --build
   ```

3. Open `http://localhost:8000/docs` to try the endpoints. `POST /auth/register` creates an account; sign in using that username and password at `POST /auth/login`. There are no built-in accounts or default credentials.

The Postgres data is stored in the `wantokpay-postgres` Docker volume and survives container restarts. Back up this volume before deleting it.

## Run the API without Docker

Install PostgreSQL and create the database/user specified in `.env.example`, then create and activate a virtual environment and install the dependencies:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
```

Edit `.env` with the PostgreSQL connection URL and a newly generated `SECRET_KEY`, then start the server:

```powershell
uvicorn app.main:app --reload
```

The API creates its tables on startup. For production, use database migrations, a managed secret store, HTTPS, and a shared Redis rate-limit store when running multiple API instances. Do not expose the development `.env`, default example values, or port 8000 directly to the public internet.

## Android app

The debug APK talks to `http://10.0.2.2:8000`, which reaches the host computer from the Android emulator. Start the API before signing in. The app's **Create account** button registers users in PostgreSQL, and **Sign in** accepts their username and password.

For a physical test phone, build with the computer's reachable LAN address:

```powershell
.\gradlew assembleDebug "-PWANTOKPAY_API_BASE_URL=http://192.168.1.20:8000"
```

Set the Android API URL to your Vercel deployment. Cleartext HTTP is enabled only in the Android debug build; release builds require HTTPS. Build a release APK with:

```powershell
.\gradlew assembleRelease "-PWANTOKPAY_API_BASE_URL=https://api.example.com"
```

## Endpoints

- `GET /health` - health check
- `POST /auth/register` - `{ "username": "samuel", "password": "AtLeast12Chars#1" }`
- `POST /auth/login` - accepts the same fields and returns a short-lived access token
- `GET /auth/me` - returns the authenticated account; send `Authorization: Bearer <access_token>`
- `GET /auth/me/data` - loads the current account's saved app data
- `PUT /auth/me/data` - updates the current account's saved app data

Usernames are case-insensitive, 3-32 characters, and unique. Passwords must contain at least 12 characters, a letter, a number, and a symbol. Failed login attempts are rate-limited. Responses do not reveal whether a username exists.

## Tests

```powershell
python -m pip install -r requirements-dev.txt
python -m pytest
```

Tests use an in-memory SQLite database and do not require a running PostgreSQL server.
