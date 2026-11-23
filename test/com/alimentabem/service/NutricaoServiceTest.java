package com.alimentabem.service;

import com.alimentabem.service.NutricaoService.Candidato;
import com.alimentabem.service.NutricaoService.Origem;
import com.alimentabem.service.NutricaoService.Resultado;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do acesso à API de alimentos (REQ05). Os testes de tradução e de leitura do JSON rodam sem internet;
 * o de contingência usa uma chave inválida de propósito, então dá o mesmo resultado com ou sem rede.
 */
class NutricaoServiceTest {

    private static final double DELTA = 1e-9;

    // Resposta no formato real da USDA: o primeiro "Energy" é em kJ (id 1062), o segundo em kcal (id 1008).
    private static final String JSON_ARROZ =
            "{\"totalHits\":2,\"foods\":["
          + "{\"fdcId\":168878,\"description\":\"Rice, white, long-grain, regular, raw, enriched\",\"dataType\":\"SR Legacy\","
          + "\"foodNutrients\":["
          + "{\"nutrientId\":1062,\"nutrientName\":\"Energy\",\"nutrientNumber\":\"268\",\"unitName\":\"kJ\",\"value\":1530},"
          + "{\"nutrientId\":1008,\"nutrientName\":\"Energy\",\"nutrientNumber\":\"208\",\"unitName\":\"KCAL\",\"value\":365},"
          + "{\"nutrientId\":1003,\"nutrientName\":\"Protein\",\"nutrientNumber\":\"203\",\"unitName\":\"G\",\"value\":7.13}]},"
          + "{\"fdcId\":2,\"description\":\"Rice noodles, cooked\",\"dataType\":\"SR Legacy\","
          + "\"foodNutrients\":[{\"nutrientId\":1008,\"unitName\":\"KCAL\",\"value\":108},"
          + "{\"nutrientId\":1003,\"unitName\":\"G\",\"value\":1.8}]}]}";

    // ---------- tradução português -> inglês ----------

    @Test
    void traduzNomesEmPortuguesParaOTermoDaUsda() {
        assertEquals("rice white long-grain raw", CatalogoAlimentos.traduzir("Arroz"));
        assertEquals("rice brown long-grain raw", CatalogoAlimentos.traduzir("ARROZ INTEGRAL"));
        assertEquals("apples raw with skin", CatalogoAlimentos.traduzir("Maçã"));
        assertEquals("bananas raw", CatalogoAlimentos.traduzir("banana prata"));
        assertEquals("broccoli raw", CatalogoAlimentos.traduzir("Brócolis"));
    }

    @Test
    void entendePluralEAcentos() {
        assertEquals(CatalogoAlimentos.traduzir("feijão"), CatalogoAlimentos.traduzir("feijões"));
        assertEquals(CatalogoAlimentos.traduzir("maçã"), CatalogoAlimentos.traduzir("MACAS"));
        assertEquals(CatalogoAlimentos.traduzir("pão"), CatalogoAlimentos.traduzir("pães"));
    }

    @Test
    void nomeMaisEspecificoVenceONomeGenerico() {
        assertEquals("beans black mature seeds raw", CatalogoAlimentos.traduzir("Feijão preto"));
        assertEquals("sweet potato raw unprepared", CatalogoAlimentos.traduzir("Batata doce"));
        assertEquals("potatoes raw flesh and skin", CatalogoAlimentos.traduzir("Batata"));
        assertEquals("milk nonfat fluid", CatalogoAlimentos.traduzir("Leite desnatado"));
        assertEquals("peanut butter smooth style", CatalogoAlimentos.traduzir("Pasta de amendoim"));
    }

    @Test
    void nomeForaDoCatalogoEEnviadoComoDigitado() {
        assertEquals("rice", CatalogoAlimentos.traduzir("rice"));
        assertEquals("quinoa", CatalogoAlimentos.traduzir("  quinoa "));
    }

    // ---------- leitura da resposta da USDA ----------

    @Test
    void leCaloriasEmKcalIgnorandoKj() {
        List<Candidato> lista = NutricaoService.interpretar(JSON_ARROZ);

        assertEquals(2, lista.size());
        assertEquals("Rice, white, long-grain, regular, raw, enriched", lista.get(0).descricao);
        assertEquals(365, lista.get(0).kcal, DELTA);     // e não 1530 (kJ)
        assertEquals(7.13, lista.get(0).proteina, DELTA);
    }

    @Test
    void aceitaEnergiaAtwaterQuandoNaoHaEnergiaPadrao() {
        String json = "{\"foods\":[{\"fdcId\":9,\"description\":\"Milk, whole\",\"dataType\":\"Foundation\","
                + "\"foodNutrients\":[{\"nutrientId\":2047,\"unitName\":\"KCAL\",\"value\":61.5},"
                + "{\"nutrientId\":1003,\"unitName\":\"G\",\"value\":3.2}]}]}";

        Candidato c = NutricaoService.interpretar(json).get(0);

        assertEquals(61.5, c.kcal, DELTA);
        assertEquals(3.2, c.proteina, DELTA);
    }

    @Test
    void escolheOResultadoQueTemAsPalavrasChaveDoTermo() {
        String json = "{\"foods\":["
                + "{\"fdcId\":1,\"description\":\"Cereals, oats\",\"dataType\":\"SR Legacy\",\"foodNutrients\":[{\"nutrientId\":1008,\"unitName\":\"KCAL\",\"value\":389}]},"
                + "{\"fdcId\":2,\"description\":\"Rice, white, raw\",\"dataType\":\"SR Legacy\",\"foodNutrients\":[{\"nutrientId\":1008,\"unitName\":\"KCAL\",\"value\":365}]}]}";

        Candidato c = NutricaoService.escolherMelhor(NutricaoService.interpretar(json), "rice white long-grain raw");

        assertEquals("Rice, white, raw", c.descricao);
    }

    @Test
    void ignoraResultadosSemEnergiaEDevolveNullSeNaoHouverNenhum() {
        String json = "{\"foods\":[{\"fdcId\":1,\"description\":\"Rice\",\"dataType\":\"SR Legacy\",\"foodNutrients\":[]}]}";

        assertNull(NutricaoService.escolherMelhor(NutricaoService.interpretar(json), "rice"));
        assertNull(NutricaoService.escolherMelhor(NutricaoService.interpretar("{\"foods\":[]}"), "rice"));
    }

    // ---------- contingência quando a API não responde ----------

    @Test
    void usaATabelaLocalQuandoAApiFalha() throws Exception {
        NutricaoService service = new NutricaoService("CHAVE_INVALIDA_DE_TESTE");

        Resultado r = service.buscar("Arroz");

        assertEquals(Origem.TABELA_LOCAL, r.getOrigem());
        assertEquals(365, r.getValor().getCaloriasPor100g(), DELTA);
        assertEquals(7.13, r.getValor().getProteinasPor100gEmGramas(), DELTA);
        assertEquals(NutricaoService.FONTE_LOCAL, r.getValor().getFonte());
        assertNotNull(r.getAviso(), "o usuário precisa ser avisado de que os dados vieram da tabela local");
    }

    @Test
    void alimentoDesconhecidoSemRedeGeraErroEmVezDeZeros() {
        NutricaoService service = new NutricaoService("CHAVE_INVALIDA_DE_TESTE");

        assertThrows(IOException.class, () -> service.buscar("alimento que nao existe xyz"));
    }

    @Test
    void semChaveUsaADemoKey() {
        assertEquals(NutricaoService.CHAVE_DEMO, new NutricaoService("  ").getChaveApi());
        assertTrue(new NutricaoService(null).usandoChaveDemo());
    }
}
