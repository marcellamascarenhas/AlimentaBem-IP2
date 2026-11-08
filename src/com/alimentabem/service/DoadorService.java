package com.alimentabem.service;

import com.alimentabem.model.Doador;
import java.util.List;

public interface DoadorService {
    void cadastrar(Doador doador);
    Doador buscarPorDocumento(String documento);
    List<Doador> listarTodos();
    void atualizar(String documento, String nome, String telefone, String email, String razaoSocial);
    void remover(String documento);
}
