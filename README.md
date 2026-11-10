# Mundo Anel

Mod Fabric para **Minecraft Java 1.21.4** criado pela GanhamosGames.

## Mundo recomendado

Ao criar o mundo, coloque esta seed no campo **Seed**:

```text
202611081214
```

O mod fixa o ponto de nascimento do Overworld em **X=0, Y=98, Z=0**, no centro da pequena ilha gramada. Isso evita nascer por acaso em um bioma de pinheiros do terreno vanilla. A seed acima deixa a parte vanilla do mundo reproduzível; a região do Mundo Anel também é determinística.

**Crie um mundo novo** depois de instalar a versão 1.1.1. Chunks que já tinham sido gerados antes do mod não são reconstruídos automaticamente.

## O que ele gera

O mundo continua usando a geração normal do Minecraft. Em torno das coordenadas **X=0, Z=0**, os chunks novos recebem uma região especial inspirada na imagem de referência:

- uma grande ilha em formato de anel, com costa irregular;
- seis biomas: área nevada/montanhosa, taiga, floresta, savana, badlands e deserto;
- uma lagoa central com uma pequena ilha gramada de nascimento;
- um rio sinuoso comprido ligado à lagoa;
- um canal externo de água ao redor da ilha;
- árvores e cactos nas regiões correspondentes.

Fora dessa área, a geração vanilla permanece intacta.

## Correção da versão 1.1.1

- Corrigido o travamento em 100% ao criar o mundo: a geração dos trechos de água consultava o mundo em vez do próprio chunk e ficava esperando por ele para sempre.
- Corrigido um bloco que sobrava flutuando acima das áreas de terra.

## Melhorias da versão 1.1.0

- O spawn do Overworld agora é fixado no centro da ilha, em terreno gramado e bioma de planícies.
- As amostras de bioma X/Z são calculadas uma vez por coluna quart e reutilizadas na altura, reduzindo cálculos repetidos.
- A leitura da altura usa o heightmap do próprio chunk, evitando consultas repetidas ao mundo.
- Chunks nos cantos do quadrado de verificação que não alcançam a região circular são ignorados.

## Requisitos

- Minecraft Java Edition **1.21.4**
- Fabric Loader 0.16.10 ou superior compatível
- Fabric API 0.119.4+1.21.4
- Java 21

## Instalação

1. Instale Fabric Loader para Minecraft 1.21.4.
2. Coloque o Fabric API 0.119.4+1.21.4 na pasta `mods`.
3. Coloque `mundo-anel-1.1.1.jar` na mesma pasta.
4. Crie um mundo novo usando a seed `202611081214`.
5. O jogador deve nascer no centro do Mundo Anel. Para voltar ao centro, use `/tp @s 0 98 0`.

A geração especial acontece quando os chunks são criados. O centro fixo da região é X=0, Z=0.

## Compilar

Com Java 21 e Gradle 8.12:

```bash
gradle clean build
```

O arquivo compilado aparece em `build/libs/mundo-anel-1.1.1.jar`.

O workflow do GitHub Actions compila o projeto a cada atualização e publica o JAR como artefato e na página Releases quando a compilação passa.
