package com.alimentabem.repository;

import com.alimentabem.model.InstituicaoBeneficiaria;
import java.util.ArrayList;
import java.util.List;

public class RepositorioInstituicaoMemoria implements IRepositorioInstituicao, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final List<InstituicaoBeneficiaria> instituicoes = new ArrayList<>();

    @Override
    public void inserir(InstituicaoBeneficiaria instituicao) {
        if (instituicao != null) {
            instituicoes.add(instituicao);
        }
    }

    @Override
    public InstituicaoBeneficiaria buscarPorId(String id) {
        if (id == null) return null;
        for (InstituicaoBeneficiaria inst : instituicoes) {
            if (id.equalsIgnoreCase(inst.getId())) {
                return inst;
            }
        }
        return null;
    }

    @Override
    public InstituicaoBeneficiaria buscarPorNome(String nome) {
        if (nome == null) return null;
        for (InstituicaoBeneficiaria inst : instituicoes) {
            if (inst.getNome() != null && inst.getNome().equalsIgnoreCase(nome)) {
                return inst;
            }
        }
        return null;
    }

    @Override
    public List<InstituicaoBeneficiaria> listarTodos() {
        return new ArrayList<>(instituicoes);
    }

    @Override
    public void atualizar(InstituicaoBeneficiaria instituicao) {
        if (instituicao == null) return;
        for (int i = 0; i < instituicoes.size(); i++) {
            if (instituicoes.get(i).getId().equalsIgnoreCase(instituicao.getId())) {
                instituicoes.set(i, instituicao);
                return;
            }
        }
    }

    @Override
    public void remover(String idOuNome) {
        if (idOuNome == null) return;
        instituicoes.removeIf(inst -> 
            (inst.getId() != null && inst.getId().equalsIgnoreCase(idOuNome)) ||
            (inst.getNome() != null && inst.getNome().equalsIgnoreCase(idOuNome))
        );
    }
}