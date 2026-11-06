#!/bin/bash
# Container entrypoint: adapts PaaS-style env vars (Render) to the Spring Boot settings in application.yml.
#   DATABASE_URL=postgres[ql]://user:pass@host[:port]/db[?params]  ->  DB_URL (jdbc:postgresql://...), DB_USERNAME, DB_PASSWORD
#   PORT                                                          ->  SERVER_PORT
#   GOOGLE_CLIENT_ID                                              ->  /app/static/index.html (meta google-client-id)
#   ADSENSE_PUBLISHER_ID (ca-pub-<digits>)                        ->  /app/static/index.html (meta adsense-publisher-id) + /app/static/ads.txt
#   /app/web/*.html (not index), robots.txt, sitemap.xml            ->  copied to /app/static (D41 static pages)
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

# Front-end page: render the build's index.html template with the runtime Google client id and AdSense publisher id.
# The placeholders live only in <meta name="google-client-id" content="__GOOGLE_CLIENT_ID__"> and
# <meta name="adsense-publisher-id" content="__ADSENSE_PUBLISHER_ID__">; an empty or malformed value renders content=""
# and the page hides the matching feature (login entries / rewarded-ad card, no ad script).
publisher_id="$(printf '%s' "${ADSENSE_PUBLISHER_ID:-}" | tr -d '[:space:]')"
# AdSense's account page shows the id as pub-<digits>; the ad script wants ca-pub-<digits>. Accept both.
case "$publisher_id" in pub-*) publisher_id="ca-$publisher_id" ;; esac
case "$publisher_id" in
  "") ;;
  ca-pub-*[!0-9]*|ca-pub-)
    echo "entrypoint: ADSENSE_PUBLISHER_ID is not ca-pub-<digits>; ads stay off" >&2
    publisher_id=""
    ;;
  ca-pub-*) ;;
  *)
    echo "entrypoint: ADSENSE_PUBLISHER_ID is not ca-pub-<digits>; ads stay off" >&2
    publisher_id=""
    ;;
esac

if [ -f /app/web/index.html ]; then
  client_id="${GOOGLE_CLIENT_ID:-}"
  case "$client_id" in
    *[!A-Za-z0-9._-]*)
      echo "entrypoint: GOOGLE_CLIENT_ID has unexpected characters; login stays hidden" >&2
      client_id=""
      ;;
  esac
  sed -e 's|<meta name="google-client-id" content="__GOOGLE_CLIENT_ID__">|<meta name="google-client-id" content="'"$client_id"'">|' \
      -e 's|<meta name="adsense-publisher-id" content="__ADSENSE_PUBLISHER_ID__">|<meta name="adsense-publisher-id" content="'"$publisher_id"'">|' \
    /app/web/index.html > /app/static/index.html
  if [ -n "$client_id" ]; then
    echo "entrypoint: index.html rendered with GOOGLE_CLIENT_ID"
  else
    echo "entrypoint: index.html rendered without GOOGLE_CLIENT_ID (login hidden)"
  fi
  if [ -n "$publisher_id" ]; then
    echo "entrypoint: index.html rendered with ADSENSE_PUBLISHER_ID"
  else
    echo "entrypoint: index.html rendered without ADSENSE_PUBLISHER_ID (ads off)"
  fi
fi

# D41 static pages from the build (guide / bosses / faq / about / updates / privacy .html, robots.txt, sitemap.xml): served as-is.
# index.html is excluded here; it is rendered above with the runtime ids.
for f in /app/web/*.html /app/web/robots.txt /app/web/sitemap.xml; do
  [ -f "$f" ] || continue
  [ "$(basename "$f")" = "index.html" ] && continue
  cp "$f" /app/static/
done
# Tile sheet and cropped ground/wall tiles written by scripts/build-pages.mjs (static pages + home letterbox).
if [ -d /app/web/assets ]; then
  mkdir -p /app/static/assets
  cp -a /app/web/assets/. /app/static/assets/
fi

# ads.txt at the site root (infrastructure.yml site_ads.ads_txt): only when a publisher id is set, otherwise /ads.txt is 404.
# AdSense writes the seller id without the ca- prefix; f08c47fec0942fa0 is Google's certification authority id.
if [ -n "$publisher_id" ]; then
  printf 'google.com, %s, DIRECT, f08c47fec0942fa0\n' "${publisher_id#ca-}" > /app/static/ads.txt
  echo "entrypoint: ads.txt written"
else
  rm -f /app/static/ads.txt
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar "$@"
