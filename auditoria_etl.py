#!/usr/bin/env python3
from __future__ import annotations

# cd C:\Users\suporte\Documents\projetos\etl-extracao-dados
# python auditoria_etl.py --start 2026-05-29 --end 2026-05-30

import argparse
import datetime as dt
import hashlib
import json
import os
import re
import time
import warnings
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Iterable

import pandas as pd
import pyodbc
import requests
from dotenv import load_dotenv


warnings.filterwarnings("ignore", category=UserWarning, message=".*pandas only supports SQLAlchemy.*")

RETRYABLE_STATUS = {429, 500, 502, 503, 504}


GRAPHQL_QUERY_FRETES = """
query BuscarFretes_Master_V8($params: FreightInput!, $after: String) {
  freight(params: $params, after: $after, first: 100) {
    edges { node {
      id accountingCreditId accountingCreditInstallmentId referenceNumber
      serviceAt createdAt finishedAt total subtotal invoicesValue invoicesWeight
      taxedWeight realWeight cubagesCubedWeight totalCubicVolume invoicesTotalVolumes
      modal modalCte status courtesy type serviceDate serviceType deliveryPredictionDate
      corporationSequenceNumber destinationCityId corporationId
    } }
    pageInfo { hasNextPage endCursor }
  }
}
""".strip()


GRAPHQL_QUERY_COLETAS = """
query BuscarColetasExpandidaV2($params: PickInput!, $after: String) {
  pick(params: $params, after: $after, first: 100) {
    edges { node {
      id status requestDate serviceDate sequenceCode requestHour serviceStartHour
      finishDate serviceEndHour requester comments agentId manifestItemPickId
      vehicleTypeId cargoClassificationId costCenterId pickTypeId pickupLocationId
    } }
    pageInfo { hasNextPage endCursor }
  }
}
""".strip()


GRAPHQL_QUERY_USUARIOS = """
query ExtrairUsuariosSistema($params: IndividualInput!, $after: String) {
  individual(params: $params, first: 1000, after: $after) {
    edges { node { id name } }
    pageInfo { hasNextPage endCursor }
  }
}
""".strip()


GRAPHQL_INTROSPECTION_PICK_INPUT = """
query CamposPickInput {
  __type(name: "PickInput") {
    inputFields { name }
  }
}
""".strip()


def env_first(*names: str, default: str | None = None) -> str | None:
    for name in names:
        value = os.getenv(name)
        if value is not None and value.strip() != "":
            return value.strip()
    return default


def required_env(*names: str) -> str:
    value = env_first(*names)
    if value is None:
        joined = " or ".join(names)
        raise RuntimeError(f"Missing required environment variable: {joined}")
    return value


def parse_bool(value: str | None, default: bool = False) -> bool:
    if value is None or value.strip() == "":
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "sim", "s"}


def date_range(start: dt.date, end: dt.date) -> Iterable[dt.date]:
    cursor = start
    while cursor <= end:
        yield cursor
        cursor += dt.timedelta(days=1)


def as_date(value: str) -> dt.date:
    return dt.date.fromisoformat(value)


def start_dt(start: dt.date) -> str:
    return f"{start.isoformat()}T00:00:00"


def end_dt_exclusive(end: dt.date) -> str:
    return f"{(end + dt.timedelta(days=1)).isoformat()}T00:00:00"


def params_date2(start: dt.date, end: dt.date) -> list[Any]:
    return [start.isoformat(), end.isoformat()]


def params_datetime2(start: dt.date, end: dt.date) -> list[Any]:
    return [start_dt(start), end_dt_exclusive(end)]


def params_datetime2_date2(start: dt.date, end: dt.date) -> list[Any]:
    return [start_dt(start), end_dt_exclusive(end), start.isoformat(), end.isoformat()]


def params_date2_datetime2(start: dt.date, end: dt.date) -> list[Any]:
    return [start.isoformat(), end.isoformat(), start_dt(start), end_dt_exclusive(end)]


def clean_id(value: Any) -> str | None:
    if value is None:
        return None
    try:
        if pd.isna(value):
            return None
    except TypeError:
        pass
    if isinstance(value, float) and value.is_integer():
        value = int(value)
    text = str(value).strip()
    if text == "" or text.lower() in {"nan", "none", "null"}:
        return None
    if re.fullmatch(r"-?\d+\.0", text):
        return text[:-2]
    return text


def jstr(value: Any) -> str:
    cleaned = clean_id(value)
    return "null" if cleaned is None else cleaned


def compact_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), default=str)


def sha256_hex(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def first_present(row: dict[str, Any], *names: str) -> Any:
    for name in names:
        if name not in row:
            continue
        value = row[name]
        if value is None:
            continue
        try:
            if pd.isna(value):
                continue
        except (TypeError, ValueError):
            pass
        return value
    return None


def key_simple(*fields: str) -> Callable[[dict[str, Any]], str | None]:
    def resolver(row: dict[str, Any]) -> str | None:
        if len(fields) == 1:
            return clean_id(row.get(fields[0]))
        parts = [clean_id(row.get(field)) for field in fields]
        if any(part is None for part in parts):
            return None
        return "|".join(parts)  # type: ignore[arg-type]

    return resolver


def key_manifestos(row: dict[str, Any]) -> str | None:
    sequence = clean_id(row.get("sequence_code"))
    if sequence is None:
        return None
    pick = clean_id(row.get("mft_pfs_pck_sequence_code")) or "-1"
    mdfe = clean_id(row.get("mft_mfs_number")) or "-1"
    return f"{sequence}|{pick}|{mdfe}"


def key_localizacao(row: dict[str, Any]) -> str | None:
    return clean_id(first_present(row, "corporation_sequence_number", "sequence_number"))


def key_contas_a_pagar(row: dict[str, Any]) -> str | None:
    return clean_id(first_present(row, "ant_ils_sequence_code", "sequence_code"))


def normalizar_texto(value: Any) -> str:
    if value is None:
        return "<null>"
    try:
        if pd.isna(value):
            return "<null>"
    except (TypeError, ValueError):
        pass
    text = str(value).strip()
    return "<empty>" if text == "" else text


def normalizar_lista(value: Any) -> str:
    if value is None:
        return "<null>"
    try:
        if pd.isna(value):
            return "<null>"
    except (TypeError, ValueError):
        pass
    if not isinstance(value, list):
        return normalizar_texto(value)
    if not value:
        return "<null>"
    normalized = [normalizar_texto(item) for item in value]
    normalized = [item for item in normalized if item not in {"<null>", "<empty>"}]
    if not normalized:
        return "<empty>"
    return ",".join(sorted(normalized))


def append_campo(parts: list[str], name: str, value: Any) -> None:
    parts.append(f"{name}={normalizar_texto(value)}|")


def key_faturas_por_cliente(row: dict[str, Any]) -> str | None:
    nfse = clean_id(first_present(row, "fit_nse_number", "nfse_number"))
    cte_number = clean_id(row.get("fit_fhe_cte_number"))
    document = clean_id(row.get("fit_ant_document"))
    billing_id = clean_id(row.get("billingId"))
    parts: list[str] = []

    if nfse is not None:
        append_campo(parts, "identitySource", "nfse")
        append_campo(parts, "nfseNumber", nfse)
        append_campo(parts, "pagadorDocumento", row.get("fit_pyr_document"))
        append_campo(parts, "remetenteDocumento", row.get("fit_rpt_document"))
        append_campo(parts, "destinatarioDocumento", row.get("fit_sdr_document"))
        return "FPC-HASH-" + sha256_hex("".join(parts))

    if cte_number is not None:
        append_campo(parts, "identitySource", "cte")
        append_campo(parts, "cteNumber", cte_number)
        append_campo(parts, "pagadorDocumento", row.get("fit_pyr_document"))
        append_campo(parts, "remetenteDocumento", row.get("fit_rpt_document"))
        append_campo(parts, "destinatarioDocumento", row.get("fit_sdr_document"))
        return "FPC-HASH-" + sha256_hex("".join(parts))

    if document is not None:
        append_campo(parts, "identitySource", "fatura")
        append_campo(parts, "document", document)
        append_campo(parts, "issueDate", row.get("fit_ant_issue_date"))
        append_campo(parts, "pagadorDocumento", row.get("fit_pyr_document"))
        append_campo(parts, "destinatarioDocumento", row.get("fit_sdr_document"))
        return "FPC-HASH-" + sha256_hex("".join(parts))

    if billing_id is not None:
        append_campo(parts, "identitySource", "billing")
        append_campo(parts, "billingId", billing_id)
        append_campo(parts, "pagadorDocumento", row.get("fit_pyr_document"))
        append_campo(parts, "destinatarioDocumento", row.get("fit_sdr_document"))
        return "FPC-HASH-" + sha256_hex("".join(parts))

    append_campo(parts, "identitySource", "fallback")
    append_campo(parts, "pagadorDocumento", row.get("fit_pyr_document"))
    append_campo(parts, "remetenteDocumento", row.get("fit_rpt_document"))
    append_campo(parts, "destinatarioDocumento", row.get("fit_sdr_document"))
    append_campo(parts, "notasFiscais", normalizar_lista(row.get("invoices_mapping")))
    append_campo(parts, "pedidosCliente", normalizar_lista(row.get("fit_fte_invoices_order_number")))
    append_campo(parts, "valorFrete", row.get("total"))
    append_campo(parts, "valorFatura", row.get("fit_ant_value"))
    return "FPC-HASH-" + sha256_hex("".join(parts))


def key_inventario(row: dict[str, Any]) -> str | None:
    if clean_id(row.get("sequence_code")) is None:
        return None
    invoices_mapping = row.get("cnr_c_s_fit_invoices_mapping")
    invoices_mapping_json = "" if invoices_mapping is None else compact_json(invoices_mapping)
    canonical = "|".join(
        [
            jstr(row.get("sequence_code")),
            jstr(row.get("cnr_c_s_fit_corporation_sequence_number")),
            invoices_mapping_json,
            "" if row.get("started_at") is None else str(row.get("started_at")),
        ]
    )
    metadata = compact_json(row)
    return sha256_hex(metadata if canonical.strip() == "" else canonical)


def key_sinistros(row: dict[str, Any]) -> str | None:
    if clean_id(row.get("sequence_code")) is None:
        return None
    canonical = "|".join(
        [
            jstr(row.get("sequence_code")),
            jstr(row.get("icm_fis_ioe_number")),
            jstr(row.get("icm_fis_fit_corporation_sequence_number")),
        ]
    )
    metadata = compact_json(row)
    return sha256_hex(metadata if canonical == "null|null|null" else canonical)


def key_raster_viagem(row: dict[str, Any]) -> str | None:
    return clean_id(first_present(row, "CodSolicitacao", "codSolicitacao"))


def key_raster_parada(row: dict[str, Any]) -> str | None:
    cod = clean_id(row.get("_cod_solicitacao"))
    ordem = clean_id(first_present(row, "Ordem", "ordem", "_ordem"))
    if cod is None or ordem is None:
        return None
    return f"{cod}|{ordem}"


@dataclass(frozen=True)
class DataExportConfig:
    template_id: int
    api_table: str
    date_field: str
    per: int
    order_by: str
    nested: bool = False
    segment_by_day: bool = False
    extra_segment_field: str | None = None
    timeout: int = 120


@dataclass(frozen=True)
class GraphQLConfig:
    query: str
    entity: str
    first: int = 100


@dataclass(frozen=True)
class EntityConfig:
    name: str
    api_kind: str
    table: str
    api_key: Callable[[dict[str, Any]], str | None]
    db_key_sql: str
    db_where_sql: str
    db_params: Callable[[dt.date, dt.date], list[Any]]
    db_not_null_sql: str
    dataexport: DataExportConfig | None = None
    graphql: GraphQLConfig | None = None


ENTITIES: list[EntityConfig] = [
    EntityConfig(
        name="fretes",
        api_kind="graphql",
        table="dbo.fretes",
        api_key=key_simple("id"),
        db_key_sql="CAST(id AS varchar(50))",
        db_where_sql="COALESCE(service_date, CONVERT(date, servico_em)) BETWEEN ? AND ?",
        db_params=params_date2,
        db_not_null_sql="id IS NOT NULL",
        graphql=GraphQLConfig(GRAPHQL_QUERY_FRETES, "freight"),
    ),
    EntityConfig(
        name="coletas",
        api_kind="graphql",
        table="dbo.coletas",
        api_key=key_simple("id"),
        db_key_sql="id",
        db_where_sql="(request_date BETWEEN ? AND ? OR service_date BETWEEN ? AND ?)",
        db_params=lambda start, end: [start.isoformat(), end.isoformat(), start.isoformat(), end.isoformat()],
        db_not_null_sql="id IS NOT NULL",
        graphql=GraphQLConfig(GRAPHQL_QUERY_COLETAS, "pick"),
    ),
    EntityConfig(
        name="manifestos",
        api_kind="dataexport",
        table="dbo.manifestos",
        api_key=key_manifestos,
        db_key_sql=(
            "CONCAT(CAST(sequence_code AS varchar(50)), '|', "
            "COALESCE(CAST(pick_sequence_code AS varchar(50)), JSON_VALUE(metadata, '$.mft_pfs_pck_sequence_code'), '-1'), '|', "
            "COALESCE(CAST(mdfe_number AS varchar(50)), JSON_VALUE(metadata, '$.mft_mfs_number'), '-1'))"
        ),
        db_where_sql=(
            "((data_extracao >= ? AND data_extracao < ?) "
            "OR TRY_CONVERT(date, JSON_VALUE(metadata, '$.service_date')) BETWEEN ? AND ?)"
        ),
        db_params=params_datetime2_date2,
        db_not_null_sql="sequence_code IS NOT NULL",
        dataexport=DataExportConfig(6399, "manifests", "service_date", 100, "sequence_code asc", timeout=120),
    ),
    EntityConfig(
        name="cotacoes",
        api_kind="dataexport",
        table="dbo.cotacoes",
        api_key=key_simple("sequence_code"),
        db_key_sql="CAST(sequence_code AS varchar(50))",
        db_where_sql="CAST(requested_at AS date) BETWEEN ? AND ?",
        db_params=params_date2,
        db_not_null_sql="sequence_code IS NOT NULL",
        dataexport=DataExportConfig(6906, "quotes", "requested_at", 1000, "sequence_code asc", timeout=60),
    ),
    EntityConfig(
        name="localizacao_cargas",
        api_kind="dataexport",
        table="dbo.localizacao_cargas",
        api_key=key_localizacao,
        db_key_sql="CAST(sequence_number AS varchar(50))",
        db_where_sql="CAST(service_at AS date) BETWEEN ? AND ?",
        db_params=params_date2,
        db_not_null_sql="sequence_number IS NOT NULL",
        dataexport=DataExportConfig(8656, "freights", "service_at", 10000, "sequence_number asc", timeout=90),
    ),
    EntityConfig(
        name="contas_a_pagar",
        api_kind="dataexport",
        table="dbo.contas_a_pagar",
        api_key=key_contas_a_pagar,
        db_key_sql="CAST(sequence_code AS varchar(50))",
        db_where_sql="COALESCE(issue_date, data_transacao, data_liquidacao, CAST(data_criacao AS date)) BETWEEN ? AND ?",
        db_params=params_date2,
        db_not_null_sql="sequence_code IS NOT NULL",
        dataexport=DataExportConfig(
            8636,
            "accounting_debits",
            "issue_date",
            100,
            "issue_date desc",
            nested=True,
            segment_by_day=True,
            extra_segment_field="created_at",
            timeout=120,
        ),
    ),
    EntityConfig(
        name="faturas_por_cliente",
        api_kind="dataexport",
        table="dbo.faturas_por_cliente",
        api_key=key_faturas_por_cliente,
        db_key_sql="unique_id",
        db_where_sql=(
            "((data_extracao >= ? AND data_extracao < ?) "
            "OR TRY_CONVERT(date, JSON_VALUE(metadata, '$.service_at')) BETWEEN ? AND ?)"
        ),
        db_params=params_datetime2_date2,
        db_not_null_sql="unique_id IS NOT NULL",
        dataexport=DataExportConfig(4924, "freights", "service_at", 100, "unique_id asc", segment_by_day=True, timeout=60),
    ),
    EntityConfig(
        name="inventario",
        api_kind="dataexport",
        table="dbo.inventario",
        api_key=key_inventario,
        db_key_sql="identificador_unico",
        db_where_sql=(
            "COALESCE(CAST(started_at AS date), CAST(performance_finished_at AS date), "
            "CAST(predicted_delivery_at AS date)) BETWEEN ? AND ?"
        ),
        db_params=params_date2,
        db_not_null_sql="identificador_unico IS NOT NULL",
        dataexport=DataExportConfig(10633, "check_in_orders", "started_at", 100, "sequence_code asc", timeout=90),
    ),
    EntityConfig(
        name="sinistros",
        api_kind="dataexport",
        table="dbo.sinistros",
        api_key=key_sinistros,
        db_key_sql="identificador_unico",
        db_where_sql="COALESCE(opening_at_date, occurrence_at_date, expected_solution_date, finished_at_date) BETWEEN ? AND ?",
        db_params=params_date2,
        db_not_null_sql="identificador_unico IS NOT NULL",
        dataexport=DataExportConfig(6392, "insurance_claims", "opening_at_date", 100, "sequence_code asc", timeout=60),
    ),
    EntityConfig(
        name="usuarios_sistema",
        api_kind="graphql",
        table="dbo.dim_usuarios",
        api_key=key_simple("id"),
        db_key_sql="CAST(user_id AS varchar(50))",
        db_where_sql=(
            "ativo = 1 AND (CAST(origem_atualizado_em AS date) BETWEEN ? AND ? "
            "OR (origem_atualizado_em IS NULL AND COALESCE(ultima_extracao_em, data_atualizacao) >= ? "
            "AND COALESCE(ultima_extracao_em, data_atualizacao) < ?))"
        ),
        db_params=params_date2_datetime2,
        db_not_null_sql="user_id IS NOT NULL",
        graphql=GraphQLConfig(GRAPHQL_QUERY_USUARIOS, "individual", first=1000),
    ),
    EntityConfig(
        name="raster_viagens",
        api_kind="raster",
        table="dbo.raster_viagens",
        api_key=key_raster_viagem,
        db_key_sql="CAST(cod_solicitacao AS varchar(50))",
        db_where_sql="CAST(data_extracao AS date) >= ? AND CAST(data_extracao AS date) <= ?",
        db_params=params_date2,
        db_not_null_sql="cod_solicitacao IS NOT NULL",
    ),
    EntityConfig(
        name="raster_viagem_paradas",
        api_kind="raster",
        table="dbo.raster_viagem_paradas",
        api_key=key_raster_parada,
        db_key_sql="CONCAT(CAST(cod_solicitacao AS varchar(50)), '|', CAST(ordem AS varchar(50)))",
        db_where_sql="CAST(data_extracao AS date) >= ? AND CAST(data_extracao AS date) <= ?",
        db_params=params_date2,
        db_not_null_sql="cod_solicitacao IS NOT NULL AND ordem IS NOT NULL",
    ),
]


def selected_entities(names: str | None, include_raster: str) -> list[EntityConfig]:
    requested = None
    if names:
        requested = {item.strip().lower() for item in names.split(",") if item.strip()}
    out: list[EntityConfig] = []
    for cfg in ENTITIES:
        if requested is not None and cfg.name.lower() not in requested:
            continue
        if cfg.api_kind == "raster" and include_raster == "false":
            continue
        out.append(cfg)
    return out


def parse_jdbc_to_odbc(jdbc_url: str, user: str, password: str) -> str:
    server_match = re.search(r"jdbc:sqlserver://([^;]+)", jdbc_url, re.IGNORECASE)
    db_match = re.search(r"databaseName=([^;]+)", jdbc_url, re.IGNORECASE)
    if not server_match or not db_match:
        raise RuntimeError("DB_URL is not a valid SQL Server JDBC URL.")
    server = server_match.group(1).replace(":", ",", 1)
    database = db_match.group(1)
    driver = env_first("ODBC_DRIVER", default="ODBC Driver 18 for SQL Server")
    return (
        f"DRIVER={{{driver}}};SERVER={server};DATABASE={database};"
        f"UID={user};PWD={password};TrustServerCertificate=yes;"
    )


def db_connection_string() -> str:
    direct = env_first("DB_CONNECTION_STRING")
    if direct:
        return direct
    return parse_jdbc_to_odbc(required_env("DB_URL"), required_env("DB_USER"), required_env("DB_PASSWORD"))


def request_with_retry(
    session: requests.Session,
    method: str,
    url: str,
    *,
    attempts: int = 6,
    sleep_seconds: float = 2.3,
    **kwargs: Any,
) -> requests.Response:
    last: requests.Response | None = None
    for attempt in range(1, attempts + 1):
        response = session.request(method, url, **kwargs)
        last = response
        if response.status_code not in RETRYABLE_STATUS:
            return response
        time.sleep(min(sleep_seconds * (2 ** (attempt - 1)), 30.0))
    assert last is not None
    return last


def extract_rows_from_graphql(body: dict[str, Any], entity: str) -> tuple[list[dict[str, Any]], bool, str | None]:
    if body.get("errors"):
        raise RuntimeError(json.dumps(body["errors"], ensure_ascii=False))
    data = (body.get("data") or {}).get(entity) or {}
    edges = data.get("edges") or []
    rows: list[dict[str, Any]] = []
    for edge in edges:
        node = edge.get("node") if isinstance(edge, dict) else None
        if isinstance(node, dict):
            rows.append(node)
    page_info = data.get("pageInfo") or {}
    return rows, bool(page_info.get("hasNextPage")), page_info.get("endCursor")


def fetch_graphql_pages(
    session: requests.Session,
    cfg: EntityConfig,
    params: dict[str, Any],
    max_pages: int,
    sleep_seconds: float,
) -> list[dict[str, Any]]:
    assert cfg.graphql is not None
    base_url = required_env("API_URL", "API_BASE_URL", "API_BASEURL").rstrip("/")
    endpoint = env_first("API_GRAPHQL_ENDPOINT", default="/graphql") or "/graphql"
    endpoint = endpoint if endpoint.startswith("/") else f"/{endpoint}"
    token = required_env("API_GRAPHQL_TOKEN", "BEARER_TOKEN")
    url = base_url + endpoint
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json", "Accept": "application/json"}
    rows: list[dict[str, Any]] = []
    after = None
    for page in range(1, max_pages + 1):
        variables: dict[str, Any] = {"params": params}
        if after:
            variables["after"] = after
        response = request_with_retry(
            session,
            "POST",
            url,
            headers=headers,
            json={"query": cfg.graphql.query, "variables": variables},
            timeout=180,
            sleep_seconds=sleep_seconds,
        )
        response.raise_for_status()
        page_rows, has_next, end_cursor = extract_rows_from_graphql(response.json(), cfg.graphql.entity)
        rows.extend(page_rows)
        if not has_next or not end_cursor or end_cursor == after:
            return rows
        after = end_cursor
        time.sleep(sleep_seconds)
    raise RuntimeError(f"{cfg.name}: GraphQL max_pages exceeded ({max_pages})")


def pick_input_fields(session: requests.Session) -> set[str]:
    base_url = required_env("API_URL", "API_BASE_URL", "API_BASEURL").rstrip("/")
    endpoint = env_first("API_GRAPHQL_ENDPOINT", default="/graphql") or "/graphql"
    endpoint = endpoint if endpoint.startswith("/") else f"/{endpoint}"
    token = required_env("API_GRAPHQL_TOKEN", "BEARER_TOKEN")
    response = request_with_retry(
        session,
        "POST",
        base_url + endpoint,
        headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
        json={"query": GRAPHQL_INTROSPECTION_PICK_INPUT},
        timeout=60,
    )
    response.raise_for_status()
    fields = ((response.json().get("data") or {}).get("__type") or {}).get("inputFields") or []
    return {str(item.get("name")) for item in fields if isinstance(item, dict) and item.get("name")}


def fetch_graphql_entity(
    session: requests.Session,
    cfg: EntityConfig,
    start: dt.date,
    end: dt.date,
    max_pages: int,
    sleep_seconds: float,
) -> pd.DataFrame:
    rows: list[dict[str, Any]] = []
    if cfg.name == "fretes":
        rows.extend(
            fetch_graphql_pages(
                session,
                cfg,
                {"serviceAt": f"{start.isoformat()} - {end.isoformat()}"},
                max_pages,
                sleep_seconds,
            )
        )
    elif cfg.name == "coletas":
        fields = pick_input_fields(session)
        filters = ["requestDate"]
        if "serviceDate" in fields:
            filters.append("serviceDate")
        for day in date_range(start, end):
            for filter_name in filters:
                rows.extend(fetch_graphql_pages(session, cfg, {filter_name: day.isoformat()}, max_pages, sleep_seconds))
    elif cfg.name == "usuarios_sistema":
        rows.extend(
            fetch_graphql_pages(
                session,
                cfg,
                {"enabled": True, "updatedAt": f"{start.isoformat()} - {end.isoformat()}"},
                max_pages,
                sleep_seconds,
            )
        )
    else:
        raise RuntimeError(f"Unsupported GraphQL entity: {cfg.name}")
    return pd.DataFrame(rows)


def dataexport_payload(
    de: DataExportConfig,
    start: dt.date,
    end: dt.date,
    page: int,
    extra_filters: dict[str, str] | None = None,
) -> dict[str, Any]:
    table_filter = {de.date_field: f"{start.isoformat()} - {end.isoformat()}"}
    if extra_filters:
        table_filter.update(extra_filters)
    return {
        "search": {de.api_table: table_filter},
        "page": str(page),
        "per": str(de.per),
        "order_by": de.order_by,
    }


def request_dataexport_page(
    session: requests.Session,
    de: DataExportConfig,
    payload: dict[str, Any],
    sleep_seconds: float,
) -> requests.Response:
    base_url = required_env("API_URL", "API_BASE_URL", "API_BASEURL").rstrip("/")
    token = required_env("API_DATAEXPORT_TOKEN", "BEARER_TOKEN")
    url = f"{base_url}/api/analytics/reports/{de.template_id}/data"
    method = (env_first("API_DATAEXPORT_HTTP_METHOD", default="POST") or "POST").upper()
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json", "Accept": "application/json"}
    body = json.dumps(payload, ensure_ascii=False)
    params = dataexport_query_params(payload)
    if method == "GET":
        return request_with_retry(
            session,
            "GET",
            url,
            headers=headers,
            params=params,
            timeout=de.timeout,
            sleep_seconds=sleep_seconds,
        )
    response = request_with_retry(
        session,
        method,
        url,
        headers=headers,
        data=body,
        timeout=de.timeout,
        sleep_seconds=sleep_seconds,
    )
    if response.status_code in {404, 405, 415, 422, 501}:
        response = request_with_retry(
            session,
            "GET",
            url,
            headers=headers,
            params=params,
            timeout=de.timeout,
            sleep_seconds=sleep_seconds,
        )
    return response


def dataexport_query_params(payload: dict[str, Any]) -> list[tuple[str, str]]:
    params: list[tuple[str, str]] = [
        ("page", str(payload.get("page", "1"))),
        ("per", str(payload.get("per", ""))),
        ("order_by", str(payload.get("order_by", ""))),
    ]
    search = payload.get("search")
    if isinstance(search, dict):
        for table, filters in search.items():
            if isinstance(filters, dict):
                for field, value in filters.items():
                    params.append((f"search[{table}][{field}]", str(value)))
    return params


def parse_dataexport_rows(response: requests.Response) -> list[dict[str, Any]]:
    response.raise_for_status()
    body = response.json()
    rows = body.get("data") if isinstance(body, dict) and "data" in body else body
    if not isinstance(rows, list):
        return []
    return [row for row in rows if isinstance(row, dict)]


def fetch_dataexport_window(
    session: requests.Session,
    de: DataExportConfig,
    start: dt.date,
    end: dt.date,
    max_pages: int,
    sleep_seconds: float,
    extra_filters: dict[str, str] | None = None,
) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for page in range(1, max_pages + 1):
        payload = dataexport_payload(de, start, end, page, extra_filters)
        response = request_dataexport_page(session, de, payload, sleep_seconds)
        page_rows = parse_dataexport_rows(response)
        if not page_rows:
            return rows
        rows.extend(page_rows)
        time.sleep(sleep_seconds)
    raise RuntimeError(f"DataExport template {de.template_id}: max_pages exceeded ({max_pages})")


def fetch_dataexport_entity(
    session: requests.Session,
    cfg: EntityConfig,
    start: dt.date,
    end: dt.date,
    max_pages: int,
    sleep_seconds: float,
) -> pd.DataFrame:
    assert cfg.dataexport is not None
    de = cfg.dataexport
    rows: list[dict[str, Any]] = []
    if de.segment_by_day:
        for day in date_range(start, end):
            if de.extra_segment_field:
                extra = {de.extra_segment_field: f"{day.isoformat()} - {day.isoformat()}"}
                rows.extend(fetch_dataexport_window(session, de, start, end, max_pages, sleep_seconds, extra))
            else:
                rows.extend(fetch_dataexport_window(session, de, day, day, max_pages, sleep_seconds))
    else:
        rows.extend(fetch_dataexport_window(session, de, start, end, max_pages, sleep_seconds))
    return pd.DataFrame(rows)


def case_insensitive_get(row: dict[str, Any], name: str) -> Any:
    if name in row:
        return row[name]
    target = name.lower()
    for key, value in row.items():
        if key.lower() == target:
            return value
    return None


def extract_raster_viagens_from_node(node: Any) -> list[dict[str, Any]]:
    if node is None:
        return []
    if isinstance(node, list):
        out: list[dict[str, Any]] = []
        for item in node:
            out.extend(extract_raster_viagens_from_node(item))
        return out
    if not isinstance(node, dict):
        return []
    viagens = case_insensitive_get(node, "Viagens")
    if viagens is not None:
        return extract_raster_viagens_from_node(viagens)
    if case_insensitive_get(node, "CodSolicitacao") is not None:
        return [node]
    return []


def raster_request_once(session: requests.Session, start: dt.date, end: dt.date) -> list[dict[str, Any]]:
    base_url = required_env("RASTER_BASE_URL").rstrip("/")
    url = base_url + "/%22getEventoFimViagem%22"
    payload = {
        "Ambiente": env_first("RASTER_AMBIENTE", default="Producao"),
        "Login": required_env("RASTER_LOGIN"),
        "Senha": required_env("RASTER_SENHA", "RASTER_PASSWORD"),
        "TipoRetorno": "JSON",
        "DataInicial": start.isoformat(),
        "DataFinal": end.isoformat(),
        "StatusViagem": env_first("RASTER_STATUS_VIAGEM", default="T"),
    }
    timeout = int(env_first("RASTER_TIMEOUT_SECONDS", default="120") or "120")
    response = request_with_retry(session, "POST", url, json=payload, timeout=timeout)
    response.raise_for_status()
    body = response.json()
    if isinstance(body, dict) and (body.get("error") or body.get("Erro")):
        raise RuntimeError("Raster API returned error.")
    result = case_insensitive_get(body, "result") if isinstance(body, dict) else None
    return extract_raster_viagens_from_node(result if result is not None else body)


def fetch_raster(session: requests.Session, start: dt.date, end: dt.date) -> dict[str, pd.DataFrame]:
    if not env_first("RASTER_LOGIN") or not env_first("RASTER_SENHA", "RASTER_PASSWORD"):
        raise RuntimeError("Raster credentials missing.")
    viagens = raster_request_once(session, start, end)
    paradas: list[dict[str, Any]] = []
    for viagem in viagens:
        cod = key_raster_viagem(viagem)
        raw_paradas = case_insensitive_get(viagem, "ColetasEntregas") or []
        if not isinstance(raw_paradas, list):
            continue
        for index, parada in enumerate(raw_paradas, start=1):
            if not isinstance(parada, dict):
                continue
            item = dict(parada)
            item["_cod_solicitacao"] = cod
            item["_ordem"] = clean_id(first_present(item, "Ordem", "ordem")) or str(index)
            paradas.append(item)
    return {
        "raster_viagens": pd.DataFrame(viagens),
        "raster_viagem_paradas": pd.DataFrame(paradas),
    }


def extract_api_dataframe(
    session: requests.Session,
    cfg: EntityConfig,
    start: dt.date,
    end: dt.date,
    max_pages: int,
    sleep_seconds: float,
    raster_cache: dict[str, pd.DataFrame],
) -> pd.DataFrame:
    if cfg.api_kind == "graphql":
        return fetch_graphql_entity(session, cfg, start, end, max_pages, sleep_seconds)
    if cfg.api_kind == "dataexport":
        return fetch_dataexport_entity(session, cfg, start, end, max_pages, sleep_seconds)
    if cfg.api_kind == "raster":
        if not raster_cache:
            raster_cache.update(fetch_raster(session, start, end))
        return raster_cache.get(cfg.name, pd.DataFrame())
    raise RuntimeError(f"Unsupported api_kind: {cfg.api_kind}")


def dataframe_with_audit_id(df: pd.DataFrame, key_fn: Callable[[dict[str, Any]], str | None]) -> pd.DataFrame:
    if df.empty:
        return pd.DataFrame(columns=["_audit_id"])
    rows = df.to_dict(orient="records")
    ids = [key_fn(row) for row in rows]
    out = df.copy()
    out["_audit_id"] = ids
    out = out[out["_audit_id"].notna()]
    out["_audit_id"] = out["_audit_id"].astype(str).str.strip()
    out = out[out["_audit_id"] != ""]
    return out


def unique_ids_from_df(df: pd.DataFrame, key_col: str) -> set[str]:
    if df.empty or key_col not in df.columns:
        return set()
    series = df[key_col].dropna().map(clean_id).dropna()
    return set(series.astype(str))


def fetch_db_dataframe(conn: pyodbc.Connection, cfg: EntityConfig, start: dt.date, end: dt.date) -> pd.DataFrame:
    sql = f"""
        SELECT {cfg.db_key_sql} AS id
        FROM {cfg.table}
        WHERE ({cfg.db_where_sql})
          AND ({cfg.db_not_null_sql})
    """
    params = cfg.db_params(start, end)
    return pd.read_sql_query(sql, conn, params=params)


def reconcile_entity(
    conn: pyodbc.Connection,
    session: requests.Session,
    cfg: EntityConfig,
    start: dt.date,
    end: dt.date,
    max_pages: int,
    sleep_seconds: float,
    raster_cache: dict[str, pd.DataFrame],
    sample_limit: int,
) -> dict[str, Any]:
    api_raw = extract_api_dataframe(session, cfg, start, end, max_pages, sleep_seconds, raster_cache)
    api_df = dataframe_with_audit_id(api_raw, cfg.api_key)
    api_unique_df = api_df.drop_duplicates(subset=["_audit_id"], keep="last")
    db_df = fetch_db_dataframe(conn, cfg, start, end)

    api_ids = unique_ids_from_df(api_unique_df, "_audit_id")
    db_ids = unique_ids_from_df(db_df, "id")
    missing = sorted(api_ids - db_ids)
    orphans = sorted(db_ids - api_ids)
    status = "OK" if not missing and not orphans else "FAIL"

    return {
        "entity": cfg.name,
        "api_kind": cfg.api_kind,
        "table": cfg.table,
        "status": status,
        "api_raw_rows": int(len(api_raw)),
        "api_unique_ids": int(len(api_ids)),
        "db_raw_rows": int(len(db_df)),
        "db_unique_ids": int(len(db_ids)),
        "missing_count": int(len(missing)),
        "orphans_count": int(len(orphans)),
        "missing": missing,
        "orphans": orphans,
        "missing_sample": missing[:sample_limit],
        "orphans_sample": orphans[:sample_limit],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Autonomous ETL reconciliation job.")
    parser.add_argument("--env-file", default=".env")
    parser.add_argument("--start", required=True, help="Start date YYYY-MM-DD.")
    parser.add_argument("--end", required=True, help="End date YYYY-MM-DD inclusive.")
    parser.add_argument("--entities", default=None, help="Comma-separated entity list. Default: all.")
    parser.add_argument("--include-raster", choices=("auto", "true", "false"), default="auto")
    parser.add_argument("--output", default="reconciliation_master_report.json")
    parser.add_argument("--max-pages", type=int, default=int(env_first("AUDIT_MAX_PAGES", default="3000") or "3000"))
    parser.add_argument("--sleep", type=float, default=float(env_first("AUDIT_PAGE_SLEEP_SECONDS", default="0.3") or "0.3"))
    parser.add_argument("--sample-limit", type=int, default=20)
    parser.add_argument("--fail-on-diff", action="store_true")
    args = parser.parse_args()

    load_dotenv(args.env_file)
    start = as_date(args.start)
    end = as_date(args.end)
    if end < start:
        raise RuntimeError("--end must be greater than or equal to --start")

    include_raster = args.include_raster
    if include_raster == "auto":
        include_raster = "true" if env_first("RASTER_LOGIN") and env_first("RASTER_SENHA", "RASTER_PASSWORD") else "false"

    configs = selected_entities(args.entities, include_raster)
    session = requests.Session()
    raster_cache: dict[str, pd.DataFrame] = {}
    report_entities: list[dict[str, Any]] = []

    with pyodbc.connect(db_connection_string(), timeout=60) as conn:
        for cfg in configs:
            print(f"[RUN] {cfg.name}")
            try:
                result = reconcile_entity(
                    conn,
                    session,
                    cfg,
                    start,
                    end,
                    args.max_pages,
                    args.sleep,
                    raster_cache,
                    args.sample_limit,
                )
            except Exception as exc:  # noqa: BLE001
                result = {
                    "entity": cfg.name,
                    "api_kind": cfg.api_kind,
                    "table": cfg.table,
                    "status": "ERROR",
                    "error": str(exc),
                    "api_raw_rows": 0,
                    "api_unique_ids": 0,
                    "db_raw_rows": 0,
                    "db_unique_ids": 0,
                    "missing_count": None,
                    "orphans_count": None,
                    "missing": [],
                    "orphans": [],
                }
            report_entities.append(result)
            print(
                "[{status}] {entity}: api={api_unique_ids} db={db_unique_ids} "
                "missing={missing_count} orphans={orphans_count}".format(**result)
            )

    summary_status = "OK"
    if any(item["status"] == "ERROR" for item in report_entities):
        summary_status = "ERROR"
    elif any(item["status"] == "FAIL" for item in report_entities):
        summary_status = "FAIL"

    report = {
        "generated_at": dt.datetime.now().isoformat(timespec="seconds"),
        "window": {"start": start.isoformat(), "end": end.isoformat()},
        "status": summary_status,
        "entities_count": len(report_entities),
        "entities_ok": sum(1 for item in report_entities if item["status"] == "OK"),
        "entities_fail": sum(1 for item in report_entities if item["status"] == "FAIL"),
        "entities_error": sum(1 for item in report_entities if item["status"] == "ERROR"),
        "entities": report_entities,
    }

    output = Path(args.output)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"[REPORT] {output.resolve()}")

    if args.fail_on_diff and summary_status != "OK":
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
