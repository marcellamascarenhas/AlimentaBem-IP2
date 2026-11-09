package com.alimentabem.repository;

import com.alimentabem.model.Alimento;
import com.alimentabem.model.LoteEstoque;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RepositorioEstoqueMemoria implements IRepositorioEstoque, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final List<LoteEstoque> lotes = new ArrayList<>();

    @Override
    public void adicionarLote(LoteEstoque lote) {
        if (lote != null) {
            lotes.add(lote);
        }
    }

    @Override
    public LoteEstoque buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return lotes.stream()
                .filter(l -> l.getId() != null && l.getId().equalsIgnoreCase(id.trim()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<LoteEstoque> buscarPorAlimento(Alimento alimento) {
        if (alimento == null) {
            return Collections.emptyList();
        }
        return lotes.stream()
                .filter(l -> l.getAlimento() != null && l.getAlimento().equals(alimento))
                .collect(Collectors.toList());
    }

    @Override
    public List<LoteEstoque> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(lotes));
    }

    @Override
    public void removerLote(String id) {
        if (id != null && !id.isBlank()) {
            lotes.removeIf(l -> l.getId() != null && l.getId().equalsIgnoreCase(id.trim()));
        }
    }
}