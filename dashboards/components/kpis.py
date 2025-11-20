"""
Componente: KPIs
================
Cartões com indicadores-chave de performance.
"""

from dash import html
import dash_bootstrap_components as dbc


def criar_cartao_kpi(id_valor: str, titulo: str, icone: str = None, cor: str = "primary"):
    """
    Cria um cartão de KPI individual.
    
    Args:
        id_valor: ID do elemento que receberá o valor
        titulo: Título do KPI
        icone: Classe do ícone FontAwesome (opcional)
        cor: Cor do cartão (primary, success, danger, etc.)
        
    Returns:
        Componente Dash do cartão KPI
    """
    return dbc.Card([
        dbc.CardBody([
            html.Div([
                html.Div([
                    html.I(className=f"{icone} fa-2x", style={"opacity": 0.3})
                    if icone else None
                ], className="text-end mb-2"),
                
                html.H6(titulo, className="text-muted mb-2"),
                html.H3("—", id=id_valor, className="mb-0 fw-bold")
            ])
        ])
    ], className=f"shadow-sm border-start border-{cor} border-4 h-100")


def criar_linha_kpis():
    """
    Cria a linha completa de KPIs (4 cartões).
    
    Returns:
        Componente Dash com a linha de KPIs
    """
    return dbc.Row([
        dbc.Col(
            criar_cartao_kpi(
                id_valor="kpi-total-a-pagar",
                titulo="Total a Pagar",
                icone="fa-solid fa-money-bill-wave",
                cor="danger"
            ),
            md=3
        ),
        dbc.Col(
            criar_cartao_kpi(
                id_valor="kpi-total-a-receber",
                titulo="Total a Receber",
                icone="fa-solid fa-hand-holding-dollar",
                cor="success"
            ),
            md=3
        ),
        dbc.Col(
            criar_cartao_kpi(
                id_valor="kpi-saldo-projetado",
                titulo="Saldo Projetado",
                icone="fa-solid fa-chart-line",
                cor="info"
            ),
            md=3
        ),
        dbc.Col(
            criar_cartao_kpi(
                id_valor="kpi-titulos-em-atraso",
                titulo="Títulos em Atraso",
                icone="fa-solid fa-exclamation-triangle",
                cor="warning"
            ),
            md=3
        ),
    ], className="g-3 mb-4")