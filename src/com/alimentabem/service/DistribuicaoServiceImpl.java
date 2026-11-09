package com.alimentabem.service;

import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.*;
import com.alimentabem.repository.IRepositorioDistribuicao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DistribuicaoServiceImpl implements DistribuicaoService {
    private final IRepositorioDistribuicao repoDistribuicao;
    private final EstoqueService estoqueService;

    public DistribuicaoServiceImpl(IRepositorioDistribuicao repoDistribuicao, EstoqueService estoqueService) {
        if (repoDistribuicao == null) {
            throw new IllegalArgumentException("O repositório de distribuições não pode ser nulo.");
        }
        if (estoqueService == null) {
            throw new IllegalArgumentException("O serviço de estoque não pode ser nulo.");
        }
        this.repoDistribuicao = repoDistribuicao;
        this.estoqueService = estoqueService;
    }

    // REQ08: registra a distribuição e baixa os itens do estoque
    // REQ09: aplica FIFO automaticamente nos lotes
    @Override
    public Distribuicao registrarDistribuicao(InstituicaoBeneficiaria instituicao,
                                             Map<Alimento, Double> itensSolicitados,
                                             int familiasAtendidas,
                                             LocalDate data) {
        if (instituicao == null) {
            throw new ValidacaoException("A instituição é obrigatória.");
        }
        if (data == null) {
            throw new ValidacaoException("A data da distribuição é obrigatória.");
        }
        if (data.isAfter(LocalDate.now())) {
            throw new ValidacaoException("A data da distribuição não pode estar no futuro.");
        }
        if (familiasAtendidas <= 0) {
            throw new ValidacaoException("O número de famílias atendidas deve ser maior que zero.");
        }
        if (itensSolicitados == null || itensSolicitados.isEmpty()) {
            throw new ValidacaoException("Selecione ao menos um item de alimento para distribuir.");
        }

        // REQ17: bloqueia distribuição se exceder a capacidade mensal da instituição
        int familiasAtendidasNoMes = calcularFamiliasAtendidasNoMes(instituicao, data);
        if (familiasAtendidasNoMes + familiasAtendidas > instituicao.getCapacidadeMensal()) {
            throw new ValidacaoException("Distribuição bloqueada: a instituição '" + instituicao.getNome() +
                "' tem capacidade para " + instituicao.getCapacidadeMensal() + " famílias/mês. Já atendeu " +
                familiasAtendidasNoMes + " este mês e tentou adicionar mais " + familiasAtendidas + ".");
        }

        Distribuicao distribuicao = new Distribuicao(instituicao, data, familiasAtendidas);

        // FASE 1: Apenas PLANEJA (não altera o estoque). Se qualquer item falhar, nada é retirado do estoque.
        List<ItemDistribuicao> plano = new ArrayList<>();
        for (Map.Entry<Alimento, Double> entry : itensSolicitados.entrySet()) {
            Alimento alimento = entry.getKey();
            Double quantidade = entry.getValue();
            if (alimento == null || quantidade == null || !(quantidade > 0) || Double.isInfinite(quantidade)) {
                throw new ValidacaoException("Informe um alimento e uma quantidade maior que zero.");
            }

            double restante = quantidade;
            double quantidadeVencida = 0;
            // Percorre TODOS os lotes do alimento (já ordenados por validade) para poder avisar quanto está vencido
            for (LoteEstoque lote : estoqueService.listarLotesPorAlimento(alimento)) {
                if (lote.getQuantidadeDisponivel() <= 0) continue;
                if (lote.estaVencido()) {                       // REQ13: lote vencido nunca é utilizado
                    quantidadeVencida += lote.getQuantidadeDisponivel();
                    continue;
                }
                if (restante <= LoteEstoque.TOLERANCIA) break;
                double qtdDoLote = Math.min(lote.getQuantidadeDisponivel(), restante); // REQ18: FIFO
                plano.add(new ItemDistribuicao(lote, qtdDoLote));
                restante -= qtdDoLote;
            }

            if (restante > LoteEstoque.TOLERANCIA) {             // REQ16: Validação de falta no estoque
                String sigla = alimento.getUnidadeMedida() != null ? alimento.getUnidadeMedida().getSigla() : "";
                String aviso = quantidadeVencida > 0
                        ? " Existem " + quantidadeVencida + " " + sigla + " em lotes vencidos, que não podem ser distribuídos (REQ13)."
                        : "";
                throw new ValidacaoException("Estoque insuficiente de '" + alimento.getNome() +
                    "'. Faltam " + restante + " " + sigla + "." + aviso);
            }
        }

        // FASE 2: Todos os itens foram aprovados na validação. Efetua as retiradas reais do estoque.
        for (ItemDistribuicao item : plano) {
            estoqueService.retirar(item.getLote().getId(), item.getQuantidadeRetirada());
            distribuicao.adicionarItem(item);
        }

        distribuicao.validarQueTemItens();
        repoDistribuicao.inserir(distribuicao);
        return distribuicao;
    }

    private int calcularFamiliasAtendidasNoMes(InstituicaoBeneficiaria instituicao, LocalDate data) {
        if (instituicao == null || data == null) return 0;
        return repoDistribuicao.listarTodos().stream()
                .filter(d -> d.getInstituicao().equals(instituicao))
                .filter(d -> d.getDataDistribuicao().getYear() == data.getYear() &&
                             d.getDataDistribuicao().getMonth() == data.getMonth())
                .mapToInt(Distribuicao::getFamiliasAtendidas)
                .sum();
    }

    @Override
    public List<Distribuicao> listarTodas() {
        return repoDistribuicao.listarTodos();
    }

    @Override
    public List<Distribuicao> buscarPorInstituicao(String nomeInstituicao) {
        return repoDistribuicao.buscarPorInstituicao(nomeInstituicao);
    }
}