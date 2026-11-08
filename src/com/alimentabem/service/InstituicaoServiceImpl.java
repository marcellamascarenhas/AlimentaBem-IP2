package com.alimentabem.service;

import com.alimentabem.exception.ElementoJaCadastradoException;
import com.alimentabem.exception.ElementoNaoEncontradoException;
import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Endereco;
import com.alimentabem.model.InstituicaoBeneficiaria;
import com.alimentabem.repository.IRepositorioDistribuicao;
import com.alimentabem.repository.IRepositorioInstituicao;

import java.util.List;

public class InstituicaoServiceImpl implements InstituicaoService {
    private final IRepositorioInstituicao repoInstituicao;
    private final IRepositorioDistribuicao repoDistribuicao;
    private final ViaCepService viaCepService;

    public InstituicaoServiceImpl(IRepositorioInstituicao repoInstituicao, 
                                  IRepositorioDistribuicao repoDistribuicao, 
                                  ViaCepService viaCepService) {
        this.repoInstituicao = repoInstituicao;
        this.repoDistribuicao = repoDistribuicao;
        this.viaCepService = viaCepService;
    }

    // REQ02: cadastro de instituição com endereço obtido automaticamente pelo CEP
    // REQ14: CEP inválido/sem retorno na API impede o cadastro
    @Override
    public InstituicaoBeneficiaria cadastrar(String nome, String cep, int capacidadeMensal) {
        if (repoInstituicao.buscarPorNome(nome) != null) {
            throw new ElementoJaCadastradoException("Já existe uma instituição cadastrada com o nome: " + nome);
        }

        String cepLimpo = cep != null ? cep.replaceAll("\\D", "") : "";
        Endereco endereco = viaCepService.buscarEnderecoPorCep(cepLimpo);

        InstituicaoBeneficiaria inst = new InstituicaoBeneficiaria(nome, cepLimpo, endereco, capacidadeMensal);
        repoInstituicao.inserir(inst);
        return inst;
    }

    @Override
    public InstituicaoBeneficiaria buscarPorNome(String nome) {
        InstituicaoBeneficiaria inst = repoInstituicao.buscarPorNome(nome);
        if (inst == null) {
            throw new ElementoNaoEncontradoException("Instituição beneficiária não encontrada: " + nome);
        }
        return inst;
    }

    @Override
    public List<InstituicaoBeneficiaria> listarTodas() {
        return repoInstituicao.listarTodos();
    }

    // REQ14: CEP alterado é revalidado na API antes de atualizar
    @Override
    public void atualizar(String nome, String novoNome, String novoCep, int novaCapacidade) {
        InstituicaoBeneficiaria existente = buscarPorNome(nome);

        if (novoNome != null && !novoNome.isBlank()) {
            existente.setNome(novoNome);
        }

        // REQ14: Se o CEP for alterado, revalida na API de CEP antes de atualizar
        if (novoCep != null && !novoCep.isBlank()) {
            String cepLimpo = novoCep.replaceAll("\\D", "");
            Endereco end = viaCepService.buscarEnderecoPorCep(cepLimpo);
            existente.setEndereco(end);
        }

        if (novaCapacidade > 0) {
            existente.setCapacidadeMensal(novaCapacidade);
        }

        repoInstituicao.atualizar(existente);
    }

    // REQ20: impede excluir instituição com distribuições registradas
    @Override
    public void remover(String nome) {
        InstituicaoBeneficiaria inst = buscarPorNome(nome);
        var historico = repoDistribuicao.buscarPorInstituicao(inst.getNome());
        if (!historico.isEmpty()) {
            throw new ValidacaoException("Impossível excluir a instituição '" + nome + "': ela já possui " + 
                                         historico.size() + " distribuição(ões) registrada(s) no sistema.");
        }
        repoInstituicao.remover(inst.getId() != null ? inst.getId() : inst.getNome());
    }
}