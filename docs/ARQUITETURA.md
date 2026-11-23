# Arquitetura do AlimentaBem

## Camadas

```
 Interface (Swing: AlimentaBemApp  |  Console: Main, MenuAlimentos, MenuInstituicoes)
                         │   só conhece a Fachada
                         ▼
 Fachada (pacote controller)  ── ponto único de entrada; grava os dados em arquivo após cada alteração
                         │
                         ▼
 Serviços (pacote service)    ── TODAS as regras de negócio (REQ01 a REQ20)
   DoadorService · DoacaoService · AlimentoService · EstoqueService
   InstituicaoService · DistribuicaoService · NutricaoService · RelatorioService
                         │
                         ▼
 Repositórios (pacote repository)  ── guardam os objetos (memória + arquivo .dat)
                         │
                         ▼
 Modelo (pacote model)             ── Doador, Alimento, LoteEstoque, Doacao, Distribuicao...
```

## Decisão: unir Serviços e Controladores

**Situação anterior.** Existiam dois estilos misturados: Doador e Doação passavam por
`ControladorDoador` / `ControladorDoacao`, e todo o resto (Alimento, Estoque, Instituição, Distribuição)
passava por serviços. Além disso, a regra "doador com doações não pode ser excluído" estava dentro da
própria `Fachada`, que deveria apenas repassar chamadas.

**Por que dava para unir.** Os dois controladores faziam exatamente o que um serviço faz: validar e chamar o
repositório. Os serviços equivalentes (`DoadorServiceImpl`, `DoacaoServiceImpl`) já existiam no projeto, com as
regras mais completas (exclusão de doador com doações; estoque validado antes de gravar a doação).

**O que foi feito.**

| Antes | Depois |
|---|---|
| `Fachada → ControladorDoador → Repositório` | `Fachada → DoadorService → Repositório` |
| `Fachada → ControladorDoacao → Repositório` (+ regra solta na Fachada) | `Fachada → DoacaoService → EstoqueService + Repositório` |
| CPF/CNPJ validados só no controlador | validação movida para `DoadorServiceImpl.cadastrar` |
| Regra "doador com doações" na Fachada | regra em `DoadorServiceImpl.remover` |

Os arquivos `ControladorDoador.java` e `ControladorDoacao.java` foram removidos. Agora **toda** regra de negócio
está na camada de serviço e a `Fachada` só delega e salva.

**Por que a `Fachada` continua separada (e no pacote `controller`).** Ela tem outro papel: é o *controlador de caso
de uso* do padrão Facade. A interface (tela ou console) não precisa saber que existem oito serviços e seis
repositórios, e a gravação em arquivo (`salvar()`) é uma preocupação transversal que não pertence a nenhum
serviço específico. Por isso o pacote `controller` agora contém uma única classe, a `Fachada`.

**Por que cada serviço tem interface + implementação (`XService` / `XServiceImpl`).** Permite trocar a
implementação (por exemplo, repositório em banco de dados) e testar o serviço isolado, como nos testes de `test/`.

## Testes

`test/` (JUnit 5) cobre as regras de serviço sem abrir a interface e sem internet: alimentos, estoque,
distribuição (FIFO, vencidos, capacidade), persistência em arquivo, doador/doação (esta união) e o acesso à API de
alimentos (tradução, leitura do JSON e contingência).
