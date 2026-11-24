package com.ecoanalytics.model;

import com.ecoanalytics.exception.ExcepcionNegocio;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas del dominio que viven en las propias entidades (Experto en información). No requieren MongoDB. */
class ModeloDominioTest {

    @Test
    void elIcaUsaElPeorSubindiceYSuCategoria() {
        IndiceCalidadAire ica = IndiceCalidadAire.combinado(40.0, 60.0);
        assertEquals(CategoriaIca.DANINA_SENSIBLES, ica.categoria());
        assertEquals(Optional.of(Alerta.Nivel.AMARILLA), ica.categoria().nivelAlerta());
        assertEquals(CategoriaIca.BUENA, IndiceCalidadAire.desdePm25(8).categoria());
    }

    @Test
    void unUsuarioPendienteOInactivoNoPuedeIniciarSesion() {
        Usuario pendiente = new Usuario("Ana", "ANA@x.org", "hash", Rol.ANALISTA, EstadoUsuario.PENDIENTE);
        assertEquals("ana@x.org", pendiente.getCorreo());
        ExcepcionNegocio e = assertThrows(ExcepcionNegocio.class, pendiente::validarInicioSesion);
        assertEquals(ExcepcionNegocio.Tipo.PROHIBIDO, e.tipo());

        pendiente.activar();
        assertDoesNotThrow(pendiente::validarInicioSesion);
    }

    @Test
    void laEstacionRechazaCoordenadasFueraDeRango() {
        assertThrows(ExcepcionNegocio.class,
                () -> new Estacion("X", "Z", 95, 0, TipoFuente.CSV, "PM2.5"));
    }

    @Test
    void laEstacionExplicaPorQueNoHayDatosActuales() {
        Estacion estacion = new Estacion("Centro", "Z", 6.2, -75.5, TipoFuente.SENSOR, "PM2.5");
        LocalDateTime ahora = LocalDateTime.now();
        Medicion vieja = new Medicion(1L, ahora.minusHours(8), 10.0, 20.0, 20.0, 70.0,
                TipoFuente.SENSOR, EstadoMedicion.VALIDA, null);
        assertTrue(estacion.motivoNoDisponible(Optional.of(vieja), ahora).orElseThrow().contains("6 horas"));

        estacion.cambiarEstado(EstadoEstacion.MANTENIMIENTO);
        assertEquals(EstadoSincronizacion.MANTENIMIENTO, estacion.sincronizacion(10L));
    }

    @Test
    void unaAnomaliaSoloSeGestionaUnaVez() {
        Estacion estacion = new Estacion("Centro", "Z", 6.2, -75.5, TipoFuente.SENSOR, "PM2.5");
        Anomalia anomalia = new Anomalia(estacion, LocalDateTime.now(), VariableAmbiental.PM25, 148.6, 20, 5, 4.2);
        assertEquals(Anomalia.Severidad.CRITICA, anomalia.getSeveridad());
        anomalia.notificar();
        assertThrows(ExcepcionNegocio.class, anomalia::descartar);
    }

    @Test
    void cadaTipoDeIndicadorSeCalculaASiMismo() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Medicion> mes = List.of(
                new Medicion(1L, ahora.minusHours(2), 10.0, null, null, null, TipoFuente.CSV, EstadoMedicion.VALIDA, null),
                new Medicion(1L, ahora.minusHours(1), 30.0, null, null, null, TipoFuente.CSV, EstadoMedicion.VALIDA, null));
        assertEquals(20.0, TipoIndicador.PROMEDIO_MOVIL_PM25_24H.calcular(mes, ahora));
        assertEquals(30.0, TipoIndicador.MAXIMO_DIARIO.calcular(mes, ahora));
    }
}
