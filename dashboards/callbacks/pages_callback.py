from dash import Input, Output


def registrar_callbacks_paginas(app):
    @app.callback(Output("titulo-pagina","children"), Input("url","pathname"))
    def atualizar_titulo(pathname):
        if pathname in ("/", "/visao-geral"):
            return "Visão Geral"
        if pathname == "/coletas":
            return "Coletas"
        if pathname == "/fretes":
            return "Fretes"
        if pathname == "/cotacoes":
            return "Cotações"
        if pathname == "/apagar":
            return "Contas a Pagar"
        if pathname == "/localizacao":
            return "Localização de Cargas"
        if pathname == "/manifestos":
            return "Manifestos"
        if pathname == "/faturas":
            return "Faturas por Cliente"
        if pathname == "/ocorrencias":
            return "Ocorrências"
        return ""