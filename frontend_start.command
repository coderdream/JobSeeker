#!/bin/bash
echo "=========================================="
echo "      Starting JobSeeker Frontend         "
echo "=========================================="

export HTTP_PROXY=http://127.0.0.1:1080
export HTTPS_PROXY=http://127.0.0.1:1080
export ALL_PROXY=socks5://127.0.0.1:1080
export NO_PROXY="localhost,127.0.0.1,::1"
# Node & npm usually in /usr/local/bin or via nvm/volta
export PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH

cd /Volumes/System/04_GitHub/JobSeeker

echo ">>> Stopping existing frontend instances..."
lsof -ti:6866 | xargs kill -9 2>/dev/null || true

echo ">>> Pulling latest code..."
git pull

cd front

echo ">>> Installing dependencies..."
npm install

echo ">>> Starting frontend..."
npm run dev
