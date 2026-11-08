package com.alimentabem.model;

import java.time.LocalDate;

// REQ04: item de doação: alimento, quantidade e data de validade
public class ItemDoacao {
    private final Alimento alimento;
    private final double quantidade;
    private final LocalDate dataValidade;
    
    public ItemDoacao(Alimento alimento, double quantidade, LocalDate dataValidade) {
        if (alimento == null) {
            throw new IllegalArgumentException("O item de doação precisa de um alimento.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade doada deve ser maior que zero.");
        }
        if (dataValidade == null) {
            throw new IllegalArgumentException("A data de validade é obrigatória.");
        }
        this.alimento = alimento;
        this.quantidade = quantidade;
        this.dataValidade = dataValidade;
    }

    public Alimento getAlimento() { return alimento; }
    public double getQuantidade() { return quantidade; }
    public LocalDate getDataValidade() { return dataValidade; }

    @Override
    public String toString() {
        return quantidade + " " + alimento.getUnidadeMedida().getSigla() + " de " + alimento.getNome() + " (validade: " + dataValidade + ")";
    }
}