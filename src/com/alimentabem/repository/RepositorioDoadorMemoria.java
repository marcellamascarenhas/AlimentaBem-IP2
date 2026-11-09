package com.alimentabem.repository;

import com.alimentabem.model.Doador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RepositorioDoadorMemoria implements IRepositorioDoador, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Doador> doadores = new ArrayList<>();

    @Override
    public void inserir(Doador doador) {
        if (doador != null) {
            doadores.add(doador);
        }
    }

    @Override
    public Doador buscarPorDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        
        // Extrai apenas os números do documento
        String procurado = documento.replaceAll("\\D", "");
        if (procurado.isEmpty()) {
            return null;
        }

        // Comparação exata apenas entre dígitos para evitar falsos positivos com 'contains'
        return doadores.stream()
                .filter(d -> d.getDocumento().replaceAll("\\D", "").equals(procurado))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Doador> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(doadores));
    }

    @Override
    public void remover(Doador doador) {
        if (doador != null) {
            remover(doador.getDocumento());
        }
    }

    // Sobrecarga para remoção direta pelo documento
    public void remover(String documento) {
        if (documento != null && !documento.isBlank()) {
            String docAlvo = documento.replaceAll("\\D", "");
            if (!docAlvo.isEmpty()) {
                doadores.removeIf(d -> d.getDocumento().replaceAll("\\D", "").equals(docAlvo));
            }
        }
    }

    // Método para atualizar um doador existente no repositório
    public void atualizar(Doador doador) {
        if (doador != null) {
            Doador existente = buscarPorDocumento(doador.getDocumento());
            if (existente != null) {
                int index = doadores.indexOf(existente);
                if (index != -1) {
                    doadores.set(index, doador);
                }
            }
        }
    }
}