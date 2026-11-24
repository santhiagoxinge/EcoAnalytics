# Nuevas vistas de EcoAnalytics

Vistas corregidas a partir del proyecto de `docs/vistas`, contrastadas con el backend Java, la interfaz React y la configuración local, junto con el despliegue AWS verificado. Los originales se conservan en su carpeta; esta entrega utiliza archivos nuevos.

## Abrir y consultar

El [archivo maestro](EcoAnalytics-Nuevas-Vistas.vpp) contiene las siete vistas y sus modelos compartidos. Es el archivo recomendado para continuar la edición. También se incluye un `.vpp` independiente con una sola vista por archivo; son copias de esta entrega y sus ediciones posteriores no se sincronizan automáticamente con el maestro.

| Vista | Imagen | Vector para ampliar | Proyecto individual |
|---|---|---|---|
| 1. Contexto | [PNG](img/1.png) | [SVG](img/1.svg) | [VPP](vpp/1-Vista-contexto.vpp) |
| 2. Funcional | [PNG](img/2.png) | [SVG](img/2.svg) | [VPP](vpp/2-Vista-funcional.vpp) |
| 3. Lógica | [PNG](img/3.png) | [SVG](img/3.svg) | [VPP](vpp/3-Vista-logica.vpp) |
| 3.1. Contratos e implementaciones | [PNG](img/3.1.png) | [SVG](img/3.1.svg) | [VPP](vpp/3.1-Contratos-implementaciones.vpp) |
| 4. Desarrollo | [PNG](img/4.png) | [SVG](img/4.svg) | [VPP](vpp/4-Vista-desarrollo.vpp) |
| 5. Despliegue AWS | [PNG](img/5.png) | [SVG](img/5.svg) | [VPP](vpp/5-Vista-despliegue.vpp) |
| 5.1. Desarrollo local | [PNG](img/5.1.png) | [SVG](img/5.1.svg) | [VPP](vpp/5.1-Vista-despliegue-local.vpp) |

Las vistas lógica y de desarrollo contienen bastante detalle. Para leer atributos, operaciones y nombres de paquetes, abrir el SVG o ampliar el PNG a tamaño original; reducir toda la imagen al ancho de una página vuelve pequeño el texto. Para una exposición, conviene utilizar contexto, funcional y contratos como explicación general, y ampliar las zonas pertinentes de lógica y desarrollo.

## Mejoras realizadas

- **Contexto:** tres perfiles humanos, aplicación completa y OpenStreetMap. Se retiraron de la vista los actores que presentaban sensores y canales simulados como integraciones externas actuales.
- **Funcional:** módulos del backend y sus interfaces; separación de flechas y títulos; dependencia del frontend con OpenStreetMap; aclaración sobre eventos Spring y emisores simulados.
- **Lógica:** nota separada de los enumerados; asociación `Pronostico–Punto` sin composición, porque `recortado()` comparte los puntos; dependencia de `EmisorAlerta.canal()` hacia `CanalAlerta.Codigo`; aclaración de que un record no garantiza inmutabilidad profunda de listas o arrays.
- **Contratos:** subvista adicional con 22 realizaciones reales: diez documentos con `ConIdSecuencial`, cuatro modelos predictivos, tres exportadores, dos lectores y tres reglas de validación. Reutiliza los elementos de las otras vistas.
- **Desarrollo:** corrección de la afirmación de independencia de Spring; el dominio conserva anotaciones MongoDB. Se separaron rutas largas de dependencias para evitar atravesar las cajas de clases.
- **Despliegue:** la vista 5 representa AWS: CloudFront, EC2 t3.small, Nginx, Java, MongoDB, EBS, S3 y Systems Manager. La vista 5.1 conserva localhost para desarrollo. El acceso público está en https://dsjd5zau1tr4h.cloudfront.net; no se representa un servidor Python.

## GRASP y verificación

La explicación de los nueve principios, con clases, métodos, beneficios y límites, se encuentra en [GRASP](../GRASP.md). La optimización concreta identificada es la reserva de ids por bloque en `InsercionMasiva`; no se inventaron porcentajes de rendimiento.

Las vistas se editaron y exportaron con el puente MCP de Visual Paradigm Community Edition 18.1. Se revisaron las imágenes y las cajas del modelo, excluyendo de la comprobación de solapamiento las contenciones intencionales. Cada VPP individual se volvió a leer mediante el puente y se comprobó que contiene una vista.

La validación documenta consistencia estructural y trazabilidad del alcance mostrado. No demuestra que todos los requisitos del proyecto estén implementados ni que las dependencias agrupadas de la vista de desarrollo sean un grafo exhaustivo de cada import. Las [pruebas Postman](../../postman/README.md) verifican operaciones HTTP y permisos de forma complementaria.
