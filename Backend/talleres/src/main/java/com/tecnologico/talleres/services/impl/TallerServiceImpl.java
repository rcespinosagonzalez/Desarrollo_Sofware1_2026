package com.tecnologico.talleres.services.impl;

import com.tecnologico.talleres.model.BusquedaHistorial;
import com.tecnologico.talleres.model.Cita;
import com.tecnologico.talleres.model.Taller;
import com.tecnologico.talleres.model.UltimaCalificacion;
import com.tecnologico.talleres.model.Valoracion;
import com.tecnologico.talleres.services.TallerService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TallerServiceImpl implements TallerService {

    private final Map<Long, Taller> talleres = new ConcurrentHashMap<>();
    private final List<Valoracion> valoraciones = new ArrayList<>();
    private final List<Cita> citas = new ArrayList<>();
    private final List<BusquedaHistorial> historial = new ArrayList<>();
    private final AtomicLong nextCitaId = new AtomicLong(4);
    private UltimaCalificacion ultimaCalificacion = new UltimaCalificacion(
            "AutoServicio Centro",
            "Cambio de aceite",
            4,
            "Muy buena atención y precio justo. Recomendado."
    );

    @PostConstruct
    public void cargarDatosIniciales() {
        Taller principal = new Taller();
        principal.setId(1L);
        principal.setNombre("Taller Mecánico El Rápido");
        principal.setDireccion("Av. Pedro de Heredia 45-20, Cartagena");
        principal.setTelefono("300 555 1234");
        principal.setDescripcion(
                "Taller especializado en mecánica general, cambio de aceite, frenos y revisión técnico-mecánica. "
                        + "Más de 15 años de experiencia. Atención con cita previa."
        );
        principal.setHorario("Lun - Sáb 8:00 a 18:00");
        principal.setServicios("Mecánica general · Cambio de aceite · Frenos · Electricidad básica · Diagnóstico");
        principal.setCalificacionPromedio(4.2);
        principal.setNumValoraciones(38);
        talleres.put(1L, principal);

        valoraciones.add(new Valoracion("Ana García", 4, "Excelente servicio en el cambio de aceite. Rápidos y con buen precio.", "04/03/2026"));
        valoraciones.add(new Valoracion("Luis Rodríguez", 5, "Solucionaron el problema de frenos al instante. Muy recomendados.", "01/03/2026"));
        valoraciones.add(new Valoracion("Carmen Díaz", 3, "Buen trato, pero la espera fue un poco larga. El trabajo bien hecho.", "28/02/2026"));

        citas.add(new Cita(1L, "Carlos Mendoza", "Cambio de aceite y filtro", "10/03/2026", "09:00"));
        citas.add(new Cita(2L, "María López", "Revisión de frenos", "10/03/2026", "11:30"));
        citas.add(new Cita(3L, "Juan Pérez", "Diagnóstico eléctrico", "11/03/2026", "14:00"));

        historial.add(new BusquedaHistorial("Mecánica general - Cartagena", "05/03/2026, 14:32"));
        historial.add(new BusquedaHistorial("Cambio de aceite cerca de Barranquilla", "02/03/2026, 09:15"));
        historial.add(new BusquedaHistorial("Frenos - Medellín", "28/02/2026, 16:45"));
    }

    @Override
    public List<Taller> listarTodos() {
        return new ArrayList<>(talleres.values());
    }

    @Override
    public Taller obtenerPorId(Long id) {
        return id == null ? null : talleres.get(id);
    }

    @Override
    public Taller obtenerPrincipal() {
        return talleres.get(1L);
    }

    @Override
    public void guardar(Taller taller) {
        if (taller == null) {
            return;
        }
        if (taller.getId() != null && talleres.containsKey(taller.getId())) {
            Taller existente = talleres.get(taller.getId());
            existente.setNombre(taller.getNombre());
            existente.setDireccion(taller.getDireccion());
            existente.setTelefono(taller.getTelefono());
            existente.setDescripcion(taller.getDescripcion());
            existente.setHorario(taller.getHorario());
            if (taller.getServicios() != null && !taller.getServicios().isBlank()) {
                existente.setServicios(taller.getServicios());
            }
        } else {
            long nuevoId = talleres.keySet().stream().mapToLong(Long::longValue).max().orElse(0) + 1;
            taller.setId(nuevoId);
            if (taller.getServicios() == null || taller.getServicios().isBlank()) {
                taller.setServicios("Servicios generales");
            }
            taller.setCalificacionPromedio(0);
            taller.setNumValoraciones(0);
            talleres.put(nuevoId, taller);
        }
    }

    @Override
    public List<Valoracion> listarValoraciones() {
        return new ArrayList<>(valoraciones);
    }

    @Override
    public List<Cita> listarCitas() {
        return new ArrayList<>(citas);
    }

    @Override
    public void agregarCita(Cita cita) {
        if (cita == null) {
            return;
        }
        cita.setId(nextCitaId.getAndIncrement());
        citas.add(0, cita);
    }

    @Override
    public List<BusquedaHistorial> listarHistorial() {
        return new ArrayList<>(historial);
    }

    @Override
    public UltimaCalificacion obtenerUltimaCalificacion() {
        return ultimaCalificacion;
    }

    @Override
    public void registrarCalificacion(UltimaCalificacion calificacion) {
        if (calificacion == null) {
            return;
        }
        this.ultimaCalificacion = calificacion;
        if (calificacion.getTallerNombre() != null && !calificacion.getTallerNombre().isBlank()) {
            valoraciones.add(0, new Valoracion(
                    "Cliente",
                    calificacion.getEstrellas(),
                    calificacion.getComentario() != null ? calificacion.getComentario() : "",
                    java.time.LocalDate.now().toString()
            ));
        }
    }
}
