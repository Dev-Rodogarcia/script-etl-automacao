import re
import pandas as pd
from sqlalchemy import create_engine, text
from sqlalchemy.engine import URL
try:
    from .ambiente import carregar_variaveis, obter_db_url, obter_db_usuario, obter_db_senha, obter_odbc_driver
except ImportError:
    from ambiente import carregar_variaveis, obter_db_url, obter_db_usuario, obter_db_senha, obter_odbc_driver

carregar_variaveis()

def _parse_jdbc(url):
    m = re.match(r"jdbc:sqlserver://([^:;]+)(?::(\d+))?;databaseName=([^;]+)", url)
    if not m:
        raise ValueError("DB_URL inválido")
    host, port, banco = m.group(1), m.group(2) or "1433", m.group(3)
    return host, port, banco

def criar_engine():
    host, port, banco = _parse_jdbc(obter_db_url())
    driver = obter_odbc_driver()
    usuario = obter_db_usuario()
    senha = obter_db_senha()
    odbc = f"DRIVER={{{driver}}};SERVER={host},{port};DATABASE={banco};UID={usuario};PWD={senha};Encrypt=yes;TrustServerCertificate=yes"
    url = URL.create("mssql+pyodbc", query={"odbc_connect": odbc})
    return create_engine(url, pool_pre_ping=True, pool_recycle=1800)

engine = criar_engine()

def consultar_tabela(sql, parametros=None):
    with engine.connect() as conexao:
        return pd.read_sql_query(text(sql), conexao, params=parametros)
