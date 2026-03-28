package com.tecnologico.talleres.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VistaController {

    @GetMapping("/buscar-talleres")
        public String buscarTalleres(){
            return "buscar-talleres";
        }

    @GetMapping("/mi-taller")
        public String miTaller(){
          return "mi-taller";

    }
    @GetMapping("/registrar-taller")
    public String registrarTaller(){
        return "registrar-taller";

    }
    @GetMapping("/valoraciones")
    public String valoraciones(){
        return "valoraciones";

    }

    @GetMapping("/citas-pendientes")
    public String citasPendientes(){
        return "citas-pendientes";

    }
}
