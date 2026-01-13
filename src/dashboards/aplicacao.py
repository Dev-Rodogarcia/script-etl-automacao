import dash
from dash import dcc, html, Input, Output, State
import dash_bootstrap_components as dbc
import plotly.express as px
import plotly.graph_objects as go
try:
    from .banco import consultar_tabela, banco_ping, banco_erro, banco_driver
    from .consultas import (
        CONTAS_PAGAR_TOP, FATURAS_CLIENTE_TOP,
        COLETAS_TOP, FRETES_TOP, COTACOES_TOP, LOCALIZACAO_TOP, MANIFESTOS_VIEW_TOP
    )
    from .layout import criar_layout
except ImportError:
    from banco import consultar_tabela, banco_ping, banco_erro, banco_driver
    from consultas import (
        CONTAS_PAGAR_TOP, FATURAS_CLIENTE_TOP,
        COLETAS_TOP, FRETES_TOP, COTACOES_TOP, LOCALIZACAO_TOP, MANIFESTOS_VIEW_TOP
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
    dash.Output("alerta-banco", "children"),
    dash.Output("alerta-banco", "color"),
    dash.Output("alerta-banco", "is_open"),
    dash.Input("seletor-entidade", "value"),
    dash.Input("botao-carregar", "n_clicks"),
    dash.Input("botao-atualizar", "n_clicks"),
    dash.State("alerta-banco", "is_open"),
    prevent_initial_call=False
)
def status_banco(_entidade, _c, _u, aberto_atual):
    ok = False
    try:
        ok = bool(banco_ping())
    except Exception:
        ok = False
    if ok:
        return "Conectado ao banco de dados", "success", bool(aberto_atual) if aberto_atual is not None else True
    return "Banco de dados indisponível. Verifique as variáveis .env e o driver ODBC.", "warning", bool(aberto_atual) if aberto_atual is not None else True

@aplicacao.callback(
    dash.Output("alertas", "children"),
    dash.Input("seletor-entidade", "value"),
    dash.Input("botao-carregar", "n_clicks"),
    dash.Input("botao-atualizar", "n_clicks"),
    prevent_initial_call=False
)
def diagnosticos(entidade, _c, _u):
    import os
    msgs = []
    def add(msg, cor):
        msgs.append(dbc.Alert(msg, color=cor, dismissable=True, className="mb-1"))
    url = os.getenv("DB_URL") or ""
    user = os.getenv("DB_USER") or ""
    pwd = os.getenv("DB_PASSWORD") or ""
    drv = os.getenv("ODBC_DRIVER") or ""
    if str(url).strip() == "":
        add("Variável DB_URL ausente", "danger")
    if str(user).strip() == "" or str(pwd).strip() == "":
        add("Credenciais DB_USER/DB_PASSWORD ausentes", "danger")
    if str(drv).strip() == "":
        add("Variável ODBC_DRIVER não definida; usando fallback de driver", "warning")
    try:
        import re as _re
        m = _re.match(r"jdbc:sqlserver://([^:;]+)(?::(\d+))?;databaseName=([^;]+)", str(url))
        if m:
            h, p, dbn = m.group(1), m.group(2) or "1433", m.group(3)
            enc = "encrypt=false" not in str(url).lower()
            add("DB_URL detectado: servidor="+h+" porta="+p+" banco="+dbn+" encrypt="+("on" if enc else "off"), "info")
        else:
            add("DB_URL com formato inválido para JDBC SQL Server", "danger")
    except Exception:
        add("Falha ao analisar DB_URL", "warning")
    try:
        import pyodbc
        det = [d for d in pyodbc.drivers() if "SQL Server" in d]
        if len(det) == 0:
            add("Nenhum driver ODBC de SQL Server instalado", "danger")
        else:
            add("Drivers ODBC detectados: " + ", ".join(det), "info")
    except Exception:
        add("pyodbc indisponível para listar drivers", "warning")
    ok = False
    try:
        ok = bool(banco_ping())
    except Exception:
        ok = False
    if not ok:
        err = banco_erro()
        if str(err).strip() != "":
            add("Conexão ao banco falhou: " + str(err), "danger")
        else:
            add("Conexão ao banco falhou", "danger")
    else:
        add("Conectado ao banco de dados", "success")
        try:
            drv_usado = banco_driver()
            if str(drv_usado).strip() != "":
                add("Driver ODBC em uso: " + str(drv_usado), "info")
        except Exception:
            pass
    try:
        import socket
        s = socket.socket()
        s.settimeout(2)
        s.connect(("localhost", 1433))
        s.close()
        add("Porta 1433 aberta em localhost", "info")
    except Exception:
        add("Porta 1433 não está acessível em localhost", "warning")
    if ok and entidade:
        nomes = {
            "contas": "vw_contas_a_pagar_powerbi",
            "faturas": "vw_faturas_por_cliente_powerbi",
            "coletas": "vw_coletas_powerbi",
            "fretes": "vw_fretes_powerbi",
            "cotacoes": "vw_cotacoes_powerbi",
            "localizacao": "vw_localizacao_cargas_powerbi",
            "manifestos_view": "vw_manifestos_powerbi",
            "manifestos": "vw_manifestos_powerbi",
        }
        alvo = nomes.get(entidade)
        if alvo:
            try:
                df_obj = consultar_tabela("SELECT COUNT(*) AS cnt FROM sys.objects WHERE name=:n", {"n": alvo})
                existe = int(df_obj["cnt"].iloc[0]) > 0 if len(df_obj) > 0 else False
                if existe:
                    add("Objeto " + alvo + " disponível no banco", "success")
                else:
                    add("Objeto " + alvo + " não encontrado no banco", "warning")
            except Exception:
                add("Falha ao verificar objeto " + str(alvo), "warning")
        try:
            df_amostra, _ = _carregar_inicial(entidade, 5)
            if len(df_amostra) == 0:
                add("Sem dados para a entidade selecionada", "warning")
        except Exception:
            add("Falha ao consultar a entidade selecionada", "danger")
    return msgs

@aplicacao.callback(
    Output("tabela", "data"), Output("tabela", "columns"),
    Output("grafico", "figure"), Output("grafico-secundario", "figure"), Output("grafico-terciario", "figure"),
    Output("kpi-total", "children"), Output("kpi-soma", "children"), Output("kpi-media", "children"),
    Output("kpi-max", "children"), Output("kpi-min", "children"), Output("kpi-mediana", "children"), Output("kpi-desvio", "children"),
    Output("kpi-ultima-data", "children"), Output("kpi-top-filial", "children"), Output("kpi-menor-filial", "children"),
    Output("seletor-filiais", "options"), Output("seletor-status", "options"), Output("seletor-pessoa", "options"), Output("seletor-uf", "options"),
    Output("seletor-cidade", "options"), Output("seletor-origem", "options"), Output("seletor-destino", "options"), Output("seletor-modal", "options"),
    Output("kpi-exec-receita", "children"), Output("kpi-exec-custo", "children"), Output("kpi-exec-lucro", "children"), Output("kpi-exec-margem", "children"), Output("kpi-exec-resultado", "children"),
    Output("kpi-yoy", "children"), Output("kpi-conversao", "children"), Output("kpi-pontualidade", "children"), Output("kpi-tempo-medio", "children"), Output("kpi-rentabilidade", "children"),
    Output("kpi-aberto", "children"), Output("kpi-em-transito", "children"), Output("kpi-principal-destino", "children"),
    Input("seletor-entidade", "value"),
    Input("filtro-periodo", "start_date"), Input("filtro-periodo", "end_date"),
    Input("seletor-filiais", "value"), Input("seletor-status", "value"), Input("seletor-pessoa", "value"), Input("seletor-uf", "value"),
    Input("seletor-cidade", "value"), Input("seletor-origem", "value"), Input("seletor-destino", "value"), Input("seletor-modal", "value"),
    Input("filtro-valor-min", "value"), Input("filtro-valor-max", "value"), Input("filtro-peso-min", "value"), Input("filtro-peso-max", "value"), Input("filtro-busca", "value"),
    Input("botao-carregar", "n_clicks"), Input("botao-atualizar", "n_clicks")
)
def carregar_dados(entidade, inicio, fim, filiais, status_sel, pessoa_sel, uf_sel, cidade_sel, origem_sel, destino_sel, modal_sel, vmin, vmax, pmin, pmax, busca, _, __):
    limite = 6000
    try:
        dados, figura = _carregar_inicial(entidade, limite)
        dados = _transformar_entidade(dados, entidade)
        dados = _aplicar_filtros(
            dados, entidade, inicio, fim, filiais,
            status_sel, pessoa_sel, uf_sel,
            cidade_sel, origem_sel, destino_sel, modal_sel,
            vmin, vmax, pmin, pmax, busca
        )
        dados = _sanitizar_df(dados)
        colunas = [{"name": c, "id": c} for c in list(dados.columns)]
        total, soma, media, maximo_raw, minimo_raw, mediana_raw, desvio_raw = _kpi_por_entidade(dados, entidade)
        col_filial = _col(dados, ["filial","Filial","branch_nickname","filial_nome","filial_atual","status_branch_nickname","destination_branch_nickname"]) 
        opcoes_filiais = []
        if col_filial in dados.columns:
            unicas = sorted([str(v) for v in dados[col_filial].dropna().unique()])
            opcoes_filiais = [{"label": _sigla_filial(v), "value": v} for v in unicas]
        opcoes_status = _opcoes_unicas(dados, ["status","Status","pago","Pago","Status_Nome","Status_PT","Status_Cotacao","status_carga","Status Carga"]) 
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
        maximo = _fmt_brl(maximo_raw)
        minimo = _fmt_brl(minimo_raw)
        mediana = _fmt_brl(mediana_raw)
        desvio = _fmt_brl(desvio_raw)
        ultima_data = _fmt_data(_ultima_data(dados, entidade))
        top_filial = _top_filial_str(dados)
        menor_filial = _menor_filial_str(dados)
        exec_rec, exec_cus, exec_luc, exec_mar, exec_res = _executivo_kpis(inicio, fim, filiais)
        yoy, conv, pont, tmedio, rentab, aberto, emtrans, pdest = _kpis_entidade_extras(dados, entidade)
        return dados.to_dict("records"), colunas, figura, fig_filial, fig_tempo, f"{total}", soma_fmt, media_fmt, maximo, minimo, mediana, desvio, ultima_data, top_filial, menor_filial, opcoes_filiais, opcoes_status, opcoes_pessoa, opcoes_uf, opcoes_cidade, opcoes_origem, opcoes_destino, opcoes_modal, exec_rec, exec_cus, exec_luc, exec_mar, exec_res, yoy, conv, pont, tmedio, rentab, aberto, emtrans, pdest
    except Exception:
        import pandas as pd
        cols = _colunas_padrao(entidade)
        df = pd.DataFrame(columns=cols)
        colunas = [{"name": c, "id": c} for c in cols]
        fig = _fig_sem_dados(_titulo_entidade(entidade))
        fig = _estilizar_fig(fig, _titulo_entidade(entidade))
        vazio = _fmt_brl(None)
        return df.to_dict("records"), colunas, fig, fig, fig, "0", vazio, vazio, vazio, vazio, vazio, vazio, "—", "—", "—", [], [], [], [], [], [], [], [], vazio, vazio, vazio, "—", "—", "—", "—", "—", "—", "—", "—", "—", "—"

def _carregar_inicial(entidade, limite):
    try:
        if entidade == "contas":
            df = consultar_tabela(CONTAS_PAGAR_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Contas a Pagar")
        elif entidade == "faturas":
            df = consultar_tabela(FATURAS_CLIENTE_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Faturas por Cliente")
        elif entidade == "coletas":
            df = consultar_tabela(COLETAS_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Coletas")
        elif entidade == "fretes":
            df = consultar_tabela(FRETES_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Fretes")
        elif entidade == "cotacoes":
            df = consultar_tabela(COTACOES_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Cotações")
        elif entidade == "localizacao":
            df = consultar_tabela(LOCALIZACAO_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Localização de Carga")
        elif entidade == "manifestos_view":
            df = consultar_tabela(MANIFESTOS_VIEW_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Manifestos (view)")
        else:
            df = consultar_tabela(MANIFESTOS_VIEW_TOP, {"n": int(limite)})
            fig = _estilizar_fig(_grafico_generico(df), "Manifestos")
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
            "manifestos": "Manifestos",
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
        "ID","Filial","CT-e","Valor notas","Peso Taxado","Peso Real","Data Emissão"
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
def _col_strict(df, candidatos):
    for c in candidatos:
        if c in df.columns:
            return c
    return None

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

def _fmt_pct(v):
    if v is None:
        return "—"
    try:
        return f"{(float(v)*100):,.2f}%".replace(",","X").replace(".",",").replace("X",".")
    except Exception:
        return "—"
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
    import os
    try:
        port = int(os.getenv("PORT", "8050"))
    except Exception:
        port = 8050
    aplicacao.run(debug=True, port=port)
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
    import pandas as pd
    df2 = df.copy()
    flag = _col(df2, ["Flag_Operacional","flag_operacional"])
    if flag in df2.columns:
        df2 = df2[df2[flag] == 1]
    v_ap = _col(df2, ["Valor a pagar","valor_a_pagar"])
    v_pg = _col(df2, ["Valor pago","valor_pago"])
    dt_venc = _col(df2, ["Vencimento Estimado","vencimento"])
    dt_emis = _col(df2, ["Emissão","issue_date","data_criacao"])
    dt_liq = _col(df2, ["Data liquidação","Baixa","data_liquidacao","Fatura/Baixa"])
    linhas = []
    if v_ap:
        col_data = dt_venc or dt_emis
        tmp = df2[[col_data, v_ap]].copy()
        tmp[v_ap] = pd.to_numeric(tmp[v_ap], errors="coerce").fillna(0)
        tmp[col_data] = pd.to_datetime(tmp[col_data], errors="coerce", dayfirst=True, format="mixed")
        a_pagar = tmp.groupby(pd.Grouper(key=col_data, freq="D"))[v_ap].sum().reset_index()
        a_pagar["tipo"] = "a pagar"
        a_pagar.rename(columns={col_data: "data", v_ap: "valor"}, inplace=True)
        linhas.append(a_pagar)
    if v_pg and dt_liq:
        tmp = df2[[dt_liq, v_pg]].copy()
        tmp[v_pg] = pd.to_numeric(tmp[v_pg], errors="coerce").fillna(0)
        tmp[dt_liq] = pd.to_datetime(tmp[dt_liq], errors="coerce", dayfirst=True, format="mixed")
        pago = tmp.groupby(pd.Grouper(key=dt_liq, freq="D"))[v_pg].sum().reset_index()
        pago["tipo"] = "pago"
        pago.rename(columns={dt_liq: "data", v_pg: "valor"}, inplace=True)
        linhas.append(pago)
    if len(linhas) == 0:
        return _fig_sem_dados("Contas a Pagar")
    base = pd.concat(linhas, ignore_index=True)
    fig = px.line(base, x="data", y="valor", color="tipo")
    return fig
 
if __name__ == "__main__":
    iniciar()

def _transformar_entidade(df, entidade):
    if entidade == "fretes":
        return _etl_fretes(df)
    if entidade == "coletas":
        return _etl_coletas(df)
    if entidade == "manifestos" or entidade == "manifestos_view":
        return _etl_manifestos(df)
    if entidade == "contas":
        return _etl_contas(df)
    if entidade == "cotacoes":
        return _etl_cotacoes(df)
    if entidade == "faturas":
        return _etl_faturas(df)
    if entidade == "localizacao":
        return _etl_localizacao(df)
    return df

def _etl_fretes(df):
    import pandas as pd
    d = df.copy()
    status_col = _col(d, ["status","Status"])
    if status_col in d.columns:
        s = d[status_col].astype(str).str.lower().str.strip()
        mapa = {
            "finished": "Concluído",
            "pending": "Pendente",
            "delivering": "Em Entrega",
            "cancelled": "Cancelado",
            "standby": "Espera",
            "in_transit": "Em Trânsito",
            "manifested": "Manifestado",
            "occurrence_treatment": "Tratamento de ocorrência",
        }
        d["Status_Nome"] = s.map(lambda x: mapa.get(x, d.get(status_col, s)))
    origem = _col(d, ["Origem","Cidade Origem","origin_city","cidade_origem","Origin"])
    uf_origem = _col(d, ["UF Origem","origem_uf","origin_state","estado_origem"])
    destino = _col(d, ["Destino","Cidade Destino","destination_city","cidade_destino","Destination"])
    uf_destino = _col(d, ["UF Destino","destino_uf","destination_state","estado_destino"])
    if origem and uf_origem and destino and uf_destino:
        d["Rota"] = d[origem].astype(str) + " (" + d[uf_origem].astype(str) + ") → " + d[destino].astype(str) + " (" + d[uf_destino].astype(str) + ")"
    modal = _col(d, ["modal","Modal","modality","service_mode"])
    if modal in d.columns:
        m = d[modal].astype(str).str.lower()
        d["Modal_PT"] = m.map(lambda x: "Rodoviário" if "rodo" in x else ("Aéreo" if "aere" in x else ("Marítimo" if "mari" in x else x)))
    return d

def _etl_coletas(df):
    import pandas as pd
    d = df.copy()
    status_col = _col(d, ["status","Status"])
    if status_col in d.columns:
        s = d[status_col].astype(str).str.lower().str.strip()
        d["Status_PT"] = s.map(lambda x: "Finalizada" if "finish" in x or "finaliz" in x else ("Pendente" if "pend" in x else ("Cancelada" if "cancel" in x else x)))
    fim = _col(d, ["Finalizacao","finish_date","service_end"])
    ag = _col(d, ["Agendamento","service_start"])
    if fim and ag:
        f = pd.to_datetime(d[fim], errors="coerce", dayfirst=True, format="mixed")
        a = pd.to_datetime(d[ag], errors="coerce", dayfirst=True, format="mixed")
        d["No_Prazo"] = pd.Series(["Sim" if (not pd.isna(fi) and not pd.isna(ai) and fi <= ai) else "Não" for fi, ai in zip(f, a)], index=d.index)
    return d

def _etl_manifestos(df):
    import pandas as pd
    d = df.copy()
    km = _col(d, ["Km manual","manual_km"])
    mdf = _col(d, ["Gerar MDF-e","mdf_e"])
    mon = _col(d, ["Solicitou Monitoramento","monitoring_requested"])
    for c in [km, mdf, mon]:
        if c:
            s = d[c].astype(str).str.strip().str.lower()
            d[c] = s.map(lambda x: True if x in ["1","true","sim","yes"] else (False if x in ["0","false","nao","não","no"] else None))
    receita = _col(d, ["invoices_value","freight_subtotal","Valor notas"])
    custo = _col(d, ["total_cost","operational_expenses_total","Custo Total"])
    if receita and custo:
        try:
            r = pd.to_numeric(d[receita], errors="coerce").fillna(0)
            c = pd.to_numeric(d[custo], errors="coerce").fillna(0)
            d["Rentabilidade"] = (r - c) / r.replace(0, pd.NA)
            kmviagem = _col(d, ["KM viagem","km_viagem"])
            if kmviagem:
                kv = pd.to_numeric(d[kmviagem], errors="coerce").fillna(0)
                d["Custo por KM"] = c / kv.replace(0, pd.NA)
            cap = _col(d, ["Carreta 1/Capacidade Peso","capacidade_peso"])
            peso = _col(d, ["Total peso taxado","taxed_weight","peso_notas"])
            if cap and peso:
                cp = pd.to_numeric(d[cap], errors="coerce").fillna(0)
                ps = pd.to_numeric(d[peso], errors="coerce").fillna(0)
                d["Utilizacao Frota"] = ps / cp.replace(0, pd.NA)
        except Exception:
            pass
    return d

def _etl_contas(df):
    import pandas as pd
    d = df.copy()
    cc = _col(d, ["Centro de Custo","Centro de custo/Nome"])
    forn = _col(d, ["Fornecedor/Nome","fornecedor"])
    if cc or forn:
        ccserie = d[cc] if cc else pd.Series([None]*len(d))
        fserie = d[forn] if forn else pd.Series([None]*len(d))
        d["Flag_Operacional"] = pd.Series([0 if (str(ccserie.iloc[i] or "").strip().upper() == "ADMINISTRATIVO GERAL" or str(fserie.iloc[i] or "").strip().upper() == "RODOGARCIA TRANSPORTES RODOVIARIOS LTDA") else 1 for i in range(len(d))], index=d.index)
    emis = _col(d, ["Emissão","issue_date"])
    cri = _col(d, ["Data criação","data_criacao"])
    base = emis or cri
    if base:
        b = pd.to_datetime(d[base], errors="coerce", dayfirst=True, format="mixed")
        d["Vencimento Estimado"] = b.fillna(pd.to_datetime(d[cri], errors="coerce", dayfirst=True, format="mixed")) + pd.to_timedelta(30, unit="D")
    return d

def _etl_cotacoes(df):
    import pandas as pd
    d = df.copy()
    cte = _col(d, ["CT-e / Data emissão","CT-e/Data de emissão"])
    if cte:
        dt = pd.to_datetime(d[cte], errors="coerce", dayfirst=True, format="mixed")
        d["Status_Cotacao"] = dt.notna().map(lambda x: "CONVERTIDA" if x else "PENDENTE")
    return d

def _etl_faturas(df):
    import pandas as pd
    d = df.copy()
    cte = _col(d, ["CT-e / Data emissão","CT-e/Data de emissão"])
    if cte in d.columns:
        dt = pd.to_datetime(d[cte], errors="coerce", dayfirst=True, format="mixed")
        d = d[dt.notna()]
    return d

def _etl_localizacao(df):
    return df

def _kpi_por_entidade(df, entidade):
    import pandas as pd
    total = len(df)
    if entidade == "fretes":
        vcol = _col(df, ["Valor Total do Servico","Valor Total do Serviço","Valor frete","valor_total","subtotal","valor_frete"])
        pcol = _col(df, ["Kg Taxado","taxed_weight","peso_notas","real_weight","Peso Taxado"])
        soma = float(pd.to_numeric(df[vcol], errors="coerce").fillna(0).sum()) if vcol else None
        media = (soma / total) if total > 0 and soma is not None else None
        return total, soma, media, _max_valor(df), _min_valor(df), _median_valor(df), _std_valor(df)
    if entidade == "contas":
        flag = _col(df, ["Flag_Operacional","flag_operacional"])
        base = df[df[flag] == 1] if flag in df.columns else df
        vcol = _col(base, ["Valor a pagar","valor_a_pagar"])
        soma = float(pd.to_numeric(base[vcol], errors="coerce").fillna(0).sum()) if vcol else None
        media = (soma / total) if total > 0 and soma is not None else None
        return total, soma, media, _max_valor(base), _min_valor(base), _median_valor(base), _std_valor(base)
    if entidade == "faturas":
        vcol = _col(df, ["Fatura / Valor","valor_fatura","Valor"])
        soma = float(pd.to_numeric(df[vcol], errors="coerce").fillna(0).sum()) if vcol else None
        media = (soma / total) if total > 0 and soma is not None else None
        return total, soma, media, _max_valor(df), _min_valor(df), _median_valor(df), _std_valor(df)
    if entidade == "manifestos" or entidade == "manifestos_view":
        vcol = _col(df, ["invoices_value","freight_subtotal","Valor notas"])
        soma = float(pd.to_numeric(df[vcol], errors="coerce").fillna(0).sum()) if vcol else None
        media = (soma / total) if total > 0 and soma is not None else None
        return total, soma, media, _max_valor(df), _min_valor(df), _median_valor(df), _std_valor(df)
    vcol = _col(df, ["valor_total","valor_fatura","valor_a_pagar","invoices_value","total_cost","paying_total","Valor","Valor NF","Valor notas"])
    soma = float(pd.to_numeric(df[vcol], errors="coerce").fillna(0).sum()) if vcol else None
    media = (soma / total) if total > 0 and soma is not None else None
    return total, soma, media, _max_valor(df), _min_valor(df), _median_valor(df), _std_valor(df)

def _executivo_kpis(inicio, fim, filiais):
    try:
        n_exec = 10000
        import pandas as pd
        try:
            df_fre = consultar_tabela(FRETES_TOP, {"n": n_exec})
        except Exception:
            df_fre = pd.DataFrame()
        try:
            df_fat = consultar_tabela(FATURAS_CLIENTE_TOP, {"n": n_exec})
        except Exception:
            df_fat = pd.DataFrame()
        try:
            df_man = consultar_tabela(MANIFESTOS_VIEW_TOP, {"n": n_exec})
        except Exception:
            df_man = pd.DataFrame()
        df_cap = consultar_tabela(CONTAS_PAGAR_TOP, {"n": n_exec})
        # filtros
        if filiais and isinstance(filiais, list) and len(filiais) > 0:
            for df_, ent in [(df_fre,"fretes"), (df_fat,"faturas"), (df_man,"manifestos"), (df_cap,"contas")]:
                col_filial = _col_strict(df_, ["filial","Filial","branch_nickname"])
                if col_filial:
                    df_[:] = df_[df_[col_filial].astype(str).isin([str(x) for x in filiais])]
        if inicio or fim:
            for df_, ent in [(df_fre,"fretes"), (df_fat,"faturas"), (df_man,"manifestos"), (df_cap,"contas")]:
                col_data = _col_date(df_, ent)
                if col_data and (col_data in df_.columns):
                    serie = _parse_datetime(df_[col_data])
                    if inicio:
                        df_[:] = df_[serie >= _parse_datetime_val(inicio)]
                    if fim:
                        df_[:] = df_[serie <= _parse_datetime_val(fim)]
        # Receita Total
        v_fre = _col_strict(df_fre, ["Valor Total do Servico","Valor Total do Serviço","valor_total","subtotal","Valor frete","valor_frete"])
        receita_fre = float(pd.to_numeric(df_fre[v_fre], errors="coerce").fillna(0).sum()) if v_fre else 0.0
        v_fat = _col_strict(df_fat, ["Fatura / Valor","valor_fatura","Valor"])
        receita_fat = float(pd.to_numeric(df_fat[v_fat], errors="coerce").fillna(0).sum()) if v_fat else 0.0
        receita_total = receita_fre + receita_fat
        # Custo Total
        c_man = _col_strict(df_man, ["total_cost","Custo Total"])
        custo_manifesto = float(pd.to_numeric(df_man[c_man], errors="coerce").fillna(0).sum()) if c_man else 0.0
        df_cap_op = _etl_contas(df_cap)
        flag = _col_strict(df_cap_op, ["Flag_Operacional","flag_operacional"])
        df_op = df_cap_op[df_cap_op[flag] == 1] if flag else df_cap_op
        v_cap_op = _col_strict(df_op, ["Valor a pagar","valor_a_pagar"])
        custo_cap = float(pd.to_numeric(df_op[v_cap_op], errors="coerce").fillna(0).sum()) if v_cap_op else 0.0
        custo_total = custo_manifesto + custo_cap
        lucro_bruto = receita_total - custo_total
        margem_bruta = (lucro_bruto / receita_total) if receita_total != 0 else None
        # Resultado Líquido (Visão de Caixa)
        df_cap_full = _etl_contas(df_cap.copy())
        flag2 = _col_strict(df_cap_full, ["Flag_Operacional","flag_operacional"])
        df_fixas = df_cap_full[df_cap_full[flag2] == 0] if flag2 else df_cap_full
        v_cap_fix = _col_strict(df_fixas, ["Valor a pagar","valor_a_pagar"])
        despesas_fixas = float(pd.to_numeric(df_fixas[v_cap_fix], errors="coerce").fillna(0).sum()) if v_cap_fix else 0.0
        resultado_liquido = receita_fat - (custo_manifesto + despesas_fixas)
        return _fmt_brl(receita_total), _fmt_brl(custo_total), _fmt_brl(lucro_bruto), _fmt_pct(margem_bruta), _fmt_brl(resultado_liquido)
    except Exception:
        vazio = _fmt_brl(None)
        return vazio, vazio, vazio, "—", vazio

@aplicacao.callback(
    dash.Output("card-basic-registros", "style"),
    dash.Output("card-basic-soma", "style"),
    dash.Output("card-basic-media", "style"),
    dash.Output("card-basic-max", "style"),
    dash.Output("card-basic-min", "style"),
    dash.Output("card-basic-mediana", "style"),
    dash.Output("card-basic-desvio", "style"),
    dash.Output("card-basic-data", "style"),
    dash.Output("card-basic-top", "style"),
    dash.Output("card-basic-menor", "style"),
    dash.Output("grid-exec", "style"),
    dash.Output("card-exec-receita", "style"),
    dash.Output("card-exec-custo", "style"),
    dash.Output("card-exec-lucro", "style"),
    dash.Output("card-exec-margem", "style"),
    dash.Output("card-exec-resultado", "style"),
    dash.Output("card-yoy", "style"),
    dash.Output("card-conversao", "style"),
    dash.Output("card-pontualidade", "style"),
    dash.Output("card-tempo-medio", "style"),
    dash.Output("card-rentabilidade", "style"),
    dash.Output("card-aberto", "style"),
    dash.Output("card-em-transito", "style"),
    dash.Output("card-principal-destino", "style"),
    dash.Input("seletor-entidade", "value")
)
def visibilidade_kpis(entidade):
    show = {}
    hide = {"display": "none"}
    visible_basic = set()
    visible_exec = set()
    if entidade == "contas":
        visible_basic = {"card-basic-registros","card-basic-soma","card-basic-media","card-basic-data","card-basic-top","card-basic-menor"}
        visible_exec = set()
    elif entidade == "faturas":
        visible_basic = {"card-basic-registros","card-basic-soma","card-basic-media","card-basic-data","card-basic-top","card-basic-menor"}
        visible_exec = {"card-aberto"}
    elif entidade == "coletas":
        visible_basic = {"card-basic-registros","card-basic-data"}
        visible_exec = {"card-pontualidade","card-tempo-medio"}
    elif entidade == "fretes":
        visible_basic = {"card-basic-registros","card-basic-soma","card-basic-media","card-basic-data","card-basic-top"}
        visible_exec = {"card-yoy"}
    elif entidade == "cotacoes":
        visible_basic = {"card-basic-registros","card-basic-soma","card-basic-media","card-basic-data"}
        visible_exec = {"card-conversao"}
    elif entidade == "localizacao":
        visible_basic = {"card-basic-registros","card-basic-data"}
        visible_exec = {"card-em-transito","card-principal-destino"}
    elif entidade == "manifestos_view":
        visible_basic = {"card-basic-registros","card-basic-soma","card-basic-media","card-basic-data","card-basic-top","card-basic-menor"}
        visible_exec = {"card-rentabilidade"}
    else:  # manifestos (tabela)
        visible_basic = {"card-basic-registros","card-basic-soma","card-basic-media","card-basic-data","card-basic-top","card-basic-menor"}
        visible_exec = {"card-exec-receita","card-exec-custo","card-exec-lucro","card-exec-margem","card-exec-resultado"}
    def st(id_):
        return {} if id_ in visible_basic or id_ in visible_exec else hide
    grid_exec_style = {} if len(visible_exec) > 0 else hide
    return (
        st("card-basic-registros"),
        st("card-basic-soma"),
        st("card-basic-media"),
        st("card-basic-max"),
        st("card-basic-min"),
        st("card-basic-mediana"),
        st("card-basic-desvio"),
        st("card-basic-data"),
        st("card-basic-top"),
        st("card-basic-menor"),
        grid_exec_style,
        st("card-exec-receita"),
        st("card-exec-custo"),
        st("card-exec-lucro"),
        st("card-exec-margem"),
        st("card-exec-resultado"),
        st("card-yoy"),
        st("card-conversao"),
        st("card-pontualidade"),
        st("card-tempo-medio"),
        st("card-rentabilidade"),
        st("card-aberto"),
        st("card-em-transito"),
        st("card-principal-destino"),
    )

def _kpis_entidade_extras(df, entidade):
    import pandas as pd
    try:
        if entidade == "fretes":
            v = _col_strict(df, ["Valor Total do Servico","Valor Total do Serviço","valor_total","subtotal","Valor frete","valor_frete"])
            dcol = _col_date(df, "fretes")
            if v and dcol and (dcol in df.columns):
                tmp = df[[dcol, v]].copy()
                tmp[v] = pd.to_numeric(tmp[v], errors="coerce").fillna(0)
                tmp[dcol] = pd.to_datetime(tmp[dcol], errors="coerce", dayfirst=True, format="mixed")
                por_ano = tmp.groupby(tmp[dcol].dt.year)[v].sum().sort_index()
                yoy = None
                if len(por_ano) >= 2:
                    atual = por_ano.iloc[-1]
                    anterior = por_ano.iloc[-2]
                    yoy = ((atual - anterior) / anterior) if anterior != 0 else None
                return _fmt_pct(yoy), "—", "—", "—", "—", "—", "—", "—"
        if entidade == "cotacoes":
            s = _col_strict(df, ["Status_Cotacao","status_conversao","conversion_status","status","Status"])
            if s and (s in df.columns):
                serie = df[s].astype(str).str.upper()
                tot = len(serie)
                conv = int((serie == "CONVERTIDA").sum())
                taxa = (conv / tot) if tot > 0 else None
                return "—", _fmt_pct(taxa), "—", "—", "—", "—", "—", "—"
        if entidade == "coletas":
            st = _col_strict(df, ["Status_PT","status","Status"])
            fim = _col_strict(df, ["Finalizacao","finish_date","service_end"])
            ini = _col_strict(df, ["Solicitacao","request_date","service_date"])
            noprazo = _col_strict(df, ["No_Prazo"])
            taxa = None
            if st and (st in df.columns):
                s = df[st].astype(str).str.lower()
                tot = len(s)
                concl = int(s.str.contains("finaliz|finish").sum())
                taxa = (concl / tot) if tot > 0 else None
            tempo_medio = None
            if fim and ini and (fim in df.columns) and (ini in df.columns):
                f = pd.to_datetime(df[fim], errors="coerce", dayfirst=True, format="mixed")
                i = pd.to_datetime(df[ini], errors="coerce", dayfirst=True, format="mixed")
                delta = (f - i).dt.total_seconds() / 3600
                tempo_medio = float(delta.dropna().mean()) if delta.notna().any() else None
            pont = None
            if noprazo and (noprazo in df.columns):
                s = df[noprazo].astype(str).str.strip().str.lower()
                tot = len(s)
                sim = int((s == "sim").sum())
                pont = (sim / tot) if tot > 0 else None
            return "—", "—", _fmt_pct(pont), f"{_fmt_num(tempo_medio) if tempo_medio is not None else '—'}", "—", "—", "—", "—"
        if entidade in ["manifestos","manifestos_view"]:
            rent = _col_strict(df, ["Rentabilidade"])
            if rent and (rent in df.columns):
                media = pd.to_numeric(df[rent], errors="coerce").mean()
                return "—", "—", "—", "—", _fmt_pct(media), "—", "—", "—"
        if entidade == "faturas":
            val = _col_strict(df, ["Fatura / Valor","valor_fatura","Valor"])
            baixa = _col_strict(df, ["Fatura/Baixa","Baixa","data_liquidacao"])
            aberto = None
            if val and (val in df.columns):
                v = pd.to_numeric(df[val], errors="coerce").fillna(0)
                if baixa and (baixa in df.columns):
                    b = df[baixa]
                    aberto = float(v[b.isna()].sum())
                else:
                    aberto = None
            return "—", "—", "—", "—", "—", _fmt_brl(aberto), "—", "—"
        if entidade == "localizacao":
            st = _col_strict(df, ["Status Carga","status_carga","status","Status"])
            emt = None
            if st and (st in df.columns):
                s = df[st].astype(str).str.lower()
                emt = int(s.str.contains("in_transfer|transfer|transit").sum())
            reg = _col_strict(df, ["Região Destino","regiao_destino","destination_region","region_destino"])
            if not reg:
                reg = _col_strict(df, ["Cidade Destino","cidade_destino","destination_city","Destination"])
            pdest = "—"
            if reg:
                grp = df[reg].dropna().astype(str).value_counts()
                if len(grp) > 0:
                    pdest = str(grp.index[0])
            return "—", "—", "—", "—", "—", "—", f"{emt or 0}", pdest
        return "—", "—", "—", "—", "—", "—", "0", "—"
    except Exception:
        return "—", "—", "—", "—", "—", "—", "0", "—"
