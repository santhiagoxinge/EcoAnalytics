package com.ecoanalytics.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Arma las respuestas de descarga de archivos que comparten varios controladores. */
final class Descargas {

    private Descargas() {
    }

    static ResponseEntity<byte[]> archivo(String nombreArchivo, String tipoContenido, byte[] contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nombreArchivo, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(tipoContenido))
                .body(contenido);
    }

    /** CSV con BOM para que Excel abra bien las tildes y la µ. */
    static ResponseEntity<byte[]> csv(String nombreArchivo, String contenido) {
        return archivo(nombreArchivo, "text/csv; charset=UTF-8", ("﻿" + contenido).getBytes(StandardCharsets.UTF_8));
    }
}
