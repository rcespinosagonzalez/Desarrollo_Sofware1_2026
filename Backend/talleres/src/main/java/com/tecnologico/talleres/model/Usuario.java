package com.tecnologico.talleres.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false,name = "nombre")
    private String nombre;

    @NotBlank(message = "El documento es obligatorio")
    @Column(nullable = false, unique = true)
    private String documento;

    @Email(message = "Email inválido")
    @Column(unique = true)
    private String email;

    private String telefono;

    private String direccion;

}
// primer entrega base de datos
// revicion completada