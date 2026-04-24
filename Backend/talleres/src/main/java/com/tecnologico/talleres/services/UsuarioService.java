package com.tecnologico.talleres.services;

import com.tecnologico.talleres.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    List<Usuario> findAll();

    Optional<Usuario> findById(Long id);

    Usuario save(Usuario usuario);

    void deleteById(Long id);

    Optional<Usuario> findByDocumento(String documento);
    Optional<Usuario> findByEmail(String email);
}