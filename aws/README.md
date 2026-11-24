# EcoAnalytics en AWS

Aplicación: **https://dsjd5zau1tr4h.cloudfront.net**. Se puede entrar con los botones de cuentas de demostración de la pantalla de acceso. La instalación utiliza datos ambientales sintéticos.

## Arquitectura instalada

El navegador accede por HTTPS a Amazon CloudFront. Una instancia EC2 `t3.small` ejecuta tres contenedores: Nginx, Java 25/Spring Boot 4.0.8 y MongoDB 8.0. Nginx sirve el frontend compilado y reenvía `/api` a Java. MongoDB conserva sus datos en un volumen EBS gp3 cifrado de 20 GiB.

Los puertos 8080 y 27017 no se publican en el host. El Security Group acepta únicamente HTTP 80 desde la lista administrada de orígenes CloudFront; Nginx exige además la cabecera privada configurada en nuestra distribución. La conexión del navegador está cifrada; CloudFront se conecta al origen mediante HTTP restringido. No se presenta esta configuración como TLS de extremo a extremo.

`/api/*` usa la política CloudFront **CachingDisabled** y reenvía la autorización, las consultas y los encabezados del cliente. Los recursos estáticos usan sus encabezados de caché de origen. Nginx resuelve las rutas de React mediante `index.html`, de modo que se puede recargar una URL como `/estaciones`.

La administración se realiza por Systems Manager, sin abrir SSH. El rol de la instancia permite leer exclusivamente los artefactos, configuración y parámetro de EcoAnalytics, y escribir en el prefijo de copias. Los secretos JWT, de MongoDB y de la cabecera de origen se obtienen de Parameter Store como `SecureString`. No se incorporan al paquete público.

La instalación es una demostración de una sola instancia y una sola zona; no implementa alta disponibilidad. Los modelos predictivos y las simulaciones se ejecutan en Java, sin un servidor Python. Correo y push continúan simulados.

## Inventario

Los identificadores públicos se conservan en [deployment.json](deployment.json).

| Recurso | Identificador |
|---|---|
| Región | `us-east-1` |
| EC2 | `i-04167941c93c0afab` — `t3.small`, 2 vCPU, 2 GiB |
| EBS gp3 cifrado | `vol-0025e8e45316594d7` — 20 GiB |
| Security Group | `sg-04aa7f452c3e79fe3` |
| Plantilla EC2 | `lt-09a5eaeccb9a65967`, versión 2 |
| CloudFront | `E38KIYWUM79W3W` |
| S3 privado | `ecoanalytics-demo-755329540997` |
| Perfil y rol EC2 | `EcoAnalyticsDemoEC2` |
| Parameter Store | `/ecoanalytics/demo/runtime` — valores privados |

Los recursos pertenecen al proyecto `ecoanalytics`. El almacenamiento y los recursos de otros proyectos de la cuenta no forman parte de este despliegue.

## Compilación y paquete

Entorno de compilación verificado: Java 25, Maven 3.10.0 y Node.js 26. Instalar Maven en `PATH` si no se dispone de un wrapper operativo. Desde las carpetas del proyecto:

```powershell
# En backend/:
mvn package

# En frontend/:
npm ci
npm run build

# Desde la raíz, con Python 3:
python aws/build_bundle.py --jar backend/target/backend-0.0.1-SNAPSHOT.jar --frontend frontend/dist --output aws/artifacts/ecoanalytics-demo.tar.gz
```

No se debe compilar sobre un JAR que esté en uso en Windows; usar una copia de compilación o detener primero ese proceso. El empaquetador comprueba que hay un JAR ejecutable y un frontend construido, incluye únicamente los archivos necesarios y produce un SHA-256.

El paquete instalado tiene SHA-256 `13a5e54ba6a27e3df95b22d7c7ee53cfabcb1d1371c1aef0f7bd5794b8367721`. El perfil `aws` exige `MONGODB_URI`, `JWT_SECRET` y `CORS_ALLOWED_ORIGINS`; la validez del JWT se configura en dos horas. El perfil local conserva su configuración de desarrollo.

## Instalación y actualización

[bootstrap.sh](bootstrap.sh) es la plantilla del inicio de EC2: instala Docker, descarga y comprueba el paquete, obtiene los secretos, prepara los archivos restringidos e inicia los contenedores. [configure-runtime.py](configure-runtime.py) genera esos archivos sin imprimir sus valores; [start-containers.sh](start-containers.sh) documenta las redes, montajes y límites de memoria.

La plantilla de lanzamiento sustituye `__BUCKET__` y `__RELEASE_SHA256__` antes de ejecutarse. S3 contiene `config/deployment.json` con la URL pública, y `releases/ecoanalytics-demo.tar.gz` con el paquete. Estos scripts de arranque están pensados para una instancia nueva: `start-containers.sh` no es un actualizador idempotente de contenedores ya creados.

Para actualizar una instalación existente, preparar y probar primero otro paquete, respaldar MongoDB y reemplazar únicamente los contenedores de aplicación/web con sus montajes; conservar los datos y la configuración privada. Validar la API y el navegador antes de dar por terminada la actualización.

## Copias y restauración comprobada

`/etc/cron.d/ecoanalytics-backup` ejecuta [backup.sh](backup.sh) diariamente a las **09:00 UTC, 04:00 de Colombia**. Se suspende brevemente el backend durante el dump para detener sus escrituras. La copia se envía a `backups/` en S3, junto con el SHA-256 y un manifiesto de conteos. La regla de ciclo de vida de ese prefijo conserva las copias durante siete días.

La primera copia verificada es `backups/20261124T050619Z.archive.gz`. Se restauró en la base separada `ecoanalytics_restore_check_20261124` y coincidieron los conteos de sus 13 colecciones, incluidas 51.365 mediciones. No se sobrescribió la base de trabajo. Esa copia es anterior a la importación CSV de tres filas realizada como prueba posterior.

Esto verifica una restauración concreta. No es una prueba de recuperación regional, conmutación automática ni disponibilidad continua. Los lotes pendientes y los modelos ajustados en memoria tienen condiciones distintas a los documentos persistidos; sus límites se explican en la wiki.

## Supervisión y apagado

Acceder a la instancia mediante Systems Manager. Los comandos de consulta útiles son `docker ps`, `docker stats --no-stream`, `docker logs ecoanalytics-api` y la lectura de `/var/log/ecoanalytics-bootstrap.log` o `/var/log/ecoanalytics-backup.log`. Los logs Docker rotan a tres archivos de 10 MB por contenedor.

Para detener voluntariamente la demostración desde una sesión AWS autorizada:

```powershell
aws ec2 stop-instances --region us-east-1 --instance-ids i-04167941c93c0afab
```

El volumen EBS y las copias se conservan; continúan sus cargos de almacenamiento. CloudFront mantiene el dominio, pero la aplicación requiere EC2 encendida. La IP pública de EC2 puede cambiar al detener/iniciar: después de arrancar se debe actualizar el dominio del origen de esta distribución, conservando sus cabeceras privadas, y actualizar `deployment.json`. No se requiere cambiar la URL pública.

El volumen está configurado para conservarse al terminar la instancia. Una eliminación completa debe contemplar por separado instancia, volumen, distribución, bucket, parámetro, plantilla, grupo de seguridad y rol de este inventario, con respaldo y autorización del responsable. Esta entrega no elimina ninguno.

## Costo y alcance de las pruebas

Estimación base con 730 horas de funcionamiento al mes, sin aplicar créditos:

| Concepto | Estimación USD/mes |
|---|---:|
| EC2 t3.small Linux — US$0,0208/h | 15,18 |
| Una dirección IPv4 — US$0,005/h | 3,65 |
| EBS gp3 — 20 GiB a US$0,08/GiB-mes | 1,60 |
| Total base aproximado | 20,43 |

A esto se añaden tráfico, solicitudes, almacenamiento S3 y otros cargos según el uso. La tarifa EC2 se consultó mediante AWS Price List API; las referencias de [IPv4](https://aws.amazon.com/vpc/pricing/) y [EBS](https://aws.amazon.com/ebs/pricing/) permiten revisar los otros componentes. La elegibilidad del tipo de instancia en el plan Free no significa que su funcionamiento sea ilimitado o siempre gratuito.

La [evidencia de validación](evidencias/RESUMEN.md) registra seis pruebas de dominio, compilación del frontend, 44 peticiones y 107 comprobaciones de Postman contra AWS, recuperación de MongoDB y revisión del navegador. No certifica todos los escenarios de requisitos no funcionales ni pruebas de carga.
