#!/bin/bash
# Plantilla de User Data. Reemplazar bucket y SHA antes de crear la instancia.
set -euo pipefail
umask 077
export AWS_DEFAULT_REGION=us-east-1
exec > >(tee -a /var/log/ecoanalytics-bootstrap.log) 2>&1
dnf install -y docker cronie
systemctl enable --now docker crond
install -d -m 0700 /opt/ecoanalytics
cd /opt/ecoanalytics
aws s3 cp s3://__BUCKET__/releases/ecoanalytics-demo.tar.gz release.tar.gz --only-show-errors
echo '__RELEASE_SHA256__  release.tar.gz' | sha256sum -c -
mkdir -p release
tar --no-same-owner -xzf release.tar.gz -C release
aws ssm get-parameter --name /ecoanalytics/demo/runtime --with-decryption --query Parameter.Value --output text > runtime.private.json

ready=0
for attempt in $(seq 1 90); do
  if aws s3 cp s3://__BUCKET__/config/deployment.json deployment.json --only-show-errors; then ready=1; break; fi
  sleep 10
done
test "$ready" = 1
python3 release/aws/configure-runtime.py
bash release/aws/start-containers.sh
install -m 0700 release/aws/backup.sh /opt/ecoanalytics/backup.sh
echo '0 9 * * * root AWS_DEFAULT_REGION=us-east-1 /opt/ecoanalytics/backup.sh >> /var/log/ecoanalytics-backup.log 2>&1' > /etc/cron.d/ecoanalytics-backup
chmod 0600 /etc/cron.d/ecoanalytics-backup
echo 'Bootstrap EcoAnalytics completado.'
