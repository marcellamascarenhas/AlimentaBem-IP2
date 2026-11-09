package com.alimentabem.model;

import com.alimentabem.exception.ValidacaoException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// REQ08: distribuição para uma instituição beneficiária com os itens retirados do estoque
public class Distribuicao implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private final InstituicaoBeneficiaria instituicao;
    private final LocalDate dataDistribuicao;
    private final List<ItemDistribuicao> itens = new ArrayList<>();
    private final int familiasAtendidas;
    
    public Distribuicao(InstituicaoBeneficiaria instituicao, LocalDate dataDistribuicao, int familiasAtendidas) {
        if (instituicao == null) {
            throw new IllegalArgumentException("A instituição beneficiária é obrigatória.");
        }
        if (dataDistribuicao == null) {
            throw new IllegalArgumentException("A data de distribuição é obrigatória.");
        }
        if (familiasAtendidas <= 0) {
            throw new IllegalArgumentException("O número de famílias atendidas deve ser maior que zero.");
        }
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.instituicao = instituicao;
        this.dataDistribuicao = dataDistribuicao;
        this.familiasAtendidas = familiasAtendidas;
    }

    public void adicionarItem(ItemDistribuicao item) {
        if (item == null) {
            throw new IllegalArgumentException("Item de distribuição não pode ser nulo.");
        }
        itens.add(item);
    }

    // REQ08: toda distribuição precisa ter ao menos um item retirado
    public void validarQueTemItens() {
        if (itens.isEmpty()) {
            throw new ValidacaoException("Não é possível registrar uma distribuição sem nenhum item.");
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public InstituicaoBeneficiaria getInstituicao() { return instituicao; }
    public LocalDate getDataDistribuicao() { return dataDistribuicao; }
    public List<ItemDistribuicao> getItens() { return Collections.unmodifiableList(itens); }
    public int getFamiliasAtendidas() { return familiasAtendidas; }

    @Override
    public String toString() {
        return "[" + id + "] Distribuição para " + instituicao.getNome() + " em " + dataDistribuicao + 
               " (" + familiasAtendidas + " famílias, " + itens.size() + " itens)";
    }
}