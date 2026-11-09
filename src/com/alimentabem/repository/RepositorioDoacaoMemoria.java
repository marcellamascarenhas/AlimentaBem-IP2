package com.alimentabem.repository;

import com.alimentabem.model.Doacao;
import java.util.ArrayList;
import java.util.List;

public class RepositorioDoacaoMemoria implements IRepositorioDoacao, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Doacao> doacoes = new ArrayList<>();

    @Override
    public void inserir(Doacao doacao) {
        if (doacao != null) {
            doacoes.add(doacao);
        }
    }

    @Override
    public List<Doacao> listarTodos() {
        return new ArrayList<>(doacoes);
    }

    @Override
    public Doacao buscarPorId(String id) {
        if (id == null) return null;
        for (Doacao d : doacoes) {
            if (id.equalsIgnoreCase(d.getId())) {
                return d;
            }
        }
        return null;
    }

    @Override
    public void remover(String id) {
        doacoes.removeIf(d -> id != null && id.equalsIgnoreCase(d.getId()));
    }
}