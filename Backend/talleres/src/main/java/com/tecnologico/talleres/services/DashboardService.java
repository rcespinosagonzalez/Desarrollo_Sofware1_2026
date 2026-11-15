package com.tecnologico.talleres.services;

import java.util.Map;

// Datos de los dashboards. Devuelven mapas listos para convertir a JSON (series con "etiquetas" y "valores").
public interface DashboardService {

    // Panorama de toda la plataforma (rol admin)
    Map<String, Object> resumenAdmin();

    // Panorama de los talleres de un dueno (rol taller)
    Map<String, Object> resumenTaller(String propietarioUsuarioId);
}
