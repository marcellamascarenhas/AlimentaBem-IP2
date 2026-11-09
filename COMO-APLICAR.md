# Como aplicar esta atualização

Esta pasta contém **só o que mudou** em relação ao projeto que você já tem no GitHub.

## Jeito mais simples (copiar por cima)
1. Faça uma cópia de segurança da pasta do projeto.
2. Copie o **conteúdo** desta pasta (`src`, `docs`, `README.md`, `.gitattributes`) para a raiz do projeto
   e aceite **substituir** os arquivos existentes. (Arquivos que não estão aqui não são tocados.)
3. No IntelliJ: *Build > Rebuild Project* e rode `com.alimentabem.ui.AlimentaBemApp`.

## Alternativa: pelos patches (`patches/`)
Na raiz do projeto, no terminal:

    git apply --check --ignore-whitespace patches/01-auditoria-codigo.patch
    git apply --ignore-whitespace patches/01-auditoria-codigo.patch
    git apply --ignore-whitespace patches/02-ajustes-visuais.patch

* `01` = as mudanças da auditoria (as mesmas descritas em `docs/MUDANCAS-EXPLICADAS.md`).
* `02` = dois ajustes visuais de largura de coluna/texto de ajuda na tela de alimentos.
* Se o `--check` der erro, use o jeito de copiar por cima.

## NÃO envie para o GitHub
* A pasta `backup-pre-auditoria/` (é uma segunda cópia de todo o `src`; confunde o IntelliJ).
* O arquivo `correcoes-alimentabem.patch` (não aplica nem na versão antiga nem na nova).
