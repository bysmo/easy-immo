# 🏠 Easy-Immo

> Plateforme SaaS de gestion de location immobilière pour l'Afrique de l'Ouest (UEMOA)

## Stack Technique

| Couche | Technologie |
|---|---|
| Microservices | Java 21 + Spring Boot 3.3 |
| Service Discovery | Spring Cloud Eureka |
| API Gateway | Spring Cloud Gateway |
| Config centralisée | Spring Cloud Config Server |
| IAM / Auth | Keycloak 24 |
| Base de données | PostgreSQL 16 (une par service) |
| Cache | Redis 7 |
| Messaging | RabbitMQ 3 |
| Stockage fichiers | MinIO |
| Frontend Web | Vue.js 3 + Vite + PrimeVue |
| Application Mobile | React Native + Expo |
| Reverse Proxy | Traefik 2 |
| PDF | JasperReports |
| Supervision | Prometheus + Grafana |

## Structure du Monorepo

```
easy-immo/
├── docker-compose.yml          # Dev local
├── docker-compose.prod.yml     # Production (Docker Swarm)
├── infrastructure/             # Traefik, Keycloak, Prometheus  Grafana, config-server, eureka-server, api-gateway
│   ├── config-server/          # Configuration centralisée
│   ├── eureka-server/          # Service Discovery
│   └── api-gateway/            # API Gateway
├── config-repo/                # Configs centralisées (lues par Config Server)
├── services/                   # Microservices métier (Business)
│   ├── tenant-service/
│   ├── property-service/
│   ├── lease-service/
│   ├── payment-service/
│   ├── document-service/
│   ├── notification-service/
│   └── billing-service/
├── web-app/                    # Vue.js 3
└── mobile-app/                 # React Native + Expo
```

## Démarrage rapide (Développement)

### Prérequis
- Docker Desktop 24+
- Java 21 (JDK Temurin)
- Node.js 20+
- Maven 3.9+

### Lancer l'environnement complet

```bash
# 1. Copier les variables d'environnement
cp .env.example .env

# 2. Démarrer toute l'infrastructure
docker compose up -d

# 3. Attendre que Keycloak soit prêt (~60s) puis importer le realm
docker compose exec keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/easy-immo-realm.json

# 4. Lancer les services Spring Boot d'infrastructure
cd infrastructure/config-server && mvn spring-boot:run &
cd infrastructure/eureka-server && mvn spring-boot:run &
cd infrastructure/api-gateway && mvn spring-boot:run &

# 5. Lancer les services Spring Boot métier
cd services/tenant-service && mvn spring-boot:run &

# 6. Lancer le frontend Vue.js
cd web-app && npm install && npm run build && npm run dev
```

### URLs de développement

| Service | URL |
|---|---|
| Frontend Web | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Keycloak Admin | http://localhost:8180 (admin/admin) |
| RabbitMQ | http://localhost:15672 (easy/easy) |
| MinIO Console | http://localhost:9001 (minioadmin/minioadmin) |
| Zipkin | http://localhost:9411 |
| Mailpit (emails dev) | http://localhost:8025 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (admin/admin) |

## Pays UEMOA supportés

🇧🇯 Bénin · 🇧🇫 Burkina Faso · 🇨🇮 Côte d'Ivoire · 🇬🇼 Guinée-Bissau · 🇲🇱 Mali · 🇳🇪 Niger · 🇸🇳 Sénégal · 🇹🇬 Togo

## Licence

Propriétaire — © 2024 Easy-Immo. Tous droits réservés.
