package com.alimentabem.repository;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Guarda os repositórios em memória num arquivo, para os dados sobreviverem ao reinício.
 *
 * Os seis repositórios são gravados juntos, num único fluxo de serialização: assim um mesmo
 * Alimento (ou Doador, ou Lote) continua sendo o MESMO objeto no catálogo, nos lotes, nas doações
 * e nas distribuições depois de carregar o arquivo.
 *
 * Local padrão: pasta do usuário, em .alimentabem/dados.dat
 * (pode ser trocado com -Dalimentabem.dados=caminho/arquivo.dat).
 */
public final class ArmazenamentoArquivo {

    /** Conteúdo gravado no arquivo. */
    private static final class Dados implements Serializable {
        private static final long serialVersionUID = 1L;
        final RepositorioDoadorMemoria doadores = new RepositorioDoadorMemoria();
        final RepositorioDoacaoMemoria doacoes = new RepositorioDoacaoMemoria();
        final RepositorioAlimentoMemoria alimentos = new RepositorioAlimentoMemoria();
        final RepositorioEstoqueMemoria estoque = new RepositorioEstoqueMemoria();
        final RepositorioInstituicaoMemoria instituicoes = new RepositorioInstituicaoMemoria();
        final RepositorioDistribuicaoMemoria distribuicoes = new RepositorioDistribuicaoMemoria();
    }

    // Só aceita classes do próprio sistema e do JDK ao ler o arquivo (proteção contra arquivos adulterados)
    private static final ObjectInputFilter FILTRO = ObjectInputFilter.Config.createFilter(
            "maxdepth=50;com.alimentabem.**;java.util.*;java.lang.*;java.time.*;!*");

    private final Path arquivo;   // null = somente memória (sem gravar)
    private final Dados dados;

    private ArmazenamentoArquivo(Path arquivo, Dados dados) {
        this.arquivo = arquivo;
        this.dados = dados;
    }

    public static Path caminhoPadrao() {
        String prop = System.getProperty("alimentabem.dados");
        if (prop != null && !prop.isBlank()) return Paths.get(prop);
        return Paths.get(System.getProperty("user.home"), ".alimentabem", "dados.dat");
    }

    /** Armazenamento que não grava nada (útil para testes). */
    public static ArmazenamentoArquivo emMemoria() {
        return new ArmazenamentoArquivo(null, new Dados());
    }

    /**
     * Carrega o arquivo se ele existir; senão começa vazio.
     * Se o arquivo estiver ilegível, ele é preservado com outro nome e o sistema começa vazio.
     */
    public static ArmazenamentoArquivo carregar(Path arquivo) {
        if (arquivo == null) return emMemoria();
        if (!Files.exists(arquivo)) return new ArmazenamentoArquivo(arquivo, new Dados());
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(arquivo)))) {
            in.setObjectInputFilter(FILTRO);
            Object lido = in.readObject();
            if (!(lido instanceof Dados)) throw new InvalidClassException("Conteúdo inesperado no arquivo de dados.");
            return new ArmazenamentoArquivo(arquivo, (Dados) lido);
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            Path copia = arquivo.resolveSibling(arquivo.getFileName() + ".ilegivel-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            try {
                Files.move(arquivo, copia);
            } catch (IOException ignorada) {
                // se nem mover for possível, segue com dados vazios sem sobrescrever nada agora
            }
            System.err.println("AlimentaBem: não foi possível ler " + arquivo + " (" + e.getMessage()
                    + "). O arquivo foi preservado como " + copia + " e o sistema iniciou sem dados.");
            return new ArmazenamentoArquivo(arquivo, new Dados());
        }
    }

    /** Grava tudo. Escreve num arquivo temporário e troca no final, para nunca deixar o arquivo pela metade. */
    public void salvar() {
        if (arquivo == null) return;
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) Files.createDirectories(pasta);
            Path tmp = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeObject(dados);
            }
            try {
                Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("A operação foi feita, mas não foi possível salvar os dados em "
                    + arquivo + ": " + e.getMessage(), e);
        }
    }

    public Path getArquivo() { return arquivo; }

    public IRepositorioDoador doadores() { return dados.doadores; }
    public IRepositorioDoacao doacoes() { return dados.doacoes; }
    public IRepositorioAlimento alimentos() { return dados.alimentos; }
    public IRepositorioEstoque estoque() { return dados.estoque; }
    public IRepositorioInstituicao instituicoes() { return dados.instituicoes; }
    public IRepositorioDistribuicao distribuicoes() { return dados.distribuicoes; }
}
