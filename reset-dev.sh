#!/bin/bash
# Script pour redémarrer Easy-Immo proprement depuis zéro

cd "$(dirname "$0")"

echo "🛑 Arrêt et suppression de tous les conteneurs Easy-Immo..."
docker compose down -v --remove-orphans 2>/dev/null || true

echo ""
echo "🗑️  Suppression des images buildées (microservices)..."
docker images --format "{{.Repository}}:{{.Tag}}" | grep "easy-immo" | xargs docker rmi -f 2>/dev/null || true

# Supprimer les images buildées par docker compose
docker compose images -q 2>/dev/null | xargs docker rmi -f 2>/dev/null || true

echo ""
echo "✅ Nettoyage terminé. Lance maintenant: ./start-dev.sh"
