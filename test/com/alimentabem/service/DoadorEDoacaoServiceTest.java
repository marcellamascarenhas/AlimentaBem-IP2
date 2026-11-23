package com.alimentabem.service;

import com.alimentabem.exception.ElementoJaCadastradoException;
import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.*;
import com.alimentabem.repository.RepositorioDoacaoMemoria;
import com.alimentabem.repository.RepositorioDoadorMemoria;
import com.alimentabem.repository.RepositorioEstoqueMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Testes das regras que antes ficavam nos controladores (REQ01, REQ04, REQ15, REQ19). */
class DoadorEDoacaoServiceTest {

    private RepositorioDoadorMemoria repoDoador;
    private RepositorioDoacaoMemoria repoDoacao;
    private EstoqueService estoque;
    private DoadorService doadorService;
    private DoacaoService doacaoService;
    private Alimento arroz;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void preparar() {
        repoDoador = new RepositorioDoadorMemoria();
        repoDoacao = new RepositorioDoacaoMemoria();
        estoque = new EstoqueService(new RepositorioEstoqueMemoria());
        doadorService = new DoadorServiceImpl(repoDoador, repoDoacao);
        doacaoService = new DoacaoServiceImpl(repoDoacao, repoDoador, estoque);
        arroz = new Alimento("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
    }

    // ---------- doadores (REQ01) ----------

    @Test
    void cpfPrecisaTerOnzeDigitosECnpjQuatorze() {
        assertThrows(ValidacaoException.class,
                () -> doadorService.cadastrar(new PessoaFisica("Ana", "1", "a@a.com", "1234567890")));
        assertThrows(ValidacaoException.class,
                () -> doadorService.cadastrar(new Empresa("Loja", "1", "l@l.com", "123", "Loja LTDA")));

        doadorService.cadastrar(new PessoaFisica("Ana", "1", "a@a.com", "52998224725"));
        doadorService.cadastrar(new Empresa("Loja", "1", "l@l.com", "12345678000199", "Loja LTDA"));

        assertEquals(2, doadorService.listarTodos().size());
    }

    @Test
    void naoPermiteDoisDoadoresComOMesmoDocumento() {
        doadorService.cadastrar(new PessoaFisica("Ana", "1", "a@a.com", "52998224725"));

        assertThrows(ElementoJaCadastradoException.class,
                () -> doadorService.cadastrar(new PessoaFisica("Outra Ana", "2", "b@b.com", "529.982.247-25")));
    }

    @Test
    void doadorComDoacoesNaoPodeSerRemovido() {
        Doador ana = new PessoaFisica("Ana", "1", "a@a.com", "52998224725");
        doadorService.cadastrar(ana);
        Doacao d = new Doacao(ana, hoje);
        d.adicionarItem(new ItemDoacao(arroz, 5, hoje.plusDays(30)));
        doacaoService.registrar(d);

        assertThrows(ValidacaoException.class, () -> doadorService.remover(ana.getDocumento()));
        assertEquals(1, doadorService.listarTodos().size());
    }

    @Test
    void doadorSemDoacoesPodeSerRemovido() {
        Doador ana = new PessoaFisica("Ana", "1", "a@a.com", "52998224725");
        doadorService.cadastrar(ana);

        doadorService.remover(ana.getDocumento());

        assertTrue(doadorService.listarTodos().isEmpty());
    }

    // ---------- doações (REQ04, REQ15, REQ19) ----------

    @Test
    void doacaoValidaVaiParaOHistoricoEParaOEstoque() {
        Doador ana = new PessoaFisica("Ana", "1", "a@a.com", "52998224725");
        doadorService.cadastrar(ana);
        Doacao d = new Doacao(ana, hoje);
        d.adicionarItem(new ItemDoacao(arroz, 5, hoje.plusDays(30)));

        doacaoService.registrar(d);

        assertEquals(1, doacaoService.listarTodas().size());
        assertEquals(1, estoque.listarLotes().size());
        assertEquals(5, estoque.listarLotes().get(0).getQuantidadeDisponivel(), 1e-9);
    }

    @Test
    void doacaoSemItensEhRejeitada() {
        Doador ana = new PessoaFisica("Ana", "1", "a@a.com", "52998224725");
        doadorService.cadastrar(ana);

        assertThrows(ValidacaoException.class, () -> doacaoService.registrar(new Doacao(ana, hoje)));
        assertTrue(doacaoService.listarTodas().isEmpty());
    }

    @Test
    void doadorPrecisaEstarCadastrado() {
        Doador fantasma = new PessoaFisica("Fantasma", "1", "f@f.com", "11144477735");
        Doacao d = new Doacao(fantasma, hoje);
        d.adicionarItem(new ItemDoacao(arroz, 1, hoje.plusDays(10)));

        assertThrows(ValidacaoException.class, () -> doacaoService.registrar(d));
        assertTrue(estoque.listarLotes().isEmpty());
    }

    @Test
    void doacaoComItemVencidoNaoGravaNemDoacaoNemEstoque() {
        Doador ana = new PessoaFisica("Ana", "1", "a@a.com", "52998224725");
        doadorService.cadastrar(ana);
        // doação datada no passado: o item ainda era válido naquele dia, mas hoje já está vencido
        Doacao antiga = new Doacao(ana, hoje.minusDays(10));
        antiga.adicionarItem(new ItemDoacao(arroz, 5, hoje.minusDays(5)));

        assertThrows(ValidacaoException.class, () -> doacaoService.registrar(antiga));

        assertTrue(doacaoService.listarTodas().isEmpty(), "a doação rejeitada não pode ficar registrada");
        assertTrue(estoque.listarLotes().isEmpty(), "o estoque não pode receber item vencido");
    }
}
