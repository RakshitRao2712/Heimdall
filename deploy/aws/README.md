# Heimdall AWS backend

Production API origin: `https://backend.heimdall.life`.
Cloudflare Pages serves the frontend and proxies `/api/*` to that origin.
Google's callback remains `https://heimdall.life/api/auth/google/callback`.
Neon remains the persistent database; no database migration is required.

## Server

- Region: Sydney (`ap-southeast-2`)
- Instance: `i-0bcb9a327f8d806af`, Ubuntu 24.04, `t3.micro` (1 GiB)
- CPU credits: Standard, to avoid surplus CPU charges
- Storage: 20 GiB gp3; 1 GiB swap
- Persistent public IPv4: `13.237.220.41` (`eipalloc-0ad76dd85bbbf4be8`)
- SSH key pair: `heimdall-deploy`; private key is outside the repository on the owner's laptop
- SSH restricted to the owner's IP at deployment; public TCP 80/443 only for web traffic
- Java listens on `127.0.0.1:8080`; Caddy handles HTTPS and certificate renewal
- No load balancer, NAT gateway, RDS or paid detailed monitoring

## Install or update

Build with Java 21 using `backend/mvnw -B test package` from the backend directory.
Upload the packaged JAR and install as `/opt/heimdall/heimdall.jar`, owned by `heimdall`, mode 640.
Install `heimdall.service` in `/etc/systemd/system/` and `Caddyfile` in `/etc/caddy/`.
Secrets belong in `/etc/heimdall/backend.env`, root-owned, mode 600; never commit or log them.
The environment includes `SPRING_PROFILES_ACTIVE=production`, the existing Neon settings,
Google credentials, frontend origin and Resend settings. Payment credentials are not needed
for free access and were not copied.

Use `systemctl restart heimdall`, `systemctl status heimdall`, and `journalctl -u heimdall`
over SSH to maintain the service. Both Caddy and Heimdall are enabled at boot.
Check `/api/auth/google/config` for readiness and complete a frontend Google login after changes.

## Costs and recovery

The account remains on the AWS Free plan. Resources consume its promotional credits;
free-plan access ends when credits run out or on 19 May 2027, whichever comes first.
The initial low-traffic estimate is about $15.30/month including compute, disk and one IPv4.
Check actual Billing usage rather than treating this estimate as a cap.

Render remains available as a fallback. To roll back, restore the API origin in
`functions/api/[[path]].js` to `https://heimdall-staging-wskj.onrender.com` and redeploy Pages.
An origin switch invalidates existing in-memory sessions; users sign in again.
If your home IP changes, update only the SSH source rule before attempting administration.
