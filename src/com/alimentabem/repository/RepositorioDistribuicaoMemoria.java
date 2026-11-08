package com.alimentabem.repository;

import com.alimentabem.model.Distribuicao;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RepositorioDistribuicaoMemoria implements IRepositorioDistribuicao {
    private final List<Distribuicao> distribuicoes = new ArrayList<>();

    @Override
    public void inserir(Distribuicao elemento) {
        if (elemento != null) {
            distribuicoes.add(elemento);
        }
    }

    @Override
    public Distribuicao buscarPorId(String id) {
        if (id == null) return null;
        for (Distribuicao d : distribuicoes) {
            if (id.equalsIgnoreCase(d.getId())) {
                return d;
            }
        }
        return null;
    }

    @Override
    public List<Distribuicao> listarTodos() {
        return new ArrayList<>(distribuicoes);
    }

    @Override
    public void remover(String id) {
        if (id == null) return;
        distribuicoes.removeIf(d -> id.equalsIgnoreCase(d.getId()));
    }

    // REQ20: consulta usada para impedir a exclusão de instituição com distribuições
    @Override
    public List<Distribuicao> buscarPorInstituicao(String nomeInstituicao) {
        if (nomeInstituicao == null || nomeInstituicao.isBlank()) {
            return List.of();
        }
        return distribuicoes.stream()
                .filter(d -> d.getInstituicao() != null 
                          && d.getInstituicao().getNome() != null 
                          && d.getInstituicao().getNome().equalsIgnoreCase(nomeInstituicao.trim()))
                .collect(Collectors.toList());
    }
}