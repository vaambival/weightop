#!/usr/bin/env bash
#
# Smoke test for Comments API.
# Requires: curl, bash.
# Assumes: containers are running.
#
set -uo pipefail

# ---------- Config ----------
BASE_URL="${BASE_URL:-http://localhost:8080}"
API_URL="${BASE_URL}/api/v1"

# ---------- Colors ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

FAILED=0

# ---------- Helper ----------
# check <expected> <method> <url> [body]
check() {
    local expected="$1"
    local method="$2"
    local url="$3"
    local body="${4:-}"

    local code
    if [[ -n "$body" ]]; then
        code=$(curl -s -o /dev/null -w "%{http_code}" \
            -X "$method" "$url" \
            -H "accept: application/json" \
            -H "Content-Type: application/json" \
            -d "$body")
    else
        code=$(curl -s -o /dev/null -w "%{http_code}" \
            -X "$method" "$url" \
            -H "accept: application/json")
    fi

    if [[ "$code" == "$expected" ]]; then
        echo -e "${GREEN}[OK ]${NC} $method $url -> $code"
    else
        echo -e "${RED}[FAIL]${NC} $method $url -> expected $expected, got $code"
        FAILED=1
    fi
}

# ---------- Run ----------
echo -e "${CYAN}Smoke test: $API_URL${NC}"
echo ""

# Read endpoints
check 200 GET "$API_URL/posts/100/comments"
check 200 GET "$API_URL/posts/999999999/comments"

# Not found
check 404 GET "$API_URL/comments/999999999"

# Update not found
check 404 PUT "$API_URL/comments/999999999" '{"text":"Updated text"}'

# Likes not found
check 404 POST   "$API_URL/comments/999999999/like"
check 404 DELETE "$API_URL/comments/999999999/like"

# Delete idempotency
check 204 DELETE "$API_URL/comments/999999999"

# Create
check 201 POST "$API_URL/comments" '{"author":1,"postId":100,"text":"Smoke test"}'

# ---------- Result ----------
echo ""
if [[ "$FAILED" -eq 1 ]]; then
    echo -e "${RED}Smoke test FAILED${NC}"
    exit 1
fi
echo -e "${GREEN}Smoke test passed${NC}"
exit 0