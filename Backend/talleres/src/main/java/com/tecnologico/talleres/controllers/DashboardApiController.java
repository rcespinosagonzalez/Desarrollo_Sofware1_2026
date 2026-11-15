package com.tecnologico.talleres.controllers;

import com.tecnologico.talleres.security.SesionActual;
import com.tecnologico.talleres.services.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// JSON de los dashboards. SecurityConfig: /api/admin/** solo ADMIN y /api/taller/** solo TALLER
@RestController
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/admin/dashboard")
    public Map<String, Object> admin() {
        return dashboardService.resumenAdmin();
    }

    @GetMapping("/api/taller/dashboard")
    public Map<String, Object> taller() {
        return dashboardService.resumenTaller(SesionActual.usuarioId());
    }
}
