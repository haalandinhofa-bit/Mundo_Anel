# Mundo Anel

Mod Fabric para **Minecraft Java 1.21.4** criado pela GanhamosGames.

## O que ele gera

O mundo continua usando a geração normal do Minecraft. Em torno das coordenadas **X=0, Z=0**, os chunks novos recebem uma região especial inspirada na imagem de referência:

- uma grande ilha em formato de anel, com costa irregular;
- seis biomas reais do Minecraft: planície nevada/montanhas, taiga, floresta, savana, badlands e deserto;
- uma lagoa central larga com uma pequena ilha rochosa no meio;
- um rio sinuoso com aproximadamente 450 blocos de percurso, ligado à lagoa e à água que contorna a ilha;
- um canal externo de água ao redor da ilha;
- árvores e cactos nas regiões correspondentes.

Fora dessa área, a geração vanilla permanece intacta.

## Requisitos

- Minecraft Java Edition **1.21.4**
- Fabric Loader 0.16.10 ou superior compatível
- Fabric API 0.119.4+1.21.4
- Java 21

## Como testar

1. Instale Fabric Loader para Minecraft 1.21.4.
2. Coloque o Fabric API 0.119.4+1.21.4 na pasta `mods`.
3. Coloque `mundo-anel-1.0.0.jar` na mesma pasta.
4. Crie um **mundo novo**.
5. Entre no mundo e use `/tp @s 0 110 0` para chegar ao centro do mapa.

A geração especial acontece quando os chunks são criados. Por isso, para ver a região completa, use um mundo novo ou explore chunks que ainda não tinham sido gerados antes de instalar o mod. O centro fixo da região é X=0, Z=0.

## Compilar

Com Java 21 e Gradle 8.12:

```bash
gradle build
```

O arquivo compilado aparece em `build/libs/mundo-anel-1.0.0.jar`.

O workflow do GitHub Actions também compila o projeto a cada atualização e publica o JAR como artefato e na página Releases quando a compilação passa.
