Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-ColetaFieldValue {
    param(
        $Coleta,
        [string]$FieldName
    )

    if ($null -eq $Coleta) {
        return $null
    }

    $prop = $Coleta.PSObject.Properties[$FieldName]
    if ($null -eq $prop) {
        return $null
    }

    return $prop.Value
}

function Get-ColetaFreshnessScore {
    param($Coleta)

    foreach ($field in @("statusUpdatedAt", "finishDate", "serviceDate", "requestDate")) {
        $value = Get-ColetaFieldValue -Coleta $Coleta -FieldName $field
        if ($value) {
            try {
                return [DateTimeOffset]::Parse($value)
            } catch {
                continue
            }
        }
    }

    return [DateTimeOffset]::MinValue
}

function Dedup-ColetasCurrentBehavior {
    param([object[]]$Coletas)

    $map = [ordered]@{}
    foreach ($coleta in $Coletas) {
        $id = Get-ColetaFieldValue -Coleta $coleta -FieldName "id"
        $sequenceCode = Get-ColetaFieldValue -Coleta $coleta -FieldName "sequenceCode"
        $key = if ($id) { $id } else { $sequenceCode }
        $map[$key] = $coleta
    }
    return @($map.Values)
}

function Dedup-ColetasByFreshness {
    param([object[]]$Coletas)

    $map = @{}
    foreach ($coleta in $Coletas) {
        $id = Get-ColetaFieldValue -Coleta $coleta -FieldName "id"
        $sequenceCode = Get-ColetaFieldValue -Coleta $coleta -FieldName "sequenceCode"
        $key = if ($id) { $id } else { $sequenceCode }
        if (-not $map.ContainsKey($key)) {
            $map[$key] = $coleta
            continue
        }

        $currentScore = Get-ColetaFreshnessScore -Coleta $map[$key]
        $candidateScore = Get-ColetaFreshnessScore -Coleta $coleta
        if ($candidateScore -gt $currentScore) {
            $map[$key] = $coleta
        }
    }
    return @($map.Values)
}

$dedupSample = @(
    [pscustomobject]@{
        id = "A-1"
        sequenceCode = 1001
        status = "delivered"
        statusUpdatedAt = "2026-04-06T12:30:00-03:00"
        serviceDate = "2026-04-06T00:00:00-03:00"
        requestDate = "2026-04-05T00:00:00-03:00"
    },
    [pscustomobject]@{
        id = "A-1"
        sequenceCode = 1001
        status = "picked"
        statusUpdatedAt = "2026-04-06T09:15:00-03:00"
        serviceDate = "2026-04-06T00:00:00-03:00"
        requestDate = "2026-04-05T00:00:00-03:00"
    }
)

$currentDedup = Dedup-ColetasCurrentBehavior -Coletas $dedupSample
$freshDedup = Dedup-ColetasByFreshness -Coletas $dedupSample

$utcZone = [System.TimeZoneInfo]::FindSystemTimeZoneById("UTC")
$spZone = [System.TimeZoneInfo]::FindSystemTimeZoneById("E. South America Standard Time")
$instant = [DateTimeOffset]::Parse("2026-04-06T02:30:00Z")
$utcDate = [System.TimeZoneInfo]::ConvertTime($instant, $utcZone).Date.ToString("yyyy-MM-dd")
$spDate = [System.TimeZoneInfo]::ConvertTime($instant, $spZone).Date.ToString("yyyy-MM-dd")

$businessDateLateUpdate = [DateTime]::Parse("2026-04-01")
$dataReferenciaFim = [DateTime]::Parse("2026-04-06")
$currentWindowStart = $dataReferenciaFim.AddDays(-1)
$retroReplayStart = $dataReferenciaFim.AddDays(-7)

$summary = [ordered]@{
    executed_at = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    deduplicacao_coletas = [ordered]@{
        current_kept_status = (Get-ColetaFieldValue -Coleta $currentDedup[0] -FieldName "status")
        current_behavior_keeps_stale_when_input_order_is_bad = ((Get-ColetaFieldValue -Coleta $currentDedup[0] -FieldName "status") -eq "picked")
        candidate_kept_status = (Get-ColetaFieldValue -Coleta $freshDedup[0] -FieldName "status")
        candidate_keeps_freshest = ((Get-ColetaFieldValue -Coleta $freshDedup[0] -FieldName "status") -eq "delivered")
        recommendation = "Deduplicar por maior frescor usando statusUpdatedAt > finishDate > serviceDate > requestDate."
    }
    timezone = [ordered]@{
        probe_instant_utc = $instant.ToString("o")
        local_date_if_host_is_utc = $utcDate
        local_date_if_config_is_america_sao_paulo = $spDate
        host_fallback_can_shift_day = ($utcDate -ne $spDate)
        recommendation = "Tornar timezone obrigatorio em producao e remover fallback silencioso."
    }
    janela_tardia = [ordered]@{
        business_date_of_late_record = $businessDateLateUpdate.ToString("yyyy-MM-dd")
        current_d1_window_start = $currentWindowStart.ToString("yyyy-MM-dd")
        current_d1_captures_late_record = ($businessDateLateUpdate -ge $currentWindowStart)
        replay_7d_window_start = $retroReplayStart.ToString("yyyy-MM-dd")
        replay_7d_captures_late_record = ($businessDateLateUpdate -ge $retroReplayStart)
        recommendation = "Sem updatedAt disponivel, combinar janela operacional curta com replay retroativo periodico."
    }
}

$summary | ConvertTo-Json -Depth 6
