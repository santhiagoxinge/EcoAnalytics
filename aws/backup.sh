#!/bin/bash
set -euo pipefail
umask 077
cd /opt/ecoanalytics
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
ARCHIVE="/opt/ecoanalytics/backup-${STAMP}.archive.gz"
BUCKET=$(python3 -c 'import json; print(json.load(open("deployment.json"))["bucket"])')
# Durante el dump se suspenden brevemente las escrituras del único backend.
docker pause ecoanalytics-api >/dev/null
trap 'docker unpause ecoanalytics-api >/dev/null 2>&1 || true' EXIT
docker exec ecoanalytics-mongo mongodump --config=/run/backup.yml --db=ecoanalytics --archive --gzip > "$ARCHIVE"
docker exec ecoanalytics-mongo mongosh --quiet --eval 'db.getSiblingDB("admin").auth("ecoanalytics_root", require("fs").readFileSync("/run/root-password", "utf8").trim()); const d=db.getSiblingDB("ecoanalytics"); print(JSON.stringify(Object.fromEntries(d.getCollectionNames().sort().map(n=>[n,d.getCollection(n).countDocuments({})]))));' > "${ARCHIVE}.manifest.json"
docker unpause ecoanalytics-api >/dev/null
trap - EXIT
aws s3 cp "$ARCHIVE" "s3://${BUCKET}/backups/${STAMP}.archive.gz" --sse AES256 --only-show-errors
sha256sum "$ARCHIVE" > "${ARCHIVE}.sha256"
aws s3 cp "${ARCHIVE}.sha256" "s3://${BUCKET}/backups/${STAMP}.archive.gz.sha256" --sse AES256 --only-show-errors
aws s3 cp "${ARCHIVE}.manifest.json" "s3://${BUCKET}/backups/${STAMP}.archive.gz.manifest.json" --sse AES256 --only-show-errors
echo "Copia completada: backups/${STAMP}.archive.gz"
