"""
Dashboard ESL Cloud
===================
Sistema de Business Intelligence para visualização de dados do ESL Cloud.

Estrutura modular e fácil manutenção:
- config/: Configurações (banco, filiais)
- data/: Carregamento e processamento de dados
- components/: Componentes visuais reutilizáveis
- pages/: Páginas individuais do dashboard
- callbacks/: Lógica de interatividade
- utils/: Funções utilitárias
"""

import os
from dash import Dash, html, dcc, dash_table
import dash_bootstrap_components as dbc

# Importa componentes
from components.filters import criar_linha_filtros
from components.navbar import criar_navbar
from components.sidebar import criar_sidebar
from components.kpis import criar_linha_kpis

# Importa callbacks
from callbacks.filtros_callback import registrar_callbacks_filtros
from pages.visao_geral import registrar_callback_visao_geral
from callbacks.pages_callback import registrar_callbacks_paginas

# ========== CONFIGURAÇÃO DO APP ==========
app = Dash(
    __name__,
    external_stylesheets=[
        dbc.themes.LUX,  # Tema elegante e profissional
        "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css"
    ],
    suppress_callback_exceptions=True,
    title="Dashboard ESL Cloud",
    update_title="Atualizando..."
)

# ========== LAYOUT PRINCIPAL ==========
app.layout = dbc.Container([
    dcc.Location(id="url", refresh=False),
    
    # Barra superior com logo
    criar_navbar(),
    
    # Container principal com sidebar + conteúdo
    dbc.Row([
        # Sidebar (menu lateral)
        dbc.Col(
            criar_sidebar(),
            width=2,
            className="p-0"
        ),
        
        # Área de conteúdo principal
        dbc.Col([
            # Linha de filtros (datas, filiais, atalhos)
            criar_linha_filtros(),
            
            # Título da página atual
            dbc.Row([
                dbc.Col(
                    html.H4(id="titulo-pagina", children="Visão Geral", className="mb-3"),
                    width=12
                )
            ]),
            
            # Linha de KPIs (cartões com indicadores)
            criar_linha_kpis(),
            
            # Área de gráficos
            dbc.Row([
                dbc.Col(
                    dcc.Graph(id="grafico-pagar-receber"),
                    md=7
                ),
                dbc.Col(
                    dcc.Graph(id="grafico-composicao"),
                    md=5
                )
            ], className="mb-4"),
            
            # Tabela de detalhes
            dbc.Row([
                dbc.Col([
                    html.H5("Detalhes", className="mb-3"),
                    dash_table.DataTable(
                        id="tabela-detalhes",
                        columns=[],
                        data=[],
                        page_size=20,
                        style_table={"overflowX": "auto"},
                        style_cell={"padding": "8px"},
                        style_header={"backgroundColor": "#f8f9fa", "fontWeight": "bold"}
                    )
                ], width=12)
            ])
            
        ], width=10, className="ps-4")
    ])
    
], fluid=True, className="p-3")

# ========== REGISTRAR CALLBACKS ==========
# Callbacks de filtros (datas, filiais)
registrar_callbacks_filtros(app)

# Callbacks de páginas
registrar_callback_visao_geral(app)
registrar_callbacks_paginas(app)

# TODO: Adicionar callbacks das outras páginas aqui
# from pages.contas_pagar import registrar_callback_contas_pagar
# registrar_callback_contas_pagar(app)


# ========== SERVIDOR ==========
server = app.server


if __name__ == "__main__":
    # Configurações do servidor
    host = os.environ.get("DASH_HOST", "127.0.0.1")
    port = int(os.environ.get("DASH_PORT", "8050"))
    debug = os.environ.get("DASH_DEBUG", "false").lower() == "true"
    
    print("=" * 60)
    print("🚀 Dashboard ESL Cloud")
    print("=" * 60)
    print(f"📍 Servidor: http://{host}:{port}")
    print(f"🔧 Debug: {debug}")
    print("=" * 60)
    print()
    print("💡 Estrutura modular:")
    print("   - config/filiais.py: Configure filiais aqui")
    print("   - data/loader.py: Carregamento de dados")
    print("   - pages/: Adicione novas páginas aqui")
    print("=" * 60)
    
    app.run(host=host, port=port, debug=debug)