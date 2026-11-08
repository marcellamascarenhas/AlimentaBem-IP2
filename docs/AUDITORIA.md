# Auditoria do projeto AlimentaBem

Revisão feita sobre o código enviado. Itens marcados com ✅ já foram aplicados nesta versão; os demais são **sugestões** (a decisão é de vocês).

## 1. Limpeza e organização

| # | Achado | Situação |
|---|---|---|
| 1 | `docs` era um **arquivo** (texto `c:\Users\julip\Downloads`), não uma pasta | ✅ removido; criada a pasta `docs/` com os diagramas |
| 2 | README começava com imagem quebrada (texto alternativo com comandos `git add/commit/push`; o arquivo `diagrama.jpg.jpeg` não existe) | ✅ corrigido |
| 3 | Imagens com extensão duplicada (`.jpg.jpeg`) soltas na raiz | ✅ movidas para `docs/` como `.jpeg` |
| 4 | `.gitignore` sem as pastas do IntelliJ | ✅ adicionados `out/`, `.idea/`, `*.iml` |
| 5 | `EnderecoService` **não é usada** e duplica `ViaCepService` (que, apesar do nome, consulta a BrasilAPI) | sugestão: apagar uma delas e renomear a outra para `BrasilApiCepService` |
| 6 | `Repositorio` e `RepositorioMemoria` (genéricos) **não são usados** | sugestão: apagar ou passar a usá-los |
| 7 | `IRepositorioDoador` e `IRepositorioEstoque` não estendem `IRepositorio`, ao contrário dos demais | sugestão: padronizar |
| 8 | Não há testes (o README cita JUnit) | sugestão: testes para REQ13 a REQ20 |

## 2. Requisitos sem acesso pelo console

`NutricaoService` (REQ05) e `RelatorioService` (REQ06, REQ10, REQ11 e bônus) existem, mas **não estão na `Fachada` nem no `Main`** de console, e também não há menu para listar doações/distribuições. A interface gráfica (`AlimentaBemApp`) cobre todos eles. ✅

## 3. Falhas de lógica

1. **`Fachada.registrarDoacao` não é atômica.** Ela grava a doação e só depois dá entrada no estoque; se o estoque rejeitar (ex.: validade vencida), a doação fica registrada sem lotes. Sugestão: validar tudo antes de gravar, por exemplo
   ```java
   estoqueService.registrarDoacao(doacao);      // valida as validades e dá entrada
   controladorDoacao.registrarDoacao(doacao);   // grava a doação por último
   ```
   (ou criar um método só de validação no controlador e chamá-lo primeiro).
2. **`ControladorDoador.removerDoador` não verifica doações existentes** (deixa doações "órfãs"). A tela gráfica já bloqueia; o ideal é a regra ficar no controlador.
3. **`NutricaoService`**
   * devolve `0.0` em silêncio quando não encontra o nutriente e ainda guarda esse zero no cache;
   * o regex de `"Energy"` pode pegar o valor em kJ em vez de kcal;
   * não filtra o tipo de dado da USDA (`dataType=Foundation,SR Legacy` garante valores por 100 g);
   * `HttpClient.newHttpClient()` sem *timeout* (pode travar);
   * chave fixa `DEMO_KEY` (limite baixo) — sugestão: ler de `System.getenv("USDA_API_KEY")`;
   * a busca precisa de nome em **inglês** ("rice"), mas o cadastro é em português.
4. **`RelatorioService.calcularContribuicao`** trata toda unidade como kg/L (×1000), inclusive `UNIDADE`, e **faz consulta à internet dentro do relatório** (efeito colateral e `IOException` no meio do cálculo).
5. **`MenuInstituicoes.menuRegistrarDistribuicao`**: `itensPedidos.put(alimento, qtd)` sobrescreve a quantidade se o mesmo alimento for digitado duas vezes. Use `itensPedidos.merge(alimento, qtd, Double::sum)`.
6. **REQ08 ("uma ou mais instituições")**: o modelo guarda **uma** instituição por `Distribuicao`. A tela gráfica resolve registrando uma distribuição por instituição selecionada, com validação prévia de todas antes de gravar qualquer uma. ✅
7. **Console**: `Integer.parseInt` sem tratamento mostra mensagens como `For input string: "x"`; vale capturar `NumberFormatException` e pedir de novo.
8. Dados somente em memória (perdem-se ao fechar). Se for desejado, serialização em arquivo/JSON.

## 4. O que foi adicionado nesta versão

* **Interface gráfica** `com.alimentabem.ui.AlimentaBemApp` (Swing, usando a `Fachada`), com a logo do app, paleta teal/menta, gradientes, ícones vetoriais, tabelas com cápsulas de status e barras de capacidade, gráfico de barras e animações leves.
* Comentário `// REQxx` acima de **cada** implementação de requisito (modelo, serviços, controladores e tela).
* Tabela "Onde está cada requisito" e instruções de execução no README.
