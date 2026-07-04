#!/bin/bash

# Easy-Immo Development Environment Starter Script
# Tous les services tournent dans Docker (pas de mvn spring-boot:run local)

set -e  # Arrêter en cas d'erreur critique

echo "================================================================="
echo "🏠 Starting Easy-Immo SaaS Platform Development Environment..."
echo "================================================================="

# 1. Aller dans le répertoire du projet
cd "$(dirname "$0")"

# 2. Vérifier que Docker est lancé
if ! docker info > /dev/null 2>&1; then
  echo "❌ Docker n'est pas démarré. Lance Docker Desktop d'abord."
  exit 1
fi

# 3. Vérifier que le .env existe
if [ ! -f .env ]; then
  echo "📝 Création du fichier .env depuis .env.example..."
  cp .env.example .env
fi

# 4. Arrêter les anciens processus Maven sur les ports clés (si lancés en dehors de Docker)
echo "🧹 Nettoyage des éventuels processus locaux sur les ports Spring..."
for PORT in 8888 8761 8080 8082 8083 8084 8085 8086 8087 8089; do
  PID=$(lsof -t -i:$PORT 2>/dev/null || true)
  if [ ! -z "$PID" ]; then
    echo "  → Arrêt du processus sur port $PORT (PID $PID)..."
    kill -9 $PID 2>/dev/null || true
  fi
done

# 5. Démarrer l'infrastructure de base
echo ""
echo "🐳 [1/4] Démarrage de l'infrastructure (BDD, Keycloak, Redis, RabbitMQ...)..."
docker compose up -d \
  db-keycloak keycloak \
  redis rabbitmq minio zipkin mailpit \
  prometheus grafana traefik \
  db-tenant db-property db-lease db-payment db-billing db-document

if [ $? -ne 0 ]; then
  echo "❌ Erreur Docker Compose. Vérifie que Docker Desktop est démarré."
  exit 1
fi

# 6. Attendre Keycloak
echo ""
echo "⏳ [2/4] Attente de Keycloak (30-60 secondes)..."
RETRY=0
until curl -s http://localhost:8180 > /dev/null 2>&1; do
  sleep 3
  RETRY=$((RETRY+1))
  echo -n "."
  if [ $RETRY -gt 30 ]; then
    echo ""
    echo "⚠️  Keycloak met du temps à démarrer, on continue quand même..."
    break
  fi
done
echo ""
echo "✅ Keycloak prêt!"

# 7. Construire et démarrer les microservices Spring Boot via Docker
echo ""
echo "🔨 [3/4] Build et démarrage des microservices Spring Boot..."
echo "   (Le build peut prendre 3-5 minutes la première fois)"
echo ""

docker compose up -d --build \
  config-server \
  eureka-server \
  api-gateway \
  tenant-service \
  property-service \
  lease-service \
  payment-service \
  document-service \
  notification-service \
  billing-service

if [ $? -ne 0 ]; then
  echo "❌ Erreur lors du build/démarrage des microservices."
  echo "   Consulte les logs: docker compose logs <service>"
  exit 1
fi

echo ""
echo "✅ Tous les microservices lancés dans Docker!"

# 8. Attendre que Eureka soit prêt
echo ""
echo "⏳ Attente de l'enregistrement dans Eureka..."
RETRY=0
until curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; do
  sleep 3
  RETRY=$((RETRY+1))
  echo -n "."
  if [ $RETRY -gt 40 ]; then
    echo ""
    echo "⚠️  Eureka pas encore prêt, les services vont s'enregistrer sous peu..."
    break
  fi
done
echo ""

# 9. Démarrer le frontend (Vite dev server local)
echo ""
echo "🖥️  [4/4] Démarrage du Frontend Vue.js..."
echo ""

cd web-app

# Installer les dépendances si nécessaire
if [ ! -d "node_modules" ]; then
  echo "📦 Installation des dépendances npm..."
  npm install
fi

echo ""
echo "================================================================="
echo "✅ Easy-Immo est prêt!"
echo ""
echo "   🌐 Frontend:       http://localhost:5173"
echo "   🔐 Keycloak:       http://localhost:8180  (admin / admin)"
echo "   📡 Eureka:         http://localhost:8761"
echo "   🚪 API Gateway:    http://localhost:8080"
echo "   🐇 RabbitMQ UI:    http://localhost:15672 (easy / easy123)"
echo "   📊 Grafana:        http://localhost:3001  (admin / admin123)"
echo ""
echo "   🔑 Connexion app:  superadmin / Admin@12345"
echo "       (Si premier lancement, changer le password dans Keycloak)"
echo ""
echo "   📋 Logs Docker:    docker compose logs -f <service>"
echo "   📋 Status Docker:  docker compose ps"
echo "================================================================="
echo ""
cd web-app
npm run build
npm run dev
