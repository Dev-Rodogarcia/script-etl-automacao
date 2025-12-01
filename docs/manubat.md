# Manual de uso dos scripts `.bat` nesta máquina (Windows)

Este guia padroniza como executar o projeto via scripts `.bat` nesta máquina. Resolve o erro "No goals have been specified" do Maven e indica os comandos corretos.

## Requisitos
- Java 17 disponível no PATH (`java -version`).
- Maven disponível no PATH (`mvn -version`) ou usar `mvn.bat` do projeto.
- Banco configurado via variáveis de ambiente ou `config.properties`.

## Code page do terminal
- Esta máquina está com code page ativa `65001` (UTF-8).
- Os scripts principais incluem `chcp 1252` para evitar problemas de acentuação e parsing em alguns consoles. Caso veja caracteres estranhos, troque para `65001` antes de executar:

```
chcp 65001 >nul
```

## Maven: como evitar "No goals have been specified"
- Se você executar `mvn.bat` sem argumentos, o wrapper desta máquina já dispara automaticamente `clean package -DskipTests`.
- Para usos avançados, chame o Maven com uma fase/goal específico:

Exemplos:
```
mvn.bat clean package -DskipTests
mvn.bat validate
mvn.bat dependency:go-offline
```

Referência de fases do Maven: `clean`, `validate`, `compile`, `test`, `package`, `install`, `deploy`.

## Scripts disponíveis

- `05-compilar_projeto.bat`
  - Compila e gera `target\extrator.jar`.
  - Usa o wrapper portátil `mvn.bat` quando presente.
  - Comando interno equivalente: `mvn clean package -DskipTests`.

- `01-executar_extracao_completa.bat`
  - Executa `java -jar target\extrator.jar --fluxo-completo`.
  - Se `java -jar` falhar (manifest), faz fallback para `java -cp target\extrator.jar br.com.extrator.Main --fluxo-completo`.

- `02-testar_api_especifica.bat`
  - Testa uma API específica com fallback semelhante ao acima.
  - Exemplos:
    - `02-testar_api_especifica.bat dataexport cotacoes`
    - `02-testar_api_especifica.bat graphql fretes`

- `mvn.bat` (wrapper Maven portátil)
  - Detecta `JAVA_HOME` a partir do `java` atual.
  - Usa `mvn` do PATH; se não encontrado, tenta `MAVEN_HOME\bin\mvn.cmd`.
  - Use sempre com objetivo/fase, por exemplo:

```
mvn.bat clean package -DskipTests
mvn.bat validate
```

## Configuração de banco

Opção 1 (recomendado): variáveis de ambiente de usuário
```
[System.Environment]::SetEnvironmentVariable("DB_URL","jdbc:sqlserver://SEU_HOST:1433;databaseName=SeuBanco","User")
[System.Environment]::SetEnvironmentVariable("DB_USER","seu_usuario","User")
[System.Environment]::SetEnvironmentVariable("DB_PASSWORD","sua_senha","User")
```

Opção 2 (dev local): `src/main/resources/config.properties`
```
db.url=jdbc:sqlserver://SEU_HOST:1433;databaseName=SeuBanco
db.user=seu_usuario
db.password=sua_senha
```

## Fluxo recomendado
- Preparar dependências: `mvn.bat dependency:go-offline`
- Compilar rápido: `mvn.bat` (sem argumentos) ou `05-compilar_projeto.bat`
- Validar execução: `java -jar target\extrator.jar --ajuda`
- Testar API: `02-testar_api_especifica.bat dataexport cotacoes`
- Extrair tudo: `01-executar_extracao_completa.bat`

## Problemas comuns
- "No goals have been specified": execute `mvn clean package -DskipTests` ou `mvn validate`.
- Falha de conexão com banco: confira `DB_URL`, `DB_USER`, `DB_PASSWORD` ou preencha `db.*` no `config.properties`.
- Acentuação incorreta no console: ajuste `chcp 65001 >nul` antes de rodar.

## Comandos comuns nesta máquina

```
:: Compilar
.\u006D
.\u006D
mvn.bat

:: Testar API DataExport de cotações
.-testar_api_especifica.bat dataexport cotacoes

:: Executar extração completa
.-executar_extracao_completa.bat
```
