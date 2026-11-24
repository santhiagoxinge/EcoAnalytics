package com.ecoanalytics.model;

import java.util.EnumSet;
import java.util.Set;

/** Cada rol conoce sus propios permisos (Experto en información). */
public enum Rol {
    ADMINISTRADOR(EnumSet.allOf(Permiso.class)),
    ANALISTA(EnumSet.of(Permiso.VER_DATOS, Permiso.EXPORTAR_REPORTES, Permiso.PREDICCIONES_ML,
            Permiso.INGESTAR_DATOS, Permiso.GESTIONAR_ANOMALIAS)),
    CIUDADANO(EnumSet.of(Permiso.VER_DATOS));

    private final Set<Permiso> permisos;

    Rol(Set<Permiso> permisos) {
        this.permisos = permisos;
    }

    public Set<Permiso> permisos() {
        return permisos;
    }

    public enum Permiso {
        VER_DATOS, EXPORTAR_REPORTES, PREDICCIONES_ML, INGESTAR_DATOS, GESTIONAR_ANOMALIAS,
        GESTIONAR_ESTACIONES, GESTIONAR_USUARIOS, ADMINISTRAR_SISTEMA
    }
}
