package com.alimentabem.ui;

import com.alimentabem.controller.Fachada;
import com.alimentabem.model.Alimento;
import com.alimentabem.model.CategoriaAlimento;
import com.alimentabem.model.LoteEstoque;
import com.alimentabem.model.UnidadeMedida;

import java.util.List;
import java.util.Scanner;

public class MenuAlimentos {
    private final Fachada fachada;
    private final Scanner scanner;

    public MenuAlimentos(Fachada fachada, Scanner scanner) {
        this.fachada = fachada;
        this.scanner = scanner;
    }

    public void exibirMenu() {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- GESTÃO DE ALIMENTOS E ESTOQUE ---");
            System.out.println("1. Cadastrar Alimento");
            System.out.println("2. Buscar Alimento por Nome");
            System.out.println("3. Listar Todos os Alimentos");
            System.out.println("4. Consultar Saldo em Estoque");
            System.out.println("5. Listar Itens Vencendo nos Próximos 7 Dias");
            System.out.println("0. Voltar ao Menu Principal");
            System.out.print("Escolha uma opção: ");

            try {
                opcao = Integer.parseInt(scanner.nextLine());
                switch (opcao) {
                    case 1 -> cadastrarAlimento();
                    case 2 -> buscarAlimento();
                    case 3 -> listarAlimentos();
                    case 4 -> consultarEstoque();
                    case 5 -> listarVencendo();
                    case 0 -> System.out.println("Retornando ao Menu Principal...");
                    default -> System.out.println("Opção inválida!");
                }
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private void cadastrarAlimento() {
        System.out.print("Nome do Alimento: ");
        String nome = scanner.nextLine();

        System.out.println("Categoria (1-GRAOS, 2-LATICINIOS, 3-HORTIFRUTI, 4-ENLATADOS): ");
        int catOp = Integer.parseInt(scanner.nextLine());
        if (catOp < 1 || catOp > CategoriaAlimento.values().length) {
            System.out.println("Categoria inválida. Escolha um número de 1 a " + CategoriaAlimento.values().length + ".");
            return;
        }
        CategoriaAlimento cat = CategoriaAlimento.values()[catOp - 1];

        System.out.println("Unidade de Medida (1-KG, 2-LITRO, 3-UNIDADE): ");
        int umOp = Integer.parseInt(scanner.nextLine());
        if (umOp < 1 || umOp > UnidadeMedida.values().length) {
            System.out.println("Unidade inválida. Escolha um número de 1 a " + UnidadeMedida.values().length + ".");
            return;
        }
        UnidadeMedida um = UnidadeMedida.values()[umOp - 1];

        Double peso = null;
        if (um == UnidadeMedida.UNIDADE) {
            System.out.print("Peso médio de 1 unidade em gramas (ex.: 50 para um ovo): ");
            peso = Double.parseDouble(scanner.nextLine().trim().replace(',', '.'));
        }

        fachada.cadastrarAlimento(nome, cat, um, peso);
        System.out.println("Alimento '" + nome + "' cadastrado com sucesso!");
    }

    private void buscarAlimento() {
        System.out.print("Digite o nome do alimento: ");
        String nome = scanner.nextLine();
        Alimento alimento = fachada.buscarAlimentoPorNome(nome);
        System.out.println("Encontrado: " + alimento);
    }

    private void listarAlimentos() {
        System.out.println("\n--- Lista de Alimentos ---");
        List<Alimento> lista = fachada.listarAlimentos();
        if (lista == null || lista.isEmpty()) {
            System.out.println("Nenhum alimento cadastrado.");
        } else {
            for (Alimento a : lista) {
                System.out.println(a);
            }
        }
    }

    private void consultarEstoque() {
        System.out.println("\n--- Saldo em Estoque por Lote ---");
        List<LoteEstoque> estoque = fachada.listarEstoque();
        if (estoque == null || estoque.isEmpty()) {
            System.out.println("Estoque vazio.");
        } else {
            for (LoteEstoque lote : estoque) {
                System.out.println(lote);
            }
        }
    }

    private void listarVencendo() {
        System.out.println("\n--- Alertas: Vencimento nos Próximos 7 Dias ---");
        List<LoteEstoque> alertados = fachada.listarEstoqueVencendoEm(7);
        if (alertados == null || alertados.isEmpty()) {
            System.out.println("Nenhum lote com vencimento próximo.");
        } else {
            for (LoteEstoque lote : alertados) {
                System.out.println(lote);
            }
        }
    }
}