package com.ecoanalytics.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Recuerda la última ejecución de cada tarea en segundo plano para mostrar su salud en la consola técnica. */
@Component
public class RegistroTareas {

    public record EjecucionTarea(String nombre, String descripcion, String programacion, boolean saludable,
                                 LocalDateTime ultimaEjecucion, long duracionMs, String detalle) {
    }

    private final Map<String, EjecucionTarea> ejecuciones = new ConcurrentHashMap<>();

    public void registrar(String nombre, String descripcion, String programacion) {
        ejecuciones.putIfAbsent(nombre, new EjecucionTarea(nombre, descripcion, programacion, true, null, 0,
                "En espera de la primera ejecución"));
    }

    public void exito(String nombre, long duracionMs, String detalle) {
        actualizar(nombre, true, duracionMs, detalle);
    }

    public void fallo(String nombre, long duracionMs, String detalle) {
        actualizar(nombre, false, duracionMs, detalle);
    }

    public Collection<EjecucionTarea> todas() {
        return ejecuciones.values();
    }

    private void actualizar(String nombre, boolean saludable, long duracionMs, String detalle) {
        ejecuciones.computeIfPresent(nombre, (k, e) -> new EjecucionTarea(k, e.descripcion(), e.programacion(),
                saludable, LocalDateTime.now(), duracionMs, detalle));
    }
}
