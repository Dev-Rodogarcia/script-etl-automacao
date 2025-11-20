from dash import html
import dash_bootstrap_components as dbc


def criar_sidebar():
    return html.Div([
        html.Div(className="titulo-barra-lateral"),
        dbc.Nav([
            dbc.NavLink([html.I(className="fa-solid fa-gauge me-2"), "Visão Geral"], href="/", active="exact", id="nav-visao-geral"),
            dbc.NavLink([html.I(className="fa-solid fa-truck me-2"), "Coletas"], href="/coletas", active="exact", id="nav-coletas"),
            dbc.NavLink([html.I(className="fa-solid fa-route me-2"), "Fretes"], href="/fretes", active="exact", id="nav-fretes"),
            dbc.NavLink([html.I(className="fa-solid fa-tags me-2"), "Cotações"], href="/cotacoes", active="exact", id="nav-cotacoes"),
            dbc.NavLink([html.I(className="fa-solid fa-money-bill-wave me-2"), "Contas a Pagar"], href="/apagar", active="exact", id="nav-contas-a-pagar"),
            dbc.NavLink([html.I(className="fa-solid fa-location-dot me-2"), "Localização de Cargas"], href="/localizacao", active="exact", id="nav-localizacao"),
            dbc.NavLink([html.I(className="fa-solid fa-file-signature me-2"), "Manifestos"], href="/manifestos", active="exact", id="nav-manifestos"),
            dbc.NavLink([html.I(className="fa-solid fa-file-invoice me-2"), "Faturas por Cliente"], href="/faturas", active="exact", id="nav-faturas"),
            dbc.NavLink([html.I(className="fa-solid fa-triangle-exclamation me-2"), "Ocorrências"], href="/ocorrencias", active="exact", id="nav-ocorrencias"),
        ], vertical=True, pills=True)
    ], className="barra-lateral p-3")