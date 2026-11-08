package com.alimentabem.model;

// REQ01: doador Empresa (possui CNPJ e razão social)
public class Empresa extends Doador {
    private final String cnpj;
    private String razaoSocial;

    public Empresa(String nome, String telefone, String email, String cnpj, String razaoSocial) {
        super(nome, telefone, email);
        if (cnpj == null || cnpj.isBlank()) {
            throw new IllegalArgumentException("O CNPJ da empresa não pode ser nulo ou vazio.");
        }
        this.cnpj = cnpj;
        setRazaoSocial(razaoSocial);
    }

    public String getCnpj() { return cnpj; }
    public String getRazaoSocial() { return razaoSocial; }

    public void setRazaoSocial(String razaoSocial) {
        if (razaoSocial == null || razaoSocial.isBlank()) {
            throw new IllegalArgumentException("A razão social da empresa não pode ser nula ou vazia.");
        }
        this.razaoSocial = razaoSocial.trim();
    }

    @Override
    public String getDocumento() {
        return "CNPJ: " + cnpj; 
    }

    @Override
    public String toString() {
        return getNome() + " - " + razaoSocial + " (" + getDocumento() + ")"; 
    }
}