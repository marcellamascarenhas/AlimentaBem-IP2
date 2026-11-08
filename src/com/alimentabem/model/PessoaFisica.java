package com.alimentabem.model;

// REQ01: doador Pessoa Física (identificado por CPF)
public class PessoaFisica extends Doador {
    private final String cpf;
    
    public PessoaFisica(String nome, String telefone, String email, String cpf) {
        super(nome, telefone, email);
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("O CPF do doador não pode ser nulo ou vazio.");
        }
        this.cpf = cpf;
    }

    public String getCpf() { return cpf; }

    @Override
    public String getDocumento() {
        return "CPF: " + cpf;
    }
}