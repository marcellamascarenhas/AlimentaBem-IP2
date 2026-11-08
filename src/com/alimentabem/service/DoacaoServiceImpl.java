package com.alimentabem.service;

import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Doacao;
import com.alimentabem.repository.IRepositorioDoacao;
import com.alimentabem.repository.IRepositorioDoador;

import java.util.List;

public class DoacaoServiceImpl implements DoacaoService {
    private final IRepositorioDoacao repositorioDoacao;
    private final IRepositorioDoador repositorioDoador;
    private final EstoqueService estoqueService;

    public DoacaoServiceImpl(IRepositorioDoacao repositorioDoacao,
                             IRepositorioDoador repositorioDoador,
                             EstoqueService estoqueService) {
        if (repositorioDoacao == null || repositorioDoador == null || estoqueService == null) {
            throw new IllegalArgumentException("Os repositórios e o serviço de estoque não podem ser nulos.");
        }
        this.repositorioDoacao = repositorioDoacao;
        this.repositorioDoador = repositorioDoador;
        this.estoqueService = estoqueService;
    }

    // REQ04: registra a doação vinculada a um doador, com seus itens (alimento, quantidade, validade)
    @Override
    public void registrar(Doacao doacao) {
        if (doacao == null) {
            throw new ValidacaoException("A doação não pode ser nula.");
        }

        // REQ15: impede o registro de uma doação sem nenhum item vinculado
        doacao.validarQueTemItens();

        if (doacao.getDoador() == null || doacao.getDoador().getDocumento() == null) {
            throw new ValidacaoException("A doação precisa de um doador válido.");
        }
        if (repositorioDoador.buscarPorDocumento(doacao.getDoador().getDocumento()) == null) {
            throw new ValidacaoException("O doador '" + doacao.getDoador().getNome()
                    + "' precisa estar cadastrado no sistema antes de realizar uma doação.");
        }

        // Dá entrada no estoque primeiro (valida validade, REQ19); só persiste a doação se tudo passar.
        estoqueService.registrarDoacao(doacao);
        repositorioDoacao.inserir(doacao);
    }

    @Override
    public List<Doacao> listarTodas() {
        return repositorioDoacao.listarTodos();
    }
}
