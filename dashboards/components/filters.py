"""
Componentes de Filtros
=======================
Filtros de data e filiais para o dashboard.
"""

from dash import dcc, html
import dash_bootstrap_components as dbc
import datetime as dt


def criar_filtro_datas():
    """
    Cria o seletor de intervalo de datas.
    
    Returns:
        Componente Dash do filtro de datas
    """
    hoje = dt.date.today()
    trinta_dias_atras = hoje - dt.timedelta(days=30)
    
    return dbc.Card([
        dbc.CardBody([
            html.Label("📅 Período:", className="fw-bold mb-2"),
            dcc.DatePickerRange(
                id="intervalo-datas",
                start_date=trinta_dias_atras,
                end_date=hoje,
                display_format="DD/MM/YYYY",
                first_day_of_week=1,  # Segunda-feira
                className="w-100"
            )
        ])
    ], className="mb-3")


def criar_filtro_filiais():
    """
    Cria o seletor de filiais com checklist.
    
    Returns:
        Componente Dash do filtro de filiais
    """
    titulo = html.Div([
        html.Span([html.I(className="fa-solid fa-building me-2"), "Filiais:"], className="fw-bold"),
        html.Small(" (vazio = todas)", className="text-muted ms-2"),
        html.Small("", id="contador-filiais", className="text-muted ms-2")
    ], className="d-flex align-items-center flex-wrap")

    conteudo = html.Div([
        dcc.Input(id="busca-filial", type="text", placeholder="Pesquisar filiais...", debounce=True, className="form-control form-control-sm mb-2"),
        html.Div([
            dcc.Checklist(
                id="seletor-filial",
                options=[],
                value=[],
                className="checklist-filiais",
                labelClassName="filial-item",
                inputClassName="me-2"
            ),
        ], style={"maxHeight": "240px", "overflowY": "auto"}),
        html.Div([
            dbc.Button("Selecionar Todas", id="btn-selecionar-todas", size="sm", color="secondary", outline=True, className="me-2"),
            dbc.Button("Limpar", id="btn-limpar-filiais", size="sm", color="secondary", outline=True),
        ], className="mt-2")
    ])

    return dbc.Accordion([
        dbc.AccordionItem(conteudo, title=titulo, item_id="acc-filiais")
    ], start_collapsed=True, flush=True, className="mb-3")


def criar_atalhos_datas():
    """
    Cria botões de atalho para períodos comuns.
    
    Returns:
        Componente Dash com botões de atalho
    """
    return dbc.Card([
        dbc.CardBody([
            html.Label("⚡ Atalhos:", className="fw-bold mb-2"),
            dbc.ButtonGroup([
                dbc.Button("Hoje", id="btn-hoje", size="sm", color="secondary", outline=True),
                dbc.Button("7 dias", id="btn-7dias", size="sm", color="secondary", outline=True),
                dbc.Button("30 dias", id="btn-30dias", size="sm", color="secondary", outline=True),
                dbc.Button("Este mês", id="btn-mes-atual", size="sm", color="secondary", outline=True),
            ], className="w-100")
        ])
    ], className="mb-3")


def criar_linha_filtros():
    """
    Cria a linha completa de filtros (datas + filiais + atalhos).
    
    Returns:
        Componente Dash com todos os filtros
    """
    return dbc.Row([
        dbc.Col(criar_filtro_datas(), md=4),
        dbc.Col(criar_filtro_filiais(), md=4),
        dbc.Col(criar_atalhos_datas(), md=4),
    ], className="mb-4")