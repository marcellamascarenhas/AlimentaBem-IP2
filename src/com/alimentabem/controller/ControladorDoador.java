package com.alimentabem.controller;

import com.alimentabem.exception.ElementoJaCadastradoException;
import com.alimentabem.exception.ElementoNaoEncontradoException;
import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Doador;
import com.alimentabem.model.Empresa;
import com.alimentabem.repository.IRepositorioDoador;

import java.util.List;

public class ControladorDoador {
    private final IRepositorioDoador repositorio;

    public ControladorDoador(IRepositorioDoador repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositório de doadores não pode ser nulo.");
        }
        this.repositorio = repositorio;
    }

    // REQ01: cadastro de doador (PF ou Empresa) sem documento duplicado
    public void cadastrarDoador(Doador doador) {
        if (doador == null) {
            throw new ValidacaoException("O doador não pode ser nulo.");
        }
        if (repositorio.buscarPorDocumento(doador.getDocumento()) != null) {
            throw new ElementoJaCadastradoException("Já existe um doador registrado com o documento: " + doador.getDocumento());
        }
        repositorio.inserir(doador);
    }

    public Doador buscarPorDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            throw new ValidacaoException("Informe um documento válido para busca.");
        }
        Doador doador = repositorio.buscarPorDocumento(documento);
        if (doador == null) {
            throw new ElementoNaoEncontradoException("Nenhum doador encontrado com o documento: " + documento);
        }
        return doador;
    }

    public List<Doador> listarDoadores() {
        return repositorio.listarTodos();
    }

    public void atualizarDoador(String documento, String nome, String telefone, String email, String razaoSocial) {
        Doador doador = buscarPorDocumento(documento);
        doador.setNome(nome);
        doador.setTelefone(telefone);
        doador.setEmail(email);
        if (doador instanceof Empresa && razaoSocial != null && !razaoSocial.isBlank()) {
            ((Empresa) doador).setRazaoSocial(razaoSocial);
        }
        repositorio.atualizar(doador);
    }

    public void removerDoador(String documento) {
        Doador doador = buscarPorDocumento(documento); 
        repositorio.remover(doador); 
    }
}