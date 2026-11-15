package com.tecnologico.talleres.dto;

// Cuerpo del POST /api/historial-busqueda desde buscar-talleres.js
public record HistorialBusquedaRequest(
        String termino,       // texto buscado en el mapa
        Double origenLat,     // lat del punto de busqueda
        Double origenLng,     // lng del punto de busqueda
        Integer radioM,       // radio en metros (0 en la app actual)
        Integer resultados    // cuantos talleres coincidieron
) {}
