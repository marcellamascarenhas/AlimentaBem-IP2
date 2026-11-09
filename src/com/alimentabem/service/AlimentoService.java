package com.alimentabem.service;

import com.alimentabem.model.Alimento;
import com.alimentabem.model.CategoriaAlimento;
import com.alimentabem.model.UnidadeMedida;
import java.util.List;

public interface AlimentoService {
    Alimento cadastrar(String nome, CategoriaAlimento categoria, UnidadeMedida unidadeMedida);
    /** pesoPorUnidadeGramas é obrigatório (> 0) quando a unidade é UNIDADE; ignorado para kg/L. */
    Alimento cadastrar(String nome, CategoriaAlimento categoria, UnidadeMedida unidadeMedida, Double pesoPorUnidadeGramas);
    Alimento buscarPorNome(String nome);
    List<Alimento> listarTodos();
    void atualizar(String nome, String novoNome, CategoriaAlimento categoria, UnidadeMedida unidade);
    void atualizar(String nome, String novoNome, CategoriaAlimento categoria, UnidadeMedida unidade, Double pesoPorUnidadeGramas);
    void remover(String nome);
}