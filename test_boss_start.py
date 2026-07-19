import requests
import json
import time

def test_boss_start():
    print("Logging in...")
    resp = requests.post("http://127.0.0.1:8889/api/auth/login", json={"username": "codex", "password": "Codex12345"})
    if resp.status_code != 200:
        print("Login failed:", resp.text)
        return
    token = resp.json().get('token')
    print("Got token:", token[:15] + "...")
    
    headers = {"Authorization": f"Bearer {token}"}
    
    print("Testing /api/boss/start...")
    resp = requests.post("http://127.0.0.1:8889/api/boss/start", headers=headers)
    print("Status code:", resp.status_code)
    print("Response:", resp.text)

if __name__ == "__main__":
    # Wait for backend to fully start
    time.sleep(10)
    test_boss_start()
