package com.alimentabem.service;

import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.Endereco;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViaCepService {

    // REQ02: completa o endereço via BrasilAPI a partir do CEP
    // REQ14: lança erro se o CEP for inválido ou não tiver retorno
    public Endereco buscarEnderecoPorCep(String cepLimpo) {
        if (cepLimpo == null || !cepLimpo.replaceAll("\\D", "").matches("\\d{8}")) {
            throw new ValidacaoException("CEP inválido: '" + cepLimpo + "'. O CEP deve conter exatamente 8 dígitos (REQ14).");
        }

        String cepNormalizado = cepLimpo.replaceAll("\\D", "");

        try {
            String url = "https://brasilapi.com.br/api/cep/v1/" + cepNormalizado;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new ValidacaoException("CEP " + cepNormalizado + " não encontrado na base de dados (REQ14).");
            }
            if (response.statusCode() != 200) {
                throw new ValidacaoException("Falha na consulta do CEP " + cepNormalizado + ". Código HTTP: " + response.statusCode());
            }

            String json = response.body();
            String rua = extrairCampo(json, "street");
            String bairro = extrairCampo(json, "neighborhood");
            String cidade = extrairCampo(json, "city");
            String estado = extrairCampo(json, "state");

            if (cidade.isEmpty()) {
                throw new ValidacaoException("A consulta do CEP " + cepNormalizado + " não retornou um endereço válido (REQ14).");
            }

            return new Endereco(cepNormalizado, rua, bairro, cidade, estado);

        } catch (ValidacaoException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidacaoException("Erro ao conectar ao serviço de busca de CEP (REQ14): " + e.getMessage());
        }
    }

    private String extrairCampo(String json, String campo) {
        Pattern padrao = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = padrao.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }
}