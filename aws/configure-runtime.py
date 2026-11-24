"""Convierte configuración privada de SSM en archivos locales de permisos restringidos."""
import json
import os
from pathlib import Path

ROOT = Path('/opt/ecoanalytics')
private = json.loads((ROOT / 'runtime.private.json').read_text())
settings = json.loads((ROOT / 'deployment.json').read_text())
password = private['mongo_password']
uri = f'mongodb://ecoanalytics_app:{password}@ecoanalytics-mongo:27017/ecoanalytics?authSource=ecoanalytics'
files = {
    'api.env': f'MONGODB_URI={uri}\nJWT_SECRET={private["jwt_secret"]}\nCORS_ALLOWED_ORIGINS={settings["public_url"]}\nSPRING_PROFILES_ACTIVE=aws\nTZ=America/Bogota\n',
    'mongo.env': f'MONGO_INITDB_ROOT_USERNAME=ecoanalytics_root\nMONGO_INITDB_ROOT_PASSWORD={private["mongo_root_password"]}\nTZ=America/Bogota\n',
    'mongo-init.js': 'db = db.getSiblingDB("ecoanalytics");\ndb.createUser(' + json.dumps({'user': 'ecoanalytics_app', 'pwd': password, 'roles': [{'role': 'readWrite', 'db': 'ecoanalytics'}]}) + ');\n',
    'backup.yml': f'uri: mongodb://ecoanalytics_root@127.0.0.1:27017/ecoanalytics?authSource=admin\npassword: {private["mongo_root_password"]}\n',
    'mongo-root-password': private['mongo_root_password'],
    'nginx.conf': (ROOT / 'release/aws/nginx.conf.template').read_text().replace('__ORIGIN_SECRET__', private['origin_secret']),
}
for name, text in files.items():
    path = ROOT / name
    path.write_text(text, encoding='utf-8')
    os.chmod(path, 0o600)
    if name == 'mongo-init.js':
        # El inicializador de la imagen oficial lee este archivo como usuario mongodb.
        # Su directorio padre permanece accesible únicamente para root en el host.
        os.chmod(path, 0o644)
os.chmod(ROOT / 'runtime.private.json', 0o600)
print('Configuración privada preparada; valores omitidos del registro.')
