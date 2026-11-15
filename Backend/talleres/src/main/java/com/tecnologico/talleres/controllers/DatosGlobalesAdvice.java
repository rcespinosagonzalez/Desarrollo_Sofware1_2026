package com.tecnologico.talleres.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Map;

// Datos que usan todas las paginas HTML: enlaces a las redes de la empresa para el pie de pagina.
// Se cambian en application.properties (app.redes.*) o con variables de entorno, sin tocar las plantillas.
@ControllerAdvice
public class DatosGlobalesAdvice {

    private final String facebook;
    private final String x;
    private final String instagram;

    public DatosGlobalesAdvice(@Value("${app.redes.facebook:https://www.facebook.com/}") String facebook,
                               @Value("${app.redes.x:https://x.com/}") String x,
                               @Value("${app.redes.instagram:https://www.instagram.com/}") String instagram) {
        this.facebook = facebook;
        this.x = x;
        this.instagram = instagram;
    }

    @ModelAttribute("redes")
    public Map<String, String> redes() {
        Map<String, String> redes = new LinkedHashMap<>();
        redes.put("facebook", facebook);
        redes.put("x", x);
        redes.put("instagram", instagram);
        return redes;
    }
}
