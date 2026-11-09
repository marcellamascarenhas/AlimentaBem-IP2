package com.alimentabem.model;

import java.util.UUID;

// REQ03: alimento: nome, categoria e unidade de medida
// REQ05: guarda o valor nutricional obtido da API USDA
public class Alimento implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nome;
    private CategoriaAlimento categoria;
    private UnidadeMedida unidadeMedida;
    private ValorNutricional valorNutricional;
    private Double pesoPorUnidadeGramas; // null em dados antigos ou quando a unidade é kg/L

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

    /** Peso médio de 1 unidade, em gramas (só faz sentido quando a unidade de medida é UNIDADE). */
    public Double getPesoPorUnidadeGramas() { return pesoPorUnidadeGramas; }

    public void setPesoPorUnidadeGramas(Double gramas) {
        if (gramas != null && (gramas.isNaN() || gramas.isInfinite() || gramas <= 0)) {
            throw new IllegalArgumentException("O peso por unidade deve ser maior que zero.");
        }
        this.pesoPorUnidadeGramas = gramas;
    }

    /**
     * REQ06/REQ11: converte uma quantidade deste alimento em gramas, para o cálculo nutricional.
     * kg → ×1000; L → ×1000 (aproximação de 1 g/ml); unidade → × peso por unidade.
     * Devolve null quando não dá para calcular (unidade sem peso cadastrado).
     */
    public Double gramasPara(double quantidade) {
        switch (unidadeMedida) {
            case QUILOGRAMA:
            case LITRO:
                return quantidade * 1000.0;
            case UNIDADE:
                return pesoPorUnidadeGramas == null ? null : quantidade * pesoPorUnidadeGramas;
            default:
                return null;
        }
    }
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