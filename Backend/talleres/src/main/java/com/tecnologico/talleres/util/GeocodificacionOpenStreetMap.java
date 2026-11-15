package com.tecnologico.talleres.util;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

// Pido lat/lng a Nominatim al guardar un taller sin coordenadas (misma idea que registrar-taller.js)
public final class GeocodificacionOpenStreetMap {

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    // Clase solo con metodos estaticos
    private GeocodificacionOpenStreetMap() {
    }

    // Devuelvo [lat, lng] o null si Nominatim no encuentra nada
    public static double[] buscar(String direccion, String ciudad) {
        String consulta = (direccion != null ? direccion.trim() : "");
        if (ciudad != null && !ciudad.isBlank()) {
            if (!consulta.isEmpty()) {
                consulta = consulta + ", ";
            }
            consulta = consulta + ciudad.trim();
        }
        if (consulta.isEmpty()) {
            return null;
        }
        consulta = consulta + ", Colombia";

        try {
            String url = "https://nominatim.openstreetmap.org/search?q="
                    + URLEncoder.encode(consulta, StandardCharsets.UTF_8)
                    + "&format=json&limit=1";

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "AsistenciaEnCarretera-Educativo/1.0")
                    .GET()
                    .build();

            HttpResponse<String> respuesta = CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200 || respuesta.body() == null || respuesta.body().isBlank()) {
                return null;
            }

            return leerLatLng(respuesta.body());
        } catch (Exception e) {
            return null;
        }
    }

    // Parseo basico del JSON: busco "lat" y "lon" sin usar Jackson
    private static double[] leerLatLng(String json) {
        Double lat = leerValorJson(json, "lat");
        Double lng = leerValorJson(json, "lon");
        if (lat == null || lng == null) {
            return null;
        }
        return new double[]{lat, lng};
    }

    // Saco un numero del JSON buscando la clave lat u lon
    private static Double leerValorJson(String json, String clave) {
        String conComillas = "\"" + clave + "\":\"";
        int pos = json.indexOf(conComillas);
        if (pos >= 0) {
            int inicio = pos + conComillas.length();
            int fin = json.indexOf('"', inicio);
            if (fin > inicio) {
                return Double.parseDouble(json.substring(inicio, fin));
            }
        }
        String sinComillas = "\"" + clave + "\":";
        pos = json.indexOf(sinComillas);
        if (pos >= 0) {
            int inicio = pos + sinComillas.length();
            int fin = json.indexOf(',', inicio);
            if (fin < 0) {
                fin = json.indexOf('}', inicio);
            }
            if (fin > inicio) {
                return Double.parseDouble(json.substring(inicio, fin).trim());
            }
        }
        return null;
    }
}
