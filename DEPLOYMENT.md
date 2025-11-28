# Deployment notes

This document describes recommended steps and environment variables to deploy the application in production.

1) Prepare environment variables
   - Copy `.env.example` to `.env` and fill in production values (DB host, credentials, token secret, CORS origins, etc.).
   - Store secrets (DB password, token secret) in a secure place (Vault, AWS Secrets Manager, etc.) and inject them at runtime.

2) Build the application

```powershell
mvn -DskipTests package
```

3) Docker image (example)

Build:

```powershell
docker build -t physio-manager:latest .
```

Run (using `.env`):

```powershell
docker run --env-file .env -p 8080:8080 physio-manager:latest
```

4) Docker Compose (production example)

If you prefer docker-compose, see `docker-compose.prod.yml` which uses an env_file to load `.env`.

5) CORS

- Configure `CORS_ALLOWED_ORIGINS` in your `.env` to list your frontend origin(s), e.g. `https://app.example.com`.
- Avoid using `*` in production, particularly when `CORS_ALLOW_CREDENTIALS=true`.

6) Logging & monitoring

- Configure log level to INFO in production, and send logs to a centralized system.

7) Health checks & readiness

- Expose a health/readiness endpoint (if not present) and wire it to your platform (Docker/Kubernetes) for graceful restarts.

8) Backups

- Ensure DB backups are scheduled and tested.

If you'd like, I can also generate Kubernetes manifests (Deployment/Service/Ingress) or a cloud-specific deployment guide (AWS/GCP/Azure).
