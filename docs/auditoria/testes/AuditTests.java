import com.alimentabem.controller.Fachada;
import com.alimentabem.model.*;
import com.alimentabem.repository.*;
import com.alimentabem.service.*;

import java.nio.file.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Driver de auditoria: exercita a camada de negócio com dados fictícios. */
public class AuditTests {
    static int ok = 0, fail = 0;
    static final List<String> linhas = new ArrayList<>();

    interface T { void run() throws Exception; }

    static void caso(String id, String desc, T t) {
        try {
            t.run();
            ok++; linhas.add("PASS | " + id + " | " + desc);
        } catch (Throwable e) {
            fail++; linhas.add("FAIL | " + id + " | " + desc + " | " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    static void check(boolean c, String msg) { if (!c) throw new AssertionError(msg); }
    static String lanca(T t) {
        try { t.run(); } catch (Throwable e) { return e.getClass().getSimpleName() + ": " + e.getMessage(); }
        return null;
    }

    static Fachada mem() { return new Fachada((Path) null); }
    static Path tmpArquivo() throws Exception {
        Path dir = Files.createTempDirectory("alimentabem-teste");
        return dir.resolve("dados.dat");
    }

    public static void main(String[] a) throws Exception {
        LocalDate hoje = LocalDate.now();
        boolean rede = a.length > 0 && a[0].equals("--rede");

        // ---------- DOADORES ----------
        caso("F01", "Cadastrar PF e Empresa (REQ01)", () -> {
            Fachada f = mem();
            f.cadastrarDoador(new PessoaFisica("Fulano Teste", "81 0000-0000", "fulano@teste.invalid", "11122233344"));
            f.cadastrarDoador(new Empresa("Mercado Ficticio", "", "", "11222333000144", "Mercado Ficticio LTDA"));
            check(f.listarDoadores().size() == 2, "esperava 2 doadores");
        });
        caso("F02", "Documento duplicado e rejeitado", () -> {
            Fachada f = mem();
            f.cadastrarDoador(new PessoaFisica("A", "", "", "11122233344"));
            String e = lanca(() -> f.cadastrarDoador(new PessoaFisica("B", "", "", "111.222.333-44")));
            check(e != null && e.contains("ElementoJaCadastrado"), "duplicado aceito: " + e);
        });
        caso("F03", "Buscar doador por documento com mascara", () -> {
            Fachada f = mem();
            f.cadastrarDoador(new PessoaFisica("A", "", "", "11122233344"));
            check(f.buscarDoadorPorDocumento("111.222.333-44") != null, "nao achou");
        });
        caso("F04", "[Console] CPF sem digitos ('abc') e rejeitado ou buscavel", () -> {
            Fachada f = mem();
            String e1 = lanca(() -> f.cadastrarDoador(new PessoaFisica("A", "", "", "abc")));
            String e2 = lanca(() -> f.cadastrarDoador(new PessoaFisica("B", "", "", "abc")));
            check(e1 != null || e2 != null, "dois doadores com CPF 'abc' foram aceitos (duplicidade nao detectada)");
        });
        caso("F05", "Remover doador com doacao e bloqueado", () -> {
            Fachada f = mem();
            Doador d = new PessoaFisica("A", "", "", "11122233344");
            f.cadastrarDoador(d);
            Alimento al = f.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            Doacao dc = new Doacao(d, hoje); dc.adicionarItem(new ItemDoacao(al, 5, hoje.plusDays(30)));
            f.registrarDoacao(dc);
            check(lanca(() -> f.removerDoador("11122233344")) != null, "removeu doador com doacao");
        });
        caso("F06", "Atualizar doador (nome/email)", () -> {
            Fachada f = mem();
            f.cadastrarDoador(new PessoaFisica("A", "", "", "11122233344"));
            f.atualizarDoador("11122233344", "Novo Nome", "1", "x@y.invalid", null);
            check(f.buscarDoadorPorDocumento("11122233344").getNome().equals("Novo Nome"), "nao atualizou");
        });

        // ---------- ALIMENTOS ----------
        caso("F07", "Alimento duplicado (case-insensitive) rejeitado (REQ03)", () -> {
            Fachada f = mem();
            f.cadastrarAlimento("Feijao", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            check(lanca(() -> f.cadastrarAlimento("  FEIJAO ", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA)) != null, "aceitou duplicado");
        });
        caso("F08", "Remover alimento com lote e bloqueado", () -> {
            Fachada f = mem();
            Doador d = new PessoaFisica("A", "", "", "11122233344"); f.cadastrarDoador(d);
            Alimento al = f.cadastrarAlimento("Leite", CategoriaAlimento.LATICINIOS, UnidadeMedida.LITRO);
            Doacao dc = new Doacao(d, hoje); dc.adicionarItem(new ItemDoacao(al, 2, hoje.plusDays(10))); f.registrarDoacao(dc);
            check(lanca(() -> f.removerAlimento("Leite")) != null, "removeu alimento com lote");
        });
        caso("F09", "Editar unidade de alimento com estoque e bloqueado", () -> {
            Fachada f = mem();
            Doador d = new PessoaFisica("A", "", "", "11122233344"); f.cadastrarDoador(d);
            Alimento al = f.cadastrarAlimento("Leite", CategoriaAlimento.LATICINIOS, UnidadeMedida.LITRO);
            Doacao dc = new Doacao(d, hoje); dc.adicionarItem(new ItemDoacao(al, 2, hoje.plusDays(10))); f.registrarDoacao(dc);
            String e = lanca(() -> f.atualizarAlimento("Leite", "Leite", CategoriaAlimento.LATICINIOS, UnidadeMedida.UNIDADE));
            check(e != null, "unidade alterada de L para un com 2 L em estoque (estoque passa a significar 2 unidades)");
        });

        // ---------- DOACOES ----------
        caso("F10", "Doacao sem itens rejeitada (REQ15)", () -> {
            Fachada f = mem();
            Doador d = new PessoaFisica("A", "", "", "11122233344"); f.cadastrarDoador(d);
            check(lanca(() -> f.registrarDoacao(new Doacao(d, hoje))) != null, "aceitou doacao vazia");
        });
        caso("F11", "Item vencido rejeitado (REQ19)", () -> {
            Fachada f = mem();
            Doador d = new PessoaFisica("A", "", "", "11122233344"); f.cadastrarDoador(d);
            Alimento al = f.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            Doacao dc = new Doacao(d, hoje);
            check(lanca(() -> dc.adicionarItem(new ItemDoacao(al, 1, hoje.minusDays(1)))) != null, "aceitou vencido");
        });
        caso("F12", "Doacao de doador nao cadastrado rejeitada", () -> {
            Fachada f = mem();
            Alimento al = f.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            Doacao dc = new Doacao(new PessoaFisica("X", "", "", "99988877766"), hoje);
            dc.adicionarItem(new ItemDoacao(al, 1, hoje.plusDays(5)));
            check(lanca(() -> f.registrarDoacao(dc)) != null, "aceitou doador inexistente");
        });
        caso("F13", "Atomicidade: doacao rejeitada pelo estoque NAO deve ficar registrada", () -> {
            Fachada f = mem();
            Doador d = new PessoaFisica("A", "", "", "11122233344"); f.cadastrarDoador(d);
            Alimento al = f.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            // doacao com data retroativa: item valido na data da doacao, mas ja vencido hoje
            Doacao dc = new Doacao(d, hoje.minusDays(10));
            dc.adicionarItem(new ItemDoacao(al, 3, hoje.minusDays(2)));
            String e = lanca(() -> f.registrarDoacao(dc));
            check(e != null, "deveria rejeitar");
            check(f.listarDoacoes().isEmpty(), "doacao ficou gravada sem lotes (" + f.listarDoacoes().size() + " doacao, " + f.listarEstoque().size() + " lotes) apos erro: " + e);
        });
        caso("F14", "Mesmo alimento+validade agrupa no mesmo lote (REQ07)", () -> {
            Fachada f = mem();
            Doador d = new PessoaFisica("A", "", "", "11122233344"); f.cadastrarDoador(d);
            Alimento al = f.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            for (int i = 0; i < 2; i++) { Doacao dc = new Doacao(d, hoje); dc.adicionarItem(new ItemDoacao(al, 5, hoje.plusDays(20))); f.registrarDoacao(dc); }
            check(f.listarEstoque().size() == 1 && f.listarEstoque().get(0).getQuantidadeDisponivel() == 10, "nao agrupou");
        });

        // ---------- DISTRIBUICOES (sem rede: instituicao criada direto no repo) ----------
        IRepositorioEstoque re = new RepositorioEstoqueMemoria();
        EstoqueService es = new EstoqueService(re);
        IRepositorioDistribuicao rd = new RepositorioDistribuicaoMemoria();
        DistribuicaoServiceImpl ds = new DistribuicaoServiceImpl(rd, es);
        Alimento arroz = new Alimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
        Alimento leite = new Alimento("Leite", CategoriaAlimento.LATICINIOS, UnidadeMedida.LITRO);
        LoteEstoque lVencido = new LoteEstoque(arroz, 50, hoje.minusDays(3)); re.adicionarLote(lVencido);
        LoteEstoque lPerto = new LoteEstoque(arroz, 10, hoje.plusDays(2)); re.adicionarLote(lPerto);
        LoteEstoque lLonge = new LoteEstoque(arroz, 20, hoje.plusDays(60)); re.adicionarLote(lLonge);
        LoteEstoque lLeite = new LoteEstoque(leite, 5, hoje.plusDays(5)); re.adicionarLote(lLeite);
        InstituicaoBeneficiaria inst = new InstituicaoBeneficiaria("Casa Ficticia", "01001000", 10);

        caso("F15", "FIFO: lote mais proximo do vencimento sai primeiro (REQ09/18) e vencido e ignorado (REQ13)", () -> {
            Distribuicao d = ds.registrarDistribuicao(inst, Map.of(arroz, 15.0), 2, hoje);
            check(d.getItens().size() == 2, "itens=" + d.getItens().size());
            check(d.getItens().get(0).getLote() == lPerto && d.getItens().get(0).getQuantidadeRetirada() == 10, "1o lote errado");
            check(d.getItens().get(1).getLote() == lLonge && d.getItens().get(1).getQuantidadeRetirada() == 5, "2o lote errado");
            check(lVencido.getQuantidadeDisponivel() == 50, "mexeu no vencido");
        });
        caso("F16", "Excesso de estoque bloqueado (REQ16), mensagem cita lotes vencidos", () -> {
            String e = lanca(() -> ds.registrarDistribuicao(inst, Map.of(arroz, 1000.0), 1, hoje));
            check(e != null && e.contains("insuficiente"), "e=" + e);
        });
        caso("F17", "Atomicidade: 2 itens, 2o insuficiente -> estoque do 1o intacto", () -> {
            double antes = lLeite.getQuantidadeDisponivel(), antesA = lLonge.getQuantidadeDisponivel();
            Map<Alimento, Double> m = new LinkedHashMap<>(); m.put(leite, 1.0); m.put(arroz, 9999.0);
            check(lanca(() -> ds.registrarDistribuicao(inst, m, 1, hoje)) != null, "aceitou");
            check(lLeite.getQuantidadeDisponivel() == antes && lLonge.getQuantidadeDisponivel() == antesA, "estoque alterado");
        });
        caso("F18", "Capacidade mensal (REQ17): 2 + 9 > 10 bloqueia", () -> {
            check(lanca(() -> ds.registrarDistribuicao(inst, Map.of(leite, 1.0), 9, hoje)) != null, "nao bloqueou");
        });
        caso("F19", "Capacidade mensal: 2 + 8 = 10 permitido", () -> {
            ds.registrarDistribuicao(inst, Map.of(leite, 1.0), 8, hoje);
        });
        caso("F20", "Retirada direta de lote vencido bloqueada (REQ13)", () -> {
            check(lanca(() -> es.retirar(lVencido.getId(), 1)) != null, "retirou vencido");
        });
        caso("F21", "Listar vencendo em 7 dias (REQ12) exclui vencidos e distantes", () -> {
            List<LoteEstoque> v = es.listarVencendoEm(7);
            check(v.contains(lLeite) && !v.contains(lVencido) && !v.contains(lLonge), "lista=" + v);
        });
        caso("F22", "Data de distribuicao futura e rejeitada", () -> {
            check(lanca(() -> ds.registrarDistribuicao(new InstituicaoBeneficiaria("Outra", "01001000", 5),
                    Map.of(leite, 0.5), 1, hoje.plusYears(5))) != null, "aceitou data daqui a 5 anos");
        });
        caso("F23", "Precisao: retirar 0.1 + 0.2 de lote com 0.3", () -> {
            Alimento oleo = new Alimento("Oleo", CategoriaAlimento.ENLATADOS, UnidadeMedida.LITRO);
            LoteEstoque l = new LoteEstoque(oleo, 0.3, hoje.plusDays(30)); re.adicionarLote(l);
            InstituicaoBeneficiaria i2 = new InstituicaoBeneficiaria("Inst P", "01001000", 100);
            ds.registrarDistribuicao(i2, Map.of(oleo, 0.1), 1, hoje);
            ds.registrarDistribuicao(i2, Map.of(oleo, 0.2), 1, hoje);
            check(Math.abs(l.getQuantidadeDisponivel()) < 1e-9, "saldo=" + l.getQuantidadeDisponivel());
        });

        // ---------- INSTITUICOES / REQ20 (repo direto) ----------
        caso("F24", "REQ20: remover instituicao com distribuicao bloqueado", () -> {
            IRepositorioInstituicao ri = new RepositorioInstituicaoMemoria();
            ri.inserir(inst);
            InstituicaoServiceImpl is = new InstituicaoServiceImpl(ri, rd, new ViaCepService());
            check(lanca(() -> is.remover("casa ficticia")) != null, "removeu");
        });
        caso("F25", "Renomear instituicao para nome ja existente e rejeitado", () -> {
            IRepositorioInstituicao ri = new RepositorioInstituicaoMemoria();
            ri.inserir(new InstituicaoBeneficiaria("Inst A", "01001000", 5));
            ri.inserir(new InstituicaoBeneficiaria("Inst B", "01001000", 5));
            InstituicaoServiceImpl is = new InstituicaoServiceImpl(ri, rd, new ViaCepService());
            String e = lanca(() -> is.atualizar("Inst B", "inst a", null, 0));
            check(e != null, "duas instituicoes com o mesmo nome 'Inst A' agora existem");
        });
        caso("F26", "REQ14: CEP com formato invalido rejeitado (sem rede)", () -> {
            check(lanca(() -> new ViaCepService().buscarEnderecoPorCep("123")) != null, "aceitou");
        });

        // ---------- RELATORIOS ----------
        caso("F27", "REQ10: CSV com doador contendo ';' continua com 7 colunas", () -> {
            Doador d = new PessoaFisica("Silva; Souza", "", "", "11122233344");
            Alimento al = new Alimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            Doacao dc = new Doacao(d, hoje); dc.adicionarItem(new ItemDoacao(al, 1, hoje.plusDays(9)));
            Path p = Files.createTempFile("audit", ".csv");
            new RelatorioService(new NutricaoService()).exportarRelatorioDoacoesCsv(List.of(dc), hoje.minusDays(1), hoje, null, p.toString());
            List<String> ls = Files.readAllLines(p);
            int cols = ls.get(1).replaceAll("\"[^\"]*\"", "X").split(";", -1).length; // ignora ; dentro de aspas
            Files.delete(p);
            check(cols == 7, "linha tem " + cols + " colunas: " + ls.get(1));
        });
        caso("F28", "REQ06: 10 ovos de 50 g (140 kcal/100g) = 700 kcal (peso por unidade)", () -> {
            Alimento ovo = new Alimento("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE);
            ovo.setPesoPorUnidadeGramas(50.0);
            ovo.setValorNutricional(new ValorNutricional(140, 12, "manual"));
            Doacao dc = new Doacao(new PessoaFisica("A", "", "", "1"), hoje); dc.adicionarItem(new ItemDoacao(ovo, 10, hoje.plusDays(9)));
            RelatorioService.ResultadoNutricional r = new RelatorioService(new NutricaoService()).calcularNutricionalDaDoacao(dc);
            check(Math.abs(r.getTotalCalorias() - 700) < 1e-6 && Math.abs(r.getTotalProteinas() - 60) < 1e-6,
                  "10 ovos = " + r.getTotalCalorias() + " kcal / " + r.getTotalProteinas() + " g");
        });
        caso("U01", "Cadastrar alimento em UNIDADE sem peso e rejeitado", () -> {
            Fachada f = mem();
            check(lanca(() -> f.cadastrarAlimento("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE)) != null, "aceitou sem peso");
            check(lanca(() -> f.cadastrarAlimento("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE, 0.0)) != null, "aceitou peso 0");
            check(f.listarAlimentos().isEmpty(), "gravou alimento invalido");
        });
        caso("U02", "Peso por unidade e salvo e sobrevive ao reinicio", () -> {
            Path arq = tmpArquivo();
            new Fachada(arq).cadastrarAlimento("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE, 50.0);
            check(new Fachada(arq).buscarAlimentoPorNome("Ovo").getPesoPorUnidadeGramas() == 50.0, "peso perdido");
        });
        caso("U03", "Alimento em kg ignora peso por unidade", () -> {
            Fachada f = mem();
            Alimento ar = f.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA, 999.0);
            check(ar.getPesoPorUnidadeGramas() == null && ar.gramasPara(2) == 2000, "peso aplicado a kg");
        });
        caso("U04", "Trocar para UNIDADE sem peso e rejeitado e nao altera nada", () -> {
            Fachada f = mem();
            f.cadastrarAlimento("Pao", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            check(lanca(() -> f.atualizarAlimento("Pao", "Pao Frances", CategoriaAlimento.GRAOS, UnidadeMedida.UNIDADE, null)) != null, "aceitou");
            Alimento pao = f.buscarAlimentoPorNome("Pao");
            check(pao.getUnidadeMedida() == UnidadeMedida.QUILOGRAMA, "alterou parcialmente: " + pao);
        });
        caso("U05", "Dado antigo em UNIDADE sem peso nao conta como 1 kg (fica fora do total)", () -> {
            Alimento ovo = new Alimento("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE);
            ovo.setValorNutricional(new ValorNutricional(140, 12, "manual"));
            Doacao dc = new Doacao(new PessoaFisica("A", "", "", "1"), hoje); dc.adicionarItem(new ItemDoacao(ovo, 10, hoje.plusDays(9)));
            double kcal = new RelatorioService(new NutricaoService()).calcularNutricionalDaDoacao(dc).getTotalCalorias();
            check(kcal == 0, "kcal=" + kcal);
        });
        caso("U06", "Editar alimento pela assinatura antiga mantem o peso", () -> {
            Fachada f = mem();
            f.cadastrarAlimento("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE, 50.0);
            f.atualizarAlimento("Ovo", "Ovo Caipira", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE);
            check(f.buscarAlimentoPorNome("Ovo Caipira").getPesoPorUnidadeGramas() == 50.0, "peso perdido na edicao");
        });
        caso("F29", "REQ11: relatorio nutricional agrega por instituicao no mes", () -> {
            arroz.setValorNutricional(new ValorNutricional(360, 7, "manual"));
            leite.setValorNutricional(new ValorNutricional(60, 3, "manual"));
            Map<String, RelatorioService.ResultadoNutricional> r = new RelatorioService(new NutricaoService())
                    .gerarRelatorioNutricionalPorInstituicao(rd.listarTodos(), YearMonth.now());
            check(r.containsKey("Casa Ficticia") && r.get("Casa Ficticia").getTotalCalorias() > 0, "r=" + r);
        });

        // ---------- PERSISTENCIA ----------
        caso("F30", "Persistencia: dados sobrevivem a nova instancia da Fachada (reinicio)", () -> {
            Path arq = tmpArquivo();
            Fachada f1 = new Fachada(arq);
            f1.cadastrarAlimento("Persistido", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            Fachada f2 = new Fachada(arq);
            check(!f2.listarAlimentos().isEmpty(), "repositorios apenas em memoria: dados perdidos ao reiniciar");
        });
        caso("P01", "Reinicio preserva doador, alimento, valor nutricional, doacao e lotes", () -> {
            Path arq = tmpArquivo();
            Fachada f1 = new Fachada(arq);
            Doador d = new PessoaFisica("Maria Ficticia", "", "m@exemplo.invalid", "11122233344"); f1.cadastrarDoador(d);
            Alimento al = f1.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            Doacao dc = new Doacao(d, hoje); dc.adicionarItem(new ItemDoacao(al, 7.5, hoje.plusDays(20))); f1.registrarDoacao(dc);
            f1.definirValorNutricional(al, new ValorNutricional(360, 7, "manual"));
            Fachada f2 = new Fachada(arq);
            check(f2.listarDoadores().size() == 1 && f2.listarDoadores().get(0).getEmail().equals("m@exemplo.invalid"), "doador");
            check(f2.listarEstoque().size() == 1 && f2.listarEstoque().get(0).getQuantidadeDisponivel() == 7.5, "lote");
            check(f2.listarDoacoes().size() == 1 && f2.listarDoacoes().get(0).getId().equals(dc.getId()), "doacao");
            check(f2.buscarAlimentoPorNome("arroz").getValorNutricional().getCaloriasPor100g() == 360, "valor nutricional");
        });
        caso("P02", "Reinicio mantem referencias compartilhadas (mesmo Alimento no catalogo, lote e doacao)", () -> {
            Path arq = tmpArquivo();
            Fachada f1 = new Fachada(arq);
            Doador d = new PessoaFisica("A", "", "", "11122233344"); f1.cadastrarDoador(d);
            Alimento al = f1.cadastrarAlimento("Leite", CategoriaAlimento.LATICINIOS, UnidadeMedida.LITRO);
            Doacao dc = new Doacao(d, hoje); dc.adicionarItem(new ItemDoacao(al, 2, hoje.plusDays(9))); f1.registrarDoacao(dc);
            Fachada f2 = new Fachada(arq);
            Alimento cat = f2.buscarAlimentoPorNome("Leite");
            check(cat == f2.listarEstoque().get(0).getAlimento(), "lote aponta para outra copia do alimento");
            check(cat == f2.listarDoacoes().get(0).getItens().get(0).getAlimento(), "doacao aponta para outra copia do alimento");
            check(f2.listarDoacoes().get(0).getDoador() == f2.listarDoadores().get(0), "doacao aponta para outra copia do doador");
        });
        caso("P03", "Operacao rejeitada nao altera o arquivo", () -> {
            Path arq = tmpArquivo();
            Fachada f1 = new Fachada(arq);
            Doador d = new PessoaFisica("A", "", "", "11122233344"); f1.cadastrarDoador(d);
            byte[] antes = Files.readAllBytes(arq);
            check(lanca(() -> f1.registrarDoacao(new Doacao(d, hoje))) != null, "aceitou doacao vazia");
            check(Arrays.equals(antes, Files.readAllBytes(arq)), "arquivo mudou apos erro");
        });
        caso("P04", "Arquivo corrompido: inicia vazio e preserva o arquivo com outro nome", () -> {
            Path arq = tmpArquivo();
            Files.write(arq, "isto nao e um arquivo de dados".getBytes());
            Fachada f = new Fachada(arq);
            check(f.listarAlimentos().isEmpty(), "carregou lixo");
            try (var ls = Files.list(arq.getParent())) {
                check(ls.anyMatch(x -> x.getFileName().toString().startsWith(arq.getFileName() + ".ilegivel-")), "copia nao preservada");
            }
        });
        caso("P05", "Filtro de desserializacao recusa classes fora do sistema", () -> {
            Path arq = tmpArquivo();
            try (var out = new java.io.ObjectOutputStream(Files.newOutputStream(arq))) { out.writeObject(new java.io.File("x")); }
            Fachada f = new Fachada(arq);
            check(f.listarAlimentos().isEmpty() && !Files.exists(arq), "arquivo com classe estranha foi aceito");
        });
        caso("P06", "Modo memoria (null) nao grava nada", () -> {
            Fachada f = new Fachada((Path) null);
            f.cadastrarAlimento("X", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
            check(f.getArquivoDados() == null, "tem arquivo");
        });

        // ---------- REDE ----------
        if (rede) {
            caso("N01", "BrasilAPI: CEP publico 01001-000 (Praca da Se/SP) completa endereco (REQ02)", () -> {
                Fachada f = mem();
                long t0 = System.currentTimeMillis();
                InstituicaoBeneficiaria i = f.cadastrarInstituicao("Inst Rede", "01001-000", 30);
                linhas.add("INFO | N01 | " + (System.currentTimeMillis() - t0) + " ms; endereco=" + i.getEndereco().getRua() + ", " + i.getEndereco().getCidade() + "/" + i.getEndereco().getEstado());
                check(!i.getEndereco().getCidade().isBlank(), "sem cidade");
            });
            caso("N02", "BrasilAPI: CEP inexistente 00000-000 bloqueia cadastro (REQ14)", () -> {
                Fachada f = mem();
                String e = lanca(() -> f.cadastrarInstituicao("Inst X", "00000000", 30));
                linhas.add("INFO | N02 | " + e);
                check(e != null && f.listarInstituicoes().isEmpty(), "aceitou");
            });
            caso("N03", "Atualizar CEP da instituicao atualiza o campo cep", () -> {
                Fachada f = mem();
                f.cadastrarInstituicao("Inst C", "01001000", 30);
                f.atualizarInstituicao("Inst C", null, "20040-020", 0);
                InstituicaoBeneficiaria i = f.buscarInstituicaoPorNome("Inst C");
                check(i.getCep().equals("20040020"), "cep=" + i.getCep() + " mas endereco=" + i.getEndereco().getCidade());
            });
            caso("P07", "Reinicio preserva instituicao e distribuicao; regras REQ17/REQ20/FIFO continuam valendo", () -> {
                Path arq = tmpArquivo();
                Fachada f1 = new Fachada(arq);
                Doador d = new PessoaFisica("A", "", "", "11122233344"); f1.cadastrarDoador(d);
                Alimento al = f1.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
                Doacao dc = new Doacao(d, hoje);
                dc.adicionarItem(new ItemDoacao(al, 10, hoje.plusDays(3)));
                dc.adicionarItem(new ItemDoacao(al, 20, hoje.plusDays(60)));
                f1.registrarDoacao(dc);
                InstituicaoBeneficiaria i = f1.cadastrarInstituicao("Casa Persistida", "01001000", 3);
                f1.registrarDistribuicao(i, Map.of(al, 12.0), 2, hoje);
                Fachada f2 = new Fachada(arq);
                InstituicaoBeneficiaria i2 = f2.buscarInstituicaoPorNome("Casa Persistida");
                check(i2.getEndereco().getCidade().equals("São Paulo"), "endereco");
                check(f2.listarTodasDistribuicoes().size() == 1, "distribuicao");
                check(lanca(() -> f2.registrarDistribuicao(i2, Map.of(f2.buscarAlimentoPorNome("Arroz"), 1.0), 2, hoje)) != null, "REQ17 esqueceu o historico");
                check(lanca(() -> f2.removerInstituicao("Casa Persistida")) != null, "REQ20 esqueceu o historico");
                check(lanca(() -> f2.removerDoador("11122233344")) != null, "removeu doador com doacao");
                double saldo = f2.listarEstoque().stream().mapToDouble(LoteEstoque::getQuantidadeDisponivel).sum();
                check(saldo == 18 && f2.listarEstoque().get(0).getQuantidadeDisponivel() == 0, "saldo/FIFO apos reinicio: " + saldo);
            });
            caso("N04", "USDA (NutricaoService, DEMO_KEY): 'rice' retorna kcal plausivel (REQ05)", () -> {
                ValorNutricional v = new NutricaoService().consultar("rice");
                linhas.add("INFO | N04 | " + v);
                check(v.getCaloriasPor100g() > 50 && v.getCaloriasPor100g() < 900, "kcal=" + v.getCaloriasPor100g());
            });
        }

        linhas.forEach(System.out::println);
        System.out.println("TOTAL: " + ok + " pass, " + fail + " fail");
    }
}
