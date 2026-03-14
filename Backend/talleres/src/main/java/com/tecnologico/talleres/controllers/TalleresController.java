package com.tecnologico.talleres.controllers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
@RestController
public class TalleresController<Talleres> {
    @GetMapping("talleres")
    public List<Talleres>lista(){
        Talleres talleres = new Talleres();
        talleres.setNombre("Taller de prueba");
        talleres.setTelefono(324255555);
        talleres.setDireccion("Dig 25 25 25");
        List<Talleres> lista = new ArrayList<>();
        lista.add(talleres);
        return lista;

    }
}
