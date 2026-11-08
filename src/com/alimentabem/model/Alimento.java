package com.alimentabem.model;

import java.util.UUID;

// REQ03: alimento: nome, categoria e unidade de medida
// REQ05: guarda o valor nutricional obtido da API USDA
public class Alimento {
    private String id;
    private String nome;
    private CategoriaAlimento categoria;
    private UnidadeMedida unidadeMedida;
    private ValorNutricional valorNutricional;

    public Alimento(String nome, CategoriaAlimento categoria, UnidadeMedida unidadeMedida) {
        this.id = UUID.randomUUID().toString().substring(0, 8); 
        setNome(nome);
        setCategoria(categoria);
        setUnidadeMedida(unidadeMedida);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public CategoriaAlimento getCategoria() { return categoria; }
    public UnidadeMedida getUnidadeMedida() { return unidadeMedida; }
    public ValorNutricional getValorNutricional() { return valorNutricional; }
    public void setValorNutricional(ValorNutricional valorNutricional) { this.valorNutricional = valorNutricional; }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do alimento não pode ser nulo ou vazio.");
        }
        this.nome = nome.trim();
    }

    public void setCategoria(CategoriaAlimento categoria) {
        if (categoria == null) {
            throw new IllegalArgumentException("A categoria do alimento não pode ser nula.");
        }
        this.categoria = categoria;
    }

    public void setUnidadeMedida(UnidadeMedida unidadeMedida) {
        if (unidadeMedida == null) {
            throw new IllegalArgumentException("A unidade de medida não pode ser nula.");
        }
        this.unidadeMedida = unidadeMedida;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alimento)) return false;
        return nome.equalsIgnoreCase(((Alimento) o).nome);
    }

    @Override
    public int hashCode() {
        return nome.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nome + " (" + categoria + ", " + unidadeMedida.getSigla() + ")";
    }
}