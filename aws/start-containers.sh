#!/bin/bash
set -euo pipefail
cd /opt/ecoanalytics
docker network inspect ecoanalytics >/dev/null 2>&1 || docker network create ecoanalytics
mkdir -p /opt/ecoanalytics/mongo-data
chmod 700 /opt/ecoanalytics

# No hay puertos publicados para MongoDB ni para Java.
docker run -d --name ecoanalytics-mongo --restart unless-stopped \
  --network ecoanalytics --memory 650m --log-opt max-size=10m --log-opt max-file=3 \
  --env-file mongo.env -v /opt/ecoanalytics/mongo-data:/data/db \
  -v /opt/ecoanalytics/mongo-init.js:/docker-entrypoint-initdb.d/01-user.js:ro \
  -v /opt/ecoanalytics/backup.yml:/run/backup.yml:ro \
  -v /opt/ecoanalytics/mongo-root-password:/run/root-password:ro \
  mongo:8.0 --wiredTigerCacheSizeGB 0.25

for attempt in $(seq 1 90); do
  if docker exec ecoanalytics-mongo mongosh --quiet --eval 'db.adminCommand({ping:1}).ok' >/dev/null 2>&1; then break; fi
  sleep 2
done
docker run -d --name ecoanalytics-api --restart unless-stopped \
  --network ecoanalytics --memory 1050m --log-opt max-size=10m --log-opt max-file=3 \
  --env-file api.env -v /opt/ecoanalytics/release/backend.jar:/app/backend.jar:ro \
  eclipse-temurin:25-jre \
  java -Xms128m -Xmx640m -Duser.timezone=America/Bogota -jar /app/backend.jar

docker run -d --name ecoanalytics-web --restart unless-stopped \
  --network ecoanalytics --memory 96m --log-opt max-size=10m --log-opt max-file=3 \
  -p 80:80 -v /opt/ecoanalytics/release/frontend:/usr/share/nginx/html:ro \
  -v /opt/ecoanalytics/nginx.conf:/etc/nginx/conf.d/default.conf:ro nginx:stable-alpine
docker exec ecoanalytics-web nginx -t
echo 'Contenedores iniciados; comprobar disponibilidad antes de declarar el despliegue listo.'
