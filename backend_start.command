#!/bin/bash
echo "=========================================="
echo "      Starting JobSeeker Backend          "
echo "=========================================="

# No global proxy for backend to ensure local Playwright/CDP and domestic scraping work normally
export PATH=/Volumes/System/03_Dev/apache-maven-3.9.9/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH

cd /Volumes/System/04_GitHub/JobSeeker

echo ">>> Pulling latest code..."
git pull

echo ">>> Stopping existing backend instances..."
pkill -f "jobsbackend" || true
pkill -f "spring-boot" || true

echo ">>> Starting backend..."
mvn spring-boot:run
