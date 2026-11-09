package com.alimentabem.controller;

import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Doacao;
import com.alimentabem.repository.IRepositorioDoacao;
import com.alimentabem.repository.IRepositorioDoador;

import java.util.List;

public class ControladorDoacao {
    private final IRepositorioDoacao repositorioDoacao;
    private final IRepositorioDoador repositorioDoador;

    public ControladorDoacao(IRepositorioDoacao repositorioDoacao, IRepositorioDoador repositorioDoador) {
        if (repositorioDoacao == null || repositorioDoador == null) {
            throw new IllegalArgumentException("Os repositórios não podem ser nulos.");
        }
        this.repositorioDoacao = repositorioDoacao;
        this.repositorioDoador = repositorioDoador;
    }

    // REQ04: registra a doação de um doador cadastrado
    // REQ15: impede registrar doação sem itens
    public void registrarDoacao(Doacao doacao) {
        validarDoacao(doacao);
        repositorioDoacao.inserir(doacao);
    }

    /** Valida a doação sem gravá-la (usado pela Fachada antes de dar entrada no estoque). */
    public void validarDoacao(Doacao doacao) {
        if (doacao == null) {
            throw new ValidacaoException("A doação não pode ser nula.");
        }

        if (doacao.getItens() == null || doacao.getItens().isEmpty()) {
            throw new ValidacaoException("A doação deve conter pelo menos um item.");
        }

        if (doacao.getDoador() == null || doacao.getDoador().getDocumento() == null) {
            throw new ValidacaoException("A doação precisa de um doador válido.");
        }

        if (repositorioDoador.buscarPorDocumento(doacao.getDoador().getDocumento()) == null) {
            throw new ValidacaoException("O doador '" + doacao.getDoador().getNome() + "' precisa estar cadastrado no sistema antes de realizar uma doação.");
        }
    }

    public List<Doacao> listarDoacoes() {
        return repositorioDoacao.listarTodos();
    }
}