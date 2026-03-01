# 📊 Como Converter Arquivos XLSX para CSV

Os 3 arquivos da API Data Export foram baixados em formato XLSX. Para facilitar a análise e comparação com as respostas da API, precisamos convertê-los para CSV.

---

## 📁 Arquivos para Converter

### Localizados em: `docs/arquivos-csv/`

1. **`relacao-de-manifestos-detalhada_2025_11_03_18_04.xlsx`** (293 linhas)
2. **`relacao-de-cotacoes-detalhada_2025_11_03_17_58.xlsx`** (276 linhas)
3. **`localizador-de-cargas_2025_11_03_17_55.xlsx`** (120 linhas)

---

## ⚡ MÉTODO 1: Usando Excel (MAIS FÁCIL)

### Passo a Passo:

1. **Abrir arquivo XLSX no Excel**
   - Localizar arquivo em `docs/arquivos-csv/`
   - Duplo clique para abrir

2. **Salvar Como CSV**
   - `Arquivo` → `Salvar Como`
   - Escolher local: **MESMO diretório** `docs/arquivos-csv/`
   - `Tipo`: Selecionar **"CSV (delimitado por vírgula) (*.csv)"**
   - Nome sugerido:
     - `manifestos-convertido.csv`
     - `cotacoes-convertido.csv`
     - `localizador-cargas-convertido.csv`
   - Clicar em `Salvar`

3. **Confirmar aviso**
   - Excel pode avisar sobre recursos não suportados
   - Clicar em **"Sim"** para continuar
   - Clicar em **"OK"** para manter formato CSV

4. **Repetir para os 3 arquivos**

**Tempo estimado:** 3 minutos (1 min por arquivo)

---

## 🔧 MÉTODO 2: Usando Google Sheets (ONLINE)

### Passo a Passo:

1. **Acessar:** https://sheets.google.com

2. **Fazer Upload do XLSX**
   - Clicar em **"Em branco"** ou abrir novo
   - `Arquivo` → `Importar`
   - `Upload` → Selecionar arquivo XLSX
   - `Importar dados` → OK

3. **Baixar como CSV**
   - `Arquivo` → `Fazer download` → `Valores separados por vírgula (.csv, planilha atual)`
   - Arquivo será baixado para `Downloads/`

4. **Mover arquivo**
   - Mover de `Downloads/` para `docs/arquivos-csv/`
   - Renomear para:
     - `manifestos-convertido.csv`
     - `cotacoes-convertido.csv`  
     - `localizador-cargas-convertido.csv`

5. **Repetir para os 3 arquivos**

---

## 🐍 MÉTODO 3: Usando Python (AUTOMÁTICO)

Se você tem Python instalado:

### Script de Conversão:

Criar arquivo `converter-xlsx.py` na raiz do projeto:

```python
import pandas as pd
import os

# Diretório dos arquivos
dir_path = "docs/arquivos-csv/"

# Arquivos para converter
files = {
    "relacao-de-manifestos-detalhada_2025_11_03_18_04.xlsx": "manifestos-convertido.csv",
    "relacao-de-cotacoes-detalhada_2025_11_03_17_58.xlsx": "cotacoes-convertido.csv",
    "localizador-de-cargas_2025_11_03_17_55.xlsx": "localizador-cargas-convertido.csv"
}

for xlsx, csv in files.items():
    xlsx_path = os.path.join(dir_path, xlsx)
    csv_path = os.path.join(dir_path, csv)
    
    print(f"Convertendo {xlsx}...")
    df = pd.read_excel(xlsx_path)
    df.to_csv(csv_path, index=False, encoding='utf-8-sig')
    print(f"✅ Criado: {csv}")

print("\n🎉 Conversão completa!")
```

### Executar:

```bash
# Instalar pandas (se necessário)
pip install pandas openpyxl

# Executar script
python converter-xlsx.py
```

---

## ✅ Verificar Conversão

Após converter, verificar que os arquivos foram criados:

```
docs/arquivos-csv/
├── manifestos-convertido.csv          ← NOVO
├── cotacoes-convertido.csv            ← NOVO
├── localizador-cargas-convertido.csv  ← NOVO
├── relacao-de-manifestos-detalhada_2025_11_03_18_04.xlsx  (original)
├── relacao-de-cotacoes-detalhada_2025_11_03_17_58.xlsx    (original)
└── localizador-de-cargas_2025_11_03_17_55.xlsx            (original)
```

---

## 📊 Analisar os CSVs Convertidos

### 1. Abrir no Excel/Editor de Texto

**Verificar:**
- Quantas colunas têm
- Quais são os nomes das colunas (primeira linha)
- Quantas linhas de dados (excluir cabeçalho)

### 2. Listar Colunas

**Criar lista de colunas para cada arquivo:**

**Manifestos** (`manifestos-convertido.csv`):
```
1. Coluna A: ...
2. Coluna B: ...
...
```

**Cotações** (`cotacoes-convertido.csv`):
```
1. Coluna A: ...
2. Coluna B: ...
...
```

**Localizador** (`localizador-cargas-convertido.csv`):
```
1. Coluna A: ...
2. Coluna B: ...
...
```

### 3. Documentar

Salvar as listas de colunas em:
- `docs/referencias-csv/colunas-manifestos.md`
- `docs/referencias-csv/colunas-cotacoes.md`
- `docs/referencias-csv/colunas-localizador.md`

---

## 🔍 Extrair Evidências Específicas

Após converter, identificar valores específicos para buscar nas respostas da API:

### Manifestos:

Abrir `manifestos-convertido.csv` e anotar:
- Números de manifestos (ex: 48037, 48158)
- Nomes de motoristas
- Placas de veículos
- Datas de serviço

### Cotações:

Abrir `cotacoes-convertido.csv` e anotar:
- Números de cotações
- Nomes de clientes
- Valores
- Datas de solicitação

### Localizador:

Abrir `localizador-cargas-convertido.csv` e anotar:
- IDs de fretes
- Chaves de CT-e
- Destinos
- Status

**Adicionar à:** `docs/referencias-csv/evidencias-para-buscar.md`

---

## 🚨 Troubleshooting

### Problema: Caracteres estranhos (�, � ...)

**Causa:** Encoding incorreto

**Solução:**
1. Reabrir CSV no Excel
2. `Salvar Como` → `CSV UTF-8 (delimitado por vírgula) (*.csv)`
3. Ou no script Python: usar `encoding='utf-8-sig'`

### Problema: Colunas separadas incorretamente

**Causa:** Delimitador incorreto (ponto-e-vírgula vs vírgula)

**Solução:**
1. Verificar se arquivo usa `;` ou `,`
2. No Excel: `Dados` → `Texto para Colunas` → Escolher delimitador
3. Ou salvar como CSV (UTF-8) que força vírgula

### Problema: Números formatados como texto

**Causa:** Excel interpreta números longos como texto

**Solução:**
- Não é problema para nossa análise
- APIs retornam JSON com tipos corretos

---

## ✅ Checklist de Conversão

Após converter os 3 arquivos:

- [ ] `manifestos-convertido.csv` criado
- [ ] `cotacoes-convertido.csv` criado
- [ ] `localizador-cargas-convertido.csv` criado
- [ ] Todos abrem corretamente no Excel
- [ ] Colunas listadas e documentadas
- [ ] Evidências específicas identificadas
- [ ] Pronto para comparar com API Data Export

---

## 📌 Próximo Passo

Após conversão:

1. ✅ Marcar to-do "convert-xlsx-to-csv" como completo
2. ➡️ Partir para testes da API Data Export:
   - `docs/insomnia/04-requisicoes-api-dataexport.md`
3. 📊 Comparar campos CSV vs API
4. 📝 Documentar mapeamento

---

**Começar conversão agora:** Use Método 1 (Excel) - 3 minutos! 🚀

