import dash
from dash import dcc, html, dash_table
import os, base64
import dash_bootstrap_components as dbc

def _obter_logo_src():
    caminho = os.path.join(os.path.dirname(__file__), "logo.png")
    try:
        with open(caminho, "rb") as f:
            conteudo = f.read()
        return "data:image/png;base64," + base64.b64encode(conteudo).decode("utf-8")
    except Exception:
        return None

def criar_cabecalho():
    img = _obter_logo_src()
    return dbc.Navbar(
        dbc.Container([
            dbc.NavbarBrand(html.Img(src=img, className="logo-rodo") if img else html.Span("RODOGARCIA", className="marca-rodo")),
            dbc.Button("Atualizar", id="botao-atualizar", className="botao-personalizado", size="sm"),
        ]), color="light", light=True, className="navbar-rodo"
    )

def criar_controles():
    return dbc.Card(
        dbc.CardBody([
            dbc.Row([
                dbc.Col([
                    html.Label("Entidade", className="rotulo"),
                    dbc.Select(
                        id="seletor-entidade",
                        options=[
                            {"label": "Contas a Pagar", "value": "contas"},
                            {"label": "Faturas por Cliente", "value": "faturas"},
                            {"label": "Coletas", "value": "coletas"},
                            {"label": "Fretes", "value": "fretes"},
                            {"label": "Cotações", "value": "cotacoes"},
                            {"label": "Localização de Carga", "value": "localizacao"},
                            {"label": "Manifestos (view)", "value": "manifestos_view"},
                            {"label": "Manifestos (tabela)", "value": "manifestos"},
                        ],
                        value="contas"
                    )
                ], md=4, className="filtro-entidade"),
                dbc.Col([
                    html.Label("Período", className="rotulo"),
                    dcc.DatePickerRange(id="filtro-periodo", display_format="DD/MM/YYYY", className="controle-periodo")
                ], md=4, className="filtro-periodo"),
                dbc.Col([
                    html.Label("Filiais", className="rotulo"),
                    dcc.Dropdown(id="seletor-filiais", multi=True, placeholder="Todas as filiais (padrão)")
                ], md=3, className="filtro-filiais"),
                dbc.Col(dbc.Button("Carregar", id="botao-carregar", color="primary", size="sm", className="btn-carregar-inline"), md="auto", className="filtro-carregar"),
                dbc.Col(dbc.Button("Mais filtros", id="toggle-mais-filtros", color="secondary", size="sm", className="btn-mais-filtros"), md="auto", className="filtro-mais-filtros"),
            ], className="linha-controles g-2 align-items-end"),
            dbc.Collapse([
                html.Div([
                    html.Div([
                        html.Label("Status", className="rotulo"),
                        dcc.Dropdown(id="seletor-status", multi=True, placeholder="Todos os status")
                    ]),
                    html.Div([
                        html.Label("Pessoa (pagador/fornecedor/cliente)", className="rotulo"),
                        dcc.Dropdown(id="seletor-pessoa", multi=True, placeholder="Todas as pessoas")
                    ]),
                    html.Div([
                        html.Label("UF/Estado", className="rotulo"),
                        dcc.Dropdown(id="seletor-uf", multi=True, placeholder="Todos")
                    ]),
                    html.Div([
                        html.Label("Cidade", className="rotulo"),
                        dcc.Dropdown(id="seletor-cidade", multi=True, placeholder="Todas as cidades")
                    ]),
                    html.Div([
                        html.Label("Origem", className="rotulo"),
                        dcc.Dropdown(id="seletor-origem", multi=True, placeholder="Todas as origens")
                    ]),
                    html.Div([
                        html.Label("Destino", className="rotulo"),
                        dcc.Dropdown(id="seletor-destino", multi=True, placeholder="Todos os destinos")
                    ]),
                    html.Div([
                        html.Label("Modal", className="rotulo"),
                        dcc.Dropdown(id="seletor-modal", multi=True, placeholder="Todos os modais")
                    ]),
                    html.Div([
                        html.Label("Valor mín.", className="rotulo"),
                        dbc.Input(id="filtro-valor-min", type="number", step="0.01", size="sm", placeholder="R$ mín.")
                    ]),
                    html.Div([
                        html.Label("Valor máx.", className="rotulo"),
                        dbc.Input(id="filtro-valor-max", type="number", step="0.01", size="sm", placeholder="R$ máx.")
                    ]),
                    html.Div([
                        html.Label("Peso mín.", className="rotulo"),
                        dbc.Input(id="filtro-peso-min", type="number", step="0.01", size="sm", placeholder="kg mín.")
                    ]),
                    html.Div([
                        html.Label("Peso máx.", className="rotulo"),
                        dbc.Input(id="filtro-peso-max", type="number", step="0.01", size="sm", placeholder="kg máx.")
                    ]),
                    html.Div([
                        html.Label("Buscar (texto)", className="rotulo"),
                        dbc.Input(id="filtro-busca", type="text", size="sm", placeholder="Digite para buscar")
                    ]),
                ], className="filtros-grid")
            ], id="container-mais-filtros", is_open=False)
        ]), className="mb-3 bloco-controles"
    )

def criar_layout():
    return dbc.Container([
        criar_cabecalho(),
        html.Br(),
        dbc.Row([
            dbc.Col(dbc.Button("Mostrar/Esconder filtros", id="toggle-filtros", color="secondary", className="btn-filtros"), md=3)
        ], className="mb-2"),
        dbc.Collapse(id="container-filtros", is_open=True, children=criar_controles()),
        html.Div([
            dbc.Card(dbc.CardBody([
                html.H6("Registros", className="texto-kpi"),
                html.H3(id="kpi-total", className="valor-kpi"),
            ]), className="card-kpi card-registros"),
            dbc.Card(dbc.CardBody([
                html.H6("Soma (valor)", className="texto-kpi"),
                html.H3(id="kpi-soma", className="valor-kpi"),
            ]), className="card-kpi card-soma"),
            dbc.Card(dbc.CardBody([
                html.H6("Média (valor)", className="texto-kpi"),
                html.H3(id="kpi-media", className="valor-kpi"),
            ]), className="card-kpi card-media"),
            dbc.Card(dbc.CardBody([
                html.H6("Máximo (valor)", className="texto-kpi"),
                html.H3(id="kpi-max", className="valor-kpi"),
            ]), className="card-kpi card-max"),
            dbc.Card(dbc.CardBody([
                html.H6("Mínimo (valor)", className="texto-kpi"),
                html.H3(id="kpi-min", className="valor-kpi"),
            ]), className="card-kpi card-min"),
            dbc.Card(dbc.CardBody([
                html.H6("Mediana (valor)", className="texto-kpi"),
                html.H3(id="kpi-mediana", className="valor-kpi"),
            ]), className="card-kpi card-mediana"),
            dbc.Card(dbc.CardBody([
                html.H6("Desvio (valor)", className="texto-kpi"),
                html.H3(id="kpi-desvio", className="valor-kpi"),
            ]), className="card-kpi card-desvio"),
            dbc.Card(dbc.CardBody([
                html.H6("Última data", className="texto-kpi"),
                html.H3(id="kpi-ultima-data", className="valor-kpi"),
            ]), className="card-kpi card-data"),
            dbc.Card(dbc.CardBody([
                html.H6("Top filial", className="texto-kpi"),
                html.H3(id="kpi-top-filial", className="valor-kpi"),
            ]), className="card-kpi card-top"),
            dbc.Card(dbc.CardBody([
                html.H6("Menor filial", className="texto-kpi"),
                html.H3(id="kpi-menor-filial", className="valor-kpi"),
            ]), className="card-kpi card-menor"),
        ], className="kpi-grid mb-3"),
        dbc.Row([
            dbc.Col(dcc.Graph(id="grafico", className="bloco-grafico"), md=6),
            dbc.Col(dcc.Graph(id="grafico-secundario", className="bloco-grafico"), md=6),
        ], className="mb-3"),
        dbc.Row([
            dbc.Col(dcc.Graph(id="grafico-terciario", className="bloco-grafico"), md=12),
        ], className="mb-4"),
        dbc.Card(dbc.CardBody([
            html.Div(
                dash_table.DataTable(
                    id="tabela",
                    page_action="none",
                    sort_action="native",
                    filter_action="native",
                ),
                className="tabela-rodo"
            )
        ]), className="bloco-tabela")
    ], fluid=True, className="container-rodo")
