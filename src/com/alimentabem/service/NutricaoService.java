package com.alimentabem.service;

import com.alimentabem.model.ValorNutricional;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acesso à API pública USDA FoodData Central (REQ05).
 *
 * Como funciona:
 *  1. O nome do alimento pode ser digitado em português OU inglês. A base da USDA é só em inglês,
 *     então o nome é traduzido pelo {@link CatalogoAlimentos} (ex.: "Maçã" -> "apples raw with skin").
 *     Se o nome não estiver no catálogo, é enviado como foi digitado (assume-se que já é inglês).
 *  2. A busca usa apenas os tipos "Foundation" e "SR Legacy" (alimentos genéricos, com valores
 *     por 100 g analisados em laboratório) e ignora produtos de marca ("Branded"), que são ruidosos.
 *  3. Calorias são lidas SOMENTE em kcal (a USDA também traz kJ) e proteína em gramas, pelo código
 *     do nutriente (1008/2047/2048 = energia, 1003 = proteína), não pelo nome.
 *  4. Se a API falhar (sem internet, chave sem cota, timeout) e o alimento estiver no catálogo,
 *     usa-se a tabela local de referência e a tela avisa a origem dos dados.
 *
 * Chave da API: variável de ambiente USDA_API_KEY (grátis em https://fdc.nal.usda.gov/api-key-signup).
 * Sem ela usa-se a DEMO_KEY, limitada a 30 consultas/hora por IP.
 */
public class NutricaoService {

    public static final String VARIAVEL_CHAVE = "USDA_API_KEY";
    public static final String CHAVE_DEMO = "DEMO_KEY";
    public static final String FONTE_API = "USDA FoodData Central";
    public static final String FONTE_LOCAL = "Tabela local de referência (USDA SR Legacy)";

    private static final String BASE_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";
    private static final int[] IDS_ENERGIA = {1008, 2047, 2048}; // Energy (kcal) e Atwater general/specific
    private static final int ID_PROTEINA = 1003;

    public enum Origem { API_USDA, TABELA_LOCAL }

    /** O que foi encontrado e de onde veio. */
    public static final class Resultado {
        private final ValorNutricional valor;
        private final String descricao;
        private final String termoUsado;
        private final Origem origem;
        private final String aviso;

        Resultado(ValorNutricional valor, String descricao, String termoUsado, Origem origem, String aviso) {
            this.valor = valor;
            this.descricao = descricao;
            this.termoUsado = termoUsado;
            this.origem = origem;
            this.aviso = aviso;
        }

        public ValorNutricional getValor() { return valor; }
        /** Descrição do alimento na base da USDA (em inglês). */
        public String getDescricao() { return descricao; }
        /** Termo realmente enviado à USDA (já traduzido). */
        public String getTermoUsado() { return termoUsado; }
        public Origem getOrigem() { return origem; }
        /** Mensagem para o usuário quando houve contingência (pode ser null). */
        public String getAviso() { return aviso; }
    }

    private final String chaveApi;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private final Map<String, Resultado> cache = new HashMap<>();

    public NutricaoService() { this(chaveDoAmbiente()); }

    public NutricaoService(String chaveApi) {
        this.chaveApi = (chaveApi == null || chaveApi.isBlank()) ? CHAVE_DEMO : chaveApi.trim();
    }

    /** USDA_API_KEY do ambiente, ou -Dusda.api.key, ou DEMO_KEY. */
    public static String chaveDoAmbiente() {
        String env = System.getenv(VARIAVEL_CHAVE);
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("usda.api.key");
        return (prop != null && !prop.isBlank()) ? prop.trim() : CHAVE_DEMO;
    }

    public String getChaveApi() { return chaveApi; }
    public boolean usandoChaveDemo() { return CHAVE_DEMO.equals(chaveApi); }

    // REQ05: consulta calorias e proteínas na API USDA FoodData Central (aceita português ou inglês)
    /**
     * @return o resultado, ou null se nem a USDA nem a tabela local conhecem o alimento
     * @throws IOException se a USDA não respondeu e o alimento também não está na tabela local
     */
    public Resultado buscar(String nome) throws IOException, InterruptedException {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do alimento.");
        }
        String chave = CatalogoAlimentos.normalizar(nome);
        Resultado emCache = cache.get(chave);
        if (emCache != null) return emCache;

        CatalogoAlimentos.Entrada local = CatalogoAlimentos.procurar(nome);
        String termo = local != null ? local.getTermoUsda() : nome.trim();

        Resultado resultado = null;
        IOException falha = null;
        try {
            resultado = consultarApi(termo);
        } catch (IOException e) {
            falha = e;
        }

        if (resultado != null) {
            cache.put(chave, resultado);          // só guarda o que veio da API (a contingência é tentada de novo depois)
            return resultado;
        }
        if (local != null && local.temValores()) {
            String motivo = falha != null ? falha.getMessage() : "a USDA não retornou resultado para esse alimento";
            return new Resultado(new ValorNutricional(local.getKcal(), local.getProteina(), FONTE_LOCAL),
                    local.getDescricaoEn(), termo, Origem.TABELA_LOCAL,
                    "Usando a tabela local de referência: " + motivo);
        }
        if (falha != null) throw falha;
        return null;
    }

    /** Versão simples: devolve só o valor nutricional ou lança IOException se não achou. */
    public ValorNutricional consultar(String nomeAlimento) throws IOException, InterruptedException {
        Resultado r = buscar(nomeAlimento);
        if (r == null) throw new IOException("Nenhum resultado encontrado para \"" + nomeAlimento + "\".");
        return r.getValor();
    }

    private Resultado consultarApi(String termo) throws IOException, InterruptedException {
        String url = BASE_URL + "?api_key=" + URLEncoder.encode(chaveApi, StandardCharsets.UTF_8)
                + "&query=" + URLEncoder.encode(termo, StandardCharsets.UTF_8)
                + "&pageSize=15&dataType=Foundation,SR%20Legacy";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .timeout(Duration.ofSeconds(12)).header("Accept", "application/json").GET().build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IOException("sem conexão com a USDA (verifique a internet)", e);
        }
        int status = response.statusCode();
        if (status == 429) {
            throw new IOException("limite de consultas da chave " + (usandoChaveDemo() ? "DEMO_KEY (30 por hora)" : "atingido")
                    + "; use uma chave própria em " + VARIAVEL_CHAVE);
        }
        if (status == 401 || status == 403) throw new IOException("chave da API USDA inválida");
        if (status != 200) throw new IOException("a USDA respondeu com erro HTTP " + status);

        Candidato melhor = escolherMelhor(interpretar(response.body()), termo);
        if (melhor == null) return null;
        ValorNutricional vn = new ValorNutricional(melhor.kcal, melhor.proteina == null ? 0.0 : melhor.proteina, FONTE_API);
        return new Resultado(vn, melhor.descricao, termo, Origem.API_USDA, null);
    }

    // ------------------------------------------------------------------
    // Interpretação da resposta JSON (sem biblioteca externa)
    // ------------------------------------------------------------------
    static final class Candidato {
        String descricao = "";
        String tipo = "";
        Double kcal;
        Double proteina;
    }

    private static final Pattern INICIO_ALIMENTO = Pattern.compile("\"fdcId\"\\s*:\\s*\\d+");
    private static final Pattern OBJETO_NUTRIENTE = Pattern.compile("\\{[^{}]*\"nutrientId\"\\s*:\\s*(\\d+)[^{}]*\\}");
    private static final Pattern VALOR = Pattern.compile("\"value\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?)");
    private static final Pattern UNIDADE = Pattern.compile("\"unitName\"\\s*:\\s*\"([^\"]*)\"");

    /** Quebra a resposta em um candidato por alimento (na mesma ordem de relevância da USDA). */
    static List<Candidato> interpretar(String json) {
        List<Candidato> lista = new ArrayList<>();
        if (json == null) return lista;
        List<Integer> inicios = new ArrayList<>();
        Matcher m = INICIO_ALIMENTO.matcher(json);
        while (m.find()) inicios.add(m.start());
        for (int i = 0; i < inicios.size(); i++) {
            int fim = i + 1 < inicios.size() ? inicios.get(i + 1) : json.length();
            String seg = json.substring(inicios.get(i), fim);
            Candidato c = new Candidato();
            c.descricao = texto(seg, "description");
            c.tipo = texto(seg, "dataType");
            for (int id : IDS_ENERGIA) {                     // só kcal; kJ (id 1062) é ignorado
                c.kcal = nutriente(seg, id, "KCAL");
                if (c.kcal != null) break;
            }
            c.proteina = nutriente(seg, ID_PROTEINA, "G");
            lista.add(c);
        }
        return lista;
    }

    private static Double nutriente(String segmento, int idProcurado, String unidadeEsperada) {
        Matcher m = OBJETO_NUTRIENTE.matcher(segmento);
        while (m.find()) {
            if (Integer.parseInt(m.group(1)) != idProcurado) continue;
            String obj = m.group();
            Matcher u = UNIDADE.matcher(obj);
            if (unidadeEsperada != null && u.find() && !unidadeEsperada.equalsIgnoreCase(u.group(1))) continue;
            Matcher v = VALOR.matcher(obj);
            if (v.find()) return Double.parseDouble(v.group(1));
        }
        return null;
    }

    private static String texto(String json, String chave) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(chave) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        if (!m.find()) return "";
        return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\")
                .replace("\\u0027", "'").replace("\\u0026", "&");
    }

    private static final Set<String> PALAVRAS_SEM_PESO = Set.of(
            "raw", "fresh", "and", "or", "with", "without", "all", "types", "style", "salad", "cooking", "regular",
            "enriched", "solids", "liquids", "drained", "skin", "flesh", "whole", "meat", "only", "unprepared",
            "commercially", "prepared", "mature", "seeds", "dry", "table", "lean", "fat", "milkfat", "fluid",
            "general", "products", "canned", "ripe", "red", "green", "yellow", "light", "white", "long", "grain");

    /**
     * Mantém a ordem de relevância da USDA, mas exige energia em kcal e prefere o primeiro resultado
     * cuja descrição contenha as palavras-chave do termo (ex.: "rice", "milk", "apples").
     */
    static Candidato escolherMelhor(List<Candidato> candidatos, String termo) {
        List<String> chaves = new ArrayList<>();
        for (String w : termo.toLowerCase().split("[^a-z]+")) {
            if (w.length() >= 3 && !PALAVRAS_SEM_PESO.contains(w)) chaves.add(w);
        }
        Candidato primeiroComEnergia = null;
        for (Candidato c : candidatos) {
            if (c.kcal == null) continue;
            if (primeiroComEnergia == null) primeiroComEnergia = c;
            String d = c.descricao.toLowerCase();
            boolean todas = true;
            for (String k : chaves) {
                if (!d.contains(k)) { todas = false; break; }
            }
            if (todas) return c;
        }
        return primeiroComEnergia;
    }
}
