package com.ecoanalytics.infraestructura.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Quién realiza la operación actual, para la auditoría. Las tareas en segundo plano actúan como "Sistema". */
public final class ActorActual {

    public static final String SISTEMA = "Sistema";

    private ActorActual() {
    }

    public static String nombre() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.isAuthenticated() && !"anonymousUser".equals(autenticacion.getName())
                ? autenticacion.getName()
                : SISTEMA;
    }
}
