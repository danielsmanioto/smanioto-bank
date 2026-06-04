#!/usr/bin/env bash
cd "$(dirname "$0")"
echo "Frontend disponível em http://localhost:3000"
python3 -m http.server 3000
