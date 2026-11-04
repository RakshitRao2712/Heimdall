FROM node:22-bookworm-slim AS frontend
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html vite.config.ts tsconfig*.json ./
COPY src ./src
COPY public ./public
COPY scripts ./scripts
ARG VITE_SITE_URL
RUN npm run build && npm run seo:check

FROM eclipse-temurin:21-jdk AS backend
WORKDIR /build
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
COPY backend/src ./src
RUN ./mvnw -B -DskipTests package

FROM caddy:2 AS proxy
FROM eclipse-temurin:21-jre
WORKDIR /app
# Render provides an unprivileged port. Install the binary without file capabilities.
RUN --mount=type=bind,from=proxy,source=/usr/bin/caddy,target=/tmp/caddy \
    install -m 0755 /tmp/caddy /usr/bin/caddy
COPY --from=frontend /build/dist /srv/heimdall/dist
COPY --from=backend /build/target/*.jar /app/heimdall.jar
COPY deploy/Caddyfile.render /etc/caddy/Caddyfile
COPY deploy/start-render.sh /app/start-render.sh
ENV SPRING_PROFILES_ACTIVE=production PORT=10000
EXPOSE 10000
CMD ["bash", "/app/start-render.sh"]
