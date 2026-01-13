# Dashboards (Dash + Plotly)

Este diretório contém a aplicação de dashboards construída com Dash, Plotly e Bootstrap.

## Pré-requisitos

- Python 3.10+ instalado
- Pip atualizado (`python -m pip install --upgrade pip`)

## Instalação rápida (Windows)

1. (Opcional) Crie um ambiente virtual:
   
   ```powershell
   python -m venv .venv
   .venv\Scripts\Activate.ps1
   ```

2. Instale as dependências mínimas:
   
   ```powershell
   pip install dash dash-bootstrap-components plotly pandas
   ```

## Como iniciar

Execute o servidor local e abra no navegador:

```powershell
python src/dashboards/aplicacao.py
```

- A aplicação sobe em `http://127.0.0.1:8050/`
- Para parar, use `Ctrl+C` no terminal

## Estrutura principal

- `src/dashboards/aplicacao.py` — callbacks, carregamento de dados e gráficos
- `src/dashboards/layout.py` — layout da página, filtros e componentes
- `src/dashboards/assets/` — estilos CSS (carregados automaticamente pelo Dash)
  - `base.css`, `navbar.css`, `buttons.css`, `filtros.css`, `cards.css`, `graficos.css`, `tabela.css`, `layout.css`

## Personalizações comuns

- Cores e espaçamento: edite os arquivos em `assets/`
- Logo: coloque `logo.png` em `src/dashboards/` (carregada automaticamente se existir)
- Porta do servidor: altere em `aplicacao.py` se desejar (`aplicacao.run_server(port=8051)`).

## Problemas comuns

- Se aparecer erro de pacote não encontrado, reinstale as dependências:
  
  ```powershell
  pip install dash dash-bootstrap-components plotly pandas
  ```

- Caso a UI do DevTools do Dash apareça, ela pode ser desativada no `aplicacao.py` com `dev_tools_ui=False` em `run_server`.

---

Qualquer ajuste visual deve ser feito preferencialmente via CSS em `assets/`, usando apenas `className` no Python.
