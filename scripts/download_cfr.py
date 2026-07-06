import urllib.request
import os

url = 'https://github.com/leibnitz27/cfr/releases/download/0.152/cfr-0.152.jar'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})

try:
    with urllib.request.urlopen(req) as response:
        with open('cfr.jar', 'wb') as f:
            f.write(response.read())
    print("Download successful")
except Exception as e:
    print(f"Download failed: {e}")
