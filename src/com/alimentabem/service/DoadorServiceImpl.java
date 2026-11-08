package com.alimentabem.service;

import com.alimentabem.exception.ElementoJaCadastradoException;
import com.alimentabem.exception.ElementoNaoEncontradoException;
import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Doacao;
import com.alimentabem.model.Doador;
import com.alimentabem.model.Empresa;
import com.alimentabem.repository.IRepositorioDoacao;
import com.alimentabem.repository.IRepositorioDoador;

import java.util.List;

public class DoadorServiceImpl implements DoadorService {
    private final IRepositorioDoador repositorio;
    private final IRepositorioDoacao repositorioDoacao;

    public DoadorServiceImpl(IRepositorioDoador repositorio, IRepositorioDoacao repositorioDoacao) {
        if (repositorio == null || repositorioDoacao == null) {
            throw new IllegalArgumentException("Os repositórios não podem ser nulos.");
        }
        this.repositorio = repositorio;
        this.repositorioDoacao = repositorioDoacao;
    }

    // REQ01: cadastro de doadores (Pessoa Física ou Empresa, via herança de Doador)
    @Override
    public void cadastrar(Doador doador) {
        if (doador == null) {
            throw new ValidacaoException("O doador não pode ser nulo.");
        }
        if (repositorio.buscarPorDocumento(doador.getDocumento()) != null) {
            throw new ElementoJaCadastradoException("Já existe um doador registrado com o documento: " + doador.getDocumento());
        }
        repositorio.inserir(doador);
    }

    @Override
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

    @Override
    public List<Doador> listarTodos() {
        return repositorio.listarTodos();
    }

    @Override
    public void atualizar(String documento, String nome, String telefone, String email, String razaoSocial) {
        Doador doador = buscarPorDocumento(documento);
        doador.setNome(nome);
        doador.setTelefone(telefone);
        doador.setEmail(email);
        if (doador instanceof Empresa && razaoSocial != null && !razaoSocial.isBlank()) {
            ((Empresa) doador).setRazaoSocial(razaoSocial);
        }
        repositorio.atualizar(doador);
    }

    @Override
    public void remover(String documento) {
        Doador doador = buscarPorDocumento(documento);

        for (Doacao doacao : repositorioDoacao.listarTodos()) {
            if (doacao.getDoador() != null && doacao.getDoador().getDocumento().equals(doador.getDocumento())) {
                throw new ValidacaoException("Não é possível remover '" + doador.getNome() + "': ele já possui doações registradas.");
            }
        }

        repositorio.remover(doador);
    }

}