# EcoAnalytics: base de datos MongoDB

Esta guía permite preparar la persistencia local y consultar su estructura. La configuración vigente está en [application.properties](../backend/src/main/resources/application.properties) y el esquema documental se deriva de las clases `@Document` de [model](../backend/src/main/java/com/ecoanalytics/model).

## Instalación y servicio local

El proyecto utiliza MongoDB Server, con base `ecoanalytics` en `localhost:27017`. MongoDB Compass es una interfaz opcional; no sustituye al servidor. `mongosh` es el cliente de consola y debe estar instalado por separado si el instalador del servidor no lo incluye.

En Windows:

1. Instalar MongoDB Community Server mediante su instalador para Windows y seleccionar la instalación como servicio. Conservar el nombre predeterminado `MongoDB` y el puerto `27017`.
2. Instalar MongoDB Shell (`mongosh`) y añadir su carpeta de ejecutables al `PATH`.
3. Consultar y, si está detenido, arrancar el servicio desde una consola PowerShell con privilegios de administrador:

```powershell
Get-Service -Name MongoDB
Start-Service -Name MongoDB
```

Si se eligió otro nombre de servicio, reemplazarlo en ambos comandos. El servicio utiliza las rutas de datos y registro definidas en su archivo `mongod.cfg`.

Como alternativa, si se instaló el servidor sin servicio, crear un directorio de datos y ejecutar `mongod` en una consola que permanezca abierta (con su carpeta de ejecutables en el `PATH`):

```powershell
New-Item -ItemType Directory -Force -Path "$env:LOCALAPPDATA\EcoAnalytics\mongodb"
mongod --dbpath "$env:LOCALAPPDATA\EcoAnalytics\mongodb" --bind_ip 127.0.0.1 --port 27017
```

Usar una sola modalidad a la vez para evitar conflictos de puerto. En Linux con un paquete que registre la unidad `mongod`, el equivalente para habilitar y arrancar el servicio es `sudo systemctl enable --now mongod`; el nombre de la unidad depende de la instalación.

Verificar la conexión sin arrancar EcoAnalytics:

```powershell
mongosh "mongodb://localhost:27017/ecoanalytics" --eval "db.runCommand({ ping: 1 })"
```

Un resultado con `ok: 1` confirma que el servidor responde. La base y las colecciones se materializan cuando se escriben datos; no hay que ejecutar un esquema SQL.

## URI y configuración

La URI predeterminada es:

```text
mongodb://localhost:27017/ecoanalytics
```

Para sustituirla, definir la variable en la misma consola desde la que se vaya a iniciar el backend:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/ecoanalytics"
```

El archivo de propiedades declara tanto `spring.mongodb.uri` como `spring.data.mongodb.uri` con `${MONGODB_URI:mongodb://localhost:27017/ecoanalytics}` y activa `spring.data.mongodb.auto-index-creation=true`. La URI local de ejemplo no incluye autenticación. Una instalación con credenciales debe proporcionar la URI correspondiente en `MONGODB_URI`.

Los antiguos scripts SQL, referencias a H2, perfiles MySQL y variables `DB_USERNAME`/`DB_PASSWORD` no forman parte de esta configuración MongoDB.

## Colecciones y campos

Los tipos siguientes son los declarados en Java. Spring Data guarda `@Id` como `_id`; en las respuestas HTTP puede seguir llamándose `id`. Los enumerados se representan por sus nombres, las listas como arreglos y `LocalDateTime` se convierte mediante los conversores de Spring Data: no contiene por sí mismo una zona horaria. Puede aparecer el metadato `_class` del mapeador, que no es un atributo del dominio.

| Colección | Clase documental | Campos (tipo Java) |
|---|---|---|
| `alertas` | [Alerta](../backend/src/main/java/com/ecoanalytics/model/Alerta.java) | `_id`: `Long`, `tipo`: `Tipo`, `nivel`: `Nivel`, `titulo`: `String`, `mensaje`: `String`, `recomendacion`: `String`, `estacionId`: `Long`, `estacionNombre`: `String`, `valorDisparador`: `Double`, `canalesEntregados`: `List<CanalAlerta.Codigo>`, `origen`: `String`, `estado`: `Estado`, `fechaCreacion`: `LocalDateTime` |
| `anomalias` | [Anomalia](../backend/src/main/java/com/ecoanalytics/model/Anomalia.java) | `_id`: `Long`, `estacionId`: `Long`, `estacionNombre`: `String`, `fechaMedicion`: `LocalDateTime`, `variable`: `VariableAmbiental`, `valorObservado`: `double`, `mediaEsperada`: `double`, `desviacionEstandar`: `double`, `puntajeZ`: `double`, `severidad`: `Severidad`, `estado`: `Estado`, `fechaDeteccion`: `LocalDateTime` |
| `canales_alerta` | [CanalAlerta](../backend/src/main/java/com/ecoanalytics/model/CanalAlerta.java) | `_id`: `String`, `codigo`: `Codigo`, `habilitado`: `boolean`, `entregas`: `int`, `ultimaEntrega`: `LocalDateTime` |
| `configuracion_indicadores` | [ConfiguracionIndicador](../backend/src/main/java/com/ecoanalytics/model/ConfiguracionIndicador.java) | `_id`: `String`, `tipo`: `TipoIndicador`, `habilitado`: `boolean`, `programacion`: `String`, `ultimaEjecucion`: `LocalDateTime`, `registrosProcesados`: `int` |
| `configuracion_sistema` | [ConfiguracionSistema](../backend/src/main/java/com/ecoanalytics/model/ConfiguracionSistema.java) | `_id`: `String` (atributo `clave`), `valor`: `String` |
| `estaciones` | [Estacion](../backend/src/main/java/com/ecoanalytics/model/Estacion.java) | `_id`: `Long`, `nombre`: `String`, `zona`: `String`, `latitud`: `double`, `longitud`: `double`, `tipoFuente`: `TipoFuente`, `variables`: `String`, `estado`: `EstadoEstacion`, `fechaInstalacion`: `LocalDateTime` |
| `auditoria` | [EventoAuditoria](../backend/src/main/java/com/ecoanalytics/model/EventoAuditoria.java) | `_id`: `Long`, `fecha`: `LocalDateTime`, `nivel`: `Nivel`, `modulo`: `String`, `descripcion`: `String`, `actor`: `String` |
| `incidencias_validacion` | [IncidenciaValidacion](../backend/src/main/java/com/ecoanalytics/model/IncidenciaValidacion.java) | `_id`: `Long`, `loteId`: `Long`, `fila`: `int`, `estacionNombre`: `String`, `variable`: `String`, `valorDetectado`: `String`, `rangoAceptable`: `String`, `tipo`: `Tipo`, `resolucion`: `Resolucion` |
| `lotes_ingesta` | [LoteIngesta](../backend/src/main/java/com/ecoanalytics/model/LoteIngesta.java) | `_id`: `Long`, `nombreArchivo`: `String`, `formato`: `TipoFuente`, `totalRegistros`: `int`, `registrosValidos`: `int`, `registrosAdvertencia`: `int`, `registrosRechazados`: `int`, `registrosImportados`: `int`, `estado`: `Estado`, `cargadoPor`: `String`, `fechaCreacion`: `LocalDateTime`, `fechaImportacion`: `LocalDateTime` |
| `mediciones` | [Medicion](../backend/src/main/java/com/ecoanalytics/model/Medicion.java) | `_id`: `Long`, `estacionId`: `Long`, `fechaMedicion`: `LocalDateTime`, `pm25`: `Double`, `pm10`: `Double`, `temperatura`: `Double`, `humedad`: `Double`, `fuente`: `TipoFuente`, `estado`: `EstadoMedicion`, `loteId`: `Long` |
| `usuarios` | [Usuario](../backend/src/main/java/com/ecoanalytics/model/Usuario.java) | `_id`: `Long`, `nombreCompleto`: `String`, `correo`: `String`, `contrasenaCifrada`: `String`, `rol`: `Rol`, `estado`: `EstadoUsuario`, `fechaCreacion`: `LocalDateTime`, `ultimoIngreso`: `LocalDateTime` |
| `valores_indicador` | [ValorIndicador](../backend/src/main/java/com/ecoanalytics/model/ValorIndicador.java) | `_id`: `Long`, `estacionId`: `Long`, `tipo`: `TipoIndicador`, `valor`: `double`, `fechaCalculo`: `LocalDateTime` |
| `versiones_modelo` | [VersionModelo](../backend/src/main/java/com/ecoanalytics/model/VersionModelo.java) | `_id`: `Long`, `version`: `String`, `algoritmo`: `AlgoritmoModelo`, `variableObjetivo`: `String`, `caracteristicas`: `String`, `r2`: `double`, `rmse`: `double`, `mae`: `double`, `muestrasEntrenamiento`: `int`, `muestrasPrueba`: `int`, `duracionMs`: `long`, `estado`: `Estado`, `entrenadoPor`: `String`, `fechaEntrenamiento`: `LocalDateTime` |
| `secuencias` | Administrada por `GeneradorSecuencias`, sin clase `@Document` | `_id`: nombre de la colección (`String`); `valor`: último número reservado (`long`). |

`Estacion.variables` y `VersionModelo.caracteristicas` son cadenas, no listas ni subdocumentos. `VersionModelo` almacena las métricas como campos escalares (`r2`, `rmse`, `mae`, etc.); no embebe un objeto `MetricasModelo`.

## índices

MongoDB crea el índice único de `_id` en cada colección. Los índices adicionales declarados en el código son:

| Colección | Declaración | Campos y sentido | Unicidad |
|---|---|---|---|
| `usuarios` | `Usuario.correo`, `@Indexed(unique = true)` | `correo: 1` | Sí |
| `estaciones` | `Estacion.nombre`, `@Indexed(unique = true)` | `nombre: 1` | Sí |
| `incidencias_validacion` | `IncidenciaValidacion.loteId`, `@Indexed` | `loteId: 1` | No |
| `mediciones` | `@CompoundIndex`, `idx_estacion_fecha` | `estacionId: 1, fechaMedicion: 1` | No |
| `valores_indicador` | `@CompoundIndex`, `idx_estacion_tipo_fecha` | `estacionId: 1, tipo: 1, fechaCalculo: -1` | No |

Los compuestos favorecen consultas por estación y período, o por estación/tipo y fecha descendente. La creación automática sucede al inicializar el mapeo en el backend; no es una migración SQL. Los nombres de los índices simples se dejan al framework. Para inspeccionar los existentes:

```javascript
use ecoanalytics
db.mediciones.getIndexes()
db.usuarios.getIndexes()
```

El índice estación/fecha no prohíbe duplicados: `IngestaService.confirmar` consulta `existsByEstacionIdAndFechaMedicion` antes de insertar y `ReglaDuplicado` detecta duplicados dentro del archivo. Esa comprobación no constituye una garantía de unicidad ante escrituras concurrentes.

## Identificadores Long secuenciales

Las diez clases con id numérico implementan `ConIdSecuencial`: `Usuario`, `Estacion`, `Medicion`, `LoteIngesta`, `IncidenciaValidacion`, `Anomalia`, `VersionModelo`, `Alerta`, `ValorIndicador` y `EventoAuditoria`.

- [GeneradorSecuencias](../backend/src/main/java/com/ecoanalytics/repository/GeneradorSecuencias.java) mantiene un contador independiente por colección en `secuencias`. `reservar(nombre, cantidad)` realiza `findOneAndUpdate` con `$inc`, `upsert(true)` y retorno del documento actualizado. Devuelve el primer número del bloque; `siguiente` reserva uno. Con un contador nuevo, el primer id es `1`.
- [AsignadorIdSecuencial](../backend/src/main/java/com/ecoanalytics/repository/AsignadorIdSecuencial.java) implementa `BeforeConvertCallback<Object>`: asigna un id solo si el objeto implementa `ConIdSecuencial` y su id es nulo. Resuelve el generador mediante `ObjectProvider` para evitar un ciclo de construcción con `MongoTemplate`.
- [InsercionMasiva](../backend/src/main/java/com/ecoanalytics/repository/InsercionMasiva.java) reserva un bloque para toda la lista, asigna los ids y llama a `MongoTemplate.insert`. Una lista vacía no consume ids. Lo utilizan la ingesta, la semilla, el simulador y el cálculo de indicadores.

El incremento del contador es atómico, pero la reserva y las inserciones son operaciones separadas. Puede haber saltos si falla una inserción; «secuencial» no significa una numeración sin huecos. Las escrituras manuales con ids explícitos no actualizan el contador automáticamente: deben evitarse o coordinarse con este mecanismo. No borrar `secuencias` conservando los documentos numerados.

Las otras tres clases usan claves naturales `String`: `CanalAlerta.id = codigo.name()`, `ConfiguracionIndicador.id = tipo.name()` y `ConfiguracionSistema.clave`.

## Referencias, desnormalización y consistencia

`Medicion.estacionId`, `Medicion.loteId`, `IncidenciaValidacion.loteId`, `Anomalia.estacionId`, `Alerta.estacionId` y `ValorIndicador.estacionId` son referencias manuales por identificador. No se usan `@DBRef`, uniones JPA ni claves foráneas impuestas por MongoDB. El lote de una medición puede ser nulo en datos sintéticos; una alerta extraordinaria puede no tener estación.

`Alerta` y `Anomalia` copian `estacionNombre` desde la estación al construirse. Esta desnormalización permite leer sus históricos sin consultar `estaciones` y conserva el nombre capturado entonces. Renombrar la estación no actualiza automáticamente esas copias. `Alerta.canalesEntregados` embebe un arreglo de códigos de canal, no documentos `CanalAlerta` completos.

No se usan transacciones: no hay `@Transactional` ni un gestor de transacciones MongoDB configurado. Un servidor local independiente basta para la implementación actual. La atomicidad de una escritura documental o del contador no vuelve atómico un flujo de varias colecciones. Por ejemplo, confirmar una ingesta escribe mediciones, actualiza el lote, publica el evento y registra auditoría en pasos separados; un fallo puede dejar cambios parciales. Los eventos internos tampoco constituyen una cola durable.

## Datos iniciales y datos en memoria

[CargadorDatosIniciales](../backend/src/main/java/com/ecoanalytics/infraestructura/semilla/CargadorDatosIniciales.java) es un `ApplicationRunner` que siembra los datos al arrancar el backend:

1. Crea siete usuarios solo si `usuarios` está vacía y cinco estaciones solo si `estaciones` está vacía.
2. Completa canales, configuraciones de indicadores y parámetros generales faltantes por clave.
3. Si no hay mediciones, genera el histórico horario sintético por estación en bloques de 5000 y evalúa anomalías y alertas recientes. `ecoanalytics.seed.history-days=430` controla su extensión; los estados de las estaciones acortan sus series.
4. Si no hay versiones de modelos, entrena los cuatro algoritmos y promueve el de mayor R².
5. Recalcula indicadores en cada arranque, incluso si ya había datos. Las comprobaciones por colección vacía no reparan por completo una siembra parcial.

Cuentas principales de demostración:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@ecoanalytics.org | Admin123! |
| Analista | analista@ecoanalytics.org | Analista123! |
| Ciudadano | ciudadano@ecoanalytics.org | Ciudadano123! |

Las contraseñas se guardan con BCrypt. Los parámetros ajustados de los modelos permanecen en `RegistroModelos` en memoria; MongoDB conserva su versión, métricas y estado. Tras reiniciar, `asegurarAjustado` vuelve a entrenar cuando necesita una versión ausente del registro. Los resultados pendientes de confirmar de `IngestaService` y la caché de pronósticos también están en memoria; un lote persistido pendiente no permite recuperar automáticamente su archivo después de un reinicio.

## Reiniciar la base de demostración

Este procedimiento elimina todos los datos de `ecoanalytics`, incluidos usuarios, auditoría y contadores. Detener primero el backend para que las tareas programadas no escriban durante el borrado. Conectar con `mongosh` a la instancia indicada por `MONGODB_URI` (o a la URI predeterminada) y ejecutar:

```javascript
use ecoanalytics
db.dropDatabase()
```

En el siguiente arranque del backend, la creación automática reconstruirá los índices de las entidades y `CargadorDatosIniciales` volverá a sembrar los datos. No hace falta ejecutar archivos SQL. Este documento describe el procedimiento; su elaboración no implica haberlo ejecutado.
