"""Empaqueta únicamente el JAR compilado, el frontend dist y scripts públicos."""
import argparse
import hashlib
from pathlib import Path
import tarfile

parser = argparse.ArgumentParser()
parser.add_argument('--jar', type=Path, required=True)
parser.add_argument('--frontend', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
assert args.jar.is_file() and args.jar.stat().st_size > 1_000_000, 'Falta el JAR ejecutable de Spring Boot'
assert (args.frontend / 'index.html').is_file(), 'Falta el frontend compilado'
args.output.parent.mkdir(parents=True, exist_ok=True)

def public_metadata(info):
    info.uid = info.gid = 0
    info.uname = info.gname = 'root'
    info.mode = 0o755 if info.isdir() or info.name.endswith('.sh') else 0o644
    return info

root = Path(__file__).resolve().parent
with tarfile.open(args.output, 'w:gz') as archive:
    archive.add(args.jar, arcname='backend.jar', filter=public_metadata)
    archive.add(args.frontend, arcname='frontend', filter=public_metadata)
    for name in ['nginx.conf.template', 'configure-runtime.py', 'start-containers.sh', 'backup.sh']:
        archive.add(root / name, arcname='aws/' + name, filter=public_metadata)
digest = hashlib.file_digest(args.output.open('rb'), 'sha256').hexdigest()
args.output.with_suffix(args.output.suffix + '.sha256').write_text(digest + '\n')
print(f'Paquete: {args.output.name}; bytes: {args.output.stat().st_size}; SHA256: {digest}')
