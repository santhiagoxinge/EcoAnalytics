package com.ecoanalytics.infraestructura.notificacion;

import com.ecoanalytics.model.Alerta;
import com.ecoanalytics.model.CanalAlerta;
import com.ecoanalytics.model.alerta.EmisorAlerta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Despliegue académico: los canales registran la entrega en el log en lugar de llamar a un proveedor
 * de push o de correo. Usar un proveedor real solo exige reemplazar uno de estos beans.
 */
@Configuration
public class EmisoresAlertaSimulados {

    private static final Logger log = LoggerFactory.getLogger(EmisoresAlertaSimulados.class);

    @Bean
    EmisorAlerta emisorPush() {
        return emisor(CanalAlerta.Codigo.PUSH, "Web Push");
    }

    @Bean
    EmisorAlerta emisorCorreo() {
        return emisor(CanalAlerta.Codigo.CORREO, "Correo institucional");
    }

    @Bean
    EmisorAlerta emisorBannerWeb() {
        return emisor(CanalAlerta.Codigo.BANNER_WEB, "Banner del portal público");
    }

    private static EmisorAlerta emisor(CanalAlerta.Codigo codigo, String etiqueta) {
        return new EmisorAlerta() {
            @Override
            public CanalAlerta.Codigo canal() {
                return codigo;
            }

            @Override
            public void enviar(Alerta alerta) {
                log.info("[{}] {} — {}", etiqueta, alerta.getTitulo(), alerta.getMensaje());
            }
        };
    }
}
