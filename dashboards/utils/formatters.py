"""
Formatadores de Valores
========================
Funções para formatar valores monetários, números e datas.
"""


def formatar_moeda(valor, simbolo="R$") -> str:
    """
    Formata valor monetário no padrão brasileiro.
    
    Args:
        valor: Valor numérico a ser formatado
        simbolo: Símbolo da moeda (padrão: R$)
        
    Returns:
        String formatada (ex: "R$ 1.234,56")
    """
    try:
        valor_float = float(valor)
        
        # Formata com separadores brasileiros
        valor_formatado = f"{valor_float:,.2f}".replace(",", "X").replace(".", ",").replace("X", ".")
        
        # Adiciona símbolo
        if valor_float < 0:
            return f"-{simbolo} {valor_formatado[1:]}"
        return f"{simbolo} {valor_formatado}"
        
    except (ValueError, TypeError):
        return f"{simbolo} 0,00"


def formatar_numero(valor, casas_decimais=0) -> str:
    """
    Formata número inteiro ou decimal.
    
    Args:
        valor: Valor numérico
        casas_decimais: Número de casas decimais (padrão: 0)
        
    Returns:
        String formatada (ex: "1.234" ou "1.234,56")
    """
    try:
        valor_float = float(valor)
        
        if casas_decimais == 0:
            valor_formatado = f"{int(valor_float):,}".replace(",", ".")
        else:
            formato = f"{{:,.{casas_decimais}f}}"
            valor_formatado = formato.format(valor_float).replace(",", "X").replace(".", ",").replace("X", ".")
        
        return valor_formatado
        
    except (ValueError, TypeError):
        return "0"


def formatar_percentual(valor, casas_decimais=1) -> str:
    """
    Formata valor percentual.
    
    Args:
        valor: Valor decimal (ex: 0.1234 para 12.34%)
        casas_decimais: Casas decimais no resultado
        
    Returns:
        String formatada (ex: "12,3%")
    """
    try:
        valor_float = float(valor) * 100
        formato = f"{{:,.{casas_decimais}f}}"
        valor_formatado = formato.format(valor_float).replace(".", ",")
        return f"{valor_formatado}%"
        
    except (ValueError, TypeError):
        return "0,0%"


def formatar_data(data, formato="%d/%m/%Y") -> str:
    """
    Formata data para exibição.
    
    Args:
        data: Objeto datetime ou string
        formato: Formato desejado (padrão: DD/MM/YYYY)
        
    Returns:
        String formatada ou valor original se erro
    """
    try:
        import pandas as pd
        
        if pd.isna(data):
            return "—"
        
        if isinstance(data, str):
            data = pd.to_datetime(data)
        
        return data.strftime(formato)
        
    except Exception:
        return str(data) if data else "—"


def formatar_status(status: str) -> str:
    """
    Formata status com badge colorido para HTML.
    
    Args:
        status: Status a ser formatado
        
    Returns:
        HTML string com badge colorido
    """
    cores = {
        "Pago": "success",
        "Pendente": "warning",
        "Vencido": "danger",
        "Cancelado": "secondary",
        "Em Andamento": "info",
        "Concluído": "success",
    }
    
    cor = cores.get(status, "secondary")
    return f'<span class="badge bg-{cor}">{status}</span>'


def abreviar_numero(valor) -> str:
    """
    Abrevia números grandes (ex: 1.234.567 -> 1,23 M).
    
    Args:
        valor: Valor numérico
        
    Returns:
        String abreviada
    """
    try:
        valor_float = float(valor)
        
        if abs(valor_float) >= 1_000_000_000:
            return f"{valor_float / 1_000_000_000:.2f}B".replace(".", ",")
        elif abs(valor_float) >= 1_000_000:
            return f"{valor_float / 1_000_000:.2f}M".replace(".", ",")
        elif abs(valor_float) >= 1_000:
            return f"{valor_float / 1_000:.2f}K".replace(".", ",")
        else:
            return formatar_numero(valor_float, casas_decimais=2)
            
    except (ValueError, TypeError):
        return "0"