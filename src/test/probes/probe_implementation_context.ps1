Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")

function Invoke-Rg {
    param([string[]]$Patterns)

    $args = @("-n", "--no-heading")
    foreach ($pattern in $Patterns) {
        $args += "-F"
        $args += "-e"
        $args += $pattern
    }
    $args += "src\\main"
    $args += "src\\test\\java"
    $args += "src\\test\\probes"

    $output = & rg.exe @args 2>$null
    if ($LASTEXITCODE -gt 1) {
        throw "Falha ao executar rg para padroes: $($Patterns -join ', ')"
    }
    return $output
}

$sections = [ordered]@{
    execution_window = Invoke-Rg @("ExecutionWindowPlanner", "planejarEntidade", "janelaPadrao", "buscarWatermarkConfirmado")
    fluxo_completo = Invoke-Rg @("FluxoCompletoUseCase", "atualizarWatermarkConfirmado", "registrarResultado")
    graphql_coletas = Invoke-Rg @("GraphQLColetaSupport", "buscarColetasComFiltrosCombinados", "deduplicarColetasPorId", "resolverChaveDeduplicacao")
    graphql_fretes = Invoke-Rg @("ClienteApiGraphQL", "buscarFretes(", "buscarColetas(")
    dataexport_contas = Invoke-Rg @("ConstantesApiDataExport", "contas_a_pagar", "issue_date desc", "ant_ils_sequence_code")
    auditoria = Invoke-Rg @("SqlServerExecutionAuditPortAdapter", "IntegridadeEtlValidator", "AUDITORIA_ESTRUTURADA_INDISPONIVEL", "isDisponivel")
    repositorio = Invoke-Rg @("AbstractRepository", "buildMonotonicUpdateGuard", "LOCK_TIMEOUT", "READ_COMMITTED", "atomic.commit", "continue.on.error")
    retry_http = Invoke-Rg @("GerenciadorRequisicaoHttp", "Retry-After", " 429", "429")
    failure_policy = Invoke-Rg @("etl.failure", "STRICT_INTEGRITY", "permiteConcluirComInvalidosAuditados", "ExtractionLoggerTest")
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Implementation Context Probe")
$lines.Add("")
$lines.Add("executed_at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$lines.Add("repo_root: $root")

foreach ($entry in $sections.GetEnumerator()) {
    $lines.Add("")
    $lines.Add("## $($entry.Key)")
    if ($entry.Value) {
        foreach ($line in $entry.Value) {
            $lines.Add($line)
        }
    } else {
        $lines.Add("(no matches)")
    }
}

$lines -join [Environment]::NewLine
