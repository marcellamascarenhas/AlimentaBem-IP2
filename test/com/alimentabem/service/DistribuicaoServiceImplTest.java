package com.alimentabem.service;

import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.*;
import com.alimentabem.repository.RepositorioDistribuicaoMemoria;
import com.alimentabem.repository.RepositorioEstoqueMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Testes da distribuição: baixa de estoque por validade (FIFO), bloqueio de vencidos e demais regras. */
class DistribuicaoServiceImplTest {

    private static final double DELTA = 1e-9;

    private RepositorioEstoqueMemoria repoEstoque;
    private RepositorioDistribuicaoMemoria repoDistribuicao;
    private EstoqueService estoqueService;
    private DistribuicaoServiceImpl service;

    private Alimento arroz;
    private InstituicaoBeneficiaria instituicao;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void preparar() {
        repoEstoque = new RepositorioEstoqueMemoria();
        repoDistribuicao = new RepositorioDistribuicaoMemoria();
        estoqueService = new EstoqueService(repoEstoque);
        service = new DistribuicaoServiceImpl(repoDistribuicao, estoqueService);

        arroz = new Alimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
        instituicao = new InstituicaoBeneficiaria("Casa Esperança", "50000000", 100);
    }

    private LoteEstoque lote(Alimento alimento, double quantidade, LocalDate validade) {
        // O repositório aceita qualquer data (inclusive vencida), o que permite simular um lote que venceu no estoque.
        LoteEstoque l = new LoteEstoque(alimento, quantidade, validade);
        repoEstoque.adicionarLote(l);
        return l;
    }

    private Map<Alimento, Double> pedido(Alimento alimento, double quantidade) {
        Map<Alimento, Double> itens = new LinkedHashMap<>();
        itens.put(alimento, quantidade);
        return itens;
    }

    // ---------------------------------------------------------------- FIFO

    @Test
    void daBaixaPrimeiroNoLoteMaisProximoDoVencimento() {
        LoteEstoque tarde = lote(arroz, 10, hoje.plusDays(30));
        LoteEstoque cedo = lote(arroz, 4, hoje.plusDays(5));
        LoteEstoque meio = lote(arroz, 10, hoje.plusDays(15));

        Distribuicao d = service.registrarDistribuicao(instituicao, pedido(arroz, 6), 3, hoje);

        assertEquals(0, cedo.getQuantidadeDisponivel(), DELTA, "o lote que vence primeiro esvazia antes");
        assertEquals(8, meio.getQuantidadeDisponivel(), DELTA, "o resto sai do segundo lote mais próximo");
        assertEquals(10, tarde.getQuantidadeDisponivel(), DELTA, "o lote mais distante fica intacto");

        List<ItemDistribuicao> itens = d.getItens();
        assertEquals(2, itens.size());
        assertSame(cedo, itens.get(0).getLote());
        assertEquals(4, itens.get(0).getQuantidadeRetirada(), DELTA);
        assertSame(meio, itens.get(1).getLote());
        assertEquals(2, itens.get(1).getQuantidadeRetirada(), DELTA);
    }

    @Test
    void registraADistribuicaoNoRepositorio() {
        lote(arroz, 10, hoje.plusDays(10));

        Distribuicao d = service.registrarDistribuicao(instituicao, pedido(arroz, 2), 1, hoje);

        assertEquals(1, service.listarTodas().size());
        assertSame(d, repoDistribuicao.buscarPorId(d.getId()));
        assertEquals(1, service.buscarPorInstituicao("Casa Esperança").size());
    }

    // ------------------------------------------------------------ vencidos

    @Test
    void nuncaUsaLoteVencido() {
        LoteEstoque vencido = lote(arroz, 50, hoje.minusDays(1));
        LoteEstoque valido = lote(arroz, 5, hoje.plusDays(10));

        service.registrarDistribuicao(instituicao, pedido(arroz, 3), 1, hoje);

        assertEquals(50, vencido.getQuantidadeDisponivel(), DELTA, "o lote vencido não pode sofrer baixa");
        assertEquals(2, valido.getQuantidadeDisponivel(), DELTA);
    }

    @Test
    void naoCompletaOPedidoComLoteVencido() {
        LoteEstoque vencido = lote(arroz, 50, hoje.minusDays(1));
        LoteEstoque valido = lote(arroz, 5, hoje.plusDays(10));

        assertThrows(ValidacaoException.class,
                () -> service.registrarDistribuicao(instituicao, pedido(arroz, 6), 1, hoje));

        assertEquals(50, vencido.getQuantidadeDisponivel(), DELTA);
        assertEquals(5, valido.getQuantidadeDisponivel(), DELTA, "falha não pode deixar baixa parcial");
        assertTrue(service.listarTodas().isEmpty());
    }

    @Test
    void bloqueiaQuandoSoExisteLoteVencido() {
        LoteEstoque vencido = lote(arroz, 20, hoje.minusDays(3));

        ValidacaoException e = assertThrows(ValidacaoException.class,
                () -> service.registrarDistribuicao(instituicao, pedido(arroz, 1), 1, hoje));

        assertTrue(e.getMessage().toLowerCase().contains("vencid"), "a mensagem deve explicar que o estoque está vencido");
        assertEquals(20, vencido.getQuantidadeDisponivel(), DELTA);
    }

    @Test
    void loteQueVenceHojeAindaPodeSair() {
        LoteEstoque venceHoje = lote(arroz, 5, hoje);

        service.registrarDistribuicao(instituicao, pedido(arroz, 5), 1, hoje);

        assertEquals(0, venceHoje.getQuantidadeDisponivel(), DELTA);
    }

    // ------------------------------------------------- estoque insuficiente

    @Test
    void estoqueInsuficienteNaoAlteraNada() {
        Alimento feijao = new Alimento("Feijão", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
        LoteEstoque loteArroz = lote(arroz, 10, hoje.plusDays(10));
        LoteEstoque loteFeijao = lote(feijao, 1, hoje.plusDays(10));

        Map<Alimento, Double> itens = new LinkedHashMap<>();
        itens.put(arroz, 2.0);      // este item sozinho seria atendido...
        itens.put(feijao, 100.0);   // ...mas este não

        assertThrows(ValidacaoException.class, () -> service.registrarDistribuicao(instituicao, itens, 1, hoje));

        assertEquals(10, loteArroz.getQuantidadeDisponivel(), DELTA, "nada pode ser retirado se algum item falhar");
        assertEquals(1, loteFeijao.getQuantidadeDisponivel(), DELTA);
        assertTrue(service.listarTodas().isEmpty());
    }

    // ------------------------------------------------ quantidades inválidas

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, -0.001, Double.NaN, Double.POSITIVE_INFINITY})
    void rejeitaQuantidadeMenorOuIgualAZeroOuInvalida(double quantidade) {
        LoteEstoque l = lote(arroz, 10, hoje.plusDays(10));

        assertThrows(ValidacaoException.class,
                () -> service.registrarDistribuicao(instituicao, pedido(arroz, quantidade), 1, hoje));

        assertEquals(10, l.getQuantidadeDisponivel(), DELTA);
    }

    // ------------------------------------------------------- demais regras

    @Test
    void exigeInstituicaoDataItensEFamilias() {
        lote(arroz, 10, hoje.plusDays(10));

        assertThrows(ValidacaoException.class, () -> service.registrarDistribuicao(null, pedido(arroz, 1), 1, hoje));
        assertThrows(ValidacaoException.class, () -> service.registrarDistribuicao(instituicao, pedido(arroz, 1), 1, null));
        assertThrows(ValidacaoException.class, () -> service.registrarDistribuicao(instituicao, Map.of(), 1, hoje));
        assertThrows(ValidacaoException.class, () -> service.registrarDistribuicao(instituicao, pedido(arroz, 1), 0, hoje));
        assertThrows(ValidacaoException.class, () -> service.registrarDistribuicao(instituicao, pedido(arroz, 1), -2, hoje));
    }

    @Test
    void naoAceitaDataNoFuturo() {
        lote(arroz, 10, hoje.plusDays(10));

        assertThrows(ValidacaoException.class,
                () -> service.registrarDistribuicao(instituicao, pedido(arroz, 1), 1, hoje.plusDays(1)));
    }

    @Test
    void respeitaACapacidadeMensalDaInstituicao() {
        InstituicaoBeneficiaria pequena = new InstituicaoBeneficiaria("Abrigo Pequeno", "50000000", 10);
        lote(arroz, 100, hoje.plusDays(10));

        service.registrarDistribuicao(pequena, pedido(arroz, 1), 6, hoje);

        assertThrows(ValidacaoException.class,
                () -> service.registrarDistribuicao(pequena, pedido(arroz, 1), 5, hoje),
                "6 + 5 ultrapassa as 10 famílias/mês");
        assertDoesNotThrow(() -> service.registrarDistribuicao(pequena, pedido(arroz, 1), 4, hoje),
                "6 + 4 chega exatamente ao limite");
        assertEquals(2, service.listarTodas().size());
    }

    @Test
    void construtorExigeDependencias() {
        assertThrows(IllegalArgumentException.class, () -> new DistribuicaoServiceImpl(null, estoqueService));
        assertThrows(IllegalArgumentException.class, () -> new DistribuicaoServiceImpl(repoDistribuicao, null));
    }
}
