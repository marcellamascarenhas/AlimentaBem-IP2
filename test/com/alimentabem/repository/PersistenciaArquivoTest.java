package com.alimentabem.repository;

import com.alimentabem.controller.Fachada;
import com.alimentabem.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Persistência: o que é salvo no arquivo .dat precisa voltar igual quando o sistema é aberto de novo.
 * "Reabrir o sistema" aqui significa criar uma nova Fachada apontando para o mesmo arquivo.
 */
class PersistenciaArquivoTest {

    private static final double DELTA = 1e-9;

    @TempDir
    Path pasta;

    private Path arquivo() {
        return pasta.resolve("dados.dat");
    }

    private PessoaFisica novoDoador() {
        return new PessoaFisica("Ana Souza", "81999990000", "ana@email.com", "52998224725");
    }

    @Test
    void dadosSalvosSaoLidosCorretamenteAoReabrirOSistema() {
        LocalDate hoje = LocalDate.now();
        InstituicaoBeneficiaria ong = new InstituicaoBeneficiaria("Casa Esperança", "50000000", 100);

        // --- primeira "sessão": cadastra, doa e distribui ---
        Fachada antes = new Fachada(arquivo());
        antes.cadastrarDoador(novoDoador());
        Alimento arroz = antes.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
        antes.definirValorNutricional(arroz, new ValorNutricional(130, 2.7, "manual"));

        Doacao doacao = new Doacao(antes.buscarDoadorPorDocumento("CPF: 52998224725"));
        doacao.adicionarItem(new ItemDoacao(arroz, 10, hoje.plusDays(30)));
        antes.registrarDoacao(doacao);
        antes.registrarDistribuicao(ong, Map.of(arroz, 3.0), 4, hoje);

        // --- segunda "sessão": abre o mesmo arquivo ---
        Fachada depois = new Fachada(arquivo());

        assertEquals(1, depois.listarDoadores().size());
        assertEquals("Ana Souza", depois.listarDoadores().get(0).getNome());
        assertEquals("CPF: 52998224725", depois.listarDoadores().get(0).getDocumento());

        assertEquals(1, depois.listarAlimentos().size());
        Alimento arrozLido = depois.buscarAlimentoPorNome("Arroz");
        assertEquals(UnidadeMedida.QUILOGRAMA, arrozLido.getUnidadeMedida());
        assertEquals(130, arrozLido.getValorNutricional().getCaloriasPor100g(), DELTA);

        assertEquals(1, depois.listarDoacoes().size());
        assertEquals(1, depois.listarDoacoes().get(0).getItens().size());

        List<LoteEstoque> estoque = depois.listarEstoque();
        assertEquals(1, estoque.size());
        assertEquals(7, estoque.get(0).getQuantidadeDisponivel(), DELTA, "10 doados - 3 distribuídos");
        assertEquals(hoje.plusDays(30), estoque.get(0).getDataValidade());
        assertSame(arrozLido, estoque.get(0).getAlimento(),
                "o lote precisa apontar para o MESMO alimento do catálogo, e não para uma cópia");

        assertEquals(1, depois.listarTodasDistribuicoes().size());
        Distribuicao d = depois.listarTodasDistribuicoes().get(0);
        assertEquals("Casa Esperança", d.getInstituicao().getNome());
        assertEquals(4, d.getFamiliasAtendidas());
        assertEquals(3, d.getItens().get(0).getQuantidadeRetirada(), DELTA);
        assertSame(estoque.get(0), d.getItens().get(0).getLote());
    }

    @Test
    void alteracoesEExclusoesTambemSaoGravadasNoArquivo() {
        Fachada antes = new Fachada(arquivo());
        PessoaFisica ana = novoDoador();
        antes.cadastrarDoador(ana);
        antes.cadastrarDoador(new PessoaFisica("Bruno Lima", "81988880000", "bruno@email.com", "11144477735"));
        antes.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
        antes.cadastrarAlimento("Leite", CategoriaAlimento.LATICINIOS, UnidadeMedida.LITRO);

        // alterações
        antes.atualizarDoador(ana.getDocumento(), "Ana Souza Lima", "81977770000", "nova@email.com", null);
        antes.atualizarAlimento("Arroz", "Arroz Integral", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
        // exclusões
        antes.removerDoador("CPF: 11144477735");
        antes.removerAlimento("Leite");

        Fachada depois = new Fachada(arquivo());

        assertEquals(1, depois.listarDoadores().size(), "a exclusão do Bruno precisa ter ido para o arquivo");
        Doador ana2 = depois.listarDoadores().get(0);
        assertEquals("Ana Souza Lima", ana2.getNome());
        assertEquals("81977770000", ana2.getTelefone());
        assertEquals("nova@email.com", ana2.getEmail());

        assertEquals(1, depois.listarAlimentos().size(), "a exclusão do Leite precisa ter ido para o arquivo");
        assertEquals("Arroz Integral", depois.listarAlimentos().get(0).getNome());
    }

    @Test
    void repositoriosDaCamadaDeArmazenamentoGravamInstituicoesEAlteracoes() {
        ArmazenamentoArquivo antes = ArmazenamentoArquivo.carregar(arquivo());
        InstituicaoBeneficiaria a = new InstituicaoBeneficiaria("Abrigo A", "50000000",
                new Endereco("50000000", "Rua 1", "Boa Vista", "Recife", "PE"), 50);
        InstituicaoBeneficiaria b = new InstituicaoBeneficiaria("Abrigo B", "50000001", 30);
        antes.instituicoes().inserir(a);
        antes.instituicoes().inserir(b);
        a.setCapacidadeMensal(80);
        antes.instituicoes().atualizar(a);
        antes.instituicoes().remover(b.getId());
        antes.salvar();

        ArmazenamentoArquivo depois = ArmazenamentoArquivo.carregar(arquivo());

        assertEquals(1, depois.instituicoes().listarTodos().size());
        InstituicaoBeneficiaria lida = depois.instituicoes().buscarPorNome("Abrigo A");
        assertNotNull(lida);
        assertEquals(80, lida.getCapacidadeMensal());
        assertEquals("Recife", lida.getEndereco().getCidade());
    }

    @Test
    void arquivoInexistenteComecaVazioSemCriarNada() {
        Fachada fachada = new Fachada(arquivo());

        assertTrue(fachada.listarDoadores().isEmpty());
        assertTrue(fachada.listarAlimentos().isEmpty());
        assertFalse(Files.exists(arquivo()), "só cria o arquivo quando algo for salvo");
    }

    @Test
    void arquivoCorrompidoNaoQuebraOSistemaEEhPreservado() throws IOException {
        Files.writeString(arquivo(), "isto não é um arquivo de dados válido");

        Fachada fachada = new Fachada(arquivo());

        assertTrue(fachada.listarDoadores().isEmpty(), "o sistema começa vazio");
        try (Stream<Path> arquivos = Files.list(pasta)) {
            assertTrue(arquivos.anyMatch(p -> p.getFileName().toString().contains(".ilegivel-")),
                    "o arquivo ruim é guardado com outro nome em vez de ser apagado");
        }
    }

    @Test
    void somenteMemoriaNaoGravaNadaEmDisco() throws IOException {
        Fachada fachada = new Fachada((Path) null);
        fachada.cadastrarAlimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);

        assertEquals(1, fachada.listarAlimentos().size());
        try (Stream<Path> arquivos = Files.list(pasta)) {
            assertEquals(0, arquivos.count());
        }
    }
}
