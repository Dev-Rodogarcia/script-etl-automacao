from dash import html
import dash_bootstrap_components as dbc


def criar_navbar():
    return dbc.NavbarSimple(
        brand=html.Img(src="/assets/logo.png", className="navbar-logo"),
        color="primary",
        dark=True,
        className="mb-3"
    )