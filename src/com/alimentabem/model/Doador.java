package com.alimentabem.model;

// REQ01: base da herança de doadores (Pessoa Física e Empresa)
public abstract class Doador {
    private String nome;
    private String telefone;
    private String email;

    protected Doador(String nome, String telefone, String email) {
        setNome(nome);
        this.telefone = telefone;
        this.email = email;
    }

    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do doador não pode ser nulo ou vazio.");
        }
        this.nome = nome.trim();
    }

    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEmail(String email) { this.email = email; }

    public abstract String getDocumento();

    @Override
    public String toString() {
        return nome + " (" + getDocumento() + ")";
    }
}