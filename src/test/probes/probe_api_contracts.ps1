param(
    [string]$DataInicio = ((Get-Date).AddDays(-1).ToString("yyyy-MM-dd")),
    [string]$DataFim = (Get-Date).ToString("yyyy-MM-dd")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Load-EnvFile {
    param([string]$Path)

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

function Invoke-CurlJson {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        [string]$Body,
        [int]$MaxAttempts = 3,
        [int]$RetryDelaySeconds = 4
    )

    $last = $null
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $args = @("-sS", "-X", $Method, $Url)
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
            $json = $bodyOnly | ConvertFrom-Json
        } catch {
            $json = $null
        }

        $last = [pscustomobject]@{
            Status = $status
            Body = $bodyOnly
            Json = $json
        }

        $needsRetry = $status -eq "429" -or $bodyOnly -match "Retry later"
        if (-not $needsRetry -or $attempt -eq $MaxAttempts) {
            return $last
        }

        Start-Sleep -Seconds $RetryDelaySeconds
    }

    return $last
}

function Get-FieldNames {
    param($TypeJson)

    if (-not $TypeJson -or -not $TypeJson.inputFields) {
        return @()
    }
    return ,@($TypeJson.inputFields | ForEach-Object { $_.name })
}

function Test-GraphQlAccepted {
    param($Response)

    if ($null -eq $Response -or $null -eq $Response.Json) {
        return $false
    }

    $errorsProp = $Response.Json.PSObject.Properties["errors"]
    return ($null -eq $errorsProp -or $null -eq $errorsProp.Value)
}

function Get-GraphQlErrorMessage {
    param($Response)

    if ($null -eq $Response -or $null -eq $Response.Json) {
        return $null
    }

    $errorsProp = $Response.Json.PSObject.Properties["errors"]
    if ($null -eq $errorsProp -or $null -eq $errorsProp.Value) {
        return $null
    }

    $firstError = @($errorsProp.Value | Select-Object -First 1)[0]
    return Get-PropertyValue -Item $firstError -PropertyName "message"
}

function Get-PropertyValue {
    param(
        $Item,
        [string]$PropertyName
    )

    if ($null -eq $Item) {
        return $null
    }

    $prop = $Item.PSObject.Properties[$PropertyName]
    if ($null -eq $prop) {
        return $null
    }

    return $prop.Value
}

function Get-Overlap {
    param(
        [object[]]$Page1,
        [object[]]$Page2,
        [string]$KeyName
    )

    $set1 = @($Page1 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName $KeyName } | Where-Object { $null -ne $_ })
    $set2 = @($Page2 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName $KeyName } | Where-Object { $null -ne $_ })
    $overlap = @($set1 | Where-Object { $set2 -contains $_ } | Select-Object -Unique)
    return $overlap
}

function Get-Duplicates {
    param(
        [object[]]$Items,
        [string]$KeyName
    )

    return @(
        $Items |
            ForEach-Object { Get-PropertyValue -Item $_ -PropertyName $KeyName } |
            Where-Object { $null -ne $_ } |
            Group-Object |
            Where-Object { $_.Count -gt 1 } |
            Select-Object -ExpandProperty Name
    )
}

function Test-TieBreakAscending {
    param(
        [object[]]$Items,
        [string]$DateField,
        [string]$KeyField
    )

    $groups = $Items | Group-Object { [string](Get-PropertyValue -Item $_ -PropertyName $DateField) }
    foreach ($group in $groups) {
        $keys = @(
            $group.Group |
                ForEach-Object { Get-PropertyValue -Item $_ -PropertyName $KeyField } |
                Where-Object { $null -ne $_ } |
                ForEach-Object { [string]$_ }
        )
        if ($keys.Count -lt 2) {
            continue
        }
        $sorted = @($keys | Sort-Object)
        if (($keys -join ",") -ne ($sorted -join ",")) {
            return $false
        }
    }
    return $true
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$cfg = Load-EnvFile -Path (Join-Path $root ".env")

$graphqlUrl = $cfg["API_BASE_URL"] + $cfg["API_GRAPHQL_ENDPOINT"]
$dataExportBase = $cfg["API_BASE_URL"] + "/api/analytics/reports"
$range = "$DataInicio - $DataFim"

$graphqlHeaders = @{
    Authorization = "Bearer " + $cfg["API_GRAPHQL_TOKEN"]
    "Content-Type" = "application/json"
}
$dataExportHeaders = @{
    Authorization = "Bearer " + $cfg["API_DATAEXPORT_TOKEN"]
    Accept = "application/json"
}

$pickRequestDateBody = '{"query":"query TestPick($params: PickInput!, $after: String){ pick(params: $params, after: $after, first: 1){ edges { node { id sequenceCode requestDate serviceDate statusUpdatedAt } } pageInfo { hasNextPage endCursor } } }","variables":{"params":{"requestDate":"__DATE__"}}}'
$pickServiceDateBody = '{"query":"query TestPick($params: PickInput!, $after: String){ pick(params: $params, after: $after, first: 1){ edges { node { id sequenceCode requestDate serviceDate statusUpdatedAt } } pageInfo { hasNextPage endCursor } } }","variables":{"params":{"serviceDate":"__DATE__"}}}'
$pickUpdatedAtBody = '{"query":"query TestPick($params: PickInput!, $after: String){ pick(params: $params, after: $after, first: 1){ edges { node { id sequenceCode requestDate serviceDate statusUpdatedAt } } pageInfo { hasNextPage endCursor } } }","variables":{"params":{"updatedAt":"__DATE__ - __DATE__"}}}'
$freightServiceAtBody = '{"query":"query TestFreight($params: FreightInput!, $after: String){ freight(params: $params, after: $after, first: 1){ edges { node { id serviceAt createdAt serviceDate } } pageInfo { hasNextPage endCursor } } }","variables":{"params":{"serviceAt":"__START__ - __END__"}}}'
$freightCreatedAtBody = '{"query":"query TestFreight($params: FreightInput!, $after: String){ freight(params: $params, after: $after, first: 1){ edges { node { id serviceAt createdAt serviceDate } } pageInfo { hasNextPage endCursor } } }","variables":{"params":{"createdAt":"__START__ - __END__"}}}'
$freightUpdatedAtBody = '{"query":"query TestFreight($params: FreightInput!, $after: String){ freight(params: $params, after: $after, first: 1){ edges { node { id serviceAt createdAt serviceDate } } pageInfo { hasNextPage endCursor } } }","variables":{"params":{"updatedAt":"__START__ - __END__"}}}'

$pickRequestDateResp = Invoke-CurlJson -Method "POST" -Url $graphqlUrl -Headers $graphqlHeaders -Body ($pickRequestDateBody.Replace("__DATE__", $DataInicio))
Start-Sleep -Seconds 2
$pickServiceDateResp = Invoke-CurlJson -Method "POST" -Url $graphqlUrl -Headers $graphqlHeaders -Body ($pickServiceDateBody.Replace("__DATE__", $DataInicio))
Start-Sleep -Seconds 2
$pickUpdatedAtResp = Invoke-CurlJson -Method "POST" -Url $graphqlUrl -Headers $graphqlHeaders -Body ($pickUpdatedAtBody.Replace("__DATE__", $DataInicio))
Start-Sleep -Seconds 2
$freightServiceAtResp = Invoke-CurlJson -Method "POST" -Url $graphqlUrl -Headers $graphqlHeaders -Body ($freightServiceAtBody.Replace("__START__", $DataInicio).Replace("__END__", $DataFim))
Start-Sleep -Seconds 2
$freightCreatedAtResp = Invoke-CurlJson -Method "POST" -Url $graphqlUrl -Headers $graphqlHeaders -Body ($freightCreatedAtBody.Replace("__START__", $DataInicio).Replace("__END__", $DataFim))
Start-Sleep -Seconds 2
$freightUpdatedAtResp = Invoke-CurlJson -Method "POST" -Url $graphqlUrl -Headers $graphqlHeaders -Body ($freightUpdatedAtBody.Replace("__START__", $DataInicio).Replace("__END__", $DataFim))
Start-Sleep -Seconds 2
$contasInfoResp = Invoke-CurlJson -Method "GET" -Url ($dataExportBase + "/8636/info") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$cotacoesInfoResp = Invoke-CurlJson -Method "GET" -Url ($dataExportBase + "/6906/info") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$cotacoesDataResp = Invoke-CurlJson -Method "GET" -Url ($dataExportBase + "/6906/data?search%5Bquotes%5D%5Brequested_at%5D=" + [uri]::EscapeDataString($range) + "&page=1&per=1&order_by=sequence_code%20asc") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$localizacaoInfoResp = Invoke-CurlJson -Method "GET" -Url ($dataExportBase + "/8656/info") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$localizacaoDataResp = Invoke-CurlJson -Method "GET" -Url ($dataExportBase + "/8656/data?search%5Bfreights%5D%5Bservice_at%5D=" + [uri]::EscapeDataString($range) + "&page=1&per=1&order_by=sequence_number%20asc") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$faturasInfoResp = Invoke-CurlJson -Method "GET" -Url ($dataExportBase + "/4924/info") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$faturasDataResp = Invoke-CurlJson -Method "GET" -Url ($dataExportBase + "/4924/data?search%5Bfreights%5D%5Bservice_at%5D=" + [uri]::EscapeDataString($range) + "&page=1&per=1&order_by=unique_id%20asc") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2

$queryBase = $dataExportBase + "/8636/data?search%5Baccounting_debits%5D%5Bissue_date%5D=" + [uri]::EscapeDataString($range) + "&search%5Baccounting_debits%5D%5Bcreated_at%5D=&per=50"
$queryBaseCreatedAt = $dataExportBase + "/8636/data?search%5Baccounting_debits%5D%5Bissue_date%5D=" + [uri]::EscapeDataString($range) + "&search%5Baccounting_debits%5D%5Bcreated_at%5D=" + [uri]::EscapeDataString($range) + "&per=5"
$currentPage1 = Invoke-CurlJson -Method "GET" -Url ($queryBase + "&page=1&order_by=issue_date%20desc") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$currentPage2 = Invoke-CurlJson -Method "GET" -Url ($queryBase + "&page=2&order_by=issue_date%20desc") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$createdAtFilteredPage1 = Invoke-CurlJson -Method "GET" -Url ($queryBaseCreatedAt + "&page=1&order_by=issue_date") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$ascendingPage1 = Invoke-CurlJson -Method "GET" -Url ($queryBase + "&page=1&order_by=issue_date%20asc") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$candidatePage1 = Invoke-CurlJson -Method "GET" -Url ($queryBase + "&page=1&order_by=issue_date%20desc%2Cant_ils_sequence_code%20asc") -Headers $dataExportHeaders -Body $null
Start-Sleep -Seconds 2
$candidatePage2 = Invoke-CurlJson -Method "GET" -Url ($queryBase + "&page=2&order_by=issue_date%20desc%2Cant_ils_sequence_code%20asc") -Headers $dataExportHeaders -Body $null

$currentItems1 = if ($currentPage1.Json -is [System.Array]) { @($currentPage1.Json) } else { @() }
$currentItems2 = if ($currentPage2.Json -is [System.Array]) { @($currentPage2.Json) } else { @() }
$createdAtFilteredItems1 = if ($createdAtFilteredPage1.Json -is [System.Array]) { @($createdAtFilteredPage1.Json) } else { @() }
$ascendingItems1 = if ($ascendingPage1.Json -is [System.Array]) { @($ascendingPage1.Json) } else { @() }
$candidateItems1 = if ($candidatePage1.Json -is [System.Array]) { @($candidatePage1.Json) } else { @() }
$candidateItems2 = if ($candidatePage2.Json -is [System.Array]) { @($candidatePage2.Json) } else { @() }

$contasOrderableFields = @()
if ($contasInfoResp.Json) {
    $fieldProps = $contasInfoResp.Json.PSObject.Properties["fields"]
    if ($fieldProps -and $fieldProps.Value) {
        $contasOrderableFields = @(
            $fieldProps.Value.PSObject.Properties |
                Where-Object { (Get-PropertyValue -Item $_.Value -PropertyName "order") -eq $true } |
                ForEach-Object { $_.Name }
        )
    }
}

$summary = [ordered]@{
    executed_at = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    period = $range
    graphql = [ordered]@{
        pick_filters = [ordered]@{
            requestDate = [ordered]@{
                http_status = $pickRequestDateResp.Status
                accepted = (Test-GraphQlAccepted -Response $pickRequestDateResp)
                error = (Get-GraphQlErrorMessage -Response $pickRequestDateResp)
            }
            serviceDate = [ordered]@{
                http_status = $pickServiceDateResp.Status
                accepted = (Test-GraphQlAccepted -Response $pickServiceDateResp)
                error = (Get-GraphQlErrorMessage -Response $pickServiceDateResp)
            }
            updatedAt = [ordered]@{
                http_status = $pickUpdatedAtResp.Status
                accepted = (Test-GraphQlAccepted -Response $pickUpdatedAtResp)
                error = (Get-GraphQlErrorMessage -Response $pickUpdatedAtResp)
            }
        }
        freight_filters = [ordered]@{
            serviceAt = [ordered]@{
                http_status = $freightServiceAtResp.Status
                accepted = (Test-GraphQlAccepted -Response $freightServiceAtResp)
                error = (Get-GraphQlErrorMessage -Response $freightServiceAtResp)
            }
            createdAt = [ordered]@{
                http_status = $freightCreatedAtResp.Status
                accepted = (Test-GraphQlAccepted -Response $freightCreatedAtResp)
                error = (Get-GraphQlErrorMessage -Response $freightCreatedAtResp)
            }
            updatedAt = [ordered]@{
                http_status = $freightUpdatedAtResp.Status
                accepted = (Test-GraphQlAccepted -Response $freightUpdatedAtResp)
                error = (Get-GraphQlErrorMessage -Response $freightUpdatedAtResp)
            }
        }
    }
    dataexport_templates = [ordered]@{
        cotacoes = [ordered]@{
            info_http_status = $cotacoesInfoResp.Status
            data_http_status = $cotacoesDataResp.Status
            data_json = [bool]($cotacoesDataResp.Json -ne $null)
            data_preview = $cotacoesDataResp.Body.Substring(0, [Math]::Min(140, $cotacoesDataResp.Body.Length))
        }
        localizacao_cargas = [ordered]@{
            info_http_status = $localizacaoInfoResp.Status
            data_http_status = $localizacaoDataResp.Status
            data_json = [bool]($localizacaoDataResp.Json -ne $null)
            data_preview = $localizacaoDataResp.Body.Substring(0, [Math]::Min(140, $localizacaoDataResp.Body.Length))
        }
        faturas_por_cliente = [ordered]@{
            info_http_status = $faturasInfoResp.Status
            data_http_status = $faturasDataResp.Status
            data_json = [bool]($faturasDataResp.Json -ne $null)
            data_preview = $faturasDataResp.Body.Substring(0, [Math]::Min(140, $faturasDataResp.Body.Length))
        }
    }
    contas_a_pagar = [ordered]@{
        info_http_status = $contasInfoResp.Status
        default_order = if ($contasInfoResp.Json) { $contasInfoResp.Json.default_order } else { $null }
        orderable_fields = @($contasOrderableFields)
        created_at_filter_probe = [ordered]@{
            page1_http_status = $createdAtFilteredPage1.Status
            page1_count = @($createdAtFilteredItems1).Count
            duplicate_sequence_codes_page1 = @(Get-Duplicates -Items $createdAtFilteredItems1 -KeyName "ant_ils_sequence_code")
            sample_page1_sequence_codes = @($createdAtFilteredItems1 | Select-Object -First 5 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName "ant_ils_sequence_code" })
        }
        current_order = [ordered]@{
            page1_http_status = $currentPage1.Status
            page2_http_status = $currentPage2.Status
            page1_count = @($currentItems1).Count
            page2_count = @($currentItems2).Count
            overlap_between_pages = @(Get-Overlap -Page1 $currentItems1 -Page2 $currentItems2 -KeyName "ant_ils_sequence_code")
            duplicate_sequence_codes_page1 = @(Get-Duplicates -Items $currentItems1 -KeyName "ant_ils_sequence_code")
            sample_page1_sequence_codes = @($currentItems1 | Select-Object -First 10 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName "ant_ils_sequence_code" })
            sample_page1_issue_dates = @($currentItems1 | Select-Object -First 10 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName "issue_date" })
            asc_desc_same_first_page = (
                (@($currentItems1 | Select-Object -First 10 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName "ant_ils_sequence_code" }) -join ",") -eq
                (@($ascendingItems1 | Select-Object -First 10 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName "ant_ils_sequence_code" }) -join ",")
            )
        }
        candidate_order = [ordered]@{
            order_by = "issue_date desc,ant_ils_sequence_code asc"
            page1_http_status = $candidatePage1.Status
            page2_http_status = $candidatePage2.Status
            page1_count = @($candidateItems1).Count
            page2_count = @($candidateItems2).Count
            overlap_between_pages = @(Get-Overlap -Page1 $candidateItems1 -Page2 $candidateItems2 -KeyName "ant_ils_sequence_code")
            duplicate_sequence_codes_page1 = @(Get-Duplicates -Items $candidateItems1 -KeyName "ant_ils_sequence_code")
            tie_break_ascending_within_page1 = (Test-TieBreakAscending -Items $candidateItems1 -DateField "issue_date" -KeyField "ant_ils_sequence_code")
            first_page_differs_from_current = (
                (@($candidateItems1 | Select-Object -First 10 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName "ant_ils_sequence_code" }) -join ",") -ne
                (@($currentItems1 | Select-Object -First 10 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName "ant_ils_sequence_code" }) -join ",")
            )
            sample_page1_sequence_codes = @($candidateItems1 | Select-Object -First 10 | ForEach-Object { Get-PropertyValue -Item $_ -PropertyName "ant_ils_sequence_code" })
        }
    }
}

$summary | ConvertTo-Json -Depth 8
