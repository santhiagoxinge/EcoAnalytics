# Aplicación de GRASP en EcoAnalytics

Los principios GRASP orientan la asignación de responsabilidades entre objetos. En EcoAnalytics se documentan a partir del código existente, con ejemplos de métodos y colaboradores concretos. Esta revisión describe decisiones de diseño y sus límites; no presenta una refactorización ni una medición de rendimiento nueva.

Las rutas de este documento parten de `backend/src/main/java/com/ecoanalytics/`, en el código fuente del repositorio. Complementa `docs/DECISIONES_DISENO.md` y las vistas corregidas de `docs/nuevas-vistas/`.

## 1. Experto en información

**Ubicación:** [model/Usuario.java](../backend/src/main/java/com/ecoanalytics/model/Usuario.java), método `validarInicioSesion()`; utilizado desde [service/AutenticacionService.java](../backend/src/main/java/com/ecoanalytics/service/AutenticacionService.java), método `iniciarSesion()`.

El usuario conoce su propio estado y comprueba si está pendiente o inactivo antes de permitir el ingreso. El servicio autentica las credenciales y coordina el proceso, pero delega esta decisión en la entidad que posee la información.

**Beneficio:** la regla de acceso por estado queda centralizada y puede reutilizarse sin repetir condicionales en los controladores. También aparece en [model/Anomalia.java](../backend/src/main/java/com/ecoanalytics/model/Anomalia.java), que controla sus transiciones mediante `notificar()` y `descartar()`, y en [model/Medicion.java](../backend/src/main/java/com/ecoanalytics/model/Medicion.java), que calcula `ica()` con sus valores.

## 2. Creador

**Ubicación:** [service/IngestaService.java](../backend/src/main/java/com/ecoanalytics/service/IngestaService.java), métodos `cargar()` y `confirmar()`.

`cargar()` reúne nombre de archivo, tipo de fuente, actor y resultados de validación. Con esos datos construye `LoteIngesta` e `IncidenciaValidacion`. `confirmar()` utiliza los registros revisados y las estaciones resueltas para construir `Medicion`.

**Beneficio:** la creación ocurre donde ya se dispone de la información necesaria y se coordina el ciclo del lote. Las entidades conservan sus constructores y reglas; el controlador no arma documentos MongoDB a partir de la petición.

La selección de algoritmos se concentra adicionalmente en `infraestructura/ml/FabricaModelosPredictivos.crear()`. Es una fábrica simple basada en `switch`; la presencia del método `crear()` no basta para afirmar que existe el patrón GoF Factory Method con creación redefinida por subclases.

## 3. Controlador

**Ubicación:** [service/IngestaService.java](../backend/src/main/java/com/ecoanalytics/service/IngestaService.java) y [service/AutenticacionService.java](../backend/src/main/java/com/ecoanalytics/service/AutenticacionService.java).

Son ejemplos de controladores de casos de uso en el sentido GRASP: coordinan una operación del sistema y delegan responsabilidades en entidades, reglas, repositorios y adaptadores. Por ejemplo, ingesta selecciona el lector, valida el archivo, registra el lote y confirma las mediciones.

Los tipos `controller/IngestaController.java` y `controller/AutenticacionController.java` adaptan las solicitudes HTTP y delegan en esos servicios. La anotación `@RestController` identifica el rol dentro de Spring, pero por sí sola no demuestra una buena asignación de responsabilidades GRASP.

**Beneficio:** el flujo del caso de uso se mantiene separado del transporte HTTP y de los detalles de validación o lectura del formato. No implica independencia completa: `IngestaService.cargar()` todavía recibe `MultipartFile` de Spring.

## 4. Bajo acoplamiento

**Ubicación:** [exception/ExcepcionNegocio.java](../backend/src/main/java/com/ecoanalytics/exception/ExcepcionNegocio.java) y [exception/ManejadorExcepciones.java](../backend/src/main/java/com/ecoanalytics/exception/ManejadorExcepciones.java), método `negocio()`.

La excepción identifica el tipo de error de negocio con su propio enumerado. El manejador traduce ese tipo al estado HTTP. Por ejemplo, la entidad `Usuario` puede rechazar una operación sin elegir directamente un `HttpStatus`.

**Beneficio:** las reglas de negocio y su representación HTTP pueden evolucionar con menos cambios conjuntos. El alcance es parcial: los documentos del paquete `model` llevan anotaciones `@Document` e `@Id`, por lo que mantienen dependencia con Spring Data MongoDB.

## 5. Alta cohesión

**Ubicación:** [service/ValidacionService.java](../backend/src/main/java/com/ecoanalytics/service/ValidacionService.java), método `validar()`; [model/validacion/ReglaIdentificacion.java](../backend/src/main/java/com/ecoanalytics/model/validacion/ReglaIdentificacion.java), `ReglaValoresMedicion.java` y `ReglaDuplicado.java`.

Cada regla comprueba un aspecto de calidad: identificación de la estación y fecha, valores ambientales o duplicados. El servicio recorre esas reglas y clasifica el registro como válido, con advertencia o descartado. La lectura CSV/JSON pertenece a los adaptadores, y la persistencia del lote a otros colaboradores.

**Beneficio:** cada responsabilidad tiene un lugar identificable, lo que facilita comprender, probar y modificar las reglas. La lista de reglas se construye explícitamente en `ValidacionService`: incorporar otra exige actualizar esa lista.

## 6. Polimorfismo

**Ubicación:** [model/prediccion/ModeloPredictivo.java](../backend/src/main/java/com/ecoanalytics/model/prediccion/ModeloPredictivo.java); implementaciones en [infraestructura/ml/ModeloMediaMovil.java](../backend/src/main/java/com/ecoanalytics/infraestructura/ml/ModeloMediaMovil.java), `ModeloRegresionLineal.java`, `ModeloKVecinos.java` y `ModeloRandomForest.java`.

Los servicios usan el contrato `ModeloPredictivo` para entrenar y predecir con algoritmos distintos. La implementación concreta se selecciona en la fábrica. Otro ejemplo es [model/TipoIndicador.java](../backend/src/main/java/com/ecoanalytics/model/TipoIndicador.java): cada constante define su cálculo y `IndicadorService` lo solicita mediante `calcular()`.

**Beneficio:** se evita colocar la implementación de cada algoritmo en el servicio de pronóstico y se pueden comparar o sustituir implementaciones mediante el mismo contrato. Agregar un algoritmo todavía exige modificar la fábrica y el enumerado correspondiente.

## 7. Fabricación pura

**Ubicación:** [repository/GeneradorSecuencias.java](../backend/src/main/java/com/ecoanalytics/repository/GeneradorSecuencias.java), métodos `siguiente()` y `reservar()`; [repository/InsercionMasiva.java](../backend/src/main/java/com/ecoanalytics/repository/InsercionMasiva.java), método `insertar()`.

Estos colaboradores no representan conceptos ambientales. Existen para aislar una necesidad técnica: asignar identificadores numéricos y realizar inserciones por lotes. Las entidades implementan `ConIdSecuencial` sin consultar MongoDB para obtener su identificador.

**Beneficio:** la numeración y la inserción se reutilizan desde ingesta, semilla y simulación. `InsercionMasiva.insertar()` reserva un bloque con una llamada a `GeneradorSecuencias.reservar()` y asigna sus ids antes de pedir la inserción de la lista.

```java
long id = secuencias.reservar(coleccion, documentos.size());
for (T documento : documentos) documento.asignarId(id++);
mongo.insert(documentos, tipo);
```

Esta es una optimización identificable en el código: para un lote no vacío se solicita una reserva de ids, en lugar de solicitar un id por documento. No se midió aquí un porcentaje de aceleración, consumo de memoria ni número exacto de paquetes enviados por el controlador MongoDB.

## 8. Indirección

**Ubicación:** [repository/AsignadorIdSecuencial.java](../backend/src/main/java/com/ecoanalytics/repository/AsignadorIdSecuencial.java), método `onBeforeConvert()`; [service/IngestaService.java](../backend/src/main/java/com/ecoanalytics/service/IngestaService.java), publicación de `MedicionesRegistradasEvento`; [service/AlertaService.java](../backend/src/main/java/com/ecoanalytics/service/AlertaService.java) y `AnomaliaService.java`, recepción de eventos.

El callback de Spring Data media entre la escritura individual y la asignación del id. Los servicios pueden guardar documentos sin repetir la lógica del contador. El callback obtiene `GeneradorSecuencias` mediante `ObjectProvider`, de forma diferida, para evitar el ciclo de construcción que produciría su dependencia con `MongoTemplate`.

En la ingesta, el publicador de eventos media entre registrar mediciones y activar sus análisis posteriores. El emisor publica `MedicionesRegistradasEvento`; los receptores registrados con `@EventListener` realizan su parte sin que ingesta invoque directamente a cada uno.

**Beneficio:** se centraliza una colaboración técnica y se reduce el conocimiento directo entre módulos. Los eventos de Spring usados aquí son internos al proceso: no prueban la existencia de una cola externa, entrega durable, ejecución asíncrona o microservicios.

## 9. Variaciones protegidas

**Ubicación:** [model/ingesta/AdaptadorFuenteMediciones.java](../backend/src/main/java/com/ecoanalytics/model/ingesta/AdaptadorFuenteMediciones.java), [model/reporte/ExportadorReporte.java](../backend/src/main/java/com/ecoanalytics/model/reporte/ExportadorReporte.java) y [model/alerta/EmisorAlerta.java](../backend/src/main/java/com/ecoanalytics/model/alerta/EmisorAlerta.java).

La lectura CSV/JSON, la exportación PDF/CSV/Excel y los canales de alertas se consumen mediante contratos. `FabricaFuentesMediciones` selecciona lectores de una lista inyectada. `ReporteService` organiza exportadores por formato y `AlertaService` organiza emisores por código de canal.

**Beneficio:** los casos de uso se apoyan en operaciones estables mientras los detalles de formato o canal se ubican en implementaciones concretas. Registrar una nueva alternativa puede requerir también ampliar un enumerado o configuración. Los emisores actuales son simulados; este diseño no demuestra envío real por correo o push.

## Relación con las vistas y comprobación

La vista de contexto delimita usuarios, sistema y dependencias externas. La funcional muestra módulos y contratos; la lógica describe entidades y relaciones; la de desarrollo ubica clases y paquetes; la de despliegue representa procesos y protocolos. GRASP explica por qué se asignaron esas responsabilidades y por qué se introdujeron los contratos y colaboradores técnicos.

Se contrastaron nombres y métodos mediante lectura del código. Las colecciones de `postman/` verifican operaciones HTTP y permisos, según su resumen de evidencia; esas pruebas no certifican por sí mismas el uso de GRASP ni todos los criterios de aceptación. La mejora de mantenibilidad descrita es un análisis de estructura, no un resultado experimental de rendimiento.
