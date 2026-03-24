---
context:
  - Documentacao
  - Legado
  - ArquivoHistorico
updated_at: 2026-03-18T00:00:32.1501533-03:00
source_of_truth: docs
classification: parcial
related_files:
  - docs/index.md
  - docs/legado/index.md
  - docs/legado/classificacao.md
---
# Security Rotation Required

Data: 2026-02-27

## Acao obrigatoria
Houve exposicao historica da credencial `DB_PASSWORD=SqlDocker!2025` em artefato versionado.

## Medidas registradas no codigo
- `database/config.bat` removido da trilha de versionamento e ignorado no `.gitignore`.
- `database/config_exemplo.bat` mantido como modelo sem segredo.
- Fallback por variavel de ambiente/configuracao sem hardcode de senha.

## Acao operacional (fora do codigo)
- Rotacionar imediatamente a senha no SQL Server.
- Invalidar credencial antiga em todos os ambientes.
- Atualizar credenciais apenas em segredo local/gerenciador de segredos.
