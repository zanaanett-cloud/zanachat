#!/usr/bin/env bash
set -e

echo "========================================"
echo " PREPARE ANDROID - KELUARGA ZANA"
echo "========================================"

if [ ! -d node_modules ]; then
  echo "node_modules belum ada."
  echo "Menjalankan npm install..."
  npm install
fi

echo
echo "=== Capacitor Version ==="
npx cap --version

echo
echo "=== Sync Android ==="

if [ ! -d android ]; then
  echo "Membuat project Android..."
  npx cap add android
else
  echo "Project Android sudah ada."
fi

npx cap sync android

echo
echo "=== SELESAI PREPARE ANDROID ==="
