"""
Página: Visão Geral
====================
Dashboard principal com KPIs financeiros e comparativos.
"""

from dash import Input, Output, html
import dash_bootstrap_components as dbc
from dash import dash_table
import plotly.graph_objects as go
import plotly.express as px

from data.loader import carregar_tabela
from data.processors import (
    aplicar_filtros,
    calcular_kpis_financeiros,
    agregar_por_filial,
    agregar_por_status,
    preparar_dados_tabela
)
from utils.formatters import formatar_moeda, formatar_numero


def registrar_callback_visao_geral(app):
    """
    Registra o callback da página de Visão Geral.
    
    Args:
        app: Instância do Dash
    """
    
    @app.callback(
        Output("kpi-total-a-pagar", "children"),
        Output("kpi-total-a-receber", "children"),
        Output("kpi-saldo-projetado", "children"),
        Output("kpi-titulos-em-atraso", "children"),
        Output("grafico-pagar-receber", "figure"),
        Output("grafico-composicao", "figure"),
        Output("tabela-detalhes", "data"),
        Output("tabela-detalhes", "columns"),
        Input("intervalo-datas", "start_date"),
        Input("intervalo-datas", "end_date"),
        Input("seletor-filial", "value"),
        Input("url", "pathname")
    )
    def atualizar_visao_geral(data_inicial, data_final, filiais_selecionadas, pathname):
        """
        Atualiza todos os componentes da Visão Geral.
        
        LÓGICA DE FILIAIS:
        - filiais_selecionadas = [] ou None: MOSTRA TODAS
        - filiais_selecionadas = ["Filial A", "Filial B"]: MOSTRA APENAS ESSAS
        """
        if pathname == "/" or pathname == "/visao-geral":
            pass
        elif pathname == "/ocorrencias":
            try:
                df_occ = carregar_tabela("ocorrencias")
                df_occ_f = aplicar_filtros(
                    df_occ,
                    "ocorrencias",
                    data_inicial,
                    data_final,
                    filiais_selecionadas
                )
                if df_occ_f.empty:
                    fig_bar = go.Figure()
                    fig_bar.add_annotation(text="Sem dados disponíveis", showarrow=False, font={"size": 14, "color": "#999"})
                    fig_bar.update_layout(height=400)
                    fig_pie = go.Figure()
                    fig_pie.add_annotation(text="Sem dados disponíveis", showarrow=False, font={"size": 14, "color": "#999"})
                    fig_pie.update_layout(height=400)
                    return "—", "—", "—", "—", fig_bar, fig_pie, [], []
                s = df_occ_f.copy()
                if "occurrence_at" in s.columns:
                    s["_dia"] = px.to_datetime(s["occurrence_at"], errors="coerce").dt.date
                    serie = s.groupby("_dia").size().reset_index(name="qtd")
                    fig_bar = px.bar(serie, x="_dia", y="qtd", title="Ocorrências por dia", template="plotly_white")
                    fig_bar.update_layout(height=400)
                else:
                    fig_bar = go.Figure()
                if "occurrence_description" in s.columns:
                    comp = s.groupby("occurrence_description").size().reset_index(name="qtd").sort_values("qtd", ascending=False).head(10)
                    fig_pie = px.pie(comp, names="occurrence_description", values="qtd", title="Top ocorrências", hole=0.4, template="plotly_white")
                    fig_pie.update_layout(height=400)
                else:
                    fig_pie = go.Figure()
                df_tab = preparar_dados_tabela(df_occ_f, max_colunas=10)
                if not df_tab.empty:
                    cols = [{"name": c, "id": c} for c in df_tab.columns]
                    data = df_tab.head(50).to_dict("records")
                else:
                    cols, data = [], []
                return "—", "—", "—", "—", fig_bar, fig_pie, data, cols
            except Exception as e:
                print(f"❌ Erro ao atualizar Ocorrências: {e}")
                return "Erro", "Erro", "Erro", "Erro", go.Figure(), go.Figure(), [], []
        else:
            return "—", "—", "—", "—", go.Figure(), go.Figure(), [], []
        
        try:
            df_pagar = carregar_tabela("contas_a_pagar")
            df_receber = carregar_tabela("faturas_por_cliente_data_export")
            
            # ========== APLICAR FILTROS ==========
            df_pagar_filtrado = aplicar_filtros(
                df_pagar,
                "contas_a_pagar",
                data_inicial,
                data_final,
                filiais_selecionadas
            )
            
            df_receber_filtrado = aplicar_filtros(
                df_receber,
                "faturas_por_cliente_data_export",
                data_inicial,
                data_final,
                filiais_selecionadas
            )
            
            # ========== CALCULAR KPIs ==========
            kpis = calcular_kpis_financeiros(df_pagar_filtrado, df_receber_filtrado)
            
            # ========== CRIAR GRÁFICO DE BARRAS (A Pagar vs A Receber) ==========
            fig_barras = go.Figure(data=[
                go.Bar(
                    name="A Pagar",
                    x=["Contas a Pagar"],
                    y=[kpis["total_pagar"]],
                    marker_color="#dc3545",
                    text=[formatar_moeda(kpis["total_pagar"])],
                    textposition="outside"
                ),
                go.Bar(
                    name="A Receber",
                    x=["Contas a Receber"],
                    y=[kpis["total_receber"]],
                    marker_color="#28a745",
                    text=[formatar_moeda(kpis["total_receber"])],
                    textposition="outside"
                )
            ])
            
            fig_barras.update_layout(
                title="Contas a Pagar vs Contas a Receber",
                template="plotly_white",
                height=400,
                showlegend=False
            )
            
            # ========== CRIAR GRÁFICO DE PIZZA (Composição por Status) ==========
            df_status = agregar_por_status(df_pagar_filtrado, "contas_a_pagar")
            
            if not df_status.empty and df_status["valor_total"].sum() > 0:
                fig_pizza = px.pie(
                    df_status,
                    names="status",
                    values="valor_total",
                    title="Composição por Status (Contas a Pagar)",
                    hole=0.4,
                    template="plotly_white"
                )
                fig_pizza.update_traces(textposition="inside", textinfo="percent+label")
            else:
                fig_pizza = go.Figure()
                fig_pizza.add_annotation(
                    text="Sem dados disponíveis",
                    showarrow=False,
                    font={"size": 14, "color": "#999"}
                )
            
            fig_pizza.update_layout(height=400)
            
            # ========== PREPARAR TABELA ==========
            df_tabela = preparar_dados_tabela(df_pagar_filtrado, max_colunas=10)
            
            if not df_tabela.empty:
                colunas_tabela = [{"name": col, "id": col} for col in df_tabela.columns]
                dados_tabela = df_tabela.head(50).to_dict("records")  # Limita a 50 linhas
            else:
                colunas_tabela = []
                dados_tabela = []
            
            # ========== RETORNAR VALORES ==========
            return (
                formatar_moeda(kpis["total_pagar"]),
                formatar_moeda(kpis["total_receber"]),
                formatar_moeda(kpis["saldo_projetado"]),
                formatar_numero(kpis["titulos_atraso"]),
                fig_barras,
                fig_pizza,
                dados_tabela,
                colunas_tabela
            )
            
        except Exception as e:
            print(f"❌ Erro ao atualizar Visão Geral: {e}")
            return "Erro", "Erro", "Erro", "Erro", go.Figure(), go.Figure(), [], []