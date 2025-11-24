from dotenv import load_dotenv
import os

def carregar_variaveis():
    load_dotenv()

def obter_variavel(nome, obrigatoria=True, padrao=None):
    valor = os.getenv(nome) if os.getenv(nome) is not None else padrao
    if obrigatoria and (valor is None or str(valor).strip() == ""):
        raise RuntimeError(f"Variável ausente: {nome}")
    return valor

def obter_db_url():
    return obter_variavel("DB_URL")

def obter_db_usuario():
    return obter_variavel("DB_USER")

def obter_db_senha():
    return obter_variavel("DB_PASSWORD")

def obter_odbc_driver():
    return os.getenv("ODBC_DRIVER", "ODBC Driver 17 for SQL Server")
