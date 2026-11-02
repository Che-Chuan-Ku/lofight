#!/bin/bash
# Container entrypoint: adapts PaaS-style env vars (Render) to the Spring Boot settings in application.yml.
#   DATABASE_URL=postgres[ql]://user:pass@host[:port]/db[?params]  ->  DB_URL (jdbc:postgresql://...), DB_USERNAME, DB_PASSWORD
#   PORT                                                          ->  SERVER_PORT
# Explicit DB_URL / DB_USERNAME / DB_PASSWORD / SERVER_PORT always win (local docker-compose sets DB_* directly).
set -eu

# Decode %XX escapes in URL userinfo (bash printf %b handles \xHH; dash does not).
urldecode() {
  printf '%b' "$(printf '%s' "$1" | sed 's/%\([0-9A-Fa-f][0-9A-Fa-f]\)/\\x\1/g')"
}

if [ -n "${DATABASE_URL:-}" ] && [ -z "${DB_URL:-}" ]; then
  rest="${DATABASE_URL#*://}"              # user:pass@host:port/db?params
  case "$rest" in
    *@*)
      userinfo="${rest%%@*}"
      hostpart="${rest#*@}"
      user="${userinfo%%:*}"
      pass=""
      [ "$userinfo" != "$user" ] && pass="${userinfo#*:}"
      : "${DB_USERNAME:=$(urldecode "$user")}"
      : "${DB_PASSWORD:=$(urldecode "$pass")}"
      export DB_USERNAME DB_PASSWORD
      ;;
    *)
      hostpart="$rest"
      ;;
  esac
  DB_URL="jdbc:postgresql://${hostpart}"
  export DB_URL
  echo "entrypoint: DB_URL derived from DATABASE_URL (host=${hostpart%%/*})"
fi

if [ -n "${PORT:-}" ] && [ -z "${SERVER_PORT:-}" ]; then
  SERVER_PORT="$PORT"
  export SERVER_PORT
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar "$@"
