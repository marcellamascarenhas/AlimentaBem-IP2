package com.alimentabem.service;

import com.alimentabem.exception.ElementoJaCadastradoException;
import com.alimentabem.exception.ElementoNaoEncontradoException;
import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Alimento;
import com.alimentabem.model.CategoriaAlimento;
import com.alimentabem.model.LoteEstoque;
import com.alimentabem.model.UnidadeMedida;
import com.alimentabem.repository.RepositorioAlimentoMemoria;
import com.alimentabem.repository.RepositorioEstoqueMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Testes da regra de negócio de alimentos (cadastro, busca, alteração e exclusão). */
class AlimentoServiceImplTest {

    private RepositorioAlimentoMemoria repoAlimento;
    private RepositorioEstoqueMemoria repoEstoque;
    private AlimentoServiceImpl service;

    @BeforeEach
    void preparar() {
        repoAlimento = new RepositorioAlimentoMemoria();
        repoEstoque = new RepositorioEstoqueMemoria();
        service = new AlimentoServiceImpl(repoAlimento, repoEstoque);
    }

    @Test
    void cadastraAlimentoValido() {
        Alimento arroz = service.cadastrar("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);

        assertEquals("Arroz", arroz.getNome());
        assertEquals(1, service.listarTodos().size());
        assertSame(arroz, service.buscarPorNome("arroz"), "a busca por nome ignora maiúsculas/minúsculas");
    }

    @Test
    void rejeitaNomeVazioOuNulo() {
        assertThrows(ValidacaoException.class,
                () -> service.cadastrar("   ", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA));
        assertThrows(ValidacaoException.class,
                () -> service.cadastrar(null, CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA));
        assertTrue(service.listarTodos().isEmpty());
    }

    @Test
    void rejeitaNomeDuplicado() {
        service.cadastrar("Feijão", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);

        assertThrows(ElementoJaCadastradoException.class,
                () -> service.cadastrar("FEIJÃO", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA));
        assertEquals(1, service.listarTodos().size());
    }

    @Test
    void alimentoEmUnidadeExigePesoMedio() {
        assertThrows(ValidacaoException.class,
                () -> service.cadastrar("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE, null));

        Alimento ovo = service.cadastrar("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE, 50.0);
        assertEquals(50.0, ovo.getPesoPorUnidadeGramas());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, -0.5, Double.NaN, Double.POSITIVE_INFINITY})
    void pesoPorUnidadeInvalidoEhRejeitado(double pesoInvalido) {
        assertThrows(ValidacaoException.class,
                () -> service.cadastrar("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE, pesoInvalido));
        assertTrue(service.listarTodos().isEmpty());
    }

    @Test
    void pesoEhIgnoradoQuandoAUnidadeEhQuiloOuLitro() {
        Alimento leite = service.cadastrar("Leite", CategoriaAlimento.LATICINIOS, UnidadeMedida.LITRO, 999.0);
        assertNull(leite.getPesoPorUnidadeGramas());
    }

    @Test
    void buscaDeAlimentoInexistenteLancaExcecao() {
        assertThrows(ElementoNaoEncontradoException.class, () -> service.buscarPorNome("Inexistente"));
        assertThrows(ValidacaoException.class, () -> service.buscarPorNome(" "));
    }

    @Test
    void atualizaNomeCategoriaEPeso() {
        service.cadastrar("Ovo", CategoriaAlimento.HORTIFRUTI, UnidadeMedida.UNIDADE, 50.0);

        service.atualizar("Ovo", "Ovo Caipira", CategoriaAlimento.LATICINIOS, UnidadeMedida.UNIDADE, 60.0);

        Alimento atualizado = service.buscarPorNome("Ovo Caipira");
        assertEquals(CategoriaAlimento.LATICINIOS, atualizado.getCategoria());
        assertEquals(60.0, atualizado.getPesoPorUnidadeGramas());
        assertThrows(ElementoNaoEncontradoException.class, () -> service.buscarPorNome("Ovo"));
    }

    @Test
    void naoAtualizaParaNomeJaExistente() {
        service.cadastrar("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
        service.cadastrar("Feijão", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);

        assertThrows(ElementoJaCadastradoException.class,
                () -> service.atualizar("Feijão", "Arroz", null, null));
    }

    @Test
    void naoTrocaAUnidadeQuandoJaExisteLoteDoAlimento() {
        Alimento arroz = service.cadastrar("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
        repoEstoque.adicionarLote(new LoteEstoque(arroz, 5, LocalDate.now().plusDays(30)));

        assertThrows(ValidacaoException.class,
                () -> service.atualizar("Arroz", null, null, UnidadeMedida.LITRO));
        assertEquals(UnidadeMedida.QUILOGRAMA, arroz.getUnidadeMedida());
    }

    @Test
    void removeAlimentoSemEstoque() {
        service.cadastrar("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);

        service.remover("Arroz");

        assertTrue(service.listarTodos().isEmpty());
    }

    @Test
    void naoRemoveAlimentoQueJaTemLote() {
        Alimento arroz = service.cadastrar("Arroz", CategoriaAlimento.GRAOS, UnidadeMedida.QUILOGRAMA);
        repoEstoque.adicionarLote(new LoteEstoque(arroz, 5, LocalDate.now().plusDays(30)));

        assertThrows(ValidacaoException.class, () -> service.remover("Arroz"));
        assertEquals(1, service.listarTodos().size());
    }
}
