package com.alimentabem.repository;

import com.alimentabem.model.Alimento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RepositorioAlimentoMemoria implements IRepositorioAlimento, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Alimento> alimentos = new ArrayList<>();

    @Override
    public void inserir(Alimento alimento) {
        if (alimento != null) {
            alimentos.add(alimento);
        }
    }

    @Override
    public Alimento buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        for (Alimento a : alimentos) {
            if (a.getNome().equalsIgnoreCase(nome.trim())) {
                return a;
            }
        }
        return null;
    }

    public Alimento buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (Alimento a : alimentos) {
            if (a.getId() != null && a.getId().equalsIgnoreCase(id.trim())) {
                return a;
            }
        }
        return null;
    }

    @Override
    public List<Alimento> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(alimentos));
    }

    @Override
    public void atualizar(Alimento alimento) {
        if (alimento != null) {
            for (int i = 0; i < alimentos.size(); i++) {
                boolean mesmoId = alimento.getId() != null && alimentos.get(i).getId().equalsIgnoreCase(alimento.getId());
                boolean mesmoNome = alimentos.get(i).getNome().equalsIgnoreCase(alimento.getNome());
                
                if (mesmoId || mesmoNome) {
                    alimentos.set(i, alimento);
                    return;
                }
            }
        }
    }

    @Override
    public void remover(String idOuNome) {
        if (idOuNome != null && !idOuNome.isBlank()) {
            alimentos.removeIf(a -> (a.getId() != null && a.getId().equalsIgnoreCase(idOuNome)) 
                                 || a.getNome().equalsIgnoreCase(idOuNome));
        }
    }

    public void remover(Alimento alimento) {
        if (alimento != null) {
            remover(alimento.getId() != null ? alimento.getId() : alimento.getNome());
        }
    }
}