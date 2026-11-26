# Identidade visual do AlimentaBem

Inspirada na logo (maçã laranja com folha e "check" verde-oliva) e em uma paleta quente e natural.

## Paleta

| Cor | Hex | Uso |
|---|---|---|
| Laranja | `#EF7B52` | destaque, barras de progresso, gráfico, bordas ao passar o mouse |
| Pêssego | `#F2B067` | formas do cabeçalho, tons suaves nos ícones |
| Amarelo | `#F6D56B` | formas do cabeçalho, avisos suaves ("vence em X dias") |
| Verde-oliva | `#939C5F` | folhas do cabeçalho, "lote válido", ícones |
| Oliva escuro | `#5F6B35` | botões principais e cartões de destaque (contraste com texto branco) |
| Laranja escuro | `#C4552F` | textos de destaque sobre fundo claro |
| Creme / areia | `#FBF5E9` / `#F3E4C6` | fundo das telas e cabeçalho das tabelas |
| Marrom | `#3B2F27` | texto (em vez de preto ou cinza-azulado) |

As cores originais (laranja, pêssego, amarelo e oliva) são usadas para decoração e destaque. Para texto e botões foram
usadas versões mais escuras delas, porque texto branco sobre o laranja puro não tem contraste suficiente para leitura.

## Decisões de design

* **Sem degradês**: cores chapadas, como papel recortado, em vez de brilhos e transições de cor.
* **Formas orgânicas**: o cabeçalho usa manchas irregulares e folhas desenhadas com curvas (não círculos perfeitos).
* **Tipografia**: títulos com serifa (Fraunces, Lora ou Georgia, a primeira instalada) e texto em fonte humanista
  (Nunito ou Segoe UI). Se nenhuma estiver instalada, o Java usa a fonte padrão do sistema.
* **Tons variados**: cada cartão do menu usa um tom diferente da paleta; só dois ficam preenchidos (laranja e oliva).
* **Sem enfeites**: sem animações de contagem, sem efeito de vidro e sem sombras fortes.
* **Acessibilidade**: textos com contraste adequado sobre o fundo creme; estados de lote com cor **e** texto.

Todas as cores ficam na classe `Theme`, dentro de `AlimentaBemApp.java`. Para ajustar a paleta, basta alterar ali.
