package com.alimentabem.model;

import java.util.UUID;

// REQ02: instituição beneficiária: nome, CEP, endereço e capacidade mensal de famílias
public class InstituicaoBeneficiaria {
    private String id;
    private String nome;
    private String cep;
    private Endereco endereco;
    private int capacidadeMensal;

    public InstituicaoBeneficiaria(String nome, String cep, Endereco endereco, int capacidadeMensal) {
        this.id = UUID.randomUUID().toString().substring(0, 8); // Gera ID único automático
        setNome(nome);
        setCep(cep);
        this.endereco = endereco;
        setCapacidadeMensal(capacidadeMensal);
    }

    public InstituicaoBeneficiaria(String nome, String cep, int capacidadeMensal) {
        this(nome, cep, null, capacidadeMensal);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    
    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da instituição beneficiária não pode ser nulo ou vazio.");
        }
        this.nome = nome.trim();
    }

    public String getCep() { return cep; }
    
    public void setCep(String cep) { 
        this.cep = cep != null ? cep.replaceAll("\\D", "") : ""; 
    }

    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }

    public int getCapacidadeMensal() { return capacidadeMensal; }
    
    public void setCapacidadeMensal(int capacidadeMensal) {
        if (capacidadeMensal <= 0) {
            throw new IllegalArgumentException("A capacidade mensal deve ser maior que zero.");
        }
        this.capacidadeMensal = capacidadeMensal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstituicaoBeneficiaria)) return false;
        return nome.equalsIgnoreCase(((InstituicaoBeneficiaria) o).nome);
    }

    @Override
    public int hashCode() {
        return nome.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nome + " (Capacidade: " + capacidadeMensal + " famílias/mês) - CEP: " + cep + 
               (endereco != null ? " - " + endereco.getCidade() + "/" + endereco.getEstado() : "");
    }
}