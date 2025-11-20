# Dashboard ESL Cloud - Business Intelligence

Sistema de visualização de dados do ESL Cloud com foco em **facilidade de manutenção** e **clareza visual**.

---

## 🎯 Características Principais

✅ **Estrutura modular** - Cada componente em seu próprio arquivo  
✅ **Filtro de filiais inteligente** - Vazio = todas, selecionadas = apenas essas  
✅ **Sem dados fictícios** - Apenas dados reais do SQL Server  
✅ **Fácil manutenção** - Adicione páginas sem tocar no código existente  
✅ **Fonte única de verdade** - Filiais configuradas em um único arquivo  

---

## 📁 Estrutura de Arquivos

```
dashboard-esl/
├── app.py                          # ⭐ Aplicação principal
├── .env                            # Configurações de banco (não versionado)
├── requirements.txt                # Dependências Python
│
├── config/                         # 🔧 Configurações
│   ├── database.py                 # Conexão com SQL Server
│   └── filiais.py                  # ⭐ FILIAIS (fonte única)
│
├── data/                           # 📊 Dados
│   ├── loader.py                   # Carregamento do SQL Server
│   └── processors.py               # Filtros e agregações
│
├── components/                     # 🎨 Componentes visuais
│   ├── navbar.py                   # Barra superior
│   ├── sidebar.py                  # Menu lateral
│   ├── filters.py                  # Filtros (datas + filiais)
│   └── kpis.py                     # Cartões de KPI
│
├── pages/                          # 📄 Páginas do dashboard
│   ├── visao_geral.py              # Visão geral (exemplo)
│   ├── contas_pagar.py             # (TODO)
│   └── ... (adicione mais aqui)
│
├── callbacks/                      # 🔄 Interatividade
│   ├── filtros_callback.py         # Callbacks de filtros
│   └── pages_callback.py           # (TODO)
│
├── utils/                          # 🛠️ Utilitários
│   ├── formatters.py               # Formatação de valores
│   └── charts.py                   # (TODO)
│
└── assets/                         # 🎨 Recursos estáticos
    ├── style.css                   # Estilos customizados
    └── logomarca-rodogarcia.png    # Logo
```

---

## 🚀 Como Usar

### 1. **Instalar Dependências**

```bash
pip install -r requirements.txt
```

### 2. **Configurar Banco de Dados**

Crie um arquivo `.env` na raiz do projeto:

```env
# Conexão com SQL Server
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=esl_cloud;encrypt=true
DB_USER=sa
DB_PASSWORD=sua_senha_aqui
ODBC_DRIVER=ODBC Driver 17 for SQL Server

# Configurações do servidor Dash (opcional)
DASH_HOST=127.0.0.1
DASH_PORT=8050
DASH_DEBUG=false
```

### 3. **Executar o Dashboard**

```bash
python app.py
```

Acesse: **http://localhost:8050**

---

## 🏢 Como Adicionar/Remover Filiais

### **⭐ IMPORTANTE: Edite apenas o arquivo `config/filiais.py`**

```python
# config/filiais.py

FILIAIS_ATIVAS = [
    "AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
    "CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
    "CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
    # ... adicione ou remova aqui
]
```

**Pronto!** As mudanças serão refletidas automaticamente em todo o sistema.

---

## 📄 Como Adicionar uma Nova Página

### Passo 1: Criar o arquivo da página

```python
# pages/minha_nova_pagina.py

from dash import Input, Output
from data.loader import carregar_tabela
from data.processors import aplicar_filtros

def registrar_callback_minha_pagina(app):
    @app.callback(
        Output("grafico-principal", "figure"),
        Input("intervalo-datas", "start_date"),
        Input("intervalo-datas", "end_date"),
        Input("seletor-filial", "value")
    )
    def atualizar_pagina(data_inicial, data_final, filiais_selecionadas):
        # Carregar dados
        df = carregar_tabela("minha_tabela")
        
        # Aplicar filtros
        df_filtrado = aplicar_filtros(
            df,
            "minha_tabela",
            data_inicial,
            data_final,
            filiais_selecionadas  # [] = todas, ["Filial A"] = apenas A
        )
        
        # Criar gráfico
        # ... seu código aqui
        
        return figura
```

### Passo 2: Adicionar no `app.py`

```python
# app.py

from pages.minha_nova_pagina import registrar_callback_minha_pagina

# ... depois de registrar_callback_visao_geral(app)
registrar_callback_minha_pagina(app)
```

### Passo 3: Adicionar link no sidebar

```python
# components/sidebar.py

dbc.NavLink([
    html.I(className="fa-solid fa-meu-icone me-2"),
    "Minha Nova Página"
], href="/minha-pagina", active="exact", id="nav-minha-pagina"),
```

**Pronto!** Sua nova página está integrada.

---

## 🔍 Lógica de Filtros

### **Filtro de Filiais**

```python
filiais_selecionadas = []          # ✅ Mostra TODAS as filiais
filiais_selecionadas = None        # ✅ Mostra TODAS as filiais
filiais_selecionadas = ["AGU", "CPQ"]  # ✅ Mostra APENAS AGU e CPQ
```

### **Filtro de Datas**

```python
data_inicial = "2024-01-01"   # Filtra >= 01/01/2024
data_final = "2024-12-31"     # Filtra <= 31/12/2024
```

---

## 🎨 Customização Visual

### **Cores dos Cartões KPI**

```python
# components/kpis.py

criar_cartao_kpi(
    id_valor="meu-kpi",
    titulo="Meu Indicador",
    icone="fa-solid fa-chart-line",
    cor="success"  # primary, secondary, success, danger, warning, info
)
```

### **Estilos Customizados**

Edite `assets/style.css` para customizar cores, fontes, espaçamentos, etc.

---

## 📊 Tabelas Disponíveis

| Tabela | Descrição | Filtro de Filial |
|--------|-----------|------------------|
| `contas_a_pagar` | Contas a pagar | ✅ Sim (`nome_filial`) |
| `faturas_por_cliente_data_export` | Faturas a receber | ✅ Sim (`filial`) |
| `coletas` | Coletas realizadas | ❌ Não |
| `fretes` | Fretes transportados | ❌ Não |
| `cotacoes` | Cotações de frete | ✅ Sim (`branch_nickname`) |
| `manifestos` | Manifestos de carga | ✅ Sim (`branch_nickname`) |
| `localizacao_cargas` | GPS das cargas | ❌ Não |
| `ocorrencias` | Ocorrências no transporte | ❌ Não |

---

## 🐛 Troubleshooting

### **Problema: "Não foi possível conectar ao banco"**

✅ Verifique se o SQL Server está rodando  
✅ Verifique as credenciais no `.env`  
✅ Teste a conexão com um cliente SQL (Azure Data Studio, SSMS)

### **Problema: "Nenhuma filial encontrada"**

✅ Verifique se as tabelas têm dados  
✅ Execute no SQL Server:
```sql
SELECT DISTINCT nome_filial FROM contas_a_pagar;
SELECT DISTINCT filial FROM faturas_por_cliente_data_export;
```

### **Problema: "Filtro de filiais não funciona"**

✅ Verifique se retornou `value=[]` no callback de filiais  
✅ Verifique se está usando `aplicar_filtros()` corretamente  
✅ Debug: imprima `filiais_selecionadas` no callback

---

## 📦 Dependências

```txt
dash>=2.14
dash-bootstrap-components>=1.6.0
plotly>=5.23
pandas>=2.2
SQLAlchemy>=2.0
pyodbc>=5.2
numpy>=2.0
```

---

## 🔒 Segurança

⚠️ **NUNCA comite o arquivo `.env` no Git!**

Adicione ao `.gitignore`:
```
.env
*.pyc
__pycache__/
.DS_Store
```

---

## 📝 Changelog

### v1.0.0 (2025-11-19)
- ✅ Estrutura modular completa
- ✅ Filtro de filiais inteligente (vazio = todas)
- ✅ Fonte única de verdade para filiais
- ✅ Apenas dados reais (sem mocks)
- ✅ Página de visão geral funcional
- ✅ Documentação completa

---

## 👨‍💻 Desenvolvedor

**Lucas Mateus**  
Sistema ETL Java + Dashboard Python para ESL Cloud

---

## 📄 Licença

Uso interno - RODOGARCIA TRANSPORTES