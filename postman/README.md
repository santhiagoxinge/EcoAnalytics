# EcoAnalytics en Postman

Estas colecciones permiten enviar peticiones al backend real de EcoAnalytics y comprobar sus respuestas sin depender de la interfaz web. Postman es el cliente de pruebas; la aplicación continúa ejecutándose en Java con MongoDB.

## Archivos

| Archivo | Uso |
|---|---|
| `EcoAnalytics.postman_collection.json` | 39 peticiones: consultas, autenticación, reportes y casos de error. |
| `EcoAnalytics-Ingesta.postman_collection.json` | 5 peticiones ordenadas para cargar, revisar y confirmar un CSV. |
| `EcoAnalytics-AWS.postman_environment.json` | Entorno público para la instalación HTTPS en AWS, con contraseñas vacías. |
| `EcoAnalytics-Local.postman_environment.json` | Plantilla de entorno para `http://localhost:8080`, con contraseñas vacías. |
| `datos/mediciones-ejemplo.csv` | Tres mediciones válidas para Estación Centro. |
| `evidencias/RESUMEN.md` | Resultado de la ejecución local comprobada. |

## Probar el despliegue AWS

Importar `EcoAnalytics-AWS.postman_environment.json` y seleccionar **EcoAnalytics — AWS**. Su `base_url` es `https://dsjd5zau1tr4h.cloudfront.net`. Completar localmente las contraseñas demo y ejecutar las mismas colecciones en orden. En este equipo se dejó además `EcoAnalytics-AWS.local.json`, privado y excluido de Git.

El 24/11/2026 las dos colecciones se ejecutaron contra AWS: **44 peticiones y 107 comprobaciones aprobadas**, sin fallos. Consultar la [evidencia AWS](../aws/evidencias/RESUMEN.md).

## Preparación local

1. Mantener MongoDB disponible en `localhost:27017` y ejecutar el backend de EcoAnalytics en el puerto 8080. El frontend no es necesario para estas pruebas.
2. En Postman, pulsar **Import** e importar las dos colecciones y el entorno. No importar reportes privados.
3. Seleccionar el entorno **EcoAnalytics — Local**.
4. Completar los valores locales de `clave_admin`, `clave_analista` y `clave_ciudadano` con las contraseñas de las cuentas demo de la aplicación. Los correos ya están definidos en el entorno. Guardar las contraseñas únicamente como valores locales.
5. Comprobar `fecha_referencia`. El valor `2026-10-31` se verificó con el histórico de esta base demo. Si se utiliza otra base, elegir una fecha con mediciones suficientes durante los catorce días anteriores. La colección calcula los períodos a partir de este valor.

Se dejó además `EcoAnalytics-Local.local.json` en esta carpeta local, con los valores demo preparados. Este archivo está excluido de Git; se puede importar localmente para evitar introducirlos otra vez.

## Ejecutar la verificación

Abrir **EcoAnalytics — Verificación de API**, pulsar **Run** y ejecutar las carpetas en su orden original, con una iteración y el entorno local seleccionado. Los inicios de sesión guardan automáticamente los tres tokens JWT en variables de la colección. No hay que copiar tokens a mano.

La primera consulta selecciona una estación disponible y guarda `estacion_id`. El backend puede registrar auditoría al iniciar sesión y generar alertas al calcular pronósticos.

Los casos negativos esperan respuestas de error: un `401`, `403`, `404`, `409` o `422` puede ser el resultado correcto. Lo que debe aparecer aprobado es la comprobación de Postman, no necesariamente un estado HTTP 200 en todas las peticiones.

## Demostrar la importación CSV

1. Abrir **EcoAnalytics — Demostración de importación**.
2. En **Cargar archivo de ejemplo → Body → form-data → archivo**, seleccionar `datos/mediciones-ejemplo.csv`. Si Postman solicita acceso a archivos, autorizar este archivo local.
3. Ejecutar las cinco peticiones en orden: iniciar sesión como analista, cargar, descargar incidencias, confirmar y repetir la confirmación.

El identificador del lote se guarda automáticamente como `lote_id`. Esta demostración escribe en la base local: deja un lote y hasta tres mediciones nuevas. Si se repite con el mismo archivo, el validador puede identificar las mediciones como duplicadas. Para demostrar de nuevo una inserción nueva, utilizar otro CSV con fechas y horas todavía inexistentes, dentro del formato permitido.

## Relación con las historias de usuario

| Historia | Peticiones incluidas |
|---|---|
| HU-01 | Login de los tres roles, perfil y credenciales/campos inválidos. |
| HU-02 | Consulta de usuarios y restricciones para analista y ciudadano. |
| HU-03 | Consulta de estaciones. |
| HU-04 y HU-05 | Consulta de lotes; carga, incidencias, confirmación y rechazo de segunda confirmación. |
| HU-06 | Condiciones públicas, detalle y estación inexistente. |
| HU-07 | Histórico paginado, exportación CSV y fechas invertidas. |
| HU-08 | Comparación, exportación y rechazo por falta de datos. |
| HU-09 | Visualización multivariable. |
| HU-10 | Consulta de indicadores. |
| HU-11 | Anomalías y línea de tiempo. |
| HU-12 | Consulta de modelos, permiso restringido y entrenamiento sin algoritmo. |
| HU-13 | Pronósticos de 24/48 horas y horizonte inválido. |
| HU-14 | Banner público y centro de alertas. |
| HU-15 | Vista previa y exportación PDF, CSV y Excel. |
| HU-16 | Monitoreo técnico y auditoría. |

Esta tabla indica las operaciones probadas. No certifica todos los criterios de aceptación: faltan, entre otros, los flujos completos de administración, entrenamiento y despliegue de modelos, edición de indicadores y gestión de alertas. La calidad predictiva, el envío real de notificaciones y los requisitos no funcionales requieren verificaciones adicionales.

## Repetir mediante Postman CLI

Con Postman CLI instalado, abrir una terminal en esta carpeta:

```powershell
postman collection run EcoAnalytics.postman_collection.json -e EcoAnalytics-Local.local.json --reporters cli,json --reporter-json-export evidencias/ejecucion-privada.json --report-events=false
postman collection run EcoAnalytics-Ingesta.postman_collection.json -e EcoAnalytics-Local.local.json --reporters cli,json --reporter-json-export evidencias/ingesta-privada.json --report-events=false
```

Los reportes completos contienen solicitudes y respuestas, incluidos tokens y credenciales demo. Por eso permanecen excluidos de Git y se publica solamente el resumen sin esos valores. La ejecución por CLI de estos archivos locales se realizó sin enviar los resultados a Postman Cloud.
