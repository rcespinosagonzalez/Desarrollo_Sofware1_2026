package com.tecnologico.talleres.controllers;
import com.tecnologico.talleres.model.Taller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
@RestController
public class TalleresController<Talleres> {
    @GetMapping("Taller")
    public List<Taller>lista(){
        Taller talleres = new Taller();
        talleres.setNombre("Taller de prueba");
        talleres.setTelefono(324255555);
        talleres.setDireccion("Dig 25 25 25");
        List<Taller> lista = new ArrayList<>();
        lista.add(talleres);
        return lista;

    }
}
