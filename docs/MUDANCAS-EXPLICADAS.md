# O que mudou no AlimentaBem (explicado para quem está começando em Java)

Este documento explica **cada mudança feita no código**, por que ela foi necessária e qual conceito de Java está por trás. A ideia é que você consiga ler o código novo e entender o motivo de cada linha.

> O relatório técnico completo (testes, capturas de tela, gravidade dos erros) está em [`RELATORIO-AUDITORIA.md`](RELATORIO-AUDITORIA.md).
> Uma cópia do código **original**, antes de qualquer mudança, ficou guardada em `backup-pre-auditoria/src`, caso você queira comparar.

---

## Sumário

1. [Antes de tudo: como o projeto é organizado](#1-antes-de-tudo-como-o-projeto-é-organizado)
2. [Doação gravada antes de ser validada](#2-doação-gravada-antes-de-ser-validada)
3. [Erro de arredondamento com `double`](#3-erro-de-arredondamento-com-double)
4. [Datas impossíveis, como 31/02](#4-datas-impossíveis-como-3102)
5. [Mesmo alimento duas vezes no console](#5-mesmo-alimento-duas-vezes-no-console)
6. [Menu do console quebrando com número inválido](#6-menu-do-console-quebrando-com-número-inválido)
7. [Validações que faltavam (CPF, nome repetido, data futura, unidade, CEP)](#7-validações-que-faltavam)
8. [Arquivo CSV com colunas deslocadas](#8-arquivo-csv-com-colunas-deslocadas)
9. [Chamadas de internet sem limite de tempo](#9-chamadas-de-internet-sem-limite-de-tempo)
10. [Ajustes na tela](#10-ajustes-na-tela)
11. [Salvar os dados em arquivo (persistência)](#11-salvar-os-dados-em-arquivo-persistência)
12. [Peso por unidade no cálculo nutricional](#12-peso-por-unidade-no-cálculo-nutricional)
13. [Como os testes foram feitos](#13-como-os-testes-foram-feitos)
14. [O que ainda dá para melhorar](#14-o-que-ainda-dá-para-melhorar)
15. [Glossário rápido](#15-glossário-rápido)

---

## 1. Antes de tudo: como o projeto é organizado

O projeto é dividido em **camadas**. Cada camada tem uma responsabilidade:

```
ui/          → telas (janela Swing e menu de console). Só conversa com a Fachada.
controller/  → Fachada e controladores: o "balcão de atendimento" do sistema.
service/     → regras de negócio (FIFO, capacidade mensal, validade, relatórios...).
repository/  → onde os objetos ficam guardados (listas em memória).
model/       → as classes de dados: Alimento, Doador, Doacao, LoteEstoque...
```

A **Fachada** (`controller/Fachada.java`) é um *padrão de projeto*: em vez de a tela conhecer dez classes diferentes, ela chama métodos de uma só classe, e a Fachada repassa para quem sabe fazer o trabalho.

Todas as mudanças abaixo **respeitaram essa organização**: nenhuma camada nova foi inventada, nenhuma funcionalidade foi removida.

---

## 2. Doação gravada antes de ser validada

**Gravidade: alta.** Arquivo: `controller/Fachada.java` (e `ControladorDoacao.java`).

### O problema

```java
// ANTES
public void registrarDoacao(Doacao doacao) {
    controladorDoacao.registrarDoacao(doacao);   // 1º grava no histórico
    estoqueService.registrarDoacao(doacao);      // 2º valida e coloca no estoque
}
```

A ordem estava invertida. Se o passo 2 lançasse uma exceção (por exemplo, um item com validade vencida), o passo 1 **já tinha acontecido**: a doação aparecia no histórico e nos relatórios, mas nenhum alimento entrava no estoque.

### A correção

```java
// DEPOIS
public void registrarDoacao(Doacao doacao) {
    controladorDoacao.validarDoacao(doacao);   // 1º só confere (não grava nada)
    estoqueService.registrarDoacao(doacao);    // 2º valida validades e dá entrada nos lotes
    controladorDoacao.registrarDoacao(doacao); // 3º só agora grava no histórico
    salvar();                                  // 4º salva em arquivo (ver seção 11)
}
```

Foi criado o método `validarDoacao()` no `ControladorDoacao`, que faz as verificações **sem gravar**.

### Conceito: "tudo ou nada"

Quando uma operação tem várias etapas, ou todas acontecem ou nenhuma deve acontecer. Uma regra prática: **valide primeiro, altere depois**. Quando uma exceção é lançada, o Java interrompe o método naquele ponto, e as linhas seguintes não rodam. Então, colocando as validações no começo, um erro impede todas as gravações.

---

## 3. Erro de arredondamento com `double`

**Gravidade: média.** Arquivos: `model/LoteEstoque.java`, `service/DistribuicaoServiceImpl.java`, `service/EstoqueService.java`.

### O problema

Ao tentar distribuir exatamente todo o saldo de um alimento, o sistema às vezes dizia:

```
Estoque insuficiente de 'Arroz'. Faltam 2.7E-17 kg.
```

`2.7E-17` significa 0,000000000000000027. Por que isso aparece?

### Conceito: `double` não é exato

O tipo `double` guarda números em **binário**. Alguns números decimais simples, como 0,1, não têm representação exata em binário (assim como 1/3 não tem representação exata em decimal: 0,3333...). Teste no seu computador:

```java
System.out.println(0.1 + 0.2);   // imprime 0.30000000000000004
```

Por isso, comparar `double` com `==` ou `>` diretamente é perigoso: sobram "migalhas" minúsculas.

### A correção

Foi criada uma **tolerância**, uma margem tão pequena que nunca faz diferença real:

```java
// model/LoteEstoque.java
public static final double TOLERANCIA = 1e-9;   // 0,000000001

public void retirar(double quantidade) {
    ...
    // só reclama se faltar MAIS do que a tolerância
    if (quantidade > quantidadeDisponivel + TOLERANCIA) {
        throw new ValidacaoException("Quantidade solicitada ... excede a quantidade disponível ...");
    }
    this.quantidadeDisponivel -= quantidade;
    if (Math.abs(this.quantidadeDisponivel) < TOLERANCIA) {
        this.quantidadeDisponivel = 0;   // limpa a "migalha"
    }
}
```

- `public static final` cria uma **constante**: um valor único, que pertence à classe e não muda.
- `Math.abs(x)` devolve o valor absoluto (sem sinal).

A mesma tolerância foi usada no algoritmo FIFO da distribuição e na sugestão de lotes.

---

## 4. Datas impossíveis, como 31/02

**Gravidade: média.** Arquivo: `ui/AlimentaBemApp.java`.

### O problema

Ao digitar a validade `31/02/2026`, o Java não reclamava e salvava `28/02/2026`. Ou seja, a data registrada era diferente da digitada.

### A correção

```java
// ANTES
DateTimeFormatter.ofPattern("dd/MM/yyyy");

// DEPOIS
DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
```

### Conceito: modo de interpretação de datas

O `DateTimeFormatter` tem três modos de lidar com datas estranhas. O padrão (`SMART`) "conserta" o dia para o último dia válido do mês. O modo `STRICT` (rigoroso) **lança uma exceção**, e a tela mostra "data inválida".

Por que trocar `yyyy` por `uuuu`? No modo `STRICT`, `yyyy` significa "ano da era" (exige saber se é antes ou depois de Cristo) e faz até datas válidas falharem. `uuuu` é o "ano" comum. É um detalhe chato da API, mas é a forma correta.

---

## 5. Mesmo alimento duas vezes no console

**Gravidade: média.** Arquivo: `ui/MenuInstituicoes.java`.

### O problema

Na distribuição pelo console, se o usuário pedisse "Arroz 3 kg" e depois "Arroz 4 kg", o sistema distribuía **só 4 kg**, em vez de 7.

```java
// ANTES
itensPedidos.put(alimento, qtd);
```

### Conceito: `Map.put` substitui

Um `Map` guarda pares **chave → valor**, e cada chave aparece uma única vez. O `put` com uma chave que já existe **substitui** o valor antigo.

### A correção

```java
// DEPOIS
itensPedidos.merge(alimento, qtd, Double::sum);
```

O `merge` faz o seguinte:
- se a chave **não existe**, guarda `qtd`;
- se **já existe**, junta o valor antigo com o novo usando a função passada; aqui, `Double::sum` (soma).

`Double::sum` é uma **referência de método**: uma forma curta de escrever `(a, b) -> a + b`.

---

## 6. Menu do console quebrando com número inválido

**Gravidade: baixa.** Arquivo: `ui/MenuAlimentos.java`.

### O problema

Ao escolher a categoria `9` (existem só 4), aparecia para o usuário:

```
Erro: Index 8 out of bounds for length 4
```

O código fazia `CategoriaAlimento.values()[catOp - 1]`. `values()` devolve um **array** com as opções do `enum`, e acessar a posição 8 de um array de tamanho 4 lança `ArrayIndexOutOfBoundsException`.

### A correção

Conferir o intervalo **antes** de acessar o array:

```java
if (catOp < 1 || catOp > CategoriaAlimento.values().length) {
    System.out.println("Categoria inválida. Escolha um número de 1 a " + CategoriaAlimento.values().length + ".");
    return;
}
CategoriaAlimento cat = CategoriaAlimento.values()[catOp - 1];
```

O mesmo foi feito para a unidade de medida. Lição: **mensagens de erro técnicas não devem chegar ao usuário**; valide a entrada e explique o que ele deve digitar.

---

## 7. Validações que faltavam

Várias regras estavam incompletas. Todas seguem a mesma ideia: **conferir antes de alterar** e lançar `ValidacaoException` com uma mensagem clara.

| O que acontecia | Onde foi corrigido | O que foi feito |
|---|---|---|
| O console aceitava CPF "abc" ou CNPJ vazio | `ControladorDoador` | CPF precisa ter 11 dígitos e CNPJ, 14 |
| Dava para renomear uma instituição com o nome de outra já existente | `InstituicaoServiceImpl.atualizar` | Verifica se o nome já existe antes de renomear (mudar só maiúsculas/minúsculas continua permitido) |
| Ao editar uma instituição, o endereço mudava mas o **CEP** antigo ficava salvo | `InstituicaoServiceImpl.atualizar` | Chama `setCep(cepLimpo)` junto com `setEndereco(...)` e valida o CEP novo antes de mudar qualquer coisa |
| Uma distribuição com data no futuro era aceita, gastando a capacidade de meses futuros | `DistribuicaoServiceImpl` | Recusa: "A data da distribuição não pode estar no futuro." |
| Dava para trocar a unidade de um alimento que já tinha estoque (2 L viravam 2 unidades) | `AlimentoServiceImpl` | Bloqueia a troca de unidade se existirem lotes |

### Conceito: onde colocar a validação

Repare que as validações ficaram na camada de **serviço/controlador**, e não na tela. Assim, a regra vale para a janela **e** para o console, sem duplicar código. A tela pode até validar também, para dar uma resposta mais rápida ao usuário, mas a palavra final é do serviço.

---

## 8. Arquivo CSV com colunas deslocadas

**Gravidade: baixa.** Arquivo: `service/RelatorioService.java`.

### O problema

O relatório CSV separa as colunas com `;`. Se o nome de um doador fosse `Mercado Bom; Filial 2`, o `;` do nome virava uma coluna extra, e tudo depois dele ficava fora do lugar no Excel.

### A correção

Foi criado um método auxiliar que segue a regra padrão do CSV: um campo que contém `;`, aspas ou quebra de linha vai **entre aspas**, e uma aspa dentro dele vira duas aspas (`""`).

```java
private static String campoCsv(String valor) {
    String s = valor == null ? "" : valor;
    if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
}
```

Além disso, o arquivo passou a ser escrito explicitamente em **UTF-8**, para que acentos (ç, ã, é) não virem símbolos estranhos:

```java
new OutputStreamWriter(new FileOutputStream(caminhoArquivo), StandardCharsets.UTF_8)
```

`"\""` é como se escreve uma aspa dentro de uma `String` em Java: a barra invertida "escapa" o caractere.

---

## 9. Chamadas de internet sem limite de tempo

Arquivos: `service/ViaCepService.java` e `service/NutricaoService.java`.

O sistema consulta duas APIs na internet: BrasilAPI (endereço pelo CEP) e USDA (calorias dos alimentos). Sem **timeout** (tempo máximo de espera), se a internet travasse, o programa ficaria esperando para sempre e a tela congelaria.

```java
private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))   // até 6 s para conectar
        .build();

HttpRequest req = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(10))          // até 10 s para responder
        .build();
```

Se o tempo acabar, o Java lança uma exceção, e o sistema mostra uma mensagem de erro em vez de travar. O `HttpClient` também passou a ser criado **uma vez só** (`static final`) e reaproveitado, em vez de ser criado a cada consulta.

---

## 10. Ajustes na tela

Arquivo: `ui/AlimentaBemApp.java`.

- **Campos cortados:** o formulário de alimento e os cartões de Relatórios ficavam com a parte de baixo escondida. O tamanho das janelas foi aumentado.
- **Botão "Sair":** agora pergunta "Encerrar o AlimentaBem?" antes de fechar, usando `JOptionPane.showConfirmDialog`.
- **Valor nutricional:** a tela da USDA alterava o alimento diretamente. Agora ela chama `fachada.definirValorNutricional(...)`, para respeitar as camadas e para que a mudança seja salva no arquivo (seção 11).
- **Rodapé:** ao passar o mouse, mostra onde o arquivo de dados está salvo.

---

## 11. Salvar os dados em arquivo (persistência)

**Antes:** tudo ficava só na memória (em `ArrayList`). Ao fechar o programa, **todos os dados sumiam**.

**Agora:** os dados são salvos num arquivo após cada alteração e carregados de volta ao abrir o programa.

### Onde fica o arquivo

```
C:\Users\<seu usuário>\.alimentabem\dados.dat
```

Para usar outro arquivo (útil em testes), basta rodar com:

```
java -Dalimentabem.dados=C:\outra\pasta\dados.dat ...
```

### Conceito: serialização

**Serializar** é transformar um objeto Java (com todos os objetos que ele referencia) numa sequência de bytes que pode ir para um arquivo. **Desserializar** é o caminho de volta.

Para uma classe poder ser serializada, ela precisa "avisar" ao Java, implementando a interface `Serializable`:

```java
public class Alimento implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    ...
}
```

- `Serializable` é uma **interface marcadora**: não tem nenhum método para implementar, só serve de etiqueta.
- `serialVersionUID` é o "número de versão" da classe. Se ele mudar, arquivos antigos deixam de abrir. Fixar em `1L` evita que o Java gere um número diferente a cada pequena mudança no código.

Isso foi adicionado em todas as classes do `model/` e nos 6 repositórios em memória.

### A nova classe `ArmazenamentoArquivo`

Ela fica em `repository/ArmazenamentoArquivo.java` e faz três coisas:

**1. Guarda os 6 repositórios juntos**

```java
private static final class Dados implements Serializable {
    final RepositorioDoadorMemoria doadores = new RepositorioDoadorMemoria();
    final RepositorioDoacaoMemoria doacoes = new RepositorioDoacaoMemoria();
    final RepositorioAlimentoMemoria alimentos = new RepositorioAlimentoMemoria();
    ... // estoque, instituições, distribuições
}
```

Por que juntos, e não um arquivo por repositório? Porque o **mesmo** objeto `Alimento` "Arroz" está no catálogo, nos lotes e nas doações. Gravando tudo de uma vez, o Java percebe que é o mesmo objeto e, ao carregar, recria **um único** Arroz compartilhado. Com arquivos separados, voltariam três "Arroz" diferentes, e mudar um não mudaria os outros.

**2. Salva com segurança**

```java
public void salvar() {
    Path tmp = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
    try (ObjectOutputStream out = new ObjectOutputStream(
            new BufferedOutputStream(Files.newOutputStream(tmp)))) {
        out.writeObject(dados);          // 1º escreve num arquivo temporário
    }
    Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING,
               StandardCopyOption.ATOMIC_MOVE);   // 2º troca de uma vez
}
```

Se o programa cair no meio da gravação, quem fica pela metade é o `.tmp`, e o arquivo verdadeiro continua inteiro.

`try (...) { }` é o **try-with-resources**: o Java fecha o arquivo automaticamente ao sair do bloco, mesmo se acontecer uma exceção.

**3. Carrega com segurança**

```java
private static final ObjectInputFilter FILTRO = ObjectInputFilter.Config.createFilter(
        "maxdepth=50;com.alimentabem.**;java.util.*;java.lang.*;java.time.*;!*");
...
in.setObjectInputFilter(FILTRO);
```

Ler um arquivo serializado adulterado pode ser perigoso, porque ele pode tentar criar objetos de classes inesperadas. O **filtro** funciona como uma lista de convidados: só aceita classes do próprio projeto (`com.alimentabem`) e do Java básico, e recusa todo o resto (`!*`).

Se o arquivo estiver corrompido, ele **não é apagado**: é renomeado para `dados.dat.ilegivel-<data>`, e o sistema começa vazio com um aviso.

### Como a Fachada usa isso

```java
public Fachada()           // usa o arquivo padrão
public Fachada(Path arq)   // usa outro arquivo; null = só memória, sem salvar (para testes)
```

Cada método da Fachada que **altera** dados (cadastrar, editar, excluir, doar, distribuir) chama `salvar()` no final, só quando deu tudo certo. Uma operação recusada não salva nada.

Repare que o resto do sistema **não precisou mudar**: os serviços continuam recebendo os mesmos repositórios de antes. É uma vantagem de usar **interfaces** (`IRepositorioAlimento` etc.): quem usa o repositório não precisa saber de onde ele veio.

### Limitação importante

Não abra a **janela e o console ao mesmo tempo**: cada um tem sua cópia na memória e, ao salvar, um apaga o que o outro gravou.

---

## 12. Peso por unidade no cálculo nutricional

**Gravidade: alta.** Arquivos: `model/Alimento.java`, `service/AlimentoServiceImpl.java`, `service/RelatorioService.java`, `controller/Fachada.java`, telas.

### O problema

As calorias da USDA vêm **por 100 gramas**. Para calcular, o sistema convertia a quantidade em gramas multiplicando sempre por 1000:

- 2 kg de arroz → 2000 g ✔️
- 12 **ovos** → 12 × 1000 = 12.000 g ❌ (como se cada ovo pesasse 1 kg!)

Resultado: 12 ovos davam **16.800 kcal**, quando o certo é cerca de 840 kcal.

### A correção

**1. Novo campo no `Alimento`**

```java
private Double pesoPorUnidadeGramas;   // ex.: 50.0 para um ovo
```

Por que `Double` (com D maiúsculo) e não `double`? `double` é um **tipo primitivo** e sempre tem um número (o padrão é 0). `Double` é uma **classe** e pode ser `null`, o que aqui quer dizer "não informado". Isso é útil porque kg e litro não precisam de peso, e alimentos antigos, salvos antes desta mudança, não têm peso.

**2. Um único método de conversão**

```java
public Double gramasPara(double quantidade) {
    switch (unidadeMedida) {
        case QUILOGRAMA:
        case LITRO:
            return quantidade * 1000.0;        // litro: considera 1 g por ml
        case UNIDADE:
            return pesoPorUnidadeGramas == null ? null : quantidade * pesoPorUnidadeGramas;
        default:
            return null;
    }
}
```

- Dois `case` seguidos sem `return` entre eles compartilham o mesmo código.
- `condição ? valorSeVerdadeiro : valorSeFalso` é o **operador ternário**, um `if/else` em uma linha.
- Devolve `null` quando **não é possível** calcular.

Antes, a tela e o `RelatorioService` faziam **cada um sua conta**. Agora os dois chamam `gramasPara`. Esse é o princípio **DRY** (*Don't Repeat Yourself*, "não se repita"): se a regra mudar, muda num lugar só.

**3. O cálculo usa o método**

```java
// service/RelatorioService.java
Double gramas = alimento.gramasPara(quantidade);
if (gramas == null) return 0;   // sem peso: não entra no total (em vez de inventar 1 kg)
...
return (gramas / 100.0) * info.getCaloriasPor100g();
```

Conta de exemplo: 12 ovos × 50 g = 600 g → 600 / 100 × 140 kcal = **840 kcal**.

**4. Regra: o peso é obrigatório para "Unidade"**

```java
// service/AlimentoServiceImpl.java
private static Double validarPeso(UnidadeMedida unidade, Double peso) {
    if (unidade != UnidadeMedida.UNIDADE) return null;   // kg/L: peso não se aplica
    if (peso == null || peso.isNaN() || peso <= 0)
        throw new ValidacaoException("Informe o peso médio de 1 unidade em gramas (maior que zero). ...");
    return peso;
}
```

`isNaN()` testa o valor especial *Not a Number* ("não é um número"), que um `double` pode assumir.

**5. Telas**
- **Janela:** novo campo "Peso de 1 unidade (g)" no formulário de alimento, ativo só quando a unidade é "Unidade". A lista mostra "Unidade (50 g)".
- **Console:** ao escolher a unidade 3, pergunta o peso médio.

**6. Compatibilidade (sobrecarga de métodos)**

Os métodos antigos continuam existindo, ao lado dos novos:

```java
cadastrarAlimento(String nome, CategoriaAlimento cat, UnidadeMedida um)               // antigo
cadastrarAlimento(String nome, CategoriaAlimento cat, UnidadeMedida um, Double peso)  // novo
```

Ter dois métodos com o mesmo nome e parâmetros diferentes é **sobrecarga** (*overloading*). O Java escolhe qual chamar pelos argumentos passados. Assim, nenhum código que já chamava a versão antiga quebrou.

**Alimentos já salvos sem peso** aparecem como "Unidade (sem peso)" e ficam fora do total, com um aviso. Para corrigir, basta editar o alimento e informar o peso.

---

## 13. Como os testes foram feitos

Foram usados **apenas dados fictícios**, e o arquivo de dados real nunca foi tocado (os testes usam arquivos temporários).

1. **Testes automáticos:** `docs/auditoria/testes/AuditTests.java`, com 47 casos que chamam a Fachada e conferem o resultado. Resultado atual: **45 passam**. As 2 falhas acontecem só porque a chave de demonstração da USDA (`DEMO_KEY`) tem limite de uso e respondeu "HTTP 429 — muitas requisições". Não é um erro do código.
2. **Console:** roteiro digitado no menu, com a saída salva em `docs/auditoria/testes/`.
3. **Janela:** todas as 16 telas foram abertas e usadas de verdade, com capturas de tela em `docs/auditoria/capturas/` (as que têm "DEPOIS" no nome mostram o resultado das correções).

Para compilar o sistema e rodar os testes (na pasta raiz do projeto, num terminal Bash):

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java") && cp -r src/com/alimentabem/ui/resources out/com/alimentabem/ui/
javac -encoding UTF-8 -cp out -d out-tests docs/auditoria/testes/AuditTests.java
java -cp "out;out-tests" AuditTests --rede
```

Sem `--rede`, os testes que dependem de internet (BrasilAPI e USDA) são pulados.

---

## 14. O que ainda dá para melhorar

Estas coisas foram **encontradas, mas não alteradas**, porque eram mudanças de interface ou de organização que não foram pedidas:

- Fechar pelo **X da janela** não pede confirmação (mas os dados já ficam salvos).
- A classe `AlimentaBemApp` tem mais de 3.000 linhas e **repete regras** que já existem nos serviços (FIFO, capacidade, USDA). O ideal seria a tela só chamar a Fachada.
- Há classes que ninguém usa (código morto), como `DoadorService` e `DoacaoService`.
- O CPF/CNPJ só tem a **quantidade** de dígitos conferida; os dígitos verificadores, não.
- Não há edição de doadores e instituições na janela (a Fachada já tem os métodos).
- Mensagens mostram códigos internos como "(REQ13)" para o usuário.
- No console, um item digitado errado descarta a doação inteira.
- A consulta de CEP na janela roda na *thread* da interface, então a tela congela enquanto espera (no máximo alguns segundos, por causa do timeout).

---

## 15. Glossário rápido

| Termo | Significado |
|---|---|
| **Camada** | Grupo de classes com uma mesma responsabilidade (tela, regra, dados) |
| **Fachada** | Classe única que a tela usa para acessar todo o sistema |
| **Exceção** | Objeto lançado com `throw` para avisar que algo deu errado; interrompe o método |
| **`double` vs `Double`** | Primitivo (sempre tem número) vs classe (pode ser `null`) |
| **Tolerância** | Margem mínima para comparar números com casas decimais |
| **`Map.merge`** | Junta um valor novo com o antigo em vez de substituir |
| **Serialização** | Transformar objetos em bytes para salvar em arquivo |
| **`Serializable`** | Interface que marca uma classe como "pode ser salva" |
| **try-with-resources** | `try (recurso) {}`: fecha o recurso automaticamente |
| **Timeout** | Tempo máximo de espera antes de desistir |
| **Sobrecarga** | Métodos com o mesmo nome e parâmetros diferentes |
| **DRY** | "Não se repita": cada regra num lugar só |
| **FIFO** | *First In, First Out*: sai primeiro o lote que vence primeiro |
| **CSV** | Arquivo de texto com colunas separadas por um caractere (aqui, `;`) |
