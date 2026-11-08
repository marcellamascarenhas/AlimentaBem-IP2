package com.alimentabem.service;

import com.alimentabem.model.Doacao;
import java.util.List;

public interface DoacaoService {
    /** Valida e registra a doação, dando entrada dos itens no estoque. */
    void registrar(Doacao doacao);
    List<Doacao> listarTodas();
}
