#!/bin/sh
# Explicit local entrypoints. No automatic schema migration, catalogue import or package installation.
set -eu
dev_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$dev_root"
case "${1:-}" in
 backend|backend-built)
  : "${DB_URL:?Set explicit local DB_URL}"
  : "${DB_USERNAME:?Set DB_USERNAME}"
  : "${DB_PASSWORD:?Set DB_PASSWORD privately}"
  case "$DB_URL" in jdbc:postgresql://127.0.0.1:*/*|jdbc:postgresql://localhost:*/*) ;; *) echo 'Local backend requires an explicit loopback PostgreSQL URL.' >&2; exit 2;; esac
  export SPRING_PROFILES_ACTIVE=local
  if [ "$1" = backend-built ]; then
    dev_jar=$(node deploy/stage-backend.mjs)
    exec java -jar "$dev_jar"
  fi
  exec ./backend/mvnw -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local;;
 frontend) exec npm --prefix frontend run dev;;
 gateway) exec node deploy/server.mjs;;
 health)
  exec node --input-type=module -e 'for(const [path,key] of [["/actuator/health","status"],["/api/auth/csrf","token"]]){const response=await fetch("http://127.0.0.1:8080"+path,{signal:AbortSignal.timeout(5000)});if(!response.ok)throw Error("Local backend check failed");const body=await response.json();if(key==="status"?body.status!=="UP":typeof body.token!=="string")throw Error("Local backend check failed")}console.log("Backend/database health and CSRF: PASS. Verify catalogues via authenticated capabilities before recording.")';;
 *) echo 'Usage: scripts/dev.sh backend|backend-built|frontend|gateway|health' >&2; exit 2;;
esac
