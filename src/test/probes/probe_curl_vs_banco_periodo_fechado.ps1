param(
    [string]$DataInicio = ((Get-Date).AddDays(-1).ToString("yyyy-MM-dd")),
    [string]$DataFim = ((Get-Date).AddDays(-1).ToString("yyyy-MM-dd")),
    [string]$ExecutionUuid
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Web.Extensions

function Load-EnvFile {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Arquivo .env nao encontrado em $Path"
    }

    $cfg = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $key, $value = $line -split "=", 2
        $cfg[$key.Trim()] = $value.Trim()
    }
    return $cfg
}

function Resolve-SqlCmdPath {
    $paths = @(
        "C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\180\Tools\Binn\SQLCMD.EXE",
        "C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\170\Tools\Binn\SQLCMD.EXE"
    )

    foreach ($candidate in $paths) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    $command = Get-Command sqlcmd.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    throw "sqlcmd.exe nao encontrado."
}

function Get-DatabaseNameFromUrl {
    param([string]$DbUrl)

    $match = [regex]::Match($DbUrl, "databaseName=([^;]+)")
    if (-not $match.Success) {
        throw "Nao foi possivel extrair databaseName do DB_URL"
    }
    return $match.Groups[1].Value
}

function Invoke-SqlLines {
    param(
        [hashtable]$Config,
        [string]$Query
    )

    $database = Get-DatabaseNameFromUrl -DbUrl $Config["DB_URL"]
    $sqlcmd = Resolve-SqlCmdPath
    $server = "tcp:localhost,1433"

    $output = & $sqlcmd `
        -S $server `
        -d $database `
        -U $Config["DB_USER"] `
        -P $Config["DB_PASSWORD"] `
        -C `
        -w 65535 `
        -h -1 `
        -s "|" `
        -Q $Query 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao executar SQLCMD: $($output -join [Environment]::NewLine)"
    }

    return @(
        $output |
            Where-Object { $_ -ne $null } |
            ForEach-Object { "$_".Trim() } |
            Where-Object { $_ -and -not $_.StartsWith("(") }
    )
}

function Invoke-SqlScalar {
    param(
        [hashtable]$Config,
        [string]$Query
    )

    $lines = Invoke-SqlLines -Config $Config -Query $Query
    if ($lines.Count -eq 0) {
        return $null
    }
    return $lines[0]
}

function ConvertFrom-JsonPreserveStrings {
    param([string]$JsonText)

    if ([string]::IsNullOrWhiteSpace($JsonText)) {
        return $null
    }

    $serializer = New-Object System.Web.Script.Serialization.JavaScriptSerializer
    $serializer.MaxJsonLength = [int]::MaxValue
    return $serializer.DeserializeObject($JsonText)
}

function Invoke-CurlJson {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        [string]$Body,
        [int]$MaxAttempts = 3,
        [int]$RetryDelaySeconds = 2
    )

    $last = $null
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $args = @("--globoff", "-sS", "-X", $Method, $Url)
        foreach ($name in $Headers.Keys) {
            $args += "-H"
            $args += ("{0}: {1}" -f $name, $Headers[$name])
        }

        $tempFile = $null
        try {
            if ($Body) {
                $tempFile = [System.IO.Path]::GetTempFileName()
                [System.IO.File]::WriteAllText($tempFile, $Body)
                $args += "--data-binary"
                $args += ("@" + $tempFile)
            }
            $args += "-w"
            $args += "`nHTTPSTATUS:%{http_code}"
            $raw = & curl.exe @args
        } finally {
            if ($tempFile -and (Test-Path -LiteralPath $tempFile)) {
                Remove-Item -LiteralPath $tempFile -Force
            }
        }

        $statusMatch = [regex]::Match($raw, "HTTPSTATUS:(\d{3})")
        $status = if ($statusMatch.Success) { $statusMatch.Groups[1].Value } else { "" }
        $bodyOnly = [regex]::Replace($raw, "\s*HTTPSTATUS:\d{3}\s*$", "")

        $json = $null
        try {
            $json = ConvertFrom-JsonPreserveStrings -JsonText $bodyOnly
        } catch {
            $json = $null
        }

        $last = [pscustomobject]@{
            Status = $status
            Body = $bodyOnly
            Json = $json
        }

        $retryable = $status -in @("429", "500", "502", "503", "504")
        if (-not $retryable -or $attempt -eq $MaxAttempts) {
            return $last
        }

        Start-Sleep -Seconds $RetryDelaySeconds
    }

    return $last
}

function Get-Value {
    param(
        $Item,
        [string]$Name
    )

    if ($null -eq $Item) {
        return $null
    }

    if ($Item -is [System.Collections.IDictionary]) {
        $containsKeyMethod = $Item.GetType().GetMethod("ContainsKey")
        if ($containsKeyMethod) {
            $hasKey = $containsKeyMethod.Invoke($Item, @($Name))
            if ($hasKey) {
                return $Item[$Name]
            }
            return $null
        }
        if ($Item.Contains($Name)) {
            return $Item[$Name]
        }
        return $null
    }

    $prop = $Item.PSObject.Properties[$Name]
    if ($null -eq $prop) {
        return $null
    }
    return $prop.Value
}

function Normalize-Text {
    param([AllowNull()][string]$Value)

    if ($null -eq $Value) {
        return "<null>"
    }

    $trimmed = $Value.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed)) {
        return "<empty>"
    }

    return $trimmed
}

function Normalize-DateOnlyValue {
    param($Value)

    if ($null -eq $Value) {
        return $null
    }
    if ($Value -is [datetime]) {
        return $Value.ToString("yyyy-MM-dd")
    }
    return [string]$Value
}

function Normalize-OffsetDateTimeValue {
    param($Value)

    if ($null -eq $Value) {
        return $null
    }

    if ($Value -is [datetime]) {
        return $Value.ToString("yyyy-MM-ddTHH:mm:ss.fffzzz")
    }

    $text = [string]$Value
    if ($text -match '^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})([+-]\d{2}:\d{2}|Z)$') {
        return "{0}.000{1}" -f $Matches[1], $Matches[2]
    }
    return $text
}

function Normalize-List {
    param($Items)

    if ($null -eq $Items) {
        return "<null>"
    }

    $normalized = New-Object System.Collections.Generic.List[string]
    foreach ($item in @($Items)) {
        $value = Normalize-Text -Value ([string]$item)
        if ($value -ne "<null>" -and $value -ne "<empty>") {
            [void]$normalized.Add($value)
        }
    }

    if ($normalized.Count -eq 0) {
        return "<empty>"
    }

    $sorted = @($normalized | Sort-Object)
    return ($sorted -join ",")
}

function Get-Sha256Hex {
    param([string]$Text)

    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Text)
        $hash = $sha.ComputeHash($bytes)
        return ([System.BitConverter]::ToString($hash)).Replace("-", "").ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Get-FaturaPorClienteCanonicalKey {
    param($Item)

    $nfseNumber = Get-Value $Item "fit_nse_number"
    if ($null -eq $nfseNumber -or [string]::IsNullOrWhiteSpace([string]$nfseNumber)) {
        $nfseNumber = Get-Value $Item "nfse_number"
    }

    $cteNumber = Get-Value $Item "fit_fhe_cte_number"
    $faturaDocument = Get-Value $Item "fit_ant_document"
    $faturaIssueDate = Normalize-DateOnlyValue (Get-Value $Item "fit_ant_issue_date")
    $billingId = Get-Value $Item "billingId"
    $pagadorDocumento = Get-Value $Item "fit_pyr_document"
    $remetenteDocumento = Get-Value $Item "fit_rpt_document"
    $destinatarioDocumento = Get-Value $Item "fit_sdr_document"
    $notasFiscais = Get-Value $Item "invoices_mapping"
    $pedidosCliente = Get-Value $Item "fit_fte_invoices_order_number"
    $valorFrete = Get-Value $Item "total"
    $valorFatura = Get-Value $Item "fit_ant_value"

    $sb = New-Object System.Text.StringBuilder
    if ($nfseNumber -ne $null -and -not [string]::IsNullOrWhiteSpace([string]$nfseNumber)) {
        [void]$sb.Append("identitySource=").Append((Normalize-Text ([string]"nfse"))).Append("|")
        [void]$sb.Append("nfseNumber=").Append((Normalize-Text ([string]$nfseNumber))).Append("|")
        [void]$sb.Append("pagadorDocumento=").Append((Normalize-Text ([string]$pagadorDocumento))).Append("|")
        [void]$sb.Append("remetenteDocumento=").Append((Normalize-Text ([string]$remetenteDocumento))).Append("|")
        [void]$sb.Append("destinatarioDocumento=").Append((Normalize-Text ([string]$destinatarioDocumento))).Append("|")
    } elseif ($cteNumber -ne $null -and -not [string]::IsNullOrWhiteSpace([string]$cteNumber)) {
        [void]$sb.Append("identitySource=").Append((Normalize-Text ([string]"cte"))).Append("|")
        [void]$sb.Append("cteNumber=").Append((Normalize-Text ([string]$cteNumber))).Append("|")
        [void]$sb.Append("pagadorDocumento=").Append((Normalize-Text ([string]$pagadorDocumento))).Append("|")
        [void]$sb.Append("remetenteDocumento=").Append((Normalize-Text ([string]$remetenteDocumento))).Append("|")
        [void]$sb.Append("destinatarioDocumento=").Append((Normalize-Text ([string]$destinatarioDocumento))).Append("|")
    } elseif (-not [string]::IsNullOrWhiteSpace([string]$faturaDocument)) {
        [void]$sb.Append("identitySource=").Append((Normalize-Text ([string]"fatura"))).Append("|")
        [void]$sb.Append("document=").Append((Normalize-Text ([string]$faturaDocument))).Append("|")
        [void]$sb.Append("issueDate=").Append((Normalize-Text ([string]$faturaIssueDate))).Append("|")
        [void]$sb.Append("pagadorDocumento=").Append((Normalize-Text ([string]$pagadorDocumento))).Append("|")
        [void]$sb.Append("destinatarioDocumento=").Append((Normalize-Text ([string]$destinatarioDocumento))).Append("|")
    } elseif (-not [string]::IsNullOrWhiteSpace([string]$billingId)) {
        [void]$sb.Append("identitySource=").Append((Normalize-Text ([string]"billing"))).Append("|")
        [void]$sb.Append("billingId=").Append((Normalize-Text ([string]$billingId))).Append("|")
        [void]$sb.Append("pagadorDocumento=").Append((Normalize-Text ([string]$pagadorDocumento))).Append("|")
        [void]$sb.Append("destinatarioDocumento=").Append((Normalize-Text ([string]$destinatarioDocumento))).Append("|")
    } else {
        [void]$sb.Append("identitySource=").Append((Normalize-Text ([string]"fallback"))).Append("|")
        [void]$sb.Append("pagadorDocumento=").Append((Normalize-Text ([string]$pagadorDocumento))).Append("|")
        [void]$sb.Append("remetenteDocumento=").Append((Normalize-Text ([string]$remetenteDocumento))).Append("|")
        [void]$sb.Append("destinatarioDocumento=").Append((Normalize-Text ([string]$destinatarioDocumento))).Append("|")
        [void]$sb.Append("notasFiscais=").Append((Normalize-List $notasFiscais)).Append("|")
        [void]$sb.Append("pedidosCliente=").Append((Normalize-List $pedidosCliente)).Append("|")
        [void]$sb.Append("valorFrete=").Append((Normalize-Text ([string]$valorFrete))).Append("|")
        [void]$sb.Append("valorFatura=").Append((Normalize-Text ([string]$valorFatura))).Append("|")
    }

    return $sb.ToString()
}

function Get-FaturaPorClienteUniqueId {
    param($Item)

    return "FPC-HASH-" + (Get-Sha256Hex -Text (Get-FaturaPorClienteCanonicalKey -Item $Item))
}

function Get-InventarioBusinessKey {
    param($Item)

    $sequenceCode = [string](Get-Value $Item "sequence_code")
    $numeroMinuta = [string](Get-Value $Item "cnr_c_s_fit_corporation_sequence_number")
    $invoicesMapping = Get-Value $Item "cnr_c_s_fit_invoices_mapping"
    $invoicesMappingJson = if ($null -eq $invoicesMapping) {
        ""
    } else {
        ConvertTo-Json @($invoicesMapping) -Compress -Depth 100
    }
    $startedAt = Normalize-OffsetDateTimeValue (Get-Value $Item "started_at")

    return "{0}|{1}|{2}|{3}" -f $sequenceCode, $numeroMinuta, $invoicesMappingJson, $startedAt
}

function Get-InventarioUniqueId {
    param($Item)

    return (Get-Sha256Hex -Text (Get-InventarioBusinessKey -Item $Item))
}

function Get-SinistroUniqueId {
    param($Item)

    $sequenceCode = [string](Get-Value $Item "sequence_code")
    $insuranceOccurrenceNumber = [string](Get-Value $Item "icm_fis_ioe_number")
    $corporationSequenceNumber = [string](Get-Value $Item "icm_fis_fit_corporation_sequence_number")
    $canonical = "{0}|{1}|{2}" -f $sequenceCode, $insuranceOccurrenceNumber, $corporationSequenceNumber
    if ($canonical -eq "||") {
        return $null
    }
    return (Get-Sha256Hex -Text $canonical)
}

function New-DataExportRequestBody {
    param(
        [string]$TabelaApi,
        [string]$CampoData,
        [string]$Range,
        [int]$Page,
        [int]$Per,
        [string]$OrderBy,
        [hashtable]$FiltrosExtras
    )

    $body = [ordered]@{
        search = [ordered]@{
            $TabelaApi = [ordered]@{
                $CampoData = $Range
            }
        }
        page = [string]$Page
        per = [string]$Per
        order_by = $OrderBy
    }

    if ($FiltrosExtras) {
        foreach ($key in ($FiltrosExtras.Keys | Sort-Object)) {
            $value = [string]$FiltrosExtras[$key]
            if (-not [string]::IsNullOrWhiteSpace($value)) {
                $body.search[$TabelaApi][$key] = $value
            }
        }
    }

    return ($body | ConvertTo-Json -Compress -Depth 20)
}

function New-DataExportUrl {
    param(
        [string]$BaseUrl,
        [int]$TemplateId,
        [string]$TabelaApi,
        [string]$CampoData,
        [string]$Range,
        [int]$Page,
        [int]$Per,
        [string]$OrderBy,
        [hashtable]$FiltrosExtras
    )

    $params = New-Object System.Collections.Generic.List[string]
    $params.Add(("page=" + $Page))
    $params.Add(("per=" + $Per))
    $params.Add(("order_by=" + [uri]::EscapeDataString($OrderBy)))
    $params.Add(
        ("search%5B{0}%5D%5B{1}%5D={2}" -f
            [uri]::EscapeDataString($TabelaApi),
            [uri]::EscapeDataString($CampoData),
            [uri]::EscapeDataString($Range))
    )

    if ($FiltrosExtras) {
        foreach ($key in ($FiltrosExtras.Keys | Sort-Object)) {
            $value = [string]$FiltrosExtras[$key]
            if (-not [string]::IsNullOrWhiteSpace($value)) {
                $params.Add(
                    ("search%5B{0}%5D%5B{1}%5D={2}" -f
                        [uri]::EscapeDataString($TabelaApi),
                        [uri]::EscapeDataString($key),
                        [uri]::EscapeDataString($value))
                )
            }
        }
    }

    return "{0}/api/analytics/reports/{1}/data?{2}" -f $BaseUrl, $TemplateId, ($params -join "&")
}

function Get-DataExportPages {
    param(
        [string]$Name,
        [string]$BaseUrl,
        [hashtable]$Headers,
        [int]$TemplateId,
        [string]$TabelaApi,
        [string]$CampoData,
        [string]$Range,
        [int]$Per,
        [string]$OrderBy,
        [hashtable]$FiltrosExtras,
        [string]$RawDir
    )

    $allItems = @()
    $page = 1
    $previousCount = $null
    $retriedUnexpectedEmpty = $false
    $anomalies = @()

    while ($true) {
        $url = "{0}/api/analytics/reports/{1}/data" -f $BaseUrl, $TemplateId
        $urlQuery = New-DataExportUrl -BaseUrl $BaseUrl -TemplateId $TemplateId -TabelaApi $TabelaApi -CampoData $CampoData -Range $Range -Page $page -Per $Per -OrderBy $OrderBy -FiltrosExtras $FiltrosExtras
        $body = New-DataExportRequestBody -TabelaApi $TabelaApi -CampoData $CampoData -Range $Range -Page $page -Per $Per -OrderBy $OrderBy -FiltrosExtras $FiltrosExtras
        $response = Invoke-CurlJson -Method "GET" -Url $url -Headers $Headers -Body $body
        if ($response.Status -in @("404", "405", "415", "422", "501")) {
            $response = Invoke-CurlJson -Method "GET" -Url $urlQuery -Headers $Headers -Body $null
        }

        if ($response.Status -ne "200") {
            throw ("Falha DataExport {0} pagina {1}: HTTP {2}" -f $Name, $page, $response.Status)
        }

        $payload = $response.Json
        $payloadData = Get-Value $payload "data"
        if ($payloadData -ne $null) {
            $payload = $payloadData
        }

        $items = @()
        if ($payload -is [System.Array]) {
            $items = @($payload)
        } elseif ($payload -ne $null) {
            $items = @($payload)
        }

        $rawPath = Join-Path $RawDir ("{0}-page{1}.json" -f $Name, $page)
        Set-Content -LiteralPath $rawPath -Value $response.Body -Encoding UTF8

        $currentCount = $items.Count
        if ($currentCount -eq 0) {
            if ($previousCount -ne $null -and $previousCount -eq $Per -and -not $retriedUnexpectedEmpty) {
                $retriedUnexpectedEmpty = $true
                $anomalies += "pagina_vazia_inesperada_na_pagina_$page"
                Start-Sleep -Seconds 2
                continue
            }
            break
        }

        $retriedUnexpectedEmpty = $false
        foreach ($item in $items) {
            $allItems += $item
        }

        $previousCount = $currentCount
        $page++
        Start-Sleep -Milliseconds 250
    }

    return [pscustomobject]@{
        Items = @($allItems)
        RawCount = $allItems.Count
        Pages = $page
        Anomalies = @($anomalies)
    }
}

function Get-GraphQlPages {
    param(
        [string]$Name,
        [string]$GraphQlUrl,
        [hashtable]$Headers,
        [string]$Query,
        [hashtable]$Params,
        [string]$RootField,
        [string]$RawDir
    )

    $allItems = @()
    $after = $null
    $page = 1
    $seenCursors = New-Object System.Collections.Generic.HashSet[string]
    $anomalies = @()

    while ($true) {
        $variables = @{
            params = $Params
            after = $after
        }

        $body = @{
            query = $Query
            variables = $variables
        } | ConvertTo-Json -Depth 30 -Compress

        $response = Invoke-CurlJson -Method "POST" -Url $GraphQlUrl -Headers $Headers -Body $body
        if ($response.Status -ne "200") {
            throw ("Falha GraphQL {0} pagina {1}: HTTP {2}" -f $Name, $page, $response.Status)
        }
        if ($null -eq $response.Json) {
            throw ("Falha GraphQL {0} pagina {1}: resposta nao JSON" -f $Name, $page)
        }
        $errors = Get-Value $response.Json "errors"
        if ($errors) {
            $message = Get-Value (@($errors)[0]) "message"
            throw ("Falha GraphQL {0} pagina {1}: {2}" -f $Name, $page, $message)
        }

        $rawPath = Join-Path $RawDir ("{0}-page{1}.json" -f $Name, $page)
        Set-Content -LiteralPath $rawPath -Value $response.Body -Encoding UTF8

        $container = Get-Value (Get-Value $response.Json "data") $RootField
        $edges = @()
        if ($container) {
            $edges = @((Get-Value $container "edges"))
        }

        foreach ($edge in $edges) {
            $node = Get-Value $edge "node"
            if ($node -ne $null) {
                $allItems += $node
            }
        }

        $pageInfo = if ($container) { Get-Value $container "pageInfo" } else { $null }
        $hasNextPage = [bool](Get-Value $pageInfo "hasNextPage")
        $endCursor = [string](Get-Value $pageInfo "endCursor")

        if (-not $hasNextPage) {
            break
        }
        if ([string]::IsNullOrWhiteSpace($endCursor)) {
            $anomalies += "hasNextPage_sem_cursor_na_pagina_$page"
            break
        }
        if (-not $seenCursors.Add($endCursor)) {
            $anomalies += "cursor_repetido_$endCursor"
            break
        }

        $after = $endCursor
        $page++
        Start-Sleep -Milliseconds 250
    }

    return [pscustomobject]@{
        Items = @($allItems)
        RawCount = $allItems.Count
        Pages = $page
        Anomalies = @($anomalies)
    }
}

function ConvertTo-KeySummary {
    param(
        [object[]]$Items,
        [scriptblock]$KeySelector
    )

    $rawKeys = @()
    foreach ($item in @($Items)) {
        $key = & $KeySelector $item
        if (-not [string]::IsNullOrWhiteSpace([string]$key)) {
            $rawKeys += [string]$key
        }
    }

    $distinct = New-Object System.Collections.Generic.HashSet[string]
    foreach ($key in $rawKeys) {
        [void]$distinct.Add($key)
    }

    return [pscustomobject]@{
        RawKeys = @($rawKeys)
        DistinctKeys = @($distinct)
        RawCount = $rawKeys.Count
        UniqueCount = $distinct.Count
    }
}

function Get-DbKeysForEntity {
    param(
        [hashtable]$Config,
        [string]$Entity,
        [pscustomobject]$Window
    )

    $windowStart = $Window.started_at
    $windowEnd = $Window.finished_at
    $periodStart = $Window.periodo_inicio
    $periodEnd = $Window.periodo_fim

    $query = switch ($Entity) {
        "coletas" {
            @"
SET NOCOUNT ON;
SELECT id
FROM dbo.coletas
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND id IS NOT NULL
ORDER BY id;
"@
        }
        "fretes" {
            @"
SET NOCOUNT ON;
SELECT CAST(id AS VARCHAR(50))
FROM dbo.fretes
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND id IS NOT NULL
ORDER BY 1;
"@
        }
        "manifestos" {
            @"
SET NOCOUNT ON;
SELECT CONCAT(
    CAST(sequence_code AS VARCHAR(50)),
    '|',
    COALESCE(CAST(pick_sequence_code AS VARCHAR(50)), '-1'),
    '|',
    COALESCE(CAST(mdfe_number AS VARCHAR(50)), '-1')
)
FROM dbo.manifestos
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND sequence_code IS NOT NULL
ORDER BY 1;
"@
        }
        "cotacoes" {
            @"
SET NOCOUNT ON;
SELECT CAST(sequence_code AS VARCHAR(50))
FROM dbo.cotacoes
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND sequence_code IS NOT NULL
ORDER BY 1;
"@
        }
        "localizacao_cargas" {
            @"
SET NOCOUNT ON;
SELECT CAST(sequence_number AS VARCHAR(50))
FROM dbo.localizacao_cargas
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND sequence_number IS NOT NULL
ORDER BY 1;
"@
        }
        "contas_a_pagar" {
            @"
SET NOCOUNT ON;
SELECT CAST(sequence_code AS VARCHAR(50))
FROM dbo.contas_a_pagar
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND sequence_code IS NOT NULL
ORDER BY 1;
"@
        }
        "faturas_por_cliente" {
            @"
SET NOCOUNT ON;
SELECT unique_id
FROM dbo.faturas_por_cliente
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND unique_id IS NOT NULL
ORDER BY 1;
"@
        }
        "inventario" {
            @"
SET NOCOUNT ON;
SELECT identificador_unico
FROM dbo.inventario
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND identificador_unico IS NOT NULL
ORDER BY 1;
"@
        }
        "sinistros" {
            @"
SET NOCOUNT ON;
SELECT identificador_unico
FROM dbo.sinistros
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND identificador_unico IS NOT NULL
ORDER BY 1;
"@
        }
        "faturas_graphql" {
            @"
SET NOCOUNT ON;
SELECT CAST(id AS VARCHAR(50))
FROM dbo.faturas_graphql
WHERE data_extracao >= '$windowStart'
  AND data_extracao <= '$windowEnd'
  AND id IS NOT NULL
ORDER BY 1;
"@
        }
        "usuarios_sistema" {
            @"
SET NOCOUNT ON;
SELECT CAST(user_id AS VARCHAR(50))
FROM dbo.dim_usuarios
WHERE ativo = 1
  AND (
        (origem_atualizado_em >= '$periodStart'
         AND origem_atualizado_em <= '$periodEnd')
     OR (origem_atualizado_em IS NULL
         AND COALESCE(ultima_extracao_em, data_atualizacao) >= '$windowStart'
         AND COALESCE(ultima_extracao_em, data_atualizacao) <= '$windowEnd')
  )
  AND user_id IS NOT NULL
ORDER BY 1;
"@
        }
        default {
            throw "Entidade sem query SQL configurada: $Entity"
        }
    }

    $rows = Invoke-SqlLines -Config $Config -Query $query
    $distinct = New-Object System.Collections.Generic.HashSet[string]
    foreach ($row in $rows) {
        [void]$distinct.Add($row)
    }

    return [pscustomobject]@{
        RawKeys = @($rows)
        DistinctKeys = @($distinct)
        RawCount = $rows.Count
        UniqueCount = $distinct.Count
    }
}

function Get-FreteAccountingCreditIds {
    param(
        [hashtable]$Config,
        [pscustomobject]$Window
    )

    $query = @"
SET NOCOUNT ON;
SELECT CAST(accounting_credit_id AS VARCHAR(50))
FROM dbo.fretes
WHERE data_extracao >= '$($Window.started_at)'
  AND data_extracao <= '$($Window.finished_at)'
  AND accounting_credit_id IS NOT NULL
ORDER BY 1;
"@

    $rows = Invoke-SqlLines -Config $Config -Query $query
    $distinct = New-Object System.Collections.Generic.HashSet[string]
    foreach ($row in $rows) {
        [void]$distinct.Add($row)
    }
    return @($distinct)
}

function New-RegularComparison {
    param(
        [string]$Entity,
        [pscustomobject]$Api,
        [pscustomobject]$Db,
        [string[]]$Anomalies
    )

    $apiSet = New-Object System.Collections.Generic.HashSet[string]
    foreach ($key in $Api.DistinctKeys) { [void]$apiSet.Add($key) }
    $dbSet = New-Object System.Collections.Generic.HashSet[string]
    foreach ($key in $Db.DistinctKeys) { [void]$dbSet.Add($key) }

    $missingInDb = @($apiSet | Where-Object { -not $dbSet.Contains($_) } | Sort-Object)
    $onlyInDb = @($dbSet | Where-Object { -not $apiSet.Contains($_) } | Sort-Object)

    $sampleKeys = @($Api.DistinctKeys | Sort-Object | Select-Object -First 5)
    $sampleHits = @(
        $sampleKeys | ForEach-Object {
            [pscustomobject]@{
                chave = $_
                hit_no_banco = $dbSet.Contains($_)
            }
        }
    )

    $ok = ($missingInDb.Count -eq 0 -and $onlyInDb.Count -eq 0 -and @($Anomalies).Count -eq 0)
    return [pscustomobject]@{
        entidade = $Entity
        api_raw = $Api.RawCount
        api_unico = $Api.UniqueCount
        banco_linhas = $Db.RawCount
        banco_unico = $Db.UniqueCount
        faltantes_no_banco = $missingInDb.Count
        somente_no_banco = $onlyInDb.Count
        anomalias_api = @($Anomalies)
        amostra_api = $sampleHits
        ok = $ok
        detalhe = if ($ok) { "CHAVES_OK" } else { "DIVERGENCIA_DE_CHAVES" }
    }
}

function New-FaturasGraphqlComparison {
    param(
        [pscustomobject]$Api,
        [pscustomobject]$Db,
        [string[]]$FreteCreditIds,
        [string[]]$Anomalies
    )

    $apiSet = New-Object System.Collections.Generic.HashSet[string]
    foreach ($key in $Api.DistinctKeys) { [void]$apiSet.Add($key) }
    $dbSet = New-Object System.Collections.Generic.HashSet[string]
    foreach ($key in $Db.DistinctKeys) { [void]$dbSet.Add($key) }
    $freteSet = New-Object System.Collections.Generic.HashSet[string]
    foreach ($key in $FreteCreditIds) { [void]$freteSet.Add($key) }

    $missingInDb = @($apiSet | Where-Object { -not $dbSet.Contains($_) } | Sort-Object)
    $onlyInDbRaw = @($dbSet | Where-Object { -not $apiSet.Contains($_) } | Sort-Object)
    $tolerated = @($onlyInDbRaw | Where-Object { $freteSet.Contains($_) } | Sort-Object)
    $onlyInDb = @($onlyInDbRaw | Where-Object { -not $freteSet.Contains($_) } | Sort-Object)

    $sampleKeys = @($Api.DistinctKeys | Sort-Object | Select-Object -First 5)
    $sampleHits = @(
        $sampleKeys | ForEach-Object {
            [pscustomobject]@{
                chave = $_
                hit_no_banco = $dbSet.Contains($_)
            }
        }
    )

    $ok = ($missingInDb.Count -eq 0 -and $onlyInDb.Count -eq 0 -and @($Anomalies).Count -eq 0)
    return [pscustomobject]@{
        entidade = "faturas_graphql"
        api_raw = $Api.RawCount
        api_unico = $Api.UniqueCount
        banco_linhas = $Db.RawCount
        banco_unico = $Db.UniqueCount
        faltantes_no_banco = $missingInDb.Count
        somente_no_banco = $onlyInDb.Count
        somente_no_banco_bruto = $onlyInDbRaw.Count
        tolerados_referenciais = $tolerated.Count
        anomalias_api = @($Anomalies)
        amostra_api = $sampleHits
        amostra_tolerados = @($tolerated | Select-Object -First 5)
        ok = $ok
        detalhe = if ($ok) { "CHAVES_OK_COM_TOLERANCIA_REFERENCIAL" } else { "DIVERGENCIA_DE_CHAVES" }
    }
}

function Save-MarkdownReport {
    param(
        [string]$Path,
        [string]$DataInicio,
        [string]$DataFim,
        [string]$ExecutionUuid,
        [pscustomobject[]]$Results
    )

    $okCount = @($Results | Where-Object { $_.ok }).Count
    $failCount = @($Results | Where-Object { -not $_.ok }).Count

    $lines = New-Object System.Collections.Generic.List[string]
    $tick = [char]96
    [void]$lines.Add("# Comparativo CURL x Banco - Periodo Fechado")
    [void]$lines.Add("")
    [void]$lines.Add("- Janela validada: $tick$DataInicio$tick a $tick$DataFim$tick")
    [void]$lines.Add("- Execution UUID ancora: $tick$ExecutionUuid$tick")
    [void]$lines.Add("- Resultado consolidado: ok=$tick$okCount$tick, falhas=$tick$failCount$tick")
    [void]$lines.Add("")

    foreach ($result in $Results) {
        [void]$lines.Add("## $($result.entidade)")
        [void]$lines.Add("")
        $status = if ($result.ok) { "OK" } else { "FALHA" }
        [void]$lines.Add("- Status: $tick$status$tick")
        [void]$lines.Add("- API: bruto=$($result.api_raw), unico=$($result.api_unico)")
        [void]$lines.Add("- Banco: linhas=$($result.banco_linhas), unico=$($result.banco_unico)")
        [void]$lines.Add("- Faltantes no banco: $($result.faltantes_no_banco)")
        [void]$lines.Add("- Somente no banco: $($result.somente_no_banco)")
        if ($result.PSObject.Properties["tolerados_referenciais"]) {
            [void]$lines.Add("- Tolerados referenciais: $($result.tolerados_referenciais)")
        }
        if (@($result.anomalias_api).Count -gt 0) {
            [void]$lines.Add("- Anomalias API: $(@($result.anomalias_api) -join ", ")")
        }
        [void]$lines.Add("- Detalhe: $($result.detalhe)")
        [void]$lines.Add("")
        [void]$lines.Add("| Chave API | Hit no banco |")
        [void]$lines.Add("|---|---:|")
        foreach ($sample in @($result.amostra_api)) {
            [void]$lines.Add("| $($sample.chave) | $(if ($sample.hit_no_banco) { 1 } else { 0 }) |")
        }
        [void]$lines.Add("")
    }

    Set-Content -LiteralPath $Path -Value $lines -Encoding UTF8
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$cfg = Load-EnvFile -Path (Join-Path $root ".env")
$baseUrl = $cfg["API_BASE_URL"]
$graphQlUrl = $baseUrl + $cfg["API_GRAPHQL_ENDPOINT"]
$range = "$DataInicio - $DataFim"
$resultsDir = Join-Path $root "src\test\results"
$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$artifactBase = "curl-vs-banco-periodo-fechado-$timestamp"
$rawDir = Join-Path $resultsDir ($artifactBase + "-raw")
$jsonPath = Join-Path $resultsDir ($artifactBase + ".json")
$mdPath = Join-Path $resultsDir ($artifactBase + ".md")

New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null
New-Item -ItemType Directory -Force -Path $rawDir | Out-Null

$graphQlHeaders = @{
    Authorization = "Bearer " + $cfg["API_GRAPHQL_TOKEN"]
    "Content-Type" = "application/json"
}
$dataExportHeaders = @{
    Authorization = "Bearer " + $cfg["API_DATAEXPORT_TOKEN"]
    Accept = "application/json"
}

$requiredEntities = @(
    "usuarios_sistema",
    "coletas",
    "fretes",
    "faturas_graphql",
    "manifestos",
    "cotacoes",
    "localizacao_cargas",
    "contas_a_pagar",
    "faturas_por_cliente",
    "inventario",
    "sinistros"
)

if (-not $ExecutionUuid) {
    $windowStart = ([datetime]::ParseExact($DataInicio, "yyyy-MM-dd", [System.Globalization.CultureInfo]::InvariantCulture)).ToString("yyyy-MM-ddT00:00:00")
    $windowEnd = ([datetime]::ParseExact($DataFim, "yyyy-MM-dd", [System.Globalization.CultureInfo]::InvariantCulture).AddDays(1)).ToString("yyyy-MM-ddT00:00:00")
    $entityList = ($requiredEntities | ForEach-Object { "'" + $_ + "'" }) -join ","
    $anchorQuery = @"
SET NOCOUNT ON;
WITH candidatos AS (
    SELECT
        execution_uuid,
        COUNT(DISTINCT entidade) AS entidades_cobertas,
        MAX(finished_at) AS ultimo_fim
    FROM dbo.sys_execution_audit
    WHERE status_execucao IN ('COMPLETO', 'RECONCILIADO', 'RECONCILED')
      AND api_completa = 1
      AND janela_consulta_inicio = '$windowStart'
      AND janela_consulta_fim = '$windowEnd'
      AND entidade IN ($entityList)
    GROUP BY execution_uuid
)
SELECT TOP 1 execution_uuid
FROM candidatos
ORDER BY entidades_cobertas DESC, ultimo_fim DESC;
"@
    $ExecutionUuid = Invoke-SqlScalar -Config $cfg -Query $anchorQuery
}

if (-not $ExecutionUuid) {
    throw "Nao foi possivel resolver execution_uuid ancora."
}

$windowsQuery = @"
SET NOCOUNT ON;
SELECT
    entidade + '|' +
    CONVERT(VARCHAR(33), started_at, 126) + '|' +
    CONVERT(VARCHAR(33), finished_at, 126) + '|' +
    CONVERT(VARCHAR(33), janela_consulta_inicio, 126) + '|' +
    CONVERT(VARCHAR(33), janela_consulta_fim, 126) + '|' +
    CAST(db_persistidos AS VARCHAR(20))
FROM dbo.sys_execution_audit
WHERE execution_uuid = '$ExecutionUuid'
  AND entidade IN ($(($requiredEntities | ForEach-Object { "'" + $_ + "'" }) -join ","))
  AND status_execucao IN ('COMPLETO', 'RECONCILIADO', 'RECONCILED')
  AND api_completa = 1
ORDER BY entidade;
"@

$windowRows = Invoke-SqlLines -Config $cfg -Query $windowsQuery
$windows = @{}
foreach ($row in $windowRows) {
    $parts = $row -split "\|", 6
    if ($parts.Count -lt 6) {
        continue
    }
    $periodStart = [datetime]::Parse($parts[3]).ToString("yyyy-MM-ddT00:00:00")
    $periodEndRaw = [datetime]::Parse($parts[4])
    $periodEnd = if ($periodEndRaw.TimeOfDay -eq [TimeSpan]::Zero -and $periodEndRaw.Date -gt [datetime]::Parse($parts[3]).Date) {
        $periodEndRaw.AddDays(-1).ToString("yyyy-MM-ddT23:59:59.997")
    } else {
        $periodEndRaw.ToString("yyyy-MM-ddT23:59:59.997")
    }
    $windows[$parts[0]] = [pscustomobject]@{
        entidade = $parts[0]
        started_at = $parts[1]
        finished_at = $parts[2]
        periodo_inicio = $periodStart
        periodo_fim = $periodEnd
        db_persistidos = [int]$parts[5]
    }
}

foreach ($entity in $requiredEntities) {
    if (-not $windows.ContainsKey($entity)) {
        throw "Janela estruturada ausente para entidade $entity na execucao $ExecutionUuid"
    }
}

$queryColetas = @'
query ProbeColetas($params: PickInput!, $after: String) {
  pick(params: $params, after: $after, first: 100) {
    edges {
      node {
        id
        sequenceCode
        requestDate
        serviceDate
        statusUpdatedAt
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
'@

$queryFretes = @'
query ProbeFretes($params: FreightInput!, $after: String) {
  freight(params: $params, after: $after, first: 100) {
    edges {
      node {
        id
        accountingCreditId
        serviceAt
        createdAt
        serviceDate
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
'@

$queryUsuarios = @'
query ProbeUsuarios($params: IndividualInput!, $after: String) {
  individual(params: $params, first: 1000, after: $after) {
    edges {
      node {
        id
        name
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
'@

$queryFaturasGraphql = @'
query ProbeFaturas($params: CreditCustomerBillingInput!, $after: String) {
  creditCustomerBilling(params: $params, first: 100, after: $after) {
    edges {
      node {
        id
        document
        dueDate
        issueDate
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
'@

$coletasPages = Get-GraphQlPages -Name "coletas" -GraphQlUrl $graphQlUrl -Headers $graphQlHeaders -Query $queryColetas -Params @{ requestDate = $DataInicio } -RootField "pick" -RawDir $rawDir
$fretesPages = Get-GraphQlPages -Name "fretes" -GraphQlUrl $graphQlUrl -Headers $graphQlHeaders -Query $queryFretes -Params @{ serviceAt = $range } -RootField "freight" -RawDir $rawDir
$usuariosPages = Get-GraphQlPages -Name "usuarios_sistema" -GraphQlUrl $graphQlUrl -Headers $graphQlHeaders -Query $queryUsuarios -Params @{ enabled = $true; updatedAt = $range } -RootField "individual" -RawDir $rawDir

$billingParams = @{ dueDate = $DataInicio }
if ($cfg.ContainsKey("API_CORPORATION_ID") -and -not [string]::IsNullOrWhiteSpace($cfg["API_CORPORATION_ID"])) {
    $billingParams["corporationId"] = $cfg["API_CORPORATION_ID"]
}
$faturasGraphqlPages = Get-GraphQlPages -Name "faturas_graphql" -GraphQlUrl $graphQlUrl -Headers $graphQlHeaders -Query $queryFaturasGraphql -Params $billingParams -RootField "creditCustomerBilling" -RawDir $rawDir

$manifestosPages = Get-DataExportPages -Name "manifestos" -BaseUrl $baseUrl -Headers $dataExportHeaders -TemplateId 6399 -TabelaApi "manifests" -CampoData "service_date" -Range $range -Per 100 -OrderBy "sequence_code asc" -FiltrosExtras @{} -RawDir $rawDir
$cotacoesPages = Get-DataExportPages -Name "cotacoes" -BaseUrl $baseUrl -Headers $dataExportHeaders -TemplateId 6906 -TabelaApi "quotes" -CampoData "requested_at" -Range $range -Per 100 -OrderBy "sequence_code asc" -FiltrosExtras @{} -RawDir $rawDir
$localizacaoPages = Get-DataExportPages -Name "localizacao_cargas" -BaseUrl $baseUrl -Headers $dataExportHeaders -TemplateId 8656 -TabelaApi "freights" -CampoData "service_at" -Range $range -Per 10000 -OrderBy "sequence_number asc" -FiltrosExtras @{} -RawDir $rawDir
$contasPages = Get-DataExportPages -Name "contas_a_pagar" -BaseUrl $baseUrl -Headers $dataExportHeaders -TemplateId 8636 -TabelaApi "accounting_debits" -CampoData "issue_date" -Range $range -Per 100 -OrderBy "issue_date desc" -FiltrosExtras @{ created_at = $range } -RawDir $rawDir
$faturasClientePages = Get-DataExportPages -Name "faturas_por_cliente" -BaseUrl $baseUrl -Headers $dataExportHeaders -TemplateId 4924 -TabelaApi "freights" -CampoData "service_at" -Range $range -Per 100 -OrderBy "unique_id asc" -FiltrosExtras @{} -RawDir $rawDir
$inventarioPages = Get-DataExportPages -Name "inventario" -BaseUrl $baseUrl -Headers $dataExportHeaders -TemplateId 10633 -TabelaApi "check_in_orders" -CampoData "started_at" -Range $range -Per 100 -OrderBy "sequence_code asc" -FiltrosExtras @{} -RawDir $rawDir
$sinistrosPages = Get-DataExportPages -Name "sinistros" -BaseUrl $baseUrl -Headers $dataExportHeaders -TemplateId 6392 -TabelaApi "insurance_claims" -CampoData "opening_at_date" -Range $range -Per 100 -OrderBy "sequence_code asc" -FiltrosExtras @{} -RawDir $rawDir

$apiColetas = ConvertTo-KeySummary -Items $coletasPages.Items -KeySelector { param($item) [string](Get-Value $item "id") }
$apiFretes = ConvertTo-KeySummary -Items $fretesPages.Items -KeySelector { param($item) [string](Get-Value $item "id") }
$apiUsuarios = ConvertTo-KeySummary -Items $usuariosPages.Items -KeySelector { param($item) [string](Get-Value $item "id") }
$apiFaturasGraphql = ConvertTo-KeySummary -Items $faturasGraphqlPages.Items -KeySelector { param($item) [string](Get-Value $item "id") }
$apiManifestos = ConvertTo-KeySummary -Items $manifestosPages.Items -KeySelector {
    param($item)
    $seq = [string](Get-Value $item "sequence_code")
    $pick = [string](Get-Value $item "mft_pfs_pck_sequence_code")
    $mdfe = [string](Get-Value $item "mft_mfs_number")
    if ([string]::IsNullOrWhiteSpace($seq)) { return $null }
    return "{0}|{1}|{2}" -f $seq, ($(if ([string]::IsNullOrWhiteSpace($pick)) { "-1" } else { $pick })), ($(if ([string]::IsNullOrWhiteSpace($mdfe)) { "-1" } else { $mdfe }))
}
$apiCotacoes = ConvertTo-KeySummary -Items $cotacoesPages.Items -KeySelector { param($item) [string](Get-Value $item "sequence_code") }
$apiLocalizacao = ConvertTo-KeySummary -Items $localizacaoPages.Items -KeySelector {
    param($item)
    $value = Get-Value $item "corporation_sequence_number"
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) {
        $value = Get-Value $item "sequence_number"
    }
    return [string]$value
}
$apiContas = ConvertTo-KeySummary -Items $contasPages.Items -KeySelector { param($item) [string](Get-Value $item "ant_ils_sequence_code") }
$apiFaturasCliente = ConvertTo-KeySummary -Items $faturasClientePages.Items -KeySelector { param($item) Get-FaturaPorClienteUniqueId -Item $item }
$apiInventario = ConvertTo-KeySummary -Items $inventarioPages.Items -KeySelector { param($item) Get-InventarioUniqueId -Item $item }
$apiSinistros = ConvertTo-KeySummary -Items $sinistrosPages.Items -KeySelector { param($item) Get-SinistroUniqueId -Item $item }

$dbColetas = Get-DbKeysForEntity -Config $cfg -Entity "coletas" -Window $windows["coletas"]
$dbFretes = Get-DbKeysForEntity -Config $cfg -Entity "fretes" -Window $windows["fretes"]
$dbUsuarios = Get-DbKeysForEntity -Config $cfg -Entity "usuarios_sistema" -Window $windows["usuarios_sistema"]
$dbFaturasGraphql = Get-DbKeysForEntity -Config $cfg -Entity "faturas_graphql" -Window $windows["faturas_graphql"]
$dbManifestos = Get-DbKeysForEntity -Config $cfg -Entity "manifestos" -Window $windows["manifestos"]
$dbCotacoes = Get-DbKeysForEntity -Config $cfg -Entity "cotacoes" -Window $windows["cotacoes"]
$dbLocalizacao = Get-DbKeysForEntity -Config $cfg -Entity "localizacao_cargas" -Window $windows["localizacao_cargas"]
$dbContas = Get-DbKeysForEntity -Config $cfg -Entity "contas_a_pagar" -Window $windows["contas_a_pagar"]
$dbFaturasCliente = Get-DbKeysForEntity -Config $cfg -Entity "faturas_por_cliente" -Window $windows["faturas_por_cliente"]
$dbInventario = Get-DbKeysForEntity -Config $cfg -Entity "inventario" -Window $windows["inventario"]
$dbSinistros = Get-DbKeysForEntity -Config $cfg -Entity "sinistros" -Window $windows["sinistros"]
$freteAccountingCreditIds = Get-FreteAccountingCreditIds -Config $cfg -Window $windows["fretes"]

$results = @(
    (New-RegularComparison -Entity "usuarios_sistema" -Api $apiUsuarios -Db $dbUsuarios -Anomalies $usuariosPages.Anomalies),
    (New-RegularComparison -Entity "coletas" -Api $apiColetas -Db $dbColetas -Anomalies $coletasPages.Anomalies),
    (New-RegularComparison -Entity "fretes" -Api $apiFretes -Db $dbFretes -Anomalies $fretesPages.Anomalies),
    (New-FaturasGraphqlComparison -Api $apiFaturasGraphql -Db $dbFaturasGraphql -FreteCreditIds $freteAccountingCreditIds -Anomalies $faturasGraphqlPages.Anomalies),
    (New-RegularComparison -Entity "manifestos" -Api $apiManifestos -Db $dbManifestos -Anomalies $manifestosPages.Anomalies),
    (New-RegularComparison -Entity "cotacoes" -Api $apiCotacoes -Db $dbCotacoes -Anomalies $cotacoesPages.Anomalies),
    (New-RegularComparison -Entity "localizacao_cargas" -Api $apiLocalizacao -Db $dbLocalizacao -Anomalies $localizacaoPages.Anomalies),
    (New-RegularComparison -Entity "contas_a_pagar" -Api $apiContas -Db $dbContas -Anomalies $contasPages.Anomalies),
    (New-RegularComparison -Entity "faturas_por_cliente" -Api $apiFaturasCliente -Db $dbFaturasCliente -Anomalies $faturasClientePages.Anomalies),
    (New-RegularComparison -Entity "inventario" -Api $apiInventario -Db $dbInventario -Anomalies $inventarioPages.Anomalies),
    (New-RegularComparison -Entity "sinistros" -Api $apiSinistros -Db $dbSinistros -Anomalies $sinistrosPages.Anomalies)
)

$summary = [ordered]@{
    executado_em = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    data_inicio = $DataInicio
    data_fim = $DataFim
    execution_uuid = $ExecutionUuid
    resultados = $results
}

$summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8
Save-MarkdownReport -Path $mdPath -DataInicio $DataInicio -DataFim $DataFim -ExecutionUuid $ExecutionUuid -Results $results

$summary | ConvertTo-Json -Depth 8
# __BODY__
# __BODY__
