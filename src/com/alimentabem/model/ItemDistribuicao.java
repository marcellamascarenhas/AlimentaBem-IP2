package com.alimentabem.model;

// REQ08: item retirado do estoque: lote de origem + quantidade
public class ItemDistribuicao implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final LoteEstoque lote;
    private final double quantidadeRetirada;

    public ItemDistribuicao(LoteEstoque lote, double quantidadeRetirada) {
        if (lote == null) {
            throw new IllegalArgumentException("O item de distribuição precisa estar vinculado a um lote.");
        }
        if (quantidadeRetirada <= 0) {
            throw new IllegalArgumentException("A quantidade retirada deve ser maior que zero.");
        }
        this.lote = lote;
        this.quantidadeRetirada = quantidadeRetirada;
    }
    
    public LoteEstoque getLote() { return lote; }
    public double getQuantidadeRetirada() { return quantidadeRetirada; }

    @Override
    public String toString() {
        return quantidadeRetirada + " " + lote.getAlimento().getUnidadeMedida().getSigla() + 
               " de " + lote.getAlimento().getNome() + " (Lote Validade: " + lote.getDataValidade() + ")";
    }
}