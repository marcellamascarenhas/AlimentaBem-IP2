package com.alimentabem.ui;

import com.alimentabem.controller.Fachada;
import com.alimentabem.model.Alimento;
import com.alimentabem.model.Distribuicao;
import com.alimentabem.model.InstituicaoBeneficiaria;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MenuInstituicoes {
    private final Fachada fachada;
    private final Scanner scanner;

    public MenuInstituicoes(Fachada fachada, Scanner scanner) {
        this.fachada = fachada;
        this.scanner = scanner;
    }

    public void menuGestaoInstituicoes() {
        System.out.println("\n--- GESTÃO DE INSTITUIÇÕES BENEFICIÁRIAS ---");
        System.out.println("1. Cadastrar Instituição");
        System.out.println("2. Listar Instituições");
        System.out.println("3. Remover Instituição");
        System.out.print("Opção: ");
        int op = Integer.parseInt(scanner.nextLine());

        switch (op) {
            case 1 -> {
                System.out.print("Nome da Instituição: ");
                String nome = scanner.nextLine();
                System.out.print("CEP (8 dígitos): ");
                String cep = scanner.nextLine();
                System.out.print("Capacidade Mensal (famílias): ");
                int cap = Integer.parseInt(scanner.nextLine());

                InstituicaoBeneficiaria inst = fachada.cadastrarInstituicao(nome, cep, cap);
                System.out.println("Instituição cadastrada com sucesso: " + inst);
            }
            case 2 -> {
                System.out.println("\n--- Lista de Instituições ---");
                List<InstituicaoBeneficiaria> lista = fachada.listarInstituicoes();
                if (lista == null || lista.isEmpty()) {
                    System.out.println("Nenhuma instituição cadastrada.");
                } else {
                    for (InstituicaoBeneficiaria inst : lista) {
                        System.out.println(inst);
                    }
                }
            }
            case 3 -> {
                System.out.print("Nome da Instituição a remover: ");
                String nome = scanner.nextLine();
                fachada.removerInstituicao(nome);
                System.out.println("Instituição removida com sucesso!");
            }
        }
    }

    public void menuRegistrarDistribuicao() {
        System.out.println("\n--- REGISTRAR DISTRIBUIÇÃO (SAÍDA FIFO) ---");
        System.out.print("Nome da Instituição Beneficiária: ");
        String nomeInst = scanner.nextLine();
        InstituicaoBeneficiaria inst = fachada.buscarInstituicaoPorNome(nomeInst);

        System.out.print("Quantidade de Famílias Atendidas nesta distribuição: ");
        int familias = Integer.parseInt(scanner.nextLine());

        Map<Alimento, Double> itensPedidos = new HashMap<>();
        boolean adicionando = true;

        while (adicionando) {
            System.out.print("Nome do Alimento a distribuir: ");
            String nomeAlimento = scanner.nextLine();
            Alimento alimento = fachada.buscarAlimentoPorNome(nomeAlimento);

            System.out.print("Quantidade desejada (" + alimento.getUnidadeMedida().getSigla() + "): ");
            double qtd = Double.parseDouble(scanner.nextLine());

            itensPedidos.merge(alimento, qtd, Double::sum); // soma se o mesmo alimento for informado de novo

            System.out.print("Deseja adicionar mais um alimento a esta distribuição? (S/N): ");
            String resp = scanner.nextLine();
            if (!resp.equalsIgnoreCase("S")) adicionando = false;
        }

        Distribuicao dist = fachada.registrarDistribuicao(inst, itensPedidos, familias, LocalDate.now());
        System.out.println("Distribuição realizada com sucesso!");
        System.out.println(dist);
    }
}