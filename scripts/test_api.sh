#!/bin/bash

fixture='{"word":"python","context":"Pyhthon is a programming language.","model":"cos_traditional_self"}'

example_prediction() {
  curl 'http://localhost:9000/predictSense' \
    -H 'content-type: application/json' \
    -d "$fixture" "$@" # Support passing additional params with $@
}

# Get status code: https://superuser.com/a/442395
status=$(example_prediction -s -o /dev/null -w "%{http_code}")

if [[ "$status" -eq "200" ]]
then
  echo "[OK] API seems to be fully functional."
else
  echo "[ERROR] API responded with status code '$status'."
fi

echo
echo "For more details run:"
cat <<CURLCMD
curl 'http://localhost:9000/predictSense' -H 'content-type: application/json' -d '{"word":"python","context":"Pyhthon is a programming language.","model":"cos_traditional_self"}'
CURLCMD

if [[ "$status" -eq "200" ]]; then exit; else exit 1; fi





