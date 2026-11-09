package com.alimentabem.controller;

import com.alimentabem.exception.ValidacaoException;
import com.alimentabem.model.*;
import com.alimentabem.repository.*;
import com.alimentabem.service.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Fachada {

    private final ControladorDoador controladorDoador;
    private final ControladorDoacao controladorDoacao;

    private final AlimentoService alimentoService;
    private final EstoqueService estoqueService;

    private final InstituicaoService instituicaoService;
    private final DistribuicaoService distribuicaoService;

    /** Salva os repositórios em arquivo após cada alteração. */
    private final ArmazenamentoArquivo armazenamento;

    /** Usa o arquivo de dados padrão (~/.alimentabem/dados.dat ou -Dalimentabem.dados). */
    public Fachada() {
        this(ArmazenamentoArquivo.caminhoPadrao());
    }

    /** @param arquivoDados arquivo onde os dados são salvos; null = somente memória (nada é gravado). */
    public Fachada(java.nio.file.Path arquivoDados) {
        this.armazenamento = ArmazenamentoArquivo.carregar(arquivoDados);
        IRepositorioDoador repoDoador = armazenamento.doadores();
        IRepositorioDoacao repoDoacao = armazenamento.doacoes();
        IRepositorioAlimento repoAlimento = armazenamento.alimentos();
        IRepositorioEstoque repoEstoque = armazenamento.estoque();
        IRepositorioInstituicao repoInstituicao = armazenamento.instituicoes();
        IRepositorioDistribuicao repoDistribuicao = armazenamento.distribuicoes();

        this.controladorDoador = new ControladorDoador(repoDoador);
        this.controladorDoacao = new ControladorDoacao(repoDoacao, repoDoador);

        this.estoqueService = new EstoqueService(repoEstoque);
        this.alimentoService = new AlimentoServiceImpl(repoAlimento, repoEstoque);

        ViaCepService viaCepService = new ViaCepService();
        this.instituicaoService = new InstituicaoServiceImpl(repoInstituicao, repoDistribuicao, viaCepService);
        this.distribuicaoService = new DistribuicaoServiceImpl(repoDistribuicao, this.estoqueService);
    }

    // --- DOADORES ---
    // REQ01: cadastrar doadores (Pessoa Física / Empresa)
    public void cadastrarDoador(Doador doador) { controladorDoador.cadastrarDoador(doador); salvar(); }
    public Doador buscarDoadorPorDocumento(String doc) { return controladorDoador.buscarPorDocumento(doc); }
    public List<Doador> listarDoadores() { return controladorDoador.listarDoadores(); }

    public void atualizarDoador(String documento, String nome, String telefone, String email, String razaoSocial) {
        controladorDoador.atualizarDoador(documento, nome, telefone, email, razaoSocial);
        salvar();
    }

    public void removerDoador(String documento) {
        Doador doador = controladorDoador.buscarPorDocumento(documento);
        for (Doacao doacao : controladorDoacao.listarDoacoes()) {
            if (doacao.getDoador().getDocumento().equals(doador.getDocumento())) {
                throw new ValidacaoException("Não é possível remover '" + doador.getNome() + "': ele já possui doações registradas.");
            }
        }
        controladorDoador.removerDoador(documento);
        salvar();
    }

    // --- DOAÇÕES ---
    // REQ04: registrar doação e dar entrada no estoque
    // REQ15: doação sem itens é rejeitada
    // REQ19: itens vencidos são rejeitados
    public void registrarDoacao(Doacao doacao) {
        // Valida tudo antes de gravar: a doação só é registrada se o estoque aceitar todos os itens.
        controladorDoacao.validarDoacao(doacao);
        estoqueService.registrarDoacao(doacao);   // valida as validades (REQ19) e dá entrada nos lotes
        controladorDoacao.registrarDoacao(doacao);
        salvar();
    }
    public List<Doacao> listarDoacoes() { return controladorDoacao.listarDoacoes(); }

    // --- ALIMENTOS E ESTOQUE ---
    // REQ03: cadastrar alimentos (nome, categoria, unidade)
    public Alimento cadastrarAlimento(String nome, CategoriaAlimento cat, UnidadeMedida um) {
        return cadastrarAlimento(nome, cat, um, null);
    }
    /** pesoPorUnidadeGramas: obrigatório quando a unidade é UNIDADE (peso médio de 1 unidade). */
    public Alimento cadastrarAlimento(String nome, CategoriaAlimento cat, UnidadeMedida um, Double pesoPorUnidadeGramas) {
        Alimento a = alimentoService.cadastrar(nome, cat, um, pesoPorUnidadeGramas);
        salvar();
        return a;
    }
    public Alimento buscarAlimentoPorNome(String nome) { return alimentoService.buscarPorNome(nome); }
    public List<Alimento> listarAlimentos() { return alimentoService.listarTodos(); }

    public void atualizarAlimento(String nome, String novoNome, CategoriaAlimento cat, UnidadeMedida um) {
        alimentoService.atualizar(nome, novoNome, cat, um);
        salvar();
    }
    public void atualizarAlimento(String nome, String novoNome, CategoriaAlimento cat, UnidadeMedida um,
                                  Double pesoPorUnidadeGramas) {
        alimentoService.atualizar(nome, novoNome, cat, um, pesoPorUnidadeGramas);
        salvar();
    }

    public void removerAlimento(String nome) {
        alimentoService.remover(nome);
        salvar();
    }

    // REQ05: registra calorias/proteínas do alimento (consulta USDA ou digitação manual)
    public void definirValorNutricional(Alimento alimento, ValorNutricional valor) {
        if (alimento == null) throw new ValidacaoException("Selecione um alimento.");
        alimentoService.buscarPorNome(alimento.getNome()).setValorNutricional(valor);
        salvar();
    }

    // REQ07: estoque por lote, ordenado por data de validade
    public List<LoteEstoque> listarEstoque() { return estoqueService.listarLotes(); }
    // REQ12: lotes que vencem nos próximos N dias (7 nos relatórios)
    public List<LoteEstoque> listarEstoqueVencendoEm(int dias) { return estoqueService.listarVencendoEm(dias); }

    // --- INSTITUIÇÕES BENEFICIÁRIAS ---
    // REQ02: cadastrar instituição (endereço completado pelo CEP)
    // REQ14: CEP inválido bloqueia o cadastro
    public InstituicaoBeneficiaria cadastrarInstituicao(String nome, String cep, int capacidadeMensal) {
        InstituicaoBeneficiaria inst = instituicaoService.cadastrar(nome, cep, capacidadeMensal);
        salvar();
        return inst;
    }
    public InstituicaoBeneficiaria buscarInstituicaoPorNome(String nome) {
        return instituicaoService.buscarPorNome(nome);
    }
    public List<InstituicaoBeneficiaria> listarInstituicoes() { return instituicaoService.listarTodas(); }

    public void atualizarInstituicao(String nome, String novoNome, String novoCep, int novaCapacidade) {
        instituicaoService.atualizar(nome, novoNome, novoCep, novaCapacidade);
        salvar();
    }

    // REQ20: não exclui instituição que já tenha distribuições
    public void removerInstituicao(String nome) {
        instituicaoService.remover(nome);
        salvar();
    }

    // --- DISTRIBUIÇÕES ---
    // REQ08: registrar distribuição com os itens retirados do estoque
    // REQ09: sugestão/aplicação automática de FIFO
    // REQ13: lotes vencidos nunca são distribuídos
    // REQ16: quantidade não pode exceder o estoque
    // REQ17: respeita a capacidade mensal da instituição
    // REQ18: lote de validade mais próxima sai primeiro
    public Distribuicao registrarDistribuicao(InstituicaoBeneficiaria inst, Map<Alimento, Double> itens, int familias, LocalDate data) {
        Distribuicao d = distribuicaoService.registrarDistribuicao(inst, itens, familias, data);
        salvar();
        return d;
    }
    public List<Distribuicao> listarTodasDistribuicoes() { return distribuicaoService.listarTodas(); }

    // --- PERSISTÊNCIA ---
    /** Grava todos os dados no arquivo (chamado após cada alteração). */
    private void salvar() { armazenamento.salvar(); }

    /** Arquivo onde os dados ficam salvos (null se estiver só em memória). */
    public java.nio.file.Path getArquivoDados() { return armazenamento.getArquivo(); }
}