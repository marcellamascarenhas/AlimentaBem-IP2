# Relatório de Auditoria — AlimentaBem

**Data:** 08/11/2026 · **Ambiente:** Windows 11, OpenJDK Temurin 25.0.4 · **Dados usados:** apenas fictícios (ex.: "Maria Ficticia", CPF 111.222.333-44, CEPs públicos 01001-000 e 20040-020)

> **Legenda de evidência** — cada achado indica como foi verificado:
> 🖥️ **GUI** = testado interagindo com a janela real · 🧪 **Teste** = executado por driver Java contra a `Fachada`/serviços · ⌨️ **Console** = executado no `Main` com entrada simulada · 📖 **Estático** = apenas leitura de código (não executado).

---

## Resumo

| Pergunta | Resposta |
|---|---|
| Compila? | ✅ Sim — `javac` puro, **0 erros**, 29 avisos de lint (serialização / `this-escape`, inofensivos). |
| Executa? | ✅ Sim — GUI Swing e console iniciam sem erros; nenhum erro no console durante ~15 min de uso da GUI. |
| As telas funcionam? | ✅ Todas as 8 áreas da GUI abrem e respondem. Havia 3 problemas visuais de corte e 1 de validação de data — **corrigidos**. |
| As regras de negócio estão corretas? | ✅ REQ01–REQ20 funcionam. Foram achados **9 defeitos de lógica** e **todos foram corrigidos** (7 na auditoria; persistência e cálculo por unidade depois, a pedido). |
| Persistência? | ❌ Não existia (confirmado reiniciando a GUI). ✅ **Implementada depois da auditoria** — salvamento em arquivo após cada alteração (ver §12.1). |
| Login / usuários? | ➖ **Não existe** no projeto (nem nos requisitos). Não há tela de login, cadastro de usuários, senhas nem banco de dados. |

**Resultado dos testes automatizados:** nos 34 casos originais, com a USDA respondendo, antes **24 ✅ / 10 ❌** → depois da auditoria **32 ✅ / 2 ❌**. Com persistência e peso por unidade implementados, a bateria tem **47 casos: 45 ✅**, e as 2 falhas são só o limite HTTP 429 da `DEMO_KEY` da USDA (F29, N04), não defeitos do código. Em reexecuções seguidas a `DEMO_KEY` da USDA passou a responder HTTP 429 e mais 2 casos (F29, N04) falharam por isso — os arquivos salvos em `auditoria/testes/` são dessas reexecuções (22/12 antes, 30/4 depois).

---

## 1. Visão geral do projeto

Sistema de gestão de um banco de alimentos: cadastra **doadores** (pessoa física / empresa), **alimentos**, **instituições beneficiárias** (endereço via CEP), registra **doações** (entrada em estoque por lote) e **distribuições** (saída FIFO), com **relatórios** (CSV, nutricional, vencimentos).

Arquitetura em camadas, comunicação sempre de cima para baixo:

```
UI (AlimentaBemApp Swing | Main console)
        │
   Fachada  ──►  Controladores (Doador, Doação) + Serviços (Alimento, Estoque, Instituição, Distribuição)
        │                         │
        │                 Repositórios em memória (ArrayList)
        │
   Integrações HTTP: BrasilAPI (CEP) · USDA FoodData Central (nutrição)
```

Fluxo principal: Doador + Alimento → **Doação** (cria/acumula lotes por alimento+validade) → **Estoque** ordenado por validade → **Distribuição** para 1+ instituições (FIFO, respeitando capacidade mensal) → **Relatórios**.

## 2. Tecnologias identificadas

| Item | Encontrado |
|---|---|
| Linguagem | Java (recursos de Java 14+: `switch ->`, `var`; README pede JDK 17+) |
| Build | **Nenhum** (sem Maven/Gradle). Projeto IntelliJ com `src` como Sources Root |
| Interface | **Swing** (`AlimentaBemApp`, 3.033 linhas) + console (`Main`, `MenuAlimentos`, `MenuInstituicoes`) |
| Bibliotecas externas | Nenhuma. JSON lido com regex; HTTP com `java.net.http.HttpClient` |
| Banco de dados | Nenhum — repositórios `*Memoria` com `ArrayList` |
| APIs | BrasilAPI `/api/cep/v1/{cep}` (sem chave) · USDA `/fdc/v1/foods/search` (`DEMO_KEY` ou `USDA_API_KEY`) |
| Testes | Nenhum no projeto (README cita JUnit, mas não há testes) |

Também encontrado: `correcoes-alimentabem.patch` — **está dessincronizado** (`git apply --check` falha nos dois sentidos); não foi aplicado. As classes `DoadorServiceImpl`/`DoacaoServiceImpl` que ele usaria existem mas **não são usadas** por nada.

## 3. Resultado da compilação

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
```
✅ 0 erros. Avisos: 24× `[serial]` (componentes Swing sem `serialVersionUID`) e 5× `[this-escape]` (setters chamados no construtor). Nenhum exige ação. Para a GUI mostrar a logo é preciso copiar `ui/resources` para a pasta de saída (o IntelliJ faz isso sozinho).

## 4. Resultado da execução

- 🖥️ `AlimentaBemApp` abre em ~1 s, janela 1120×740, sem saída no stderr em 3 execuções.
- ⌨️ `Main` (console) executa todos os menus.
- 🧪 BrasilAPI respondeu em 250–570 ms; USDA respondeu, mas a `DEMO_KEY` **atingiu limite (HTTP 429)** durante os testes — reproduzido.

## 5. Telas encontradas e testadas (🖥️)

| # | Tela | Abriu | Interações testadas | Problemas visuais |
|---|---|---|---|---|
| 1 | Painel inicial | ✅ | 4 contadores, 8 cartões, maximizar | Maximizada sobra muito espaço vazio nos cartões ([18](auditoria/capturas/18-janela-maximizada.jpg)) |
| 2 | Doadores (lista) | ✅ | busca, excluir sem seleção, duplo cadastro | Coluna mostra "CPF: 111…" (repete o cabeçalho, sem máscara); razão social não aparece |
| 3 | Novo Doador | ✅ | vazio, e-mail inválido, CPF curto, PF, Empresa, CPF duplicado | — |
| 4 | Alimentos (lista) | ✅ | novo, duplicado, editar, excluir | Coluna "Quilograma (k…" truncada ([05](auditoria/capturas/05-alimentos-lista-coluna-truncada.jpg)) |
| 5 | Novo/Alterar Alimento | ✅ | 3 alimentos, combos | **Cartão cortado embaixo** ([04](auditoria/capturas/04-form-alimento-cortado.jpg)) → corrigido ([21](auditoria/capturas/21-DEPOIS-form-alimento.jpg)) |
| 6 | Valor Nutricional (USDA) | ✅ | busca "Arroz", "rice", valores negativos/texto | "rice" traz **"Rice crackers"** ([06](auditoria/capturas/06-usda-rice-crackers.jpg)); fonte padrão "USDA" mesmo quando digitado à mão |
| 7 | Instituições (lista) | ✅ | excluir com distribuição | Cabeçalho "Capacidade men…" truncado |
| 8 | Nova Instituição | ✅ | CEP inexistente, CEP válido, capacidade 0, salvar sem "Buscar" | — |
| 9 | Estoque por Lote | ✅ | ordenação, chips | — ([09](auditoria/capturas/09-estoque-fifo.jpg)) |
| 10 | Doações / Nova Doação / Adicionar Item | ✅ | sem itens, data vencida, **31/02**, remover item, 4 itens, detalhes (duplo clique) | Seleção por tecla no combo não funciona |
| 11 | Distribuições / Nova / Adicionar Item / Sugestão FIFO / Detalhes | ✅ | sem instituição, excesso de estoque, multi-seleção, capacidade, registro duplo | — ([12](auditoria/capturas/12-sugestao-fifo.jpg)) |
| 12 | Relatórios (menu) | ✅ | 3 cartões | **2ª linha cortada** ([15](auditoria/capturas/15-relatorios-cartoes-cortados.jpg)) → corrigido ([19](auditoria/capturas/19-DEPOIS-relatorios-cartoes.jpg)) |
| 13 | Relatório de Doações | ✅ | filtro categoria, datas invertidas, exportar CSV | Seletor de arquivos sem o tema do app |
| 14 | Relatório Nutricional | ✅ | mês inválido, gráfico, gerar HTML (abriu no navegador) | — ([16](auditoria/capturas/16-relatorio-nutricional.jpg)) |
| 15 | Estoque a Vencer | ✅ | lista 7 dias | — |
| 16 | Sair | ✅ | | Fechava **sem confirmar** (perde tudo) → corrigido ([22](auditoria/capturas/22-DEPOIS-confirmar-sair.jpg)) |

**Não existem** (e por isso não foram testadas): tela de login, cadastro de usuários, gerenciamento de usuários, edição de doadores/instituições na GUI (a `Fachada` tem `atualizarDoador`/`atualizarInstituicao`, mas nenhuma tela usa).

**Limitação:** o relatório HTML abriu no navegador padrão, que não estava liberado para controle; o conteúdo foi verificado lendo o arquivo gerado.

## 6. Funcionalidades testadas

REQ01–REQ20 e o bônus HTML foram exercitados de verdade (GUI e/ou testes). Persistência e reinício também.

## 7 e 8. Tabela de testes

### 7.1 Interface gráfica (🖥️)

| ID | Teste | Resultado | Observação |
|---|---|---|---|
| G01 | Abrir app / painel | ✅ | contadores animados |
| G02 | Excluir doador sem seleção | ✅ | aviso "Selecione um doador" |
| G03 | Salvar doador vazio / e-mail sem @ / CPF 3 dígitos | ✅ | um aviso por vez |
| G04 | Cadastrar PF e Empresa | ✅ | lista e contador atualizam |
| G05 | CPF duplicado | ✅ | erro claro ([03](auditoria/capturas/03-doador-cpf-duplicado.jpg)) |
| G06 | Busca na lista ("merc") | ✅ | filtra |
| G07 | CPF com dígitos verificadores inválidos | ❌ | 111.222.333-44 é aceito (só conta 11 dígitos) |
| G08 | Alimento duplicado ("ARROZ") | ✅ | bloqueado |
| G09 | Editar alimento (renomear) | ✅ | |
| G10 | USDA "Arroz" / "rice" | ⚠️ | responde, mas pega o 1º resultado ("Arroz con gandules", "Rice crackers") |
| G11 | Valor nutricional −5 / "abc" | ✅ | aviso |
| G12 | Finalizar doação sem itens (REQ15) | ✅ | |
| G13 | Item com validade 01/01/2020 (REQ19) | ✅ | |
| G14 | Validade **31/02/2027** | ❌→✅ | era aceita como 28/02/2027 ([07](auditoria/capturas/07-data-31-02-aceita-como-28-02.jpg)); corrigido ([20](auditoria/capturas/20-DEPOIS-data-31-02-rejeitada.jpg)) |
| G15 | Doação com 4 itens + detalhes | ✅ | |
| G16 | Calorias de 12 ovos | ❌→✅ | era **16.800 kcal** ([08](auditoria/capturas/08-doacao-12-ovos-16800-kcal.jpg)); agora 840 kcal com 50 g/unidade ([27](auditoria/capturas/27-DEPOIS-12-ovos-840-kcal.jpg)) |
| G17 | Estoque ordenado por validade (REQ07) | ✅ | |
| G18 | CEP 00000-000 (REQ14) | ✅ | bloqueado na busca e no salvar |
| G19 | CEP 01001-000 completa endereço (REQ02) | ✅ | Praça da Sé, São Paulo/SP |
| G20 | Capacidade 0 | ✅ | aviso |
| G21 | Distribuir sem instituição | ✅ | aviso |
| G22 | Quantidade > estoque (REQ16) | ✅ | |
| G23 | Sugestão FIFO 2 instituições (REQ09/18) | ✅ | 10 kg do lote 11/11 + 20 kg do lote 08/02 |
| G24 | 5 famílias em instituição de capacidade 3 (REQ17) | ✅ | bloqueado |
| G25 | Registrar distribuição para 2 instituições (REQ08) | ✅ | lotes corretos em cada uma |
| G26 | Nova distribuição com capacidade 2/3 (REQ17) | ✅ | bloqueado |
| G27 | Excluir instituição com distribuição (REQ20) | ✅ | bloqueado (mas pergunta "confirma?" antes) |
| G28 | Relatório doações: filtro, datas invertidas, CSV (REQ10) | ✅ | CSV correto |
| G29 | Relatório nutricional, mês 13/2026, HTML (REQ11 + bônus) | ✅ | |
| G30 | Estoque a vencer (REQ12) | ✅ | lote esgotado não aparece |
| G31 | Reiniciar e ver dados | ❌ | tudo zerado ([17](auditoria/capturas/17-apos-reinicio-dados-zerados.jpg)) |

### 7.2 Regras de negócio (🧪, arquivo [`AuditTests.java`](auditoria/testes/AuditTests.java))

| ID | Teste | Antes | Depois |
|---|---|---|---|
| F01–F03, F05–F08 | Cadastro/busca/remoção de doadores e alimentos | ✅ | ✅ |
| F04 | CPF sem dígitos no console ("abc" duas vezes) | ❌ aceitava duplicado | ✅ rejeita |
| F09 | Trocar unidade (L→un) de alimento com estoque | ❌ permitia | ✅ bloqueia |
| F10–F12, F14 | REQ15, REQ19, doador inexistente, agrupamento de lote | ✅ | ✅ |
| F13 | Doação rejeitada pelo estoque não pode ficar gravada | ❌ ficava gravada **sem lotes** | ✅ |
| F15–F21 | FIFO, REQ13, REQ16, REQ17, REQ12, atomicidade da distribuição | ✅ | ✅ |
| F22 | Distribuição com data daqui a 5 anos | ❌ aceitava | ✅ rejeita |
| F23 | Retirar 0,1 + 0,2 de lote com 0,3 | ❌ "Faltam 2.7E-17 l" | ✅ |
| F24, F26 | REQ20, REQ14 formato | ✅ | ✅ |
| F25 | Renomear instituição para nome existente | ❌ aceitava | ✅ rejeita |
| F27 | CSV com ";" no nome do doador | ❌ coluna deslocada | ✅ campo entre aspas |
| F28 | 10 ovos de 50 g no cálculo nutricional | ❌ 14.000 kcal | ✅ 700 kcal (§12.2) |
| U01–U06 | Peso por unidade: obrigatório, salvo, ignorado em kg, troca atômica, dados antigos sem peso, edição mantém o peso | — | ✅ 6/6 |
| F29 | REQ11 agregado | ✅ / ❌ com HTTP 429* | ✅ / ❌ com HTTP 429* |
| F30 | Persistência após reinício | ❌ | ✅ (implementada, §12.1) |
| P01–P07 | Persistência: reinício, referências, erro não grava, arquivo corrompido, filtro, modo memória, regras após recarregar | — | ✅ 7/7 |
| N01–N02 | BrasilAPI válido / inexistente | ✅ | ✅ |
| N03 | Atualizar CEP da instituição | ❌ CEP antigo + endereço novo | ✅ |
| N04 | USDA via `NutricaoService` | ✅ / ❌ com HTTP 429* | ✅ / ❌ com HTTP 429* |

\* F29/N04 dependem da cota da `DEMO_KEY`: o relatório nutricional do serviço **consulta a internet no meio do cálculo** e falha inteiro se a USDA recusar (ver §11).

### 7.3 Console (⌨️, [saída](auditoria/testes/console-antes.txt))

| ID | Teste | Antes | Depois |
|---|---|---|---|
| C01 | Categoria "9" | ❌ "Index 8 out of bounds for length 4" | ✅ "Categoria inválida. Escolha 1 a 4" |
| C02 | Distribuir Arroz 3 kg + Arroz 4 kg | ❌ retirou só 4 kg | ✅ retira 7 kg |
| C03 | Quantidade "abc" | ⚠️ "digite um número válido" e **descarta a doação inteira** | ⚠️ igual |
| C04 | Item vencido no meio da doação | ⚠️ erro correto, mas descarta os itens já digitados | ⚠️ igual |

## 9. Erros encontrados (por gravidade)

| Grav. | Erro | Consequência | Evidência | Status |
|---|---|---|---|---|
| 🔴 Alta | `Fachada.registrarDoacao` gravava a doação **antes** de o estoque validar | Doação aparece no histórico/relatórios sem nenhum lote em estoque | 🧪 F13 | ✅ Corrigido |
| 🔴 Alta | Cálculo nutricional trata **1 unidade = 1 kg** | Relatórios de impacto inflados (12 ovos = 16.800 kcal) | 🖥️ G16, 🧪 F28 | ✅ Corrigido — peso por unidade (§12.2) |
| 🔴 Alta | Sem persistência: tudo se perde ao fechar | Perda total de dados a cada execução | 🖥️ G31, 🧪 F30 | ✅ Implementada (§12.1) |
| 🟠 Média | Datas impossíveis (31/02) viram o último dia do mês | Validade registrada diferente da digitada | 🖥️ G14 | ✅ Corrigido |
| 🟠 Média | Erro de ponto flutuante bloqueia distribuição do saldo exato | "Estoque insuficiente… Faltam 2.7E-17" | 🧪 F23 | ✅ Corrigido |
| 🟠 Média | Console: mesmo alimento duas vezes sobrescreve a quantidade | Distribui menos do que o pedido, sem aviso | ⌨️ C02 | ✅ Corrigido |
| 🟠 Média | `atualizarInstituicao` trocava o endereço mas mantinha o CEP antigo | Dados inconsistentes | 🧪 N03 | ✅ Corrigido |
| 🟠 Média | Trocar unidade de alimento com estoque | "2 L" passam a valer "2 un" | 🧪 F09 | ✅ Corrigido |
| 🟡 Baixa | Renomear instituição para nome existente | Duas instituições com o mesmo nome (busca pega a primeira) | 🧪 F25 | ✅ Corrigido |
| 🟡 Baixa | Distribuição com data futura aceita pelo serviço | Capacidade mensal de meses futuros consumida | 🧪 F22 | ✅ Corrigido |
| 🟡 Baixa | Console aceita CPF/CNPJ sem dígitos e duplica | Doador impossível de achar para doar | 🧪 F04 | ✅ Corrigido |
| 🟡 Baixa | CSV não escapa `;` | Colunas deslocadas no Excel | 🧪 F27 | ✅ Corrigido |
| 🟡 Baixa | Console: `ArrayIndexOutOfBounds` exibido ao usuário | Mensagem técnica | ⌨️ C01 | ✅ Corrigido |
| 🟡 Baixa | Console: um item inválido descarta a doação inteira | Retrabalho | ⌨️ C03/C04 | ⏳ Restante |

## 10. Problemas de interface e experiência

**Pontos fortes (🖥️):** identidade visual consistente (paleta teal, cabeçalhos, ícones), hierarquia clara, mensagens de erro em português e quase sempre específicas, FIFO e capacidade mensal muito bem visualizados (barras, chips, "Restam 1 de 3 famílias").

| # | Problema | Sugestão | Status |
|---|---|---|---|
| U1 | Formulário de alimento e cartões de Relatórios cortados | Aumentar altura / usar `pack()` | ✅ Corrigido (altura) |
| U2 | "Sair" fechava sem confirmar, apagando tudo | Confirmar | ✅ Corrigido no cartão "Sair" — o **X da janela** ainda fecha direto |
| U3 | Colunas truncadas ("Quilograma (k…", "Capacidade men…") | Larguras preferenciais ou textos curtos | ⏳ |
| U4 | Sem edição de doadores e instituições na GUI | Botão "Editar" (a Fachada já tem os métodos) | ⏳ |
| U5 | Documento mostrado como "CPF: 11122233344" na coluna "CPF / CNPJ" | Mostrar com máscara, sem prefixo; exibir razão social | ⏳ |
| U6 | Mensagens com "(REQ14)", "(REQ13)" para o usuário | Remover códigos internos das mensagens | ⏳ |
| U7 | Excluir pergunta "Confirma?" e só depois diz que não pode | Verificar antes de pedir confirmação | ⏳ |
| U8 | USDA usa sempre o 1º resultado e fonte padrão "USDA" mesmo em digitação manual | Listar resultados para escolha; mudar fonte ao editar à mão | ⏳ |
| U9 | Combos não respondem a digitar a inicial (usa `toString()` "[id] Nome") | `toString()` do Alimento começar pelo nome, ou `KeySelectionManager` | ⏳ |
| U10 | Validação mostra um erro por vez | Destacar todos os campos inválidos de uma vez | ⏳ |
| U11 | Datas digitadas livremente | Máscara `##/##/####` ou seletor | ⏳ |
| U12 | Até 3 diálogos modais empilhados (Distribuições → Nova → Adicionar) | Aceitável; considerar painel lateral | ⏳ |
| U13 | Botão vermelho "Excluir" colado ao botão primário | Separar ações destrutivas | ⏳ |
| U14 | Cartão "Lotes válidos em estoque" conta lotes que vencem em 7 dias; chip do Estoque não | Mesma definição nas duas telas | ⏳ |
| U15 | CSV com categoria "GRAOS" e data ISO, sem BOM UTF-8 | Usar rótulos da tela e BOM para o Excel | ⏳ |
| U16 | Salvar instituição faz a chamada de rede na thread da interface | A tela congela até o fim (agora limitada por timeout) | ⏳ |

## 11. Problemas de segurança e qualidade técnica

**Não se aplicam** (verificado 📖): não há banco de dados → **sem SQL e sem risco de SQL Injection**; não há login → **sem senhas armazenadas**.

| Grav. | Achado | Consequência | Status |
|---|---|---|---|
| 🟠 Média | `RelatorioService` chama a USDA **dentro** do cálculo do relatório | Relatório inteiro falha com HTTP 429 (reproduzido, F29) | ⏳ |
| 🟠 Média | Chamadas HTTP de `ViaCepService` e `NutricaoService` **sem timeout** | Tela/console podiam travar indefinidamente | ✅ timeouts 6 s/10–12 s |
| 🟡 Baixa | CPF/CNPJ só verificam a quantidade de dígitos | Documentos inválidos aceitos | ⏳ |
| 🟡 Baixa | CSV não neutraliza fórmulas (nome iniciado por `=`, `+`, `-`, `@`) | "CSV injection" ao abrir no Excel | ⏳ |
| 🟡 Baixa | `NutricaoService`: regex pode pegar kJ no lugar de kcal, guarda 0 no cache quando não acha; a GUI usa outra implementação (mesmo termo "rice": 139 kcal no serviço vs 416 na tela) | Valores divergentes | ⏳ |
| 🟡 Baixa | Mensagens de exceção exibidas cruas | Detalhes técnicos ao usuário | parcial |
| ℹ️ | `DEMO_KEY` fixa; chave aparece em texto na tela | Limite de uso baixo (429) | ⏳ |

**Organização do código (📖):**
- `AlimentaBemApp` tem **3.033 linhas** e **reimplementa regras** que já existem nos serviços (FIFO `planejarFIFO`, capacidade `familiasNoMes`, cálculo nutricional, consulta de CEP e USDA). Duas versões da mesma regra podem divergir — já divergem na USDA.
- **Código morto:** `DoadorService(Impl)`, `DoacaoService(Impl)`, `EnderecoService`, `Repositorio`/`RepositorioMemoria`, `RelatorioService.listarItensVencendoEm7Dias`, além do `.patch` desatualizado.
- `Doador.getDocumento()` devolve `"CPF: …"` (formatação de tela dentro do modelo) e é usado como chave de busca.
- `Alimento` e `InstituicaoBeneficiaria` usam o **nome mutável** em `equals/hashCode`; renomear um objeto que está num `HashMap` corrompe o mapa.
- Desempenho: tudo O(n) em listas; irrelevante no volume atual (a tabela recalcula `familiasNoMes` por linha renderizada).

## 12. Correções realizadas

Backup integral dos fontes originais em [`backup-pre-auditoria/src`](../backup-pre-auditoria/src). 14 arquivos alterados, ~120 linhas; arquitetura e funcionalidades preservadas; nada removido.

| Arquivo | Mudança |
|---|---|
| `controller/ControladorDoacao.java` | Novo `validarDoacao()`; `registrarDoacao()` valida e grava |
| `controller/Fachada.java` | `registrarDoacao`: valida → estoque → grava (atomicidade, F13) |
| `controller/ControladorDoador.java` | CPF 11 / CNPJ 14 dígitos (F04) |
| `model/LoteEstoque.java` | `TOLERANCIA = 1e-9` na retirada e zera resíduos (F23) |
| `service/DistribuicaoServiceImpl.java` | Tolerância no FIFO; rejeita data futura (F22, F23) |
| `service/EstoqueService.java` | Tolerância na sugestão FIFO |
| `service/InstituicaoServiceImpl.java` | Atualiza o CEP junto do endereço; bloqueia nome duplicado; valida antes de alterar (N03, F25) |
| `service/AlimentoServiceImpl.java` | Bloqueia troca de unidade com lotes existentes (F09) |
| `service/RelatorioService.java` | CSV com aspas quando necessário e UTF-8 explícito (F27) |
| `service/ViaCepService.java`, `service/NutricaoService.java` | Timeouts de conexão e de requisição |
| `ui/AlimentaBemApp.java` | Datas estritas (`dd/MM/uuuu` + `STRICT`); alturas do formulário de alimento e de Relatórios; confirmação no "Sair" |
| `ui/MenuInstituicoes.java` | `merge` em vez de `put` (C02) |
| `ui/MenuAlimentos.java` | Valida opção de categoria/unidade (C01) |

Todas as correções foram **reverificadas**: recompilação sem erros, 34 testes (32 ✅ com a USDA disponível), roteiro do console e reabertura da GUI ([19](auditoria/capturas/19-DEPOIS-relatorios-cartoes.jpg)–[22](auditoria/capturas/22-DEPOIS-confirmar-sair.jpg)).

### 12.1 Persistência em arquivo (implementada a pedido, após a auditoria)

**Como funciona:** os mesmos repositórios em memória continuam sendo usados; a nova classe `repository/ArmazenamentoArquivo` grava os 6 juntos num único arquivo após cada operação da `Fachada` que altera dados, e os recarrega ao abrir. Gravar os 6 num só fluxo mantém as referências compartilhadas (o mesmo `Alimento` no catálogo, nos lotes e nas doações), o que arquivos separados por repositório perderiam.

- **Arquivo:** `~/.alimentabem/dados.dat` (ou `-Dalimentabem.dados=...`).
- **Segurança e robustez:** grava num `.tmp` e troca no final (falha no meio não corrompe); filtro de desserialização aceita só classes `com.alimentabem` e do JDK; arquivo ilegível é preservado como `dados.dat.ilegivel-<data>` e o sistema inicia vazio com aviso no console; operação rejeitada não grava nada.
- **Alterações:** `Serializable` + `serialVersionUID = 1L` nas classes do modelo e nos 6 repositórios em memória; `Fachada()` usa o arquivo e `Fachada(Path)` aceita outro (ou `null` = só memória); novo `Fachada.definirValorNutricional` (a tela USDA alterava o alimento direto, sem passar pela Fachada, e isso não seria salvo); tooltip no rodapé com o local do arquivo; mensagem do "Sair" atualizada; README atualizado.
- **Verificado:** 🧪 F30 e P01–P07 (reinício preserva tudo, inclusive valor nutricional e instituições com endereço; referências compartilhadas; REQ17/REQ20/FIFO continuam valendo com o histórico recarregado; arquivo corrompido; classe estranha recusada; erro não grava) — 8/8 ✅. 🖥️ GUI: cadastrar alimento + valor nutricional, fechar pelo **X da janela**, reabrir → dados presentes ([23](auditoria/capturas/23-DEPOIS-reaberto-1-alimento.jpg), [24](auditoria/capturas/24-DEPOIS-alimento-persistido.jpg)).
- **Limitações:** GUI e console abertos ao mesmo tempo sobrescrevem um ao outro; o formato binário só é lido por Java; se uma classe do modelo mudar de forma incompatível (ex.: remover/trocar o tipo de um campo), arquivos antigos podem não abrir (serão preservados como `.ilegivel`). Os avisos novos de lint `[serial]` vêm de campos declarados como `List` (interface); em tempo de execução são `ArrayList`, que é serializável.

### 12.2 Peso por unidade (implementado a pedido, após a auditoria)

**Problema:** o cálculo nutricional convertia toda quantidade com ×1000 g, então 1 "unidade" valia 1 kg.

**Solução:** novo campo `pesoPorUnidadeGramas` no `Alimento` e um único método de conversão, `Alimento.gramasPara(quantidade)` — kg e L → ×1000 (L considera 1 g/ml); unidade → × peso. O `RelatorioService` e a GUI passam a usar esse mesmo método (antes cada um tinha a sua conta).

- **Regras:** o peso é **obrigatório e > 0** quando a unidade é "Unidade" (validado no serviço, então vale para GUI e console); para kg/L ele não se aplica e fica vazio; a validação acontece antes de qualquer alteração (edição recusada não muda nada).
- **GUI:** campo "Peso de 1 unidade (g)" no formulário de alimento, habilitado só para "Unidade" ([25](auditoria/capturas/25-DEPOIS-peso-obrigatorio.jpg)); a lista mostra "Unidade (50 g)" ([26](auditoria/capturas/26-DEPOIS-lista-unidade-50g.jpg)).
- **Console:** pergunta o peso ao cadastrar alimento com unidade 3.
- **Dados já salvos:** alimentos em "Unidade" cadastrados antes não têm peso — aparecem como "Unidade (sem peso)", **ficam fora do total** (em vez de contar como 1 kg) e entram no aviso "item(ns) sem valor nutricional (ou sem peso por unidade) não entraram no cálculo"; basta editar o alimento e informar o peso. O relatório do serviço também não consulta mais a USDA para itens que não podem ser calculados.
- **Compatibilidade:** as assinaturas antigas de `cadastrarAlimento`/`atualizarAlimento` continuam existindo (a de edição mantém o peso atual).
- **Verificado:** 🧪 F28 (10 ovos × 50 g × 140 kcal/100 g = 700 kcal) e U01–U06 — 7/7 ✅; ⌨️ console pede e grava o peso; 🖥️ GUI: peso vazio é recusado, 12 ovos de 50 g = **840 kcal / 72 g** ([27](auditoria/capturas/27-DEPOIS-12-ovos-840-kcal.jpg)).

## 13. Problemas que permaneceram

1. ~~Persistência~~ — implementada (§12.1).
2. ~~Cálculo nutricional para `UNIDADE`~~ — resolvido com peso por unidade (§12.2).
3. Itens de UX ⏳ da §10 e de qualidade ⏳ da §11.
4. A conversão de litros assume 1 g/ml (óleo ≈ 0,92 g/ml) — aceitável para o propósito do relatório.

## 14. Recomendações (em ordem de prioridade)

1. Fazer a GUI usar os serviços em vez de duplicar regras (FIFO, capacidade, nutrição, CEP/USDA) e tirar a chamada de rede de dentro do `RelatorioService` (usar só valores já salvos no Alimento).
2. Remover código morto e o `.patch` desatualizado; padronizar `IRepositorioDoador`/`IRepositorioEstoque` com `IRepositorio`.
3. Transformar `AuditTests.java` em testes JUnit dentro do projeto (cobrem REQ01–REQ20).
4. UX: editar doadores/instituições, colunas sem truncar, mensagens sem "REQxx", confirmar também no X da janela, seleção de resultado da USDA.
5. Validar dígitos verificadores de CPF/CNPJ; neutralizar fórmulas no CSV.

---

### Como reproduzir

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java") && cp -r src/com/alimentabem/ui/resources out/com/alimentabem/ui/
java -cp out com.alimentabem.ui.AlimentaBemApp
javac -encoding UTF-8 -cp out -d out-tests docs/auditoria/testes/AuditTests.java
java -cp "out;out-tests" AuditTests --rede        # sem --rede pula BrasilAPI/USDA
```
Resultados completos: [antes](auditoria/testes/resultado-antes-das-correcoes.txt) · [depois](auditoria/testes/resultado-depois-das-correcoes.txt) · capturas em [`docs/auditoria/capturas/`](auditoria/capturas/).
