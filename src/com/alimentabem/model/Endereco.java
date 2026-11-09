package com.alimentabem.model;

// REQ02: endereço preenchido automaticamente a partir do CEP (BrasilAPI)
public class Endereco implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String cep;
    private String rua;
    private String bairro;
    private String cidade;
    private String estado;

    public Endereco(String cep, String rua, String bairro, String cidade, String estado) {
        this.cep = cep;
        this.rua = rua;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
    }

    public String getCep() { return cep; }
    public String getRua() { return rua; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }
}