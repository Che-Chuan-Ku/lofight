#!/bin/bash
# Container entrypoint: adapts PaaS-style env vars (Render) to the Spring Boot settings in application.yml.
#   DATABASE_URL=postgres[ql]://user:pass@host[:port]/db[?params]  ->  DB_URL (jdbc:postgresql://...), DB_USERNAME, DB_PASSWORD
#   PORT                                                          ->  SERVER_PORT
#   GOOGLE_CLIENT_ID                                              ->  /app/static/index.html (meta google-client-id)
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

# Front-end page: render the build's index.html template with the runtime Google client id.
# The placeholder lives only in <meta name="google-client-id" content="__GOOGLE_CLIENT_ID__">; an empty or
# malformed id renders content="" and the page hides its login entries.
if [ -f /app/web/index.html ]; then
  client_id="${GOOGLE_CLIENT_ID:-}"
  case "$client_id" in
    *[!A-Za-z0-9._-]*)
      echo "entrypoint: GOOGLE_CLIENT_ID has unexpected characters; login stays hidden" >&2
      client_id=""
      ;;
  esac
  sed 's|<meta name="google-client-id" content="__GOOGLE_CLIENT_ID__">|<meta name="google-client-id" content="'"$client_id"'">|' \
    /app/web/index.html > /app/static/index.html
  if [ -n "$client_id" ]; then
    echo "entrypoint: index.html rendered with GOOGLE_CLIENT_ID"
  else
    echo "entrypoint: index.html rendered without GOOGLE_CLIENT_ID (login hidden)"
  fi
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar "$@"
