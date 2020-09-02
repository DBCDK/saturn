import requests
import json

def requests_post(url, data):
    requests.post(url, data=json.dumps(data, indent=2), headers={"Content-Type": "application/json"}).raise_for_status()

def requests_get(url):
    response = requests.get(url)
    response.raise_for_status()
    return response.json()