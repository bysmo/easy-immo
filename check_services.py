import urllib.request
import json

services = {
    "config-server": "http://localhost:8888/actuator/health",
    "eureka-server": "http://localhost:8761/actuator/health",
    "api-gateway": "http://localhost:8080/actuator/health",
    "tenant-service": "http://localhost:8082/actuator/health",
    "property-service": "http://localhost:8083/actuator/health",
    "lease-service": "http://localhost:8084/actuator/health",
    "keycloak": "http://localhost:8180/realms/easy-immo/.well-known/openid-configuration"
}

print("=== CHECKING EASY-IMMO SERVICES ===")
for name, url in services.items():
    try:
        req = urllib.request.Request(url, method="GET")
        with urllib.request.urlopen(req, timeout=3) as response:
            status = response.getcode()
            body = response.read().decode('utf-8')
            print(f"{name}: UP (HTTP {status})")
    except Exception as e:
        print(f"{name}: DOWN ({str(e)})")

print("\n=== EUREKA APPS ===")
try:
    req = urllib.request.Request("http://localhost:8761/eureka/apps", headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=3) as response:
        data = json.loads(response.read().decode('utf-8'))
        apps = data.get("applications", {}).get("application", [])
        for app in apps:
            app_name = app.get("name")
            instances = [inst.get("instanceId") for inst in app.get("instance", [])]
            print(f"- {app_name}: {len(instances)} instance(s) {instances}")
except Exception as e:
    print(f"Failed to fetch Eureka apps: {str(e)}")
