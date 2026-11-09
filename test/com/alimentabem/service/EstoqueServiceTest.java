package com.alimentabem.service;

import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Alimento;
import com.alimentabem.model.CategoriaAlimento;
import com.alimentabem.model.ItemDoacao;
import com.alimentabem.model.LoteEstoque;
import com.alimentabem.model.UnidadeMedida;
import com.alimentabem.repository.RepositorioEstoqueMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Testes das regras de entrada e saída de estoque. */
class EstoqueServiceTest {

    private static final double DELTA = 1e-9;

    private RepositorioEstoqueMemoria repo;
    private EstoqueService service;
    private Alimento arroz;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void preparar() {
        repo = new RepositorioEstoqueMemoria();
        service = new EstoqueService(repo);
        arroz = new Alimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
    }

    @Test
    void entradasComMesmaValidadeSaoAgrupadasNoMesmoLote() {
        LoteEstoque a = service.registrarEntrada(arroz, 3, hoje.plusDays(20));
        LoteEstoque b = service.registrarEntrada(arroz, 2, hoje.plusDays(20));

        assertSame(a, b);
        assertEquals(5, a.getQuantidadeDisponivel(), DELTA);
        assertEquals(1, service.listarLotes().size());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void naoAceitaEntradaComQuantidadeMenorOuIgualAZero(double quantidade) {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarEntrada(arroz, quantidade, hoje.plusDays(20)));
        assertTrue(service.listarLotes().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -3, Double.NaN})
    void itemDeDoacaoNaoAceitaQuantidadeInvalida(double quantidade) {
        assertThrows(IllegalArgumentException.class, () -> new ItemDoacao(arroz, quantidade, hoje.plusDays(20)));
    }

    @Test
    void naoAceitaEntradaComValidadeVencida() {
        assertThrows(ValidacaoException.class, () -> service.registrarEntrada(arroz, 5, hoje.minusDays(1)));
        assertTrue(service.listarLotes().isEmpty());
    }

    @Test
    void naoPermiteRetirarDeLoteVencido() {
        LoteEstoque vencido = new LoteEstoque(arroz, 10, hoje.minusDays(2));
        repo.adicionarLote(vencido);

        assertThrows(ValidacaoException.class, () -> service.retirar(vencido.getId(), 1));
        assertEquals(10, vencido.getQuantidadeDisponivel(), DELTA);
    }

    @Test
    void naoPermiteRetirarMaisQueOSaldoDoLote() {
        LoteEstoque lote = service.registrarEntrada(arroz, 5, hoje.plusDays(10));

        assertThrows(ValidacaoException.class, () -> service.retirar(lote.getId(), 5.5));
        assertEquals(5, lote.getQuantidadeDisponivel(), DELTA);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN})
    void naoPermiteRetirarQuantidadeInvalida(double quantidade) {
        LoteEstoque lote = service.registrarEntrada(arroz, 5, hoje.plusDays(10));

        assertThrows(IllegalArgumentException.class, () -> service.retirar(lote.getId(), quantidade));
        assertEquals(5, lote.getQuantidadeDisponivel(), DELTA);
    }

    @Test
    void sugestaoSegueOrdemDeValidadeEIgnoraVencidos() {
        repo.adicionarLote(new LoteEstoque(arroz, 100, hoje.minusDays(1)));       // vencido: nunca sugerido
        LoteEstoque longe = service.registrarEntrada(arroz, 10, hoje.plusDays(40));
        LoteEstoque perto = service.registrarEntrada(arroz, 3, hoje.plusDays(4));

        List<EstoqueService.SugestaoRetirada> sugestao = service.sugerirLotesParaDistribuicao(arroz, 5);

        assertEquals(2, sugestao.size());
        assertSame(perto, sugestao.get(0).getLote());
        assertEquals(3, sugestao.get(0).getQuantidade(), DELTA);
        assertSame(longe, sugestao.get(1).getLote());
        assertEquals(2, sugestao.get(1).getQuantidade(), DELTA);
    }

    @Test
    void saldoDisponivelNaoContaLotesVencidos() {
        repo.adicionarLote(new LoteEstoque(arroz, 100, hoje.minusDays(1)));
        service.registrarEntrada(arroz, 7, hoje.plusDays(10));

        assertEquals(7, service.saldoDisponivel(arroz), DELTA);
    }
}
