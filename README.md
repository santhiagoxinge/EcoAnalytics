# EcoAnalytics

Sistema de análisis y monitoreo ambiental con Machine Learning, desarrollado como proyecto académico de la asignatura Diseño de Software de la Universidad Cooperativa de Colombia, campus Pasto.

El proyecto aborda la gestión, validación, análisis y visualización de información de calidad del aire, con capacidades de pronóstico y generación de alertas.

## Aplicación desplegada

**[Abrir EcoAnalytics en AWS](https://dsjd5zau1tr4h.cloudfront.net)**. La pantalla de acceso ofrece cuentas de demostración de Administrador, Analista y Ciudadano. Los datos ambientales son sintéticos.

La instalación utiliza CloudFront, una EC2 t3.small y contenedores Nginx, Java 25/Spring Boot 4.0.8 y **MongoDB 8.0**. La persistencia reside en EBS cifrado; las copias diarias se conservan en S3 privado. Consulta la [guía operativa y costos](aws/README.md) y la [evidencia de validación](aws/evidencias/RESUMEN.md).

## Código y ejecución local

El [backend](backend/src/main/java/com/ecoanalytics) contiene las reglas de negocio, API y persistencia. El [frontend](frontend/src) utiliza React y TypeScript. La [guía de MongoDB](database/README.md) describe el diseño documental.

Entorno verificado: Java 25, Maven 3.10.0, Node.js 26 y MongoDB disponible en el puerto 27017. Usar terminales separadas:

```powershell
# En backend/:
mvn package
java -jar target/backend-0.0.1-SNAPSHOT.jar

# En frontend/:
npm ci
npm run dev
```

El frontend local está en `http://localhost:5173` y su proxy dirige `/api` al backend 8080. El perfil AWS exige secretos externos; no se deben utilizar los valores de desarrollo como configuración de un despliegue real.

## Documentación

Consulta la [wiki de EcoAnalytics](https://github.com/santhiagoxinge/EcoAnalytics/wiki) para conocer la presentación y la introducción del proyecto.

La [sección 2: Descripción General](https://github.com/santhiagoxinge/EcoAnalytics/wiki/2.-Descripción-General) reúne los perfiles, funciones, entorno operativo y diagramas del proyecto, siguiendo la estructura de la wiki guía.

Las [colecciones de Postman](postman/README.md) permiten probar la API local. Incluyen autenticación, consultas, reportes, importación CSV y una [evidencia de ejecución](postman/evidencias/RESUMEN.md) con 44 peticiones y 107 comprobaciones aprobadas.

Los [principios GRASP](docs/GRASP.md) se explican mediante sus clases y métodos. Las [vistas de diseño corregidas](docs/nuevas-vistas/README.md) incluyen contexto, componentes, dominio, contratos, desarrollo y despliegue local, con imágenes PNG/SVG y proyectos editables de Visual Paradigm.

## Autor

Cristhian Santiago Parra Erazo — ID 926222  
Ingeniería de Software — Cuarto semestre  
Docente: Miguel Tovar Bastidas
