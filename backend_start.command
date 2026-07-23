#!/bin/bash
echo "=========================================="
echo "      Starting JobSeeker Backend          "
echo "=========================================="

export HTTP_PROXY=http://127.0.0.1:1080
export HTTPS_PROXY=http://127.0.0.1:1080
export ALL_PROXY=socks5://127.0.0.1:1080
export NO_PROXY="localhost,127.0.0.1,::1"
export PATH=/Volumes/System/03_Dev/apache-maven-3.9.9/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH

cd /Volumes/System/04_GitHub/JobSeeker

echo ">>> Pulling latest code..."
git pull

echo ">>> Stopping existing backend instances..."
pkill -f "jobsbackend" || true
pkill -f "spring-boot" || true

echo ">>> Starting backend..."
mvn spring-boot:run
