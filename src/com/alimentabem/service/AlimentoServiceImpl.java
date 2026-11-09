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
        return cadastrar(nome, categoria, unidadeMedida, null);
    }

    // REQ06: alimentos contados em UNIDADE precisam do peso médio de 1 unidade para o cálculo nutricional
    @Override
    public Alimento cadastrar(String nome, CategoriaAlimento categoria, UnidadeMedida unidadeMedida, Double pesoPorUnidadeGramas) {
        if (nome == null || nome.isBlank()) {
            throw new ValidacaoException("O nome do alimento não pode ser nulo ou vazio.");
        }
        if (repositorio.buscarPorNome(nome) != null) {
            throw new ElementoJaCadastradoException("Já existe um alimento cadastrado com o nome: " + nome);
        }
        Double peso = validarPeso(unidadeMedida, pesoPorUnidadeGramas);
        Alimento alimento = new Alimento(nome, categoria, unidadeMedida);
        alimento.setPesoPorUnidadeGramas(peso);
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
        // mantém o peso atual
        atualizar(nomeAntigo, novoNome, novaCategoria, novaUnidade, buscarPorNome(nomeAntigo).getPesoPorUnidadeGramas());
    }

    @Override
    public void atualizar(String nomeAntigo, String novoNome, CategoriaAlimento novaCategoria, UnidadeMedida novaUnidade,
                          Double pesoPorUnidadeGramas) {
        Alimento existente = buscarPorNome(nomeAntigo);
        UnidadeMedida unidadeFinal = novaUnidade != null ? novaUnidade : existente.getUnidadeMedida();
        Double peso = validarPeso(unidadeFinal, pesoPorUnidadeGramas);
        // Trocar a unidade com lotes em estoque mudaria o significado das quantidades (ex.: 2 L viram 2 un)
        if (novaUnidade != null && novaUnidade != existente.getUnidadeMedida()
                && !repositorioEstoque.buscarPorAlimento(existente).isEmpty()) {
            throw new ValidacaoException("Não é possível alterar a unidade de medida de '" + existente.getNome()
                    + "': já existem lotes registrados com esse alimento.");
        }
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
        existente.setPesoPorUnidadeGramas(peso);
        repositorio.atualizar(existente);
    }

    /** Peso obrigatório e positivo para UNIDADE; para kg/L não se aplica (fica null). */
    private static Double validarPeso(UnidadeMedida unidade, Double peso) {
        if (unidade != UnidadeMedida.UNIDADE) return null;
        if (peso == null || peso.isNaN() || peso <= 0) {
            throw new ValidacaoException("Informe o peso médio de 1 unidade em gramas (maior que zero). "
                    + "Ele é usado para calcular calorias e proteínas.");
        }
        return peso;
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