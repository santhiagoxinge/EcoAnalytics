# Vista de desarrollo completa de EcoAnalytics

Un solo diagrama del backend Java con 215 clases y tipos internos, atributos, métodos públicos declarados y 602 relaciones. Conserva el azul de Visual Paradigm y agrupa los tipos en sus nueve paquetes principales de presentación; los subpaquetes permanecen en el modelo.

| Imagen | Vector ampliable | Proyecto editable |
| --- | --- | --- |
| [PNG](img/4.png) | [SVG](img/4.svg) | [VPP](vpp/4-Vista-desarrollo-completa.vpp) |

El VPP contiene exactamente un diagrama. La imagen PNG mide 12282 × 5926 píxeles; abrir el SVG y ampliar para leer los atributos y las operaciones.

Incluye controladores, servicios, repositorios, entidades, interfaces, enumeraciones, DTO, infraestructura, configuración y manejo de errores. Cada tipo aparece una vez con su detalle. Los records muestran sus campos; sus accesores implícitos no se duplican como métodos escritos.

Las líneas continuas indican referencias mantenidas como atributos; las flechas discontinuas, dependencias por firmas o usos del código; los triángulos vacíos discontinuos, realizaciones de interfaces. Los tipos de bibliotecas externas se conservan en atributos y firmas. Los repositorios no se presentan como subclases de las entidades que persisten.

DTO significa objeto de transferencia de datos: define los datos recibidos o devueltos por la API. Por ejemplo, EstacionSolicitud contiene datos de entrada y EstacionRespuesta contiene la información de salida.

Se verificó la presencia de los 215 tipos, sin cajas de clase superpuestas, conexiones sin extremos ni tramos que atraviesen otras cajas. La [evidencia de verificación](verificacion.json) documenta el alcance. Esta incorporación solo modifica documentación.

La [vista de desarrollo anterior](../nuevas-vistas/README.md) se conserva y esta versión se añade como una vista complementaria con atributos y conexiones entre clases.
