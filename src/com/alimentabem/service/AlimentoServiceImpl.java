package com.alimentabem.service;

import com.alimentabem.exception.ElementoJaCadastradoException;
import com.alimentabem.exception.ElementoNaoEncontradoException;
import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Alimento;
import com.alimentabem.model.CategoriaAlimento;
import com.alimentabem.model.UnidadeMedida;
import com.alimentabem.repository.IRepositorioAlimento;
import com.alimentabem.repository.IRepositorioEstoque;

import java.util.List;

public class AlimentoServiceImpl implements AlimentoService {
    private final IRepositorioAlimento repositorio;
    private final IRepositorioEstoque repositorioEstoque;

    public AlimentoServiceImpl(IRepositorioAlimento repositorio, IRepositorioEstoque repositorioEstoque) {
        if (repositorio == null || repositorioEstoque == null) {
            throw new IllegalArgumentException("Os repositórios não podem ser nulos.");
        }
        this.repositorio = repositorio;
        this.repositorioEstoque = repositorioEstoque;
    }

    // REQ03: cadastro de alimento sem nome duplicado
    @Override
    public Alimento cadastrar(String nome, CategoriaAlimento categoria, UnidadeMedida unidadeMedida) {
        if (nome == null || nome.isBlank()) {
            throw new ValidacaoException("O nome do alimento não pode ser nulo ou vazio.");
        }
        if (repositorio.buscarPorNome(nome) != null) {
            throw new ElementoJaCadastradoException("Já existe um alimento cadastrado com o nome: " + nome);
        }
        Alimento alimento = new Alimento(nome, categoria, unidadeMedida);
        repositorio.inserir(alimento);
        return alimento;
    }

    @Override
    public Alimento buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ValidacaoException("Informe um nome válido para buscar o alimento.");
        }
        Alimento alimento = repositorio.buscarPorNome(nome);
        if (alimento == null) {
            throw new ElementoNaoEncontradoException("Alimento não encontrado: " + nome);
        }
        return alimento;
    }

    @Override
    public List<Alimento> listarTodos() {
        return repositorio.listarTodos();
    }

    @Override
    public void atualizar(String nomeAntigo, String novoNome, CategoriaAlimento novaCategoria, UnidadeMedida novaUnidade) {
        Alimento existente = buscarPorNome(nomeAntigo);
        if (novoNome != null && !novoNome.isBlank() && !novoNome.trim().equalsIgnoreCase(existente.getNome())) {
            if (repositorio.buscarPorNome(novoNome.trim()) != null) {
                throw new ElementoJaCadastradoException("Já existe um alimento cadastrado com o nome: " + novoNome.trim());
            }
            existente.setNome(novoNome);
        }
        if (novaCategoria != null) {
            existente.setCategoria(novaCategoria);
        }
        if (novaUnidade != null) {
            existente.setUnidadeMedida(novaUnidade);
        }
        repositorio.atualizar(existente);
    }

    @Override
    public void remover(String nome) {
        Alimento alimento = buscarPorNome(nome);
        if (!repositorioEstoque.buscarPorAlimento(alimento).isEmpty()) {
            throw new ValidacaoException("Não é possível remover '" + alimento.getNome() + "': já existem doações/lotes registrados com esse alimento.");
        }
        repositorio.remover(alimento.getId());
    }
}