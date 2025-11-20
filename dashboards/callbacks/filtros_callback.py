"""
Callbacks de Filtros
====================
Gerencia interações com filtros de data e filiais.
"""

from dash import Input, Output, State, no_update, callback_context
import datetime as dt
from data.loader import obter_filiais_disponiveis


def registrar_callbacks_filtros(app):
    """
    Registra todos os callbacks relacionados a filtros.
    
    Args:
        app: Instância do Dash
    """
    
    # ========== CALLBACK: Carregar filiais disponíveis ==========
    @app.callback(
        Output("seletor-filial", "options"),
        Input("url", "pathname"),
        Input("busca-filial", "value"),
        prevent_initial_call=False
    )
    def carregar_e_filtrar_filiais(pathname, busca):
        filiais = obter_filiais_disponiveis()
        if not filiais:
            return []
        if busca:
            termo = str(busca).strip().lower()
            filiais = [f for f in filiais if termo in f.lower()]
        opcoes = [{"label": f, "value": f} for f in filiais]
        return opcoes

    

    @app.callback(
        Output("contador-filiais", "children"),
        Input("seletor-filial", "value"),
        prevent_initial_call=False
    )
    def contar_filiais(valor):
        if not valor:
            return "• todas as filiais"
        return f"• {len(valor)} selecionada(s)"
    
    
    # ========== CALLBACK: Botão "Selecionar Todas" ==========
    @app.callback(
        Output("seletor-filial", "value", allow_duplicate=True),
        Input("btn-selecionar-todas", "n_clicks"),
        State("seletor-filial", "options"),
        prevent_initial_call=True
    )
    def selecionar_todas_filiais(n_clicks, opcoes):
        """
        Seleciona todas as filiais disponíveis.
        """
        if not n_clicks:
            return no_update
        
        # Seleciona todas as filiais
        return [opcao["value"] for opcao in opcoes]
    
    
    # ========== CALLBACK: Botão "Limpar" ==========
    @app.callback(
        Output("seletor-filial", "value", allow_duplicate=True),
        Input("btn-limpar-filiais", "n_clicks"),
        prevent_initial_call=True
    )
    def limpar_selecao_filiais(n_clicks):
        """
        Limpa a seleção de filiais (volta para mostrar todas).
        """
        if not n_clicks:
            return no_update
        
        # Retorna lista vazia = mostrar todas
        return []
    
    
    # ========== CALLBACK: Atalhos de datas ==========
    @app.callback(
        Output("intervalo-datas", "start_date"),
        Output("intervalo-datas", "end_date"),
        Input("btn-hoje", "n_clicks"),
        Input("btn-7dias", "n_clicks"),
        Input("btn-30dias", "n_clicks"),
        Input("btn-mes-atual", "n_clicks"),
        prevent_initial_call=True
    )
    def atalhos_datas(hoje, sete_dias, trinta_dias, mes_atual):
        """
        Aplica atalhos de período (hoje, 7 dias, 30 dias, mês atual).
        """
        ctx = callback_context
        
        if not ctx.triggered:
            return no_update, no_update
        
        botao_clicado = ctx.triggered[0]["prop_id"].split(".")[0]
        
        data_fim = dt.date.today()
        
        if botao_clicado == "btn-hoje":
            data_inicio = data_fim
        elif botao_clicado == "btn-7dias":
            data_inicio = data_fim - dt.timedelta(days=7)
        elif botao_clicado == "btn-30dias":
            data_inicio = data_fim - dt.timedelta(days=30)
        elif botao_clicado == "btn-mes-atual":
            # Primeiro dia do mês atual
            data_inicio = data_fim.replace(day=1)
        else:
            return no_update, no_update
        
        return data_inicio, data_fim