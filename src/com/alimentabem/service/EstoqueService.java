package com.alimentabem.service;

import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Alimento;
import com.alimentabem.model.Doacao;
import com.alimentabem.model.ItemDoacao;
import com.alimentabem.model.LoteEstoque;
import com.alimentabem.repository.IRepositorioEstoque;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EstoqueService {
    private final IRepositorioEstoque repositorioEstoque;

    public EstoqueService(IRepositorioEstoque repositorioEstoque) {
        if (repositorioEstoque == null) {
            throw new IllegalArgumentException("O repositório de estoque não pode ser nulo.");
        }
        this.repositorioEstoque = repositorioEstoque;
    }

    // Classe auxiliar para representar a sugestão de retirada por lote
    public static class SugestaoRetirada {
        private final LoteEstoque lote;
        private final double quantidade;

        public SugestaoRetirada(LoteEstoque lote, double quantidade) {
            this.lote = lote;
            this.quantidade = quantidade;
        }

        public LoteEstoque getLote() { return lote; }
        public double getQuantidade() { return quantidade; }
    }

    // REQ04: dá entrada no estoque dos itens da doação
    // REQ15: rejeita doação sem itens
    // REQ19: rejeita itens com validade vencida
    public List<LoteEstoque> registrarDoacao(Doacao doacao) {
        if (doacao == null) {
            throw new IllegalArgumentException("A doação não pode ser nula.");
        }
        doacao.validarQueTemItens();
        for (ItemDoacao item : doacao.getItens()) {
            validarValidade(item.getAlimento(), item.getDataValidade());
        }
        List<LoteEstoque> afetados = new ArrayList<>();
        for (ItemDoacao item : doacao.getItens()) {
            afetados.add(registrarEntrada(item));
        }
        return afetados;
    }

    public LoteEstoque registrarEntrada(ItemDoacao item) {
        return registrarEntrada(item.getAlimento(), item.getQuantidade(), item.getDataValidade());
    }

    // REQ07: entrada agrupada por lote (alimento + data de validade)
    public LoteEstoque registrarEntrada(Alimento alimento, double quantidade, LocalDate dataValidade) {
        if (alimento == null) {
            throw new IllegalArgumentException("O alimento não pode ser nulo.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        validarValidade(alimento, dataValidade);

        for (LoteEstoque lote : repositorioEstoque.listarTodos()) {
            if (lote.getAlimento().equals(alimento) && lote.getDataValidade().equals(dataValidade)) {
                lote.adicionar(quantidade);
                return lote;
            }
        }
        LoteEstoque novo = new LoteEstoque(alimento, quantidade, dataValidade);
        repositorioEstoque.adicionarLote(novo);
        return novo;
    }

    // REQ19: bloqueia recebimento de item já vencido
    private void validarValidade(Alimento alimento, LocalDate dataValidade) {
        if (dataValidade == null) {
            throw new IllegalArgumentException("A data de validade é obrigatória.");
        }
        if (dataValidade.isBefore(LocalDate.now())) {
            throw new ValidacaoException("Não é possível receber '" + alimento.getNome()
                + "': a validade (" + dataValidade + ") já está vencida.");
        }
    }

    // REQ13: lote vencido não pode ser retirado
    // REQ16: quantidade retirada não pode exceder o saldo do lote
    public void retirar(String idLote, double quantidade) {
        LoteEstoque lote = buscarLotePorId(idLote)
            .orElseThrow(() -> new ValidacaoException("Lote não encontrado: " + idLote));
        if (lote.estaVencido()) {
            throw new ValidacaoException("O lote " + lote + " está vencido e não pode ser distribuído.");
        }
        lote.retirar(quantidade);
    }

    // Algoritmo de Sugestão FIFO (REQ18) com bloqueio de vencidos (REQ13) e alerta de falta (REQ16)
    // REQ09: sugestão automática de lotes (FIFO)
    public List<SugestaoRetirada> sugerirLotesParaDistribuicao(Alimento alimento, double quantidadeNecessaria) {
        if (alimento == null) {
            throw new IllegalArgumentException("O alimento não pode ser nulo.");
        }
        if (quantidadeNecessaria <= 0) {
            throw new IllegalArgumentException("A quantidade necessária deve ser maior que zero.");
        }

        List<SugestaoRetirada> sugestoes = new ArrayList<>();
        double restante = quantidadeNecessaria;
        double quantidadeVencida = 0;

        List<LoteEstoque> candidatos = new ArrayList<>();
        for (LoteEstoque lote : repositorioEstoque.listarTodos()) {
            if (!lote.getAlimento().equals(alimento) || lote.getQuantidadeDisponivel() <= 0) {
                continue;
            }
            if (lote.estaVencido()) {
                quantidadeVencida += lote.getQuantidadeDisponivel(); // REQ13: lote vencido nunca é sugerido
            } else {
                candidatos.add(lote);
            }
        }

        candidatos.sort(Comparator.comparing(LoteEstoque::getDataValidade)); // REQ18: ordenação FIFO por vencimento

        for (LoteEstoque lote : candidatos) {
            if (restante <= LoteEstoque.TOLERANCIA) break;
            double retirarDesteLote = Math.min(restante, lote.getQuantidadeDisponivel());
            sugestoes.add(new SugestaoRetirada(lote, retirarDesteLote));
            restante -= retirarDesteLote;
        }

        if (restante > LoteEstoque.TOLERANCIA) {
            String sigla = alimento.getUnidadeMedida() != null ? alimento.getUnidadeMedida().getSigla() : "";
            String aviso = quantidadeVencida > 0
                    ? " Existem " + quantidadeVencida + " " + sigla + " em lotes vencidos, que não podem ser distribuídos (REQ13)."
                    : "";
            throw new ValidacaoException("Estoque insuficiente de '" + alimento.getNome() + "'. Faltam "
                    + restante + " " + sigla + " (REQ16)." + aviso);
        }
        return sugestoes;
    }

    public Optional<LoteEstoque> buscarLotePorId(String idLote) {
        if (idLote == null) return Optional.empty();
        return Optional.ofNullable(repositorioEstoque.buscarPorId(idLote));
    }

    // REQ07: estoque por lote ordenado por data de validade
    public List<LoteEstoque> listarLotes() {
        return repositorioEstoque.listarTodos().stream()
            .sorted(Comparator.comparing(LoteEstoque::getDataValidade))
            .collect(Collectors.toList());
    }

    public List<LoteEstoque> listarLotesPorAlimento(Alimento alimento) {
        return listarLotes().stream()
            .filter(l -> l.getAlimento().equals(alimento))
            .collect(Collectors.toList());
    }

    public List<LoteEstoque> listarLotesDisponiveisFIFO(Alimento alimento) {
        return listarLotesPorAlimento(alimento).stream()
            .filter(l -> !l.estaVencido() && l.getQuantidadeDisponivel() > 0)
            .collect(Collectors.toList());
    }

    public double saldoDisponivel(Alimento alimento) {
        return listarLotesDisponiveisFIFO(alimento).stream()
            .mapToDouble(LoteEstoque::getQuantidadeDisponivel)
            .sum();
    }

    // REQ12: lotes com vencimento nos próximos N dias (7)
    public List<LoteEstoque> listarVencendoEm(int dias) {
        LocalDate limite = LocalDate.now().plusDays(dias);
        return listarLotes().stream()
            .filter(l -> l.getQuantidadeDisponivel() > 0)
            .filter(l -> !l.estaVencido() && !l.getDataValidade().isAfter(limite))
            .collect(Collectors.toList());
    }

    public boolean possuiLotesComSaldo(Alimento alimento) {
        return repositorioEstoque.listarTodos().stream()
            .anyMatch(l -> l.getAlimento().equals(alimento) && l.getQuantidadeDisponivel() > 0);
    }
}