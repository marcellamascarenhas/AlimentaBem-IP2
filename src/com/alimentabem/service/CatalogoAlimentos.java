package com.alimentabem.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Catálogo dos alimentos mais comuns em doações, em português.
 *
 * A base da USDA (FoodData Central) é toda em INGLÊS: buscar "arroz" não retorna nada útil.
 * Por isso o sistema guarda o nome em português (para o usuário) e usa este catálogo para
 * traduzir o nome para o termo de busca da USDA (ex.: "Arroz" -> "rice white long-grain raw").
 *
 * Cada entrada também traz valores de referência por 100 g (USDA SR Legacy), usados apenas como
 * contingência quando a API está fora do ar, sem internet ou com a chave sem cota. Os valores são
 * aproximados e a origem aparece na tela como "Tabela local".
 */
public final class CatalogoAlimentos {

    /** Uma linha do catálogo. kcal/proteína nulos = só serve para tradução (sem valor de contingência). */
    public static final class Entrada {
        private final String[] nomesPt;
        private final String termoUsda;
        private final String descricaoEn;
        private final Double kcal;
        private final Double proteina;

        Entrada(String[] nomesPt, String termoUsda, String descricaoEn, Double kcal, Double proteina) {
            this.nomesPt = nomesPt;
            this.termoUsda = termoUsda;
            this.descricaoEn = descricaoEn;
            this.kcal = kcal;
            this.proteina = proteina;
        }

        public String getTermoUsda() { return termoUsda; }
        public String getDescricaoEn() { return descricaoEn; }
        public Double getKcal() { return kcal; }
        public Double getProteina() { return proteina; }
        public boolean temValores() { return kcal != null && proteina != null; }
    }

    private static final List<Entrada> ENTRADAS = new ArrayList<>();
    /** Pares (nome normalizado -> entrada), do nome mais longo para o mais curto. */
    private static final List<Object[]> INDICE = new ArrayList<>();

    private static void add(String nomes, String termo, String descricao, Double kcal, Double prot) {
        String[] pt = nomes.split("\\|");
        Entrada e = new Entrada(pt, termo, descricao, kcal, prot);
        ENTRADAS.add(e);
        for (String n : pt) INDICE.add(new Object[]{singular(normalizar(n)), e});
    }

    static {
        // Grãos e cereais
        add("arroz|arroz branco", "rice white long-grain raw", "Rice, white, long-grain, regular, raw", 365.0, 7.13);
        add("arroz integral", "rice brown long-grain raw", "Rice, brown, long-grain, raw", 370.0, 7.94);
        add("feijao|feijao carioca|feijao pinto|feijao vermelho", "beans pinto mature seeds raw", "Beans, pinto, mature seeds, raw", 347.0, 21.42);
        add("feijao preto", "beans black mature seeds raw", "Beans, black, mature seeds, raw", 341.0, 21.6);
        add("feijao em lata|feijao enlatado", "beans pinto mature seeds canned solids and liquids", "Beans, pinto, mature seeds, canned", null, null);
        add("lentilha", "lentils mature seeds raw", "Lentils, mature seeds, raw", 352.0, 24.63);
        add("grao de bico", "chickpeas mature seeds raw", "Chickpeas, mature seeds, raw", 378.0, 20.47);
        add("ervilha", "peas green raw", "Peas, green, raw", 81.0, 5.42);
        add("soja", "soybeans mature seeds raw", "Soybeans, mature seeds, raw", 446.0, 36.49);
        add("milho|milho verde", "corn sweet yellow raw", "Corn, sweet, yellow, raw", 86.0, 3.27);
        add("fuba|farinha de milho", "cornmeal whole-grain yellow", "Cornmeal, whole-grain, yellow", 361.0, 6.93);
        add("aveia|flocos de aveia", "cereals oats regular and quick not fortified dry", "Cereals, oats, regular and quick, dry", 389.0, 16.89);
        add("macarrao|massa|espaguete|talharim", "pasta dry enriched", "Pasta, dry, enriched", 371.0, 13.04);
        add("farinha de trigo|farinha", "wheat flour white all-purpose enriched", "Wheat flour, white, all-purpose, enriched", 364.0, 10.33);
        add("pao|pao frances|pao de forma", "bread white commercially prepared", "Bread, white, commercially prepared", 266.0, 8.85);
        // Laticínios
        add("leite|leite integral", "milk whole 3.25% milkfat", "Milk, whole, 3.25% milkfat", 61.0, 3.15);
        add("leite desnatado", "milk nonfat fluid", "Milk, nonfat, fluid", 34.0, 3.37);
        add("leite em po", "milk dry whole", "Milk, dry, whole", 496.0, 26.32);
        add("queijo|queijo mussarela|mussarela", "cheese mozzarella whole milk", "Cheese, mozzarella, whole milk", 300.0, 22.17);
        add("iogurte", "yogurt plain whole milk", "Yogurt, plain, whole milk", 61.0, 3.47);
        add("manteiga", "butter salted", "Butter, salted", 717.0, 0.85);
        // Proteínas
        add("ovo|ovo de galinha", "egg whole raw fresh", "Egg, whole, raw, fresh", 143.0, 12.56);
        add("frango|peito de frango", "chicken broilers or fryers breast meat only raw", "Chicken, broilers or fryers, breast, meat only, raw", 120.0, 22.5);
        add("carne moida|carne bovina|carne", "beef ground 80% lean meat 20% fat raw", "Beef, ground, 80% lean meat / 20% fat, raw", 254.0, 17.17);
        add("peixe|tilapia", "fish tilapia raw", "Fish, tilapia, raw", 96.0, 20.08);
        add("atum|atum em lata", "fish tuna light canned in water drained solids", "Fish, tuna, light, canned in water, drained solids", 116.0, 25.51);
        add("sardinha|sardinha em lata", "fish sardine Atlantic canned in oil drained solids with bone", "Fish, sardine, Atlantic, canned in oil, drained solids with bone", 208.0, 24.62);
        add("amendoim", "peanuts all types raw", "Peanuts, all types, raw", 567.0, 25.8);
        add("pasta de amendoim", "peanut butter smooth style", "Peanut butter, smooth style", 588.0, 25.09);
        // Hortifrúti
        add("maca", "apples raw with skin", "Apples, raw, with skin", 52.0, 0.26);
        add("banana", "bananas raw", "Bananas, raw", 89.0, 1.09);
        add("laranja", "oranges raw", "Oranges, raw", 47.0, 0.94);
        add("limao", "lemons raw without peel", "Lemons, raw, without peel", 29.0, 1.1);
        add("melancia", "watermelon raw", "Watermelon, raw", 30.0, 0.61);
        add("manga", "mangos raw", "Mangos, raw", 60.0, 0.82);
        add("abacaxi", "pineapple raw", "Pineapple, raw", 50.0, 0.54);
        add("mamao", "papayas raw", "Papayas, raw", 43.0, 0.47);
        add("uva", "grapes red or green raw", "Grapes, red or green, raw", 69.0, 0.72);
        add("abacate", "avocados raw all commercial varieties", "Avocados, raw, all commercial varieties", 160.0, 2.0);
        add("batata|batata inglesa", "potatoes raw flesh and skin", "Potatoes, flesh and skin, raw", 77.0, 2.05);
        add("batata doce", "sweet potato raw unprepared", "Sweet potato, raw, unprepared", 86.0, 1.57);
        add("mandioca|aipim|macaxeira", "cassava raw", "Cassava, raw", 160.0, 1.36);
        add("tomate", "tomatoes red ripe raw", "Tomatoes, red, ripe, raw", 18.0, 0.88);
        add("molho de tomate", "tomato products canned sauce", "Tomato products, canned, sauce", null, null);
        add("cenoura", "carrots raw", "Carrots, raw", 41.0, 0.93);
        add("cebola", "onions raw", "Onions, raw", 40.0, 1.1);
        add("alface", "lettuce green leaf raw", "Lettuce, green leaf, raw", 15.0, 1.36);
        add("repolho", "cabbage raw", "Cabbage, raw", 25.0, 1.28);
        add("brocolis", "broccoli raw", "Broccoli, raw", 34.0, 2.82);
        add("abobora", "pumpkin raw", "Pumpkin, raw", 26.0, 1.0);
        add("beterraba", "beets raw", "Beets, raw", 43.0, 1.61);
        add("pepino", "cucumber with peel raw", "Cucumber, with peel, raw", 15.0, 0.65);
        add("pimentao", "peppers sweet red raw", "Peppers, sweet, red, raw", 31.0, 0.99);
        // Básicos
        add("acucar|acucar refinado|acucar cristal", "sugars granulated", "Sugars, granulated", 387.0, 0.0);
        add("oleo de soja|oleo", "oil soybean salad or cooking", "Oil, soybean, salad or cooking", 884.0, 0.0);
        add("sal", "salt table", "Salt, table", 0.0, 0.0);

        INDICE.sort(Comparator.comparingInt((Object[] o) -> ((String) o[0]).length()).reversed());
    }

    private CatalogoAlimentos() { }

    /** Minúsculas, sem acentos nem pontuação, espaços únicos. "Maçã!" -> "maca". */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Tira o plural simples de cada palavra: "feijões" -> "feijao", "ovos" -> "ovo", "pães" -> "pao". */
    static String singular(String normalizado) {
        StringBuilder sb = new StringBuilder();
        for (String w : normalizado.split(" ")) {
            if (w.isEmpty()) continue;
            String x = w;
            if (x.length() > 4 && x.endsWith("oes")) x = x.substring(0, x.length() - 3) + "ao";
            else if (x.length() > 3 && x.endsWith("aes")) x = x.substring(0, x.length() - 3) + "ao";
            else if (x.length() > 3 && x.endsWith("s") && !x.endsWith("ss")) x = x.substring(0, x.length() - 1);
            if (sb.length() > 0) sb.append(' ');
            sb.append(x);
        }
        return sb.toString();
    }

    /** Procura o alimento no catálogo (aceita plural, acento e nomes compostos como "arroz integral"). */
    public static Entrada procurar(String nome) {
        String n = singular(normalizar(nome));
        if (n.isEmpty()) return null;
        String cercado = " " + n + " ";
        for (Object[] par : INDICE) {                         // do nome mais específico para o mais genérico
            if (cercado.contains(" " + par[0] + " ")) return (Entrada) par[1];
        }
        return null;
    }

    /** Termo para enviar à USDA. Se o nome não está no catálogo, assume que o usuário já digitou em inglês. */
    public static String traduzir(String nome) {
        Entrada e = procurar(nome);
        return e != null ? e.getTermoUsda() : (nome == null ? "" : nome.trim());
    }

    public static int tamanho() { return ENTRADAS.size(); }
}
