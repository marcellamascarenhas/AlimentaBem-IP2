# Como o sistema usa a API de alimentos (USDA FoodData Central)

Requisito **REQ05**. Classe responsável: `service/NutricaoService` (+ `service/CatalogoAlimentos`).

## 1. O que é a API
A USDA FoodData Central é a base pública de composição de alimentos do governo dos EUA. O endpoint usado é
`GET https://api.nal.usda.gov/fdc/v1/foods/search`, que devolve, para um termo de busca, uma lista de alimentos com
seus nutrientes **por 100 g**. A tabela brasileira TACO não tem API REST, por isso foi escolhida a USDA.

## 2. Português ou inglês? **Inglês.**
A base da USDA é toda em inglês: buscar `arroz` não retorna o alimento certo; `rice` retorna.
Por isso o sistema separa as duas coisas:

* o **nome exibido** ao usuário continua em português ("Arroz", "Maçã");
* o **termo enviado à USDA** é traduzido por um catálogo interno (`CatalogoAlimentos`, com os alimentos mais comuns
  em doações): `Arroz → rice white long-grain raw`, `Maçã → apples raw with skin`, `Feijão preto → beans black mature seeds raw`.

O catálogo entende acentos, maiúsculas, plural ("feijões", "maçãs") e prefere o nome mais específico
("batata doce" ganha de "batata"). Se o nome **não** estiver no catálogo, ele é enviado como foi digitado, então o
usuário pode digitar diretamente em inglês (ex.: `quinoa`). Na tela, o termo realmente enviado aparece abaixo do campo.

## 3. O que é enviado e lido
* `dataType=Foundation,SR Legacy`: só alimentos **genéricos**, com valores analisados em laboratório. Os produtos de
  marca ("Branded") têm dados ruidosos e ficam de fora.
* `pageSize=15`: o sistema escolhe, entre os 15 primeiros, o primeiro cuja descrição contém as palavras-chave do termo.
* **Calorias** são lidas só em **kcal** (a USDA também devolve kJ, que é ~4,2 vezes maior): nutriente de código 1008
  (ou 2047/2048 em alimentos "Foundation"). **Proteína** em gramas: código 1003. A leitura é pelo **código** do nutriente,
  não pelo nome.
* Se a USDA não trouxer energia para o alimento, o resultado é descartado (antes, o sistema gravava 0 kcal em silêncio).

## 4. Chave da API
* Sem configurar nada, usa-se a `DEMO_KEY`, limitada a poucas consultas por hora (cerca de 30 por hora e 50 por dia por IP,
  segundo a documentação da USDA). Em apresentação/teste em grupo, com vários computadores na mesma rede, ela pode acabar
  (erro HTTP 429).
* Chave **gratuita e instantânea**: https://fdc.nal.usda.gov/api-key-signup (limite bem maior).
* Para usar: variável de ambiente `USDA_API_KEY` (no IntelliJ: *Run → Edit Configurations → Environment variables*),
  ou digitar no campo "Chave da API" da tela de valor nutricional.

## 5. Contingência (se a internet ou a chave falharem)
Se a USDA não responder (sem internet, timeout, chave sem cota/inválida) e o alimento estiver no catálogo, o sistema usa
os **valores de referência da própria tabela local** (USDA SR Legacy, por 100 g, aproximados) e a tela avisa em laranja
que a origem foi a "Tabela local de referência". Se o alimento também não estiver no catálogo, aparece o erro e o usuário
pode digitar os valores manualmente. Assim a demonstração não depende da rede.

## 6. Limitações
* A tradução cobre os alimentos mais comuns do catálogo; para outros, digite em inglês.
* Alimentos brasileiros sem equivalente direto na USDA (ex.: tapioca, farinha de mandioca) podem não ser encontrados.
* Os valores são **por 100 g**; alimentos em "unidade" dependem do peso médio da unidade informado no cadastro.
* A leitura do JSON é feita com expressões regulares (sem biblioteca externa); se a USDA mudar o formato da resposta, é
  este o ponto a ajustar.
