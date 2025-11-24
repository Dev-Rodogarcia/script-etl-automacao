import dash
from dash import dcc, html, Input, Output, State
import dash_bootstrap_components as dbc
import plotly.express as px
import plotly.graph_objects as go
try:
    from .banco import consultar_tabela
    from .consultas import (
        CONTAS_PAGAR_TOP, FATURAS_CLIENTE_TOP, MANIFESTOS_RESUMO,
        COLETAS_TOP, FRETES_TOP, COTACOES_TOP, LOCALIZACAO_TOP, MANIFESTOS_VIEW_TOP,
        FATURAS_CLIENTE_BASE, FRETES_BASE, LOCALIZACAO_BASE
    )
    from .layout import criar_layout
except ImportError:
    from banco import consultar_tabela
    from consultas import (
        CONTAS_PAGAR_TOP, FATURAS_CLIENTE_TOP, MANIFESTOS_RESUMO,
        COLETAS_TOP, FRETES_TOP, COTACOES_TOP, LOCALIZACAO_TOP, MANIFESTOS_VIEW_TOP,
        FATURAS_CLIENTE_BASE, FRETES_BASE, LOCALIZACAO_BASE
    )
    from layout import criar_layout

aplicacao = dash.Dash(__name__, external_stylesheets=[dbc.themes.BOOTSTRAP])
aplicacao.title = "RODOGARCIA"
aplicacao.layout = criar_layout()

@aplicacao.callback(
    dash.Output("container-filtros", "is_open"),
    dash.Input("toggle-filtros", "n_clicks"),
    dash.State("container-filtros", "is_open"),
    prevent_initial_call=True
)
def alternar_filtros(n, aberto):
    return not aberto

@aplicacao.callback(
    dash.Output("container-mais-filtros", "is_open"),
    dash.Input("toggle-mais-filtros", "n_clicks"),
    dash.State("container-mais-filtros", "is_open"),
    prevent_initial_call=True
)
def alternar_mais_filtros(n, aberto):
    return not aberto

@aplicacao.callback(
    Output("tabela", "data"), Output("tabela", "columns"),
    Output("grafico", "figure"), Output("grafico-secundario", "figure"), Output("grafico-terciario", "figure"),
    Output("kpi-total", "children"), Output("kpi-soma", "children"), Output("kpi-media", "children"),
    Output("kpi-max", "children"), Output("kpi-min", "children"), Output("kpi-mediana", "children"), Output("kpi-desvio", "children"),
    Output("kpi-ultima-data", "children"), Output("kpi-top-filial", "children"), Output("kpi-menor-filial", "children"),
    Output("seletor-filiais", "options"), Output("seletor-status", "options"), Output("seletor-pessoa", "options"), Output("seletor-uf", "options"),
    Output("seletor-cidade", "options"), Output("seletor-origem", "options"), Output("seletor-destino", "options"), Output("seletor-modal", "options"),
    Input("seletor-entidade", "value"),
    Input("filtro-periodo", "start_date"), Input("filtro-periodo", "end_date"),
    Input("seletor-filiais", "value"), Input("seletor-status", "value"), Input("seletor-pessoa", "value"), Input("seletor-uf", "value"),
    Input("seletor-cidade", "value"), Input("seletor-origem", "value"), Input("seletor-destino", "value"), Input("seletor-modal", "value"),
    Input("filtro-valor-min", "value"), Input("filtro-valor-max", "value"), Input("filtro-peso-min", "value"), Input("filtro-peso-max", "value"), Input("filtro-busca", "value"),
    Input("botao-carregar", "n_clicks"), Input("botao-atualizar", "n_clicks")
)
def carregar_dados(entidade, inicio, fim, filiais, status_sel, pessoa_sel, uf_sel, cidade_sel, origem_sel, destino_sel, modal_sel, vmin, vmax, pmin, pmax, busca, _, __):
    limite = 12000
    try:
        dados, figura = _carregar_inicial(entidade, limite)
        dados = _aplicar_filtros(
            dados, entidade, inicio, fim, filiais,
            status_sel, pessoa_sel, uf_sel,
            cidade_sel, origem_sel, destino_sel, modal_sel,
            vmin, vmax, pmin, pmax, busca
        )
        dados = _sanitizar_df(dados)
        colunas = [{"name": c, "id": c} for c in list(dados.columns)]
        total = len(dados)
        soma = _soma_valor(dados)
        media = (soma / total) if total > 0 and soma is not None else None
        col_filial = _col(dados, ["filial","Filial","branch_nickname","filial_nome","filial_atual","status_branch_nickname","destination_branch_nickname"]) 
        opcoes_filiais = []
        if col_filial in dados.columns:
            unicas = sorted([str(v) for v in dados[col_filial].dropna().unique()])
            opcoes_filiais = [{"label": _sigla_filial(v), "value": v} for v in unicas]
        opcoes_status = _opcoes_unicas(dados, ["status","Status","pago","Pago"]) 
        opcoes_pessoa = _opcoes_unicas(dados, ["pagador_nome","Pagador / Nome","fornecedor","Fornecedor/Nome","cliente_nome"]) 
        opcoes_uf = _opcoes_unicas(dados, ["estado","uf","UF","Estado"]) 
        opcoes_cidade = _opcoes_unicas(dados, ["cidade","Cidade","city","City"]) 
        opcoes_origem = _opcoes_unicas(dados, ["origem","cidade_origem","origin_city","Origin","Cidade Origem"]) 
        opcoes_destino = _opcoes_unicas(dados, ["destino","cidade_destino","destination_city","Destination","Cidade Destino"]) 
        opcoes_modal = _opcoes_unicas(dados, ["modal","Modal","modality","service_mode"]) 
        if entidade == "manifestos":
            figura = _estilizar_fig(_scatter_manifestos(dados), "Quadrante do lucro")
            fig_filial = _estilizar_fig(_barras_manifestos_filial(dados), "Receita vs Custo por filial")
            fig_tempo = _estilizar_fig(_margem_manifestos_tempo(dados), "Margem diária")
        elif entidade == "fretes":
            try:
                figura = _estilizar_fig(_sankey_fluxo(dados), "Fluxo UF Origem → UF Destino")
            except Exception:
                figura = _estilizar_fig(_grafico_generico(dados), "Fluxo UF Origem → UF Destino")
            try:
                fig_filial = _estilizar_fig(_grafico_por_filial(dados), "Top rotas")
            except Exception:
                fig_filial = _estilizar_fig(_grafico_generico(dados), "Top rotas")
            try:
                fig_tempo = _estilizar_fig(_grafico_tempo(dados, entidade), "Evolução diária")
            except Exception:
                fig_tempo = _estilizar_fig(_grafico_generico(dados), "Evolução diária")
        elif entidade == "cotacoes":
            figura = _estilizar_fig(_funnel_cotacoes(dados), "Funil de Vendas")
            fig_filial = _estilizar_fig(_donut_motivos_perda(dados), "Por que perdemos?")
            fig_tempo = _estilizar_fig(_grafico_tempo(dados, entidade), "Evolução diária")
        elif entidade == "coletas":
            figura = _estilizar_fig(_heatmap_coletas(dados), "Coletas por hora x dia")
            fig_filial = _estilizar_fig(_grafico_por_filial(dados), "Resumo por categoria")
            fig_tempo = _estilizar_fig(_grafico_tempo(dados, entidade), "Evolução diária")
        elif entidade == "contas":
            figura = _estilizar_fig(_grafico_contas_linha(dados), "Contas a Pagar")
            fig_filial = _estilizar_fig(_treemap_generico(dados, ["centro_custo","Centro de Custo","fornecedor","Fornecedor/Nome"], ["valor_a_pagar","Valor a pagar"]), "Quem leva o dinheiro?")
            fig_tempo = _estilizar_fig(_grafico_tempo(dados, entidade), "Evolução diária")
        elif entidade == "faturas":
            figura = _estilizar_fig(_grafico_faturas_linha(dados), "Recebíveis")
            fig_filial = _estilizar_fig(_grafico_por_filial(dados), "Por cliente/filial")
            fig_tempo = _estilizar_fig(_grafico_tempo(dados, entidade), "Evolução diária")
        elif entidade == "localizacao":
            figura = _estilizar_fig(_heatmap_localizacao(dados), "Matriz de localização")
            fig_filial = _estilizar_fig(_funnel_pipeline_logistico(dados), "Pipeline logístico")
            fig_tempo = _estilizar_fig(_grafico_tempo(dados, entidade), "Evolução diária")
        else:
            fig_filial = _estilizar_fig(_grafico_por_filial(dados), "Por filial")
            fig_tempo = _estilizar_fig(_grafico_tempo(dados, entidade), "Evolução diária")
        soma_fmt = _fmt_brl(soma)
        media_fmt = _fmt_brl(media)
        maximo = _fmt_brl(_max_valor(dados))
        minimo = _fmt_brl(_min_valor(dados))
        mediana = _fmt_brl(_median_valor(dados))
        desvio = _fmt_brl(_std_valor(dados))
        ultima_data = _fmt_data(_ultima_data(dados, entidade))
        top_filial = _top_filial_str(dados)
        menor_filial = _menor_filial_str(dados)
        return dados.to_dict("records"), colunas, figura, fig_filial, fig_tempo, f"{total}", soma_fmt, media_fmt, maximo, minimo, mediana, desvio, ultima_data, top_filial, menor_filial, opcoes_filiais, opcoes_status, opcoes_pessoa, opcoes_uf, opcoes_cidade, opcoes_origem, opcoes_destino, opcoes_modal
    except Exception:
        import pandas as pd
        cols = _colunas_padrao(entidade)
        df = pd.DataFrame(columns=cols)
        colunas = [{"name": c, "id": c} for c in cols]
        fig = _fig_sem_dados(_titulo_entidade(entidade))
        fig = _estilizar_fig(fig, _titulo_entidade(entidade))
        vazio = _fmt_brl(None)
        return df.to_dict("records"), colunas, fig, fig, fig, "0", vazio, vazio, vazio, vazio, vazio, vazio, "—", "—", "—", [], [], [], [], [], [], [], []

def _carregar_inicial(entidade, limite):
    try:
        if entidade == "contas":
            df = consultar_tabela(CONTAS_PAGAR_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Contas a Pagar")
        elif entidade == "faturas":
            try:
                df = consultar_tabela(FATURAS_CLIENTE_TOP, {"n": int(limite)})
            except Exception:
                df = consultar_tabela(FATURAS_CLIENTE_BASE, {"n": int(limite)})
            if len(df) == 0:
                df = consultar_tabela(FATURAS_CLIENTE_BASE, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Faturas por Cliente")
        elif entidade == "coletas":
            df = consultar_tabela(COLETAS_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Coletas")
        elif entidade == "fretes":
            try:
                df = consultar_tabela(FRETES_TOP, {"n": int(limite)})
            except Exception:
                df = consultar_tabela(FRETES_BASE, {"n": int(limite)})
            if len(df) == 0:
                df = consultar_tabela(FRETES_BASE, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Fretes")
        elif entidade == "cotacoes":
            df = consultar_tabela(COTACOES_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Cotações")
        elif entidade == "localizacao":
            try:
                df = consultar_tabela(LOCALIZACAO_TOP, {"n": int(limite)})
            except Exception:
                df = consultar_tabela(LOCALIZACAO_BASE, {"n": int(limite)})
            if len(df) == 0:
                df = consultar_tabela(LOCALIZACAO_BASE, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Localização de Carga")
        elif entidade == "manifestos_view":
            df = consultar_tabela(MANIFESTOS_VIEW_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Manifestos (view)")
        else:
            df = consultar_tabela(MANIFESTOS_RESUMO, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Manifestos (tabela)")
        return df, fig
    except Exception:
        import pandas as pd
        cols = _colunas_padrao(entidade)
        df = pd.DataFrame(columns=cols)
        titulos = {
            "contas": "Contas a Pagar",
            "faturas": "Faturas por Cliente",
            "coletas": "Coletas",
            "fretes": "Fretes",
            "cotacoes": "Cotações",
            "localizacao": "Localização de Carga",
            "manifestos_view": "Manifestos (view)",
            "manifestos": "Manifestos (tabela)",
        }
        return df, _fig_sem_dados(titulos.get(entidade, "Dados"))

def _sanitizar_df(df):
    import pandas as pd
    import decimal
    import datetime as dt
    df = df.copy()
    try:
        dtcols = list(df.select_dtypes(include=["datetime64[ns]", "datetimetz"]).columns)
        for c in dtcols:
            df[c] = pd.to_datetime(df[c], errors="coerce", dayfirst=True, format="mixed").dt.strftime("%Y-%m-%d %H:%M:%S")
    except Exception:
        pass
    for c in df.columns:
        s = df[c]
        if s.dtype == "object":
            try:
                from pandas import Timestamp
                def cv(x):
                    if isinstance(x, Timestamp):
                        try:
                            return x.to_pydatetime().isoformat()
                        except Exception:
                            return str(x)
                    if isinstance(x, dt.datetime):
                        try:
                            return x.isoformat()
                        except Exception:
                            return str(x)
                    if isinstance(x, decimal.Decimal):
                        try:
                            return float(x)
                        except Exception:
                            return str(x)
                    return x
                df[c] = s.map(cv)
            except Exception:
                df[c] = s.astype(str)
    return df

def _colunas_padrao(entidade):
    if entidade == "contas":
        return [
            "sequence_code","document_number","issue_date","pago","fornecedor","filial","valor_a_pagar"
        ]
    if entidade == "faturas":
        return [
            "unique_id","filial","pagador_nome","valor_fatura","data_vencimento_fatura"
        ]
    if entidade == "coletas":
        return [
            "ID","Coleta","Cliente","Solicitante","Local da Coleta","Cidade","UF","Solicitacao","Hora (Solicitacao)",
            "Agendamento","Horario (Inicio)","Finalizacao","Hora (Fim)","Status","Volumes","Peso Real","Peso Taxado",
            "Valor NF","Usuario","Agente","Numero Manifesto","Veiculo","Data de extracao"
        ]
    if entidade == "fretes":
        return [
            "ID","Filial","Modal","UF Origem","UF Destino","Valor frete","Peso Taxado","Peso Real","CT-e","Data Emissão"
        ]
    if entidade == "cotacoes":
        return [
            "ID","Cliente","Solicitante","Status","Valor","Peso","Solicitacao"
        ]
    if entidade == "localizacao":
        return [
            "ID","Filial","Local","Cidade","UF","Status","Valor NF","Data"
        ]
    if entidade == "manifestos_view":
        return [
            "ID","Filial","CT-e","Valor notas","Peso Taxado","Peso Real","Data Emissão"
        ]
    return [
        "sequence_code","branch_nickname","invoices_value","total_cost","paying_total","created_at"
    ]

def _titulo_entidade(entidade):
    mapa = {
        "contas": "Contas a Pagar",
        "faturas": "Faturas por Cliente",
        "coletas": "Coletas",
        "fretes": "Fretes",
        "cotacoes": "Cotações",
        "localizacao": "Localização de Carga",
        "manifestos_view": "Manifestos (view)",
        "manifestos": "Manifestos (tabela)",
    }
    return mapa.get(entidade, "Dados")

def _col(df, candidatos):
    for c in candidatos:
        if c in df.columns:
            return c
    # fallback: primeira coluna
    return df.columns[0]

def _abreviar_texto(s):
    s = str(s or "").strip()
    if "|" in s:
        s = s.split("|")[-1].strip()
    if "-" in s:
        s = s.split("-")[0].strip()
    return s[:16] + "…" if len(s) > 16 else s

def _grafico_generico(df):
    # tenta encontrar uma coluna numérica típica
    col_valor = _col_valor_generico(df)
    # tenta encontrar uma coluna categórica típica
    candidatos_cat = [
        "filial","Filial","branch_nickname","cliente_nome","pagador_nome","status","cidade","estado","uf"
    ]
    col_cat = None
    for c in candidatos_cat:
        if c in df.columns:
            col_cat = c
            break
    if col_valor and col_cat:
        import pandas as pd
        df2 = df[[col_cat, col_valor]].copy()
        df2[col_valor] = pd.to_numeric(df2[col_valor], errors="coerce").fillna(0)
        agrupado = df2.groupby(col_cat, as_index=False)[col_valor].sum().sort_values(col_valor, ascending=False).head(10)
        # abreviar rótulos para evitar poluição visual
        agrupado["__label"] = agrupado[col_cat].apply(_abreviar_texto)
        fig = px.bar(agrupado, y="__label", x=col_valor, orientation="h", text=col_valor)
        fig.update_layout(showlegend=False)
        fig.update_traces(texttemplate="R$ %{x:,.2f}", textposition="outside", marker_line_color="#dddddd", marker_line_width=1, cliponaxis=False)
        return fig
    # fallback gráfico de linhas com contagem
    return px.histogram(df, x=df.columns[0])

def _grafico_por_filial(df):
    col_valor = _col_valor_generico(df)
    col_filial = None
    for c in ["filial","Filial","branch_nickname","filial_nome","status_branch_nickname","destination_branch_nickname"]:
        if c in df.columns:
            col_filial = c
            break
    if col_valor and col_filial:
        import pandas as pd
        df2 = df[[col_filial, col_valor]].copy()
        df2[col_valor] = pd.to_numeric(df2[col_valor], errors="coerce").fillna(0)
        agrupado = df2.groupby(col_filial, as_index=False)[col_valor].sum().sort_values(col_valor, ascending=False).head(10)
        agrupado["__label"] = agrupado[col_filial].apply(_sigla_filial)
        fig = px.bar(agrupado, y="__label", x=col_valor, orientation="h", text=col_valor)
        fig.update_layout(showlegend=False)
        fig.update_traces(texttemplate="R$ %{x:,.2f}", textposition="outside", marker_line_color="#dddddd", marker_line_width=1, cliponaxis=False)
        return fig
    return _grafico_generico(df)

def _grafico_tempo(df, entidade):
    col_valor = _col_valor_generico(df)
    col_data = _col_date(df, entidade)
    if col_valor and col_data:
        import pandas as pd
        tmp = df[[col_data, col_valor]].copy()
        tmp[col_valor] = pd.to_numeric(tmp[col_valor], errors="coerce").fillna(0)
        tmp[col_data] = pd.to_datetime(tmp[col_data], errors="coerce", dayfirst=True, format="mixed")
        agrupado = tmp.groupby(pd.Grouper(key=col_data, freq="D"))[col_valor].sum().reset_index()
        fig = px.line(agrupado, x=col_data, y=col_valor)
        fig.update_traces(line_shape="spline")
        return fig
    return _grafico_generico(df)

def _estilizar_fig(fig, titulo=None):
    fig.update_layout(
        template="plotly_white",
        margin=dict(l=16, r=16, t=48, b=20),
        legend=dict(orientation="h", y=-0.25),
        title=dict(text=titulo or "", font=dict(size=18, color="#111")),
        xaxis=dict(showgrid=True, gridcolor="#f3f3f3", zeroline=False, tickangle=0),
        yaxis=dict(showgrid=False, automargin=True, tickfont=dict(size=11)),
        colorway=["#1e90ff", "#00bcd4", "#ff9800", "#4caf50", "#9c27b0"],
        bargap=0.35,
        height=480,
        hoverlabel=dict(bgcolor="#ffffff", bordercolor="#dddddd", font=dict(color="#111")),
        paper_bgcolor="#ffffff",
        plot_bgcolor="#ffffff",
    )
    fig.update_traces(opacity=0.95)
    fig.update_traces(marker_line_color="#dddddd", marker_line_width=1, selector=dict(type="bar"))
    fig.update_traces(marker_line_color="#dddddd", marker_line_width=1, selector=dict(type="histogram"))
    return fig

def _fig_sem_dados(titulo):
    f = go.Figure()
    f.update_layout(
        title=dict(text=titulo or "Sem dados", font=dict(size=16)),
        xaxis=dict(visible=False),
        yaxis=dict(visible=False),
        annotations=[dict(text="Sem dados", x=0.5, y=0.5, showarrow=False, font=dict(size=14, color="#666"))],
        height=360,
        template="plotly_white",
    )
    return f

def _estilos_kpi_por_entidade(entidade):
    mapa = {
        "contas": ("#1e90ff", "#e8f2ff"),
        "faturas": ("#6a5acd", "#eee9fd"),
        "coletas": ("#00bcd4", "#e8f9fc"),
        "fretes": ("#ff9800", "#fff1e0"),
        "cotacoes": ("#4caf50", "#eaf7ea"),
        "localizacao": ("#9c27b0", "#f5e8fb"),
        "manifestos_view": ("#607d8b", "#eef3f5"),
        "manifestos": ("#607d8b", "#eef3f5"),
    }
    cor, fundo = mapa.get(entidade, ("#1e90ff", "#f2f6ff"))
    estilo_texto = {"color": cor}
    estilo_card = {"border": f"1px solid {cor}", "boxShadow": "0 2px 10px rgba(0,0,0,0.06)", "borderRadius": "12px", "backgroundColor": fundo}
    return estilo_texto, estilo_card

def _fmt_brl(v):
    if v is None:
        return "R$ —"
    return f"R$ { _fmt_num(v) }"

def _col_valor_generico(df):
    preferidas = [
        "valor_total","valor_fatura","valor_a_pagar","invoices_value","total_cost","paying_total",
        "subtotal","total_value",
        "Valor Frete","Valor NF","Valor Total do Servico",
        "Valor a pagar","Fatura / Valor","Valor notas"
    ]
    for c in preferidas:
        if c in df.columns:
            return c
    # por substring e ser numérica
    tokens = ["valor","value","amount","total","price","preco","custo","cost"]
    for col in df.columns:
        lc = str(col).lower()
        if any(tok in lc for tok in tokens):
            try:
                import pandas as pd
                pd.to_numeric(df[col], errors="coerce")
                return col
            except Exception:
                continue
    # fallback: primeira coluna numérica
    try:
        import pandas as pd
        for col in df.columns:
            serie = pd.to_numeric(df[col], errors="coerce")
            if serie.notna().any():
                return col
    except Exception:
        pass
    return None

def _soma_valor(df):
    c = _col_valor_generico(df)
    if c and c in df.columns:
        try:
            return float(df[c].fillna(0).astype(float).sum())
        except Exception:
            return None
    return None

def _fmt_num(v):
    if v is None:
        return "—"
    return f"{v:,.2f}".replace(",","X").replace(".",",").replace("X",".")

def _aplicar_filtros(df, entidade, inicio, fim, filiais,
                    status_sel=None, pessoa_sel=None, uf_sel=None,
                    cidade_sel=None, origem_sel=None, destino_sel=None, modal_sel=None,
                    vmin=None, vmax=None, pmin=None, pmax=None, busca=None):
    # Filial
    col_filial = _col(df, ["filial","Filial","branch_nickname"]) 
    if filiais and isinstance(filiais, list) and len(filiais) > 0 and col_filial in df.columns:
        df = df[df[col_filial].astype(str).isin([str(x) for x in filiais])]
    # Período
    col_data = _col_date(df, entidade)
    if col_data and col_data in df.columns and (inicio or fim):
        serie = _parse_datetime(df[col_data])
        if inicio:
            df = df[serie >= _parse_datetime_val(inicio)]
        if fim:
            df = df[serie <= _parse_datetime_val(fim)]
    # Status
    col_status = _col(df, ["status","Status","Status Carga","pago","Pago"]) 
    if col_status in df.columns and status_sel:
        df = df[df[col_status].astype(str).isin([str(x) for x in (status_sel if isinstance(status_sel, list) else [status_sel])])]
    # Pessoa (pagador/fornecedor/cliente)
    col_pessoa = _col(df, ["pagador_nome","Pagador / Nome","fornecedor","Fornecedor/Nome","cliente_nome"]) 
    if col_pessoa in df.columns and pessoa_sel:
        df = df[df[col_pessoa].astype(str).isin([str(x) for x in (pessoa_sel if isinstance(pessoa_sel, list) else [pessoa_sel])])]
    # UF/Estado
    col_uf = _col(df, ["estado","uf","UF","Estado"]) 
    if col_uf in df.columns and uf_sel:
        df = df[df[col_uf].astype(str).isin([str(x) for x in (uf_sel if isinstance(uf_sel, list) else [uf_sel])])]
    # Cidade
    col_cidade = _col(df, ["cidade","Cidade","city","City"]) 
    if col_cidade in df.columns and cidade_sel:
        df = df[df[col_cidade].astype(str).isin([str(x) for x in (cidade_sel if isinstance(cidade_sel, list) else [cidade_sel])])]
    # Origem
    col_origem = _col(df, ["origem","cidade_origem","origin_city","Origin","Cidade Origem"]) 
    if col_origem in df.columns and origem_sel:
        df = df[df[col_origem].astype(str).isin([str(x) for x in (origem_sel if isinstance(origem_sel, list) else [origem_sel])])]
    # Destino
    col_destino = _col(df, ["destino","cidade_destino","destination_city","Destination","Cidade Destino"]) 
    if col_destino in df.columns and destino_sel:
        df = df[df[col_destino].astype(str).isin([str(x) for x in (destino_sel if isinstance(destino_sel, list) else [destino_sel])])]
    # Modal
    col_modal = _col(df, ["modal","Modal","modality","service_mode"]) 
    if col_modal in df.columns and modal_sel:
        df = df[df[col_modal].astype(str).isin([str(x) for x in (modal_sel if isinstance(modal_sel, list) else [modal_sel])])]
    # Faixa de valor
    col_valor = None
    for c in ["valor_total","valor_fatura","valor_a_pagar","invoices_value","total_cost","paying_total"]:
        if c in df.columns:
            col_valor = c
            break
    if col_valor:
        import pandas as pd
        serie_val = pd.to_numeric(df[col_valor], errors="coerce").fillna(0)
        if vmin is not None:
            df = df[serie_val >= float(vmin)]
        if vmax is not None:
            df = df[serie_val <= float(vmax)]
    # Faixa de peso
    col_peso = None
    for c in ["taxed_weight","peso_notas","real_weight","peso","weight"]:
        if c in df.columns:
            col_peso = c
            break
    if col_peso:
        import pandas as pd
        serie_peso = pd.to_numeric(df[col_peso], errors="coerce").fillna(0)
        if pmin is not None:
            df = df[serie_peso >= float(pmin)]
        if pmax is not None:
            df = df[serie_peso <= float(pmax)]
    # Busca global
    if busca and str(busca).strip() != "":
        termo = str(busca).strip().lower()
        import pandas as pd
        mask = pd.Series(False, index=df.index)
        for c in df.columns:
            try:
                valores = df[c].astype(str).str.lower()
                mask = mask | valores.str.contains(termo, na=False)
            except Exception:
                continue
        df = df[mask]
    return df

def _col_date(df, entidade):
    # Mapeia colunas de data por entidade e por aliases comuns nas views
    candidatos_por_entidade = {
        "contas": ["issue_date","Emissão","data_criacao","data_liquidacao"],
        "faturas": ["data_vencimento_fatura","Parcelas / Vencimento","data_emissao_fatura","CT-e / Data emissão"],
        "coletas": ["service_date","request_date","finish_date"],
        "fretes": ["Data frete","servico_em","criado_em","data_previsao_entrega"],
        "cotacoes": ["requested_at"],
        "localizacao": ["Data Emissão","Previsão Entrega","service_at","predicted_delivery_at"],
        "manifestos_view": ["created_at","departured_at","finished_at"],
        "manifestos": ["created_at","departured_at","finished_at"],
    }
    candidatos = candidatos_por_entidade.get(entidade, []) + [
        "Emissão","issue_date","service_at","requested_at","created_at","Data frete","Data Emissão"
    ]
    for c in candidatos:
        if c in df.columns:
            return c
    return None

def _parse_datetime(serie):
    try:
        import pandas as pd
        return pd.to_datetime(serie, errors="coerce", dayfirst=True, format="mixed")
    except Exception:
        return serie

def _parse_datetime_val(s):
    try:
        import pandas as pd
        return pd.to_datetime(s, errors="coerce", dayfirst=True, format="mixed")
    except Exception:
        return s

def _opcoes_unicas(df, candidatos):
    col = _col(df, candidatos)
    if col in df.columns:
        unicas = sorted([str(v) for v in df[col].dropna().unique()])
        return [{"label": v, "value": v} for v in unicas]
    return []

def iniciar():
    aplicacao.run_server(debug=True, dev_tools_ui=False)
def _max_valor(df):
    c = _col_valor_generico(df)
    if c and c in df.columns:
        try:
            return float(df[c].fillna(0).astype(float).max())
        except Exception:
            return None
    return None

def _min_valor(df):
    c = _col_valor_generico(df)
    if c and c in df.columns:
        try:
            return float(df[c].fillna(0).astype(float).min())
        except Exception:
            return None
    return None

def _qtd_filiais(df):
    col = _col(df, ["filial","Filial","branch_nickname","filial_nome","filial_atual","status_branch_nickname","destination_branch_nickname"]) 
    if col in df.columns:
        return int(df[col].dropna().nunique())
    return 0

def _ultima_data(df, entidade):
    col = _col_date(df, entidade)
    if col and col in df.columns:
        import pandas as pd
        serie = pd.to_datetime(df[col], errors="coerce", dayfirst=True, format="mixed")
        if serie.notna().any():
            return pd.to_datetime(serie.max())
    return None

def _fmt_data(d):
    if d is None:
        return "—"
    try:
        return d.strftime("%d/%m/%Y")
    except Exception:
        return str(d)

def _median_valor(df):
    c = _col_valor_generico(df)
    if c and c in df.columns:
        try:
            return float(df[c].fillna(0).astype(float).median())
        except Exception:
            return None
    return None

def _std_valor(df):
    c = _col_valor_generico(df)
    if c and c in df.columns:
        try:
            return float(df[c].fillna(0).astype(float).std())
        except Exception:
            return None
    return None

def _sigla_filial(nome):
    s = str(nome or "").strip()
    if "|" in s:
        token = s.split("|")[-1].strip()
        return token.split()[0]
    if "-" in s:
        token = s.split("-")[0].strip()
        return token.split()[0]
    return s.split()[0] if s else ""

def _top_filial_str(df):
    col_valor = _col_valor_generico(df)
    col_filial = _col(df, ["filial","Filial","branch_nickname","filial_nome","filial_atual","status_branch_nickname","destination_branch_nickname"]) 
    if col_valor and col_filial in df.columns:
        import pandas as pd
        tmp = df[[col_filial, col_valor]].copy()
        tmp[col_valor] = pd.to_numeric(tmp[col_valor], errors="coerce").fillna(0)
        grp = tmp.groupby(col_filial, as_index=False)[col_valor].sum().sort_values(col_valor, ascending=False)
        if len(grp) > 0:
            sigla = _sigla_filial(grp.iloc[0][col_filial])
            return f"{sigla} · R$ { _fmt_num(grp.iloc[0][col_valor]) }"
    # fallback por contagem
    if col_filial in df.columns:
        import pandas as pd
        grp = df.groupby(col_filial).size().sort_values(ascending=False)
        if len(grp) > 0:
            sigla = _sigla_filial(grp.index[0])
            return f"{sigla} · {int(grp.iloc[0])}"
    return "—"

def _menor_filial_str(df):
    col_valor = _col_valor_generico(df)
    col_filial = _col(df, ["filial","Filial","branch_nickname","filial_nome","filial_atual","status_branch_nickname","destination_branch_nickname"]) 
    if col_valor and col_filial in df.columns:
        import pandas as pd
        tmp = df[[col_filial, col_valor]].copy()
        tmp[col_valor] = pd.to_numeric(tmp[col_valor], errors="coerce").fillna(0)
        grp = tmp.groupby(col_filial, as_index=False)[col_valor].sum().sort_values(col_valor, ascending=True)
        if len(grp) > 0:
            sigla = _sigla_filial(grp.iloc[0][col_filial])
            return f"{sigla} · R$ { _fmt_num(grp.iloc[0][col_valor]) }"
    # fallback por contagem
    if col_filial in df.columns:
        import pandas as pd
        grp = df.groupby(col_filial).size().sort_values(ascending=True)
        if len(grp) > 0:
            sigla = _sigla_filial(grp.index[0])
            return f"{sigla} · {int(grp.iloc[0])}"
    return "—"

 

def _treemap_generico(df, group_cols, value_cols):
    col_group = _col(df, group_cols)
    col_val = _col(df, value_cols)
    import pandas as pd
    tmp = df[[col_group, col_val]].copy()
    tmp[col_val] = pd.to_numeric(tmp[col_val], errors="coerce").fillna(0)
    tmp2 = tmp.groupby(col_group, as_index=False)[col_val].sum().sort_values(col_val, ascending=False).head(20)
    return px.treemap(tmp2, path=[col_group], values=col_val)

def _sankey_fluxo(df):
    origem = _col(df, ["origin_state","UF Origem","uf_origem","estado_origem","origem_uf"]) 
    destino = _col(df, ["destination_state","UF Destino","uf_destino","estado_destino","destino_uf"]) 
    valor = _col(df, ["Valor Frete","valor_frete","freight_value","subtotal","valor_total","Valor Total do Servico"]) 
    import pandas as pd
    tmp = df[[origem, destino, valor]].copy()
    tmp[valor] = pd.to_numeric(tmp[valor], errors="coerce").fillna(0)
    grp = tmp.groupby([origem, destino], as_index=False)[valor].sum().sort_values(valor, ascending=False).head(30)
    nodes = sorted(list(set(grp[origem]) | set(grp[destino])))
    idx = {n:i for i,n in enumerate(nodes)}
    src = grp[origem].map(idx).tolist()
    tgt = grp[destino].map(idx).tolist()
    val = grp[valor].tolist()
    link = dict(source=src, target=tgt, value=val)
    fig = go.Figure(go.Sankey(node=dict(label=nodes, pad=12, thickness=12), link=link))
    return fig

def _funnel_cotacoes(df):
    status_col = _col(df, ["status_conversao","conversion_status","status","Status"]) 
    import pandas as pd
    s = df[status_col].astype(str).str.lower()
    etapas = {
        "totais": len(df),
        "pendentes": int((s.str.contains("pend") | s.str.contains("abert")).sum()),
        "ganhas": int(s.str.contains("ganh").sum()),
    }
    tmp = pd.DataFrame({"etapa": list(etapas.keys()), "valor": list(etapas.values())})
    return px.funnel(tmp, x="valor", y="etapa")

def _donut_motivos_perda(df):
    col = _col(df, ["motivo_perda","disapprove_comments","loss_reason"]) 
    import pandas as pd
    valores = df[col].dropna().astype(str)
    valores = valores[valores.str.strip() != ""]
    if len(valores) == 0:
        return px.pie(names=["Sem dados"], values=[1], hole=0.55)
    grp = valores.value_counts().reset_index()
    grp.columns = ["motivo","qtde"]
    return px.pie(grp, names="motivo", values="qtde", hole=0.55)

def _heatmap_localizacao(df):
    filial = _col(df, ["Filial Atual","status_branch_nickname","filial_atual","branch_nickname","Filial","filial","destination_branch_nickname"]) 
    valor_nf = _col(df, ["Valor Frete","Valor NF","total_value","valor_total","valor_nf","valor_notas","invoices_value","Valor Total do Servico"]) 
    import pandas as pd
    df2 = df[[filial, valor_nf]].copy()
    df2[valor_nf] = pd.to_numeric(df2[valor_nf], errors="coerce").fillna(0)
    c = df2.groupby(filial)[valor_nf].agg(["count","sum"]).reset_index()
    if len(c) == 0:
        return _fig_sem_dados("Matriz de localização")
    c["label"] = c[filial].apply(_sigla_filial)
    z = [c["count"].tolist()]
    return px.imshow(z, x=c["label"], y=["CT-e"], aspect="auto", color_continuous_scale="Blues")

def _funnel_pipeline_logistico(df):
    status_col = _col(df, ["status_carga","status","Status"]) 
    import pandas as pd
    s = df[status_col].astype(str).str.lower()
    ordem = ["coletado","transfer","destino","entrega","finaliz"]
    etapas = {}
    for tok in ordem:
        etapas[tok] = int(s.str.contains(tok).sum())
    tmp = pd.DataFrame({"etapa": list(etapas.keys()), "valor": list(etapas.values())})
    return px.funnel(tmp, x="valor", y="etapa")

def _heatmap_coletas(df):
    hora_col = _col(df, ["request_hour","service_start_hour","service_end_hour"]) 
    data_col = _col(df, ["request_date","service_date","finish_date"]) 
    import pandas as pd
    h = df[hora_col].astype(str).str.slice(0,2)
    d = pd.to_datetime(df[data_col], errors="coerce", dayfirst=True, format="mixed")
    dia = d.dt.day_name() if hasattr(d.dt, "day_name") else d.dt.weekday
    tmp = pd.DataFrame({"hora": h, "dia": dia})
    grp = tmp.groupby(["dia","hora"]).size().reset_index(name="qtde")
    pivot = grp.pivot(index="dia", columns="hora", values="qtde").fillna(0)
    if pivot.size == 0:
        return _fig_sem_dados("Coletas por hora x dia")
    return px.imshow(pivot, color_continuous_scale="Viridis")

def _scatter_manifestos(df):
    receita = _col(df, ["invoices_value","freight_subtotal","receita","Valor notas"]) 
    custo = _col(df, ["total_cost","operational_expenses_total","Custo Total"]) 
    cor = _col(df, ["vehicle_plate","branch_nickname","Filial"]) 
    import pandas as pd
    df2 = df[[receita, custo, cor]].copy()
    for c in [receita, custo]:
        df2[c] = pd.to_numeric(df2[c], errors="coerce").fillna(0)
    if len(df2) == 0:
        return _fig_sem_dados("Quadrante do lucro")
    fig = px.scatter(df2, x=custo, y=receita, color=cor)
    mx = float(df2[[custo, receita]].max().max())
    fig.add_shape(type="line", x0=0, y0=0, x1=mx, y1=mx, line=dict(color="#999", dash="dot"))
    return fig

def _barras_manifestos_filial(df):
    receita = _col(df, ["invoices_value","freight_subtotal","receita","Valor notas"]) 
    custo = _col(df, ["total_cost","operational_expenses_total","Custo Total"]) 
    filial = _col(df, ["branch_nickname","Filial"]) 
    import pandas as pd
    df2 = df[[filial, receita, custo]].copy()
    for c in [receita, custo]:
        df2[c] = pd.to_numeric(df2[c], errors="coerce").fillna(0)
    grp = df2.groupby(filial, as_index=False)[[receita, custo]].sum().sort_values(receita, ascending=False).head(10)
    grp["__label"] = grp[filial].apply(_sigla_filial)
    if len(grp) == 0:
        return _fig_sem_dados("Receita vs Custo por filial")
    long = grp.melt(id_vars="__label", value_vars=[receita, custo], var_name="tipo", value_name="valor")
    fig = px.bar(long, y="__label", x="valor", color="tipo", orientation="h")
    fig.update_layout(barmode="group")
    return fig

def _margem_manifestos_tempo(df):
    receita = _col(df, ["invoices_value","freight_subtotal","receita","Valor notas"]) 
    custo = _col(df, ["total_cost","operational_expenses_total","Custo Total"]) 
    data_col = _col_date(df, "manifestos")
    import pandas as pd
    tmp = df[[data_col, receita, custo]].copy()
    tmp[receita] = pd.to_numeric(tmp[receita], errors="coerce").fillna(0)
    tmp[custo] = pd.to_numeric(tmp[custo], errors="coerce").fillna(0)
    tmp[data_col] = pd.to_datetime(tmp[data_col], errors="coerce", dayfirst=True, format="mixed")
    tmp["margem"] = tmp[receita] - tmp[custo]
    agrupado = tmp.groupby(pd.Grouper(key=data_col, freq="D"))["margem"].sum().reset_index()
    if len(agrupado) == 0:
        return _fig_sem_dados("Margem diária")
    fig = px.line(agrupado, x=data_col, y="margem")
    fig.update_traces(line_shape="spline")
    return fig

def _grafico_faturas_linha(df):
    data_col = _col(df, ["data_vencimento_fatura","Parcelas / Vencimento","data_emissao_fatura","CT-e / Data emissão","vencimento","Data Emissão"]) 
    val_col = _col(df, ["valor_fatura","Valor","valor_total"]) 
    import pandas as pd
    tmp = df[[data_col, val_col]].copy()
    tmp[val_col] = pd.to_numeric(tmp[val_col], errors="coerce").fillna(0)
    tmp[data_col] = pd.to_datetime(tmp[data_col], errors="coerce", dayfirst=True, format="mixed")
    agrupado = tmp.groupby(pd.Grouper(key=data_col, freq="D"))[val_col].sum().reset_index()
    if len(agrupado) == 0:
        return _fig_sem_dados("Recebíveis")
    return px.line(agrupado, x=data_col, y=val_col)

def _grafico_contas_linha(df):
    data_col = _col(df, ["Emissão","issue_date","data_criacao","data_liquidacao","vencimento"]) 
    val_col = _col(df, ["valor_a_pagar","Valor a pagar","valor_total"]) 
    import pandas as pd
    tmp = df[[data_col, val_col]].copy()
    tmp[val_col] = pd.to_numeric(tmp[val_col], errors="coerce").fillna(0)
    tmp[data_col] = pd.to_datetime(tmp[data_col], errors="coerce", dayfirst=True, format="mixed")
    agrupado = tmp.groupby(pd.Grouper(key=data_col, freq="D"))[val_col].sum().reset_index()
    if len(agrupado) == 0:
        return _fig_sem_dados("Contas a Pagar")
    return px.line(agrupado, x=data_col, y=val_col)
 
if __name__ == "__main__":
    iniciar()
