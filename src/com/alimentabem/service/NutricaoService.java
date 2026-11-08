package com.alimentabem.service;

import com.alimentabem.model.ValorNutricional;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NutricaoService {
    private static final String API_KEY = "DEMO_KEY";
    private static final String BASE_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    // cache para não repetir consultas
    private final Map<String, ValorNutricional> cache = new HashMap<>();
    
    // REQ05: consulta calorias e proteínas na API USDA FoodData Central
    public ValorNutricional consultar(String nomeAlimento) throws IOException, InterruptedException {
        String chaveCache = nomeAlimento.trim().toLowerCase();
        
        // cache para não repetir consultas
        if (cache.containsKey(chaveCache)) {
            return cache.get(chaveCache);
        }
        
        String query = URLEncoder.encode(nomeAlimento, StandardCharsets.UTF_8);
        String url = BASE_URL + "?api_key=" + API_KEY + "&query=" + query + "&pageSize=1";
        
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Falha ao consultar a API USDA. Código HTTP: " + response.statusCode());
        }
        
        String json = response.body();
        double calorias = extrairValorDoNutriente(json, "Energy");
        double proteinas = extrairValorDoNutriente(json, "Protein");
        
        ValorNutricional valorNutricional = new ValorNutricional(calorias, proteinas, "USDA FoodData Central");
        cache.put(chaveCache, valorNutricional);
        return valorNutricional;
    }

    private double extrairValorDoNutriente(String json, String nomeNutriente) {
        Pattern padrao = Pattern.compile(
            "\"nutrientName\"\\s*:\\s*\"" + nomeNutriente + "\"[^}]*?\"value\"\\s*:\\s*([0-9.]+)");
        Matcher m = padrao.matcher(json);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 0.0;
    }
}