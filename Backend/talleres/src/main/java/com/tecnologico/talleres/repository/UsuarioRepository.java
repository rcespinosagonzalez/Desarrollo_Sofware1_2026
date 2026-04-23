package com.tecnologico.talleres.repository;

import com.tecnologico.talleres.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByDocumento(String documento);
    boolean existsByDocumento(String documento);
}
//revicion completada