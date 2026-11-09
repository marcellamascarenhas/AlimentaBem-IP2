package com.alimentabem.model;

import com.alimentabem.exception.ValidacaoException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// REQ04: doação vinculada a um doador, composta por itens de doação
public class Doacao implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private final Doador doador;
    private final LocalDate dataDoacao;
    private final List<ItemDoacao> itens = new ArrayList<>();
    
    public Doacao(Doador doador) {
        this(doador, LocalDate.now());
    }

    public Doacao(Doador doador, LocalDate dataDoacao) {
        if (doador == null) {
            throw new IllegalArgumentException("Toda doação precisa de um doador.");
        }
        if (dataDoacao == null) {
            throw new IllegalArgumentException("A data da doação é obrigatória.");
        }
        this.id = UUID.randomUUID().toString().substring(0, 8); 
        this.doador = doador; 
        this.dataDoacao = dataDoacao;
    }

    // REQ19: bloqueia item com validade já vencida na data do recebimento
    public void adicionarItem(ItemDoacao item) {
        if (item == null) {
            throw new IllegalArgumentException("O item de doação não pode ser nulo.");
        }
        if (item.getDataValidade().isBefore(dataDoacao)) {
            throw new ValidacaoException(
                "Não é possível cadastrar o item '" + item.getAlimento().getNome() + "' : a validade (" + item.getDataValidade() + ") já está vencida na data da doação (" + dataDoacao + ").");
        }
        itens.add(item);
    }

    // REQ15: impede doação sem nenhum item vinculado
    public void validarQueTemItens() {
        if (itens.isEmpty()) {
            throw new ValidacaoException("Não é possível cadastrar uma doação sem itens.");
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Doador getDoador() { return doador; }
    public LocalDate getDataDoacao() { return dataDoacao; }
    public List<ItemDoacao> getItens() { return Collections.unmodifiableList(itens); }

    @Override
    public String toString() {
        return "Doação [" + id + "] de " + doador.getNome() + " em " + dataDoacao + " com " + itens.size() + " item(ns).";
    }
}