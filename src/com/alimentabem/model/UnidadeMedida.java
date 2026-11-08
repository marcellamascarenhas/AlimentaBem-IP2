package com.alimentabem.model;

// REQ03: unidades de medida dos alimentos
public enum UnidadeMedida {
    QUILOGRAMA("kg"),
    LITRO("l"),
    UNIDADE("un");

    private final String sigla;

    UnidadeMedida(String sigla) {
        this.sigla = sigla;
    }

    public String getSigla() {
        return sigla;
    }
}