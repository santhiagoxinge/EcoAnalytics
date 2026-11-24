package com.ecoanalytics.model.alerta;

import com.ecoanalytics.model.Alerta;
import com.ecoanalytics.model.CanalAlerta;

/** Un canal de entrega. Canales nuevos (SMS, WhatsApp...) son nuevas implementaciones de este contrato. */
public interface EmisorAlerta {

    CanalAlerta.Codigo canal();

    void enviar(Alerta alerta);
}
