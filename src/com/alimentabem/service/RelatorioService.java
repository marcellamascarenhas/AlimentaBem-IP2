package com.alimentabem.service;

import com.alimentabem.model.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelatorioService {
    private final NutricaoService nutricaoService;

    public RelatorioService(NutricaoService nutricaoService) {
        this.nutricaoService = nutricaoService;
    }

    // REQ06: total de calorias e proteínas de uma doação
    public ResultadoNutricional calcularNutricionalDaDoacao(Doacao doacao) throws IOException, InterruptedException {
        double totalCalorias = 0;
        double totalProteinas = 0;
        for (ItemDoacao item : doacao.getItens()) {
            totalCalorias += calcularContribuicao(item.getAlimento(), item.getQuantidade(), true);
            totalProteinas += calcularContribuicao(item.getAlimento(), item.getQuantidade(), false);
        }
        return new ResultadoNutricional(totalCalorias, totalProteinas);
    }

    private double calcularContribuicao(Alimento alimento, double quantidadeEmKgOuL, boolean calorias)
            throws IOException, InterruptedException {
        // kg/L → ×1000 g; unidade → × peso por unidade. Sem peso cadastrado, o item não entra no total.
        Double gramas = alimento.gramasPara(quantidadeEmKgOuL);
        if (gramas == null) return 0;
        ValorNutricional info = alimento.getValorNutricional();
        if (info == null) {
            info = nutricaoService.consultar(alimento.getNome());
            alimento.setValorNutricional(info);
        }
        double fatorPara100g = gramas / 100.0;
        return fatorPara100g * (calorias ? info.getCaloriasPor100g() : info.getProteinasPor100gEmGramas());
    }

    // REQ10: relatório de doações por período e categoria, exportado em CSV
    public void exportarRelatorioDoacoesCsv(List<Doacao> todasAsDoacoes, LocalDate inicio, LocalDate fim, CategoriaAlimento categoriaFiltro, String caminhoArquivo) throws IOException {
        try (Writer escritor = new OutputStreamWriter(new FileOutputStream(caminhoArquivo), StandardCharsets.UTF_8)) {
            escritor.write("data_doacao;doador;alimento;categoria;quantidade;unidade;validade\n");
            for (Doacao doacao : todasAsDoacoes) {
                boolean dentroDoPeriodo = !doacao.getDataDoacao().isBefore(inicio) && !doacao.getDataDoacao().isAfter(fim);
                if (!dentroDoPeriodo) continue;
                for (ItemDoacao item : doacao.getItens()) {
                    if (categoriaFiltro != null && item.getAlimento().getCategoria() != categoriaFiltro) continue;
                    escritor.write(String.join(";",
                            doacao.getDataDoacao().toString(),
                            campoCsv(doacao.getDoador().getNome()),
                            campoCsv(item.getAlimento().getNome()),
                            item.getAlimento().getCategoria().toString(),
                            String.valueOf(item.getQuantidade()),
                            item.getAlimento().getUnidadeMedida().getSigla(),
                            item.getDataValidade().toString()
                    ) + "\n");
                }
            }
        }
    }

    /** Coloca entre aspas campos com ';', aspas ou quebra de linha (padrão CSV), para não deslocar colunas. */
    private static String campoCsv(String valor) {
        String s = valor == null ? "" : valor;
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // REQ11: relatório nutricional agregado por instituição no mês
    public Map<String, ResultadoNutricional> gerarRelatorioNutricionalPorInstituicao(
            List<Distribuicao> historico, YearMonth mes) throws IOException, InterruptedException {

        Map<String, ResultadoNutricional> totalPorInstituicao = new HashMap<>();
        for (Distribuicao distribuicao : historico) {
            if (!YearMonth.from(distribuicao.getDataDistribuicao()).equals(mes)) continue;
            String nomeInstituicao = distribuicao.getInstituicao().getNome();
            ResultadoNutricional acumulado =
                    totalPorInstituicao.getOrDefault(nomeInstituicao, new ResultadoNutricional(0, 0));
            for (ItemDistribuicao item : distribuicao.getItens()) {
                double calorias = calcularContribuicao(item.getLote().getAlimento(), item.getQuantidadeRetirada(), true);
                double proteinas = calcularContribuicao(item.getLote().getAlimento(), item.getQuantidadeRetirada(), false);
                acumulado = acumulado.somar(calorias, proteinas);
            }
            totalPorInstituicao.put(nomeInstituicao, acumulado);
        }
        return totalPorInstituicao;
    }

    // REQ12: itens de estoque que vencem nos próximos 7 dias
    public List<LoteEstoque> listarItensVencendoEm7Dias(EstoqueService estoqueService) {
        return estoqueService.listarVencendoEm(7); // Método alinhado
    }

    public static class ResultadoNutricional {
        private final double totalCalorias;
        private final double totalProteinas;

        public ResultadoNutricional(double totalCalorias, double totalProteinas) {
            this.totalCalorias = totalCalorias;
            this.totalProteinas = totalProteinas;
        }

        public ResultadoNutricional somar(double calorias, double proteinas) {
            return new ResultadoNutricional(this.totalCalorias + calorias, this.totalProteinas + proteinas);
        }

        public double getTotalCalorias() { return totalCalorias; }
        public double getTotalProteinas() { return totalProteinas; }

        @Override
        public String toString() {
            return String.format("%.1f kcal / %.1f g de proteína", totalCalorias, totalProteinas);
        }
    }
}