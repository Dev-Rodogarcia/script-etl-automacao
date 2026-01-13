import re
import pandas as pd
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

_ultimo_erro = None
_driver_usado = None

def criar_engine():
    from sqlalchemy import create_engine
    from sqlalchemy.engine import URL
    global _ultimo_erro, _driver_usado
    raw_url = obter_db_url()
    host, port, banco = _parse_jdbc(raw_url)
    usuario = obter_db_usuario()
    senha = obter_db_senha()
    preferido = obter_odbc_driver()
    candidatos = [
        preferido,
        "ODBC Driver 18 for SQL Server",
        "ODBC Driver 17 for SQL Server",
        "SQL Server Native Client 11.0",
        "SQL Server Native Client 10.0",
        "SQL Server",
    ]
    ultima_excecao = None
    usar_encrypt = "encrypt=false" not in str(raw_url).lower()
    for drv in candidatos:
        try:
            enc = "yes" if usar_encrypt else "no"
            legacy = drv in ["SQL Server", "SQL Server Native Client 11.0", "SQL Server Native Client 10.0"]
            if legacy:
                odbc = f"DRIVER={{{drv}}};SERVER={host},{port};DATABASE={banco};UID={usuario};PWD={senha};Connection Timeout=5"
            else:
                odbc = f"DRIVER={{{drv}}};SERVER={host},{port};DATABASE={banco};UID={usuario};PWD={senha};Encrypt={enc};TrustServerCertificate=yes;Connection Timeout=5"
            url = URL.create("mssql+pyodbc", query={"odbc_connect": odbc})
            eng = create_engine(url, pool_pre_ping=True, pool_recycle=1800)
            with eng.connect() as conn:
                conn.exec_driver_sql("SELECT 1")
            _driver_usado = drv
            _ultimo_erro = None
            return eng
        except Exception as e:
            ultima_excecao = e
            _ultimo_erro = e
            continue
    raise RuntimeError(f"Falha ao conectar ao SQL Server via ODBC: {ultima_excecao}")

engine = None
try:
    engine = criar_engine()
except Exception:
    engine = None

def consultar_tabela(sql, parametros=None):
    if engine is None:
        raise RuntimeError("Banco indisponível")
    from sqlalchemy import text
    with engine.connect() as conexao:
        return pd.read_sql_query(text(sql), conexao, params=parametros)

def banco_ping():
    global _ultimo_erro
    try:
        if engine is None:
            return False
        with engine.connect() as conn:
            conn.exec_driver_sql("SELECT 1")
        _ultimo_erro = None
        return True
    except Exception as e:
        _ultimo_erro = e
        return False

def banco_erro():
    try:
        return str(_ultimo_erro) if _ultimo_erro else ""
    except Exception:
        return ""

def banco_driver():
    try:
        return str(_driver_usado) if _driver_usado else ""
    except Exception:
        return ""
