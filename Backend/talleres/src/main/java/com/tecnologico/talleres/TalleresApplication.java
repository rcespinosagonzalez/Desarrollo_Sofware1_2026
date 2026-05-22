package com.tecnologico.talleres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Arranca Spring Boot: controladores, vistas Thymeleaf y conexion JDBC a MySQL
@SpringBootApplication
public class TalleresApplication {

    // Punto de entrada: levanta el servidor en el puerto de application.properties
    public static void main(String[] args) {
        SpringApplication.run(TalleresApplication.class, args);
    }
}
