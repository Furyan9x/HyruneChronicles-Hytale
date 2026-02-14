param(
    [string]$ItemsRoot = (Join-Path (Resolve-Path ".").Path "src\\main\\resources\\Server\\Item\\Items"),
    [string]$AssetsRoot = "C:\\Users\\devin\\Desktop\\HytaleServer\\Assets",
    [string]$ReportPath = (Join-Path (Resolve-Path ".").Path "codex\\bench_normalize_report.md"),
    [switch]$DryRun,
    [switch]$ForceAll
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Utf8NoBomEncoding = New-Object System.Text.UTF8Encoding($false)

# ===========================
# User-Editable Bench Routing
# ===========================
$ArcaneArmorKeywords = @("cloth", "linen", "silk", "wool")
$ArcaneArmorCategoryBySlot = @{
    "head" = "Arcane_Armor_Head"
    "chest" = "Arcane_Armor_Chest"
    "legs" = "Arcane_Armor_Legs"
    "hands" = "Arcane_Armor_Hands"
}
$ForcedWeaponBenchRouting = @{
    "spear" = @{ BenchId = "Weapon_Bench"; Category = "Weapon_Spear" }
    "longsword" = @{ BenchId = "Weapon_Bench"; Category = "Weapon_Longsword" }
    "staff" = @{ BenchId = "Arcanebench"; Category = "Arcane_Weapons" }
    "wand" = @{ BenchId = "Arcanebench"; Category = "Arcane_Weapons" }
    "spellbook" = @{ BenchId = "Arcanebench"; Category = "Arcane_Weapons" }
    "shortbow" = @{ BenchId = "Fletcher_Bench"; Category = "Fletcher_Bows" }
    "crossbow" = @{ BenchId = "Fletcher_Bench"; Category = "Fletcher_Bows" }
    "dart" = @{ BenchId = "Fletcher_Bench"; Category = "Fletcher_Darts" }
    "arrow" = @{ BenchId = "Fletcher_Bench"; Category = "Fletcher_Arrows" }
    "blowgun" = @{ BenchId = "Fletcher_Bench"; Category = "Fletcher_Blowguns" }
}

function Normalize-Id {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return "" }
    return $Value.Trim().ToLowerInvariant()
}

function Ensure-Array {
    param($Value)
    if ($null -eq $Value) { return @() }
    if ($Value -is [System.Array]) { return $Value }
    return @($Value)
}

function Get-PropValue {
    param($Obj, [string]$Name)
    if ($null -eq $Obj) { return $null }
    $prop = $Obj.PSObject.Properties[$Name]
    if ($null -eq $prop) { return $null }
    return $prop.Value
}

function Is-EligibleItemId {
    param([string]$ItemId)
    $id = Normalize-Id $ItemId
    return $id.StartsWith("weapon_") -or $id.StartsWith("armor_") -or $id.StartsWith("tool_")
}

function Parse-Shape {
    param([string]$ItemId)
    $tokens = (Normalize-Id $ItemId).Split("_")
    if ($tokens.Length -lt 2) { return @("", "") }
    return @($tokens[0], $tokens[1])
}

function Get-ArmorSlot {
    param([string]$ItemId, $Data)
    $id = Normalize-Id $ItemId
    if ($id.EndsWith("_head")) { return "head" }
    if ($id.EndsWith("_hands")) { return "hands" }
    if ($id.EndsWith("_legs")) { return "legs" }
    if ($id.EndsWith("_chest")) { return "chest" }

    $armor = Get-PropValue -Obj $Data -Name "Armor"
    $slot = Normalize-Id (Get-PropValue -Obj $armor -Name "ArmorSlot")
    if ($slot -eq "head" -or $slot -eq "hands" -or $slot -eq "legs" -or $slot -eq "chest") { return $slot }
    return "chest"
}

function Get-ArmorCategoryForSlot {
    param([string]$Slot)
    if ([string]::IsNullOrWhiteSpace($Slot)) { return "Armor_Chest" }
    return "Armor_" + ($Slot.Substring(0,1).ToUpperInvariant() + $Slot.Substring(1))
}

function Resolve-BenchOverride {
    param([string]$ItemId, [string]$ItemClass, [string]$SubType, $Data)
    $id = Normalize-Id $ItemId

    if ($ItemClass -eq "weapon") {
        if ($ForcedWeaponBenchRouting.ContainsKey($SubType)) {
            $route = $ForcedWeaponBenchRouting[$SubType]
            return @([PSCustomObject]@{
                Id = $route.BenchId
                Type = "Crafting"
                Categories = @($route.Category)
            })
        }
        return $null
    }

    if ($ItemClass -eq "armor") {
        $slot = Get-ArmorSlot -ItemId $ItemId -Data $Data
        foreach ($keyword in $ArcaneArmorKeywords) {
            if ($id.Contains($keyword)) {
                $arcaneCategory = if ($ArcaneArmorCategoryBySlot.ContainsKey($slot)) { $ArcaneArmorCategoryBySlot[$slot] } else { "Arcane_Armor_Chest" }
                return @([PSCustomObject]@{
                    Id = "Arcanebench"
                    Type = "Crafting"
                    Categories = @($arcaneCategory)
                })
            }
        }
        return @([PSCustomObject]@{
            Id = "Armor_Bench"
            Type = "Crafting"
            Categories = @(Get-ArmorCategoryForSlot -Slot $slot)
        })
    }

    return $null
}

function Normalize-BenchRequirement {
    param($BenchRequirement)
    $normalized = @()
    foreach ($bench in (Ensure-Array $BenchRequirement)) {
        if ($null -eq $bench) { continue }
        $id = Get-PropValue -Obj $bench -Name "Id"
        if ([string]::IsNullOrWhiteSpace($id) -or $id -eq "TODO") { continue }
        $type = Get-PropValue -Obj $bench -Name "Type"
        $categories = @(Ensure-Array (Get-PropValue -Obj $bench -Name "Categories"))
        $tier = Get-PropValue -Obj $bench -Name "RequiredTierLevel"
        $out = [ordered]@{
            Id = $id
            Type = $(if ([string]::IsNullOrWhiteSpace($type)) { "Crafting" } else { $type })
            Categories = $categories
        }
        if ($null -ne $tier) {
            $out["RequiredTierLevel"] = [int]$tier
        }
        $normalized += [PSCustomObject]$out
    }
    return $normalized
}

function Is-AcceptableCraftBench {
    param([array]$BenchRequirement)
    $benches = @(Normalize-BenchRequirement $BenchRequirement)
    if ($benches.Count -eq 0) { return $false }
    foreach ($bench in $benches) {
        $benchId = Normalize-Id (Get-PropValue -Obj $bench -Name "Id")
        $benchType = Normalize-Id (Get-PropValue -Obj $bench -Name "Type")
        if ($benchType -ne "crafting" -and $benchType -ne "diagramcrafting") {
            return $false
        }
        if ($benchId.Contains("salvage") -or $benchId.Contains("processing")) {
            return $false
        }
    }
    return $true
}

function Bench-Signature {
    param([array]$BenchRequirement)
    return ((Normalize-BenchRequirement $BenchRequirement) | ConvertTo-Json -Depth 20 -Compress)
}

function Is-GenericWorkbenchBench {
    param([array]$BenchRequirement)
    $benches = @(Normalize-BenchRequirement $BenchRequirement)
    if ($benches.Count -eq 0) { return $true }
    if ($benches.Count -ne 1) { return $false }
    $b = $benches[0]
    $id = Normalize-Id (Get-PropValue -Obj $b -Name "Id")
    if ($id -ne "workbench") { return $false }
    $cats = @(Ensure-Array (Get-PropValue -Obj $b -Name "Categories"))
    foreach ($c in $cats) {
        if ((Normalize-Id $c) -eq "workbench_crafting") { return $true }
    }
    return $false
}

function Get-DefaultBench {
    param([string]$ItemId, [string]$ItemClass, [string]$SubType, $Data)
    $forced = Resolve-BenchOverride -ItemId $ItemId -ItemClass $ItemClass -SubType $SubType -Data $Data
    if ($forced) { return $forced }
    if ($ItemClass -eq "weapon") {
        $sub = if ($SubType) { $SubType.Substring(0,1).ToUpperInvariant() + $SubType.Substring(1) } else { "Generic" }
        return @([PSCustomObject]@{ Id = "Weapon_Bench"; Type = "Crafting"; Categories = @("Weapon_$sub") })
    }
    if ($ItemClass -eq "armor") {
        $slot = Get-ArmorSlot -ItemId $ItemId -Data $Data
        return @([PSCustomObject]@{ Id = "Armor_Bench"; Type = "Crafting"; Categories = @(Get-ArmorCategoryForSlot -Slot $slot) })
    }
    return @([PSCustomObject]@{ Id = "Workbench"; Type = "Crafting"; Categories = @("Workbench_Tools") })
}

if (-not (Test-Path $ItemsRoot)) {
    throw "Items root not found: $ItemsRoot"
}
$assetsItemsRoot = Join-Path $AssetsRoot "Server\\Item\\Items"
if (-not (Test-Path $assetsItemsRoot)) {
    throw "Assets items root not found: $assetsItemsRoot"
}

$exactBenchById = @{}
$mostCommonBySubtype = @{}
$mostCommonByClass = @{}
$subtypeBenchCount = @{}
$classBenchCount = @{}

Get-ChildItem -Recurse -File -Path $assetsItemsRoot -Filter *.json | ForEach-Object {
    $itemId = $_.BaseName
    if (-not (Is-EligibleItemId $itemId)) { return }
    try { $data = Get-Content -Raw $_.FullName | ConvertFrom-Json } catch { return }
    if (-not $data) { return }
    $recipe = Get-PropValue -Obj $data -Name "Recipe"
    $bench = Normalize-BenchRequirement (Get-PropValue -Obj $recipe -Name "BenchRequirement")
    if (@($bench).Count -eq 0) { return }
    if (-not (Is-AcceptableCraftBench $bench)) { return }

    $shape = Parse-Shape $itemId
    $itemClass = $shape[0]
    $subType = $shape[1]
    $idKey = Normalize-Id $itemId
    $exactBenchById[$idKey] = $bench

    $sig = Bench-Signature $bench
    $subKey = "$itemClass|$subType"
    if (-not $subtypeBenchCount.ContainsKey($subKey)) { $subtypeBenchCount[$subKey] = @{} }
    if (-not $subtypeBenchCount[$subKey].ContainsKey($sig)) { $subtypeBenchCount[$subKey][$sig] = @{ Count = 0; Bench = $bench } }
    $subtypeBenchCount[$subKey][$sig].Count++

    if (-not $classBenchCount.ContainsKey($itemClass)) { $classBenchCount[$itemClass] = @{} }
    if (-not $classBenchCount[$itemClass].ContainsKey($sig)) { $classBenchCount[$itemClass][$sig] = @{ Count = 0; Bench = $bench } }
    $classBenchCount[$itemClass][$sig].Count++
}

foreach ($key in $subtypeBenchCount.Keys) {
    $best = $null
    foreach ($sig in $subtypeBenchCount[$key].Keys) {
        $candidate = $subtypeBenchCount[$key][$sig]
        if ($null -eq $best -or $candidate.Count -gt $best.Count) { $best = $candidate }
    }
    if ($best) { $mostCommonBySubtype[$key] = $best.Bench }
}

foreach ($key in $classBenchCount.Keys) {
    $best = $null
    foreach ($sig in $classBenchCount[$key].Keys) {
        $candidate = $classBenchCount[$key][$sig]
        if ($null -eq $best -or $candidate.Count -gt $best.Count) { $best = $candidate }
    }
    if ($best) { $mostCommonByClass[$key] = $best.Bench }
}

$changed = 0
$unchanged = 0
$sourceExact = 0
$sourceSubtype = 0
$sourceClass = 0
$sourceDefault = 0
$sourceForced = 0
$changedFiles = @()

Get-ChildItem -Recurse -File -Path $ItemsRoot -Filter *.json | Sort-Object FullName | ForEach-Object {
    $itemId = $_.BaseName
    if (-not (Is-EligibleItemId $itemId)) { return }
    try { $data = Get-Content -Raw $_.FullName | ConvertFrom-Json } catch { return }
    if (-not $data) { return }
    $recipe = Get-PropValue -Obj $data -Name "Recipe"
    if (-not $recipe) {
        $recipe = [PSCustomObject]@{ TimeSeconds = 2; KnowledgeRequired = $false; Input = @(); BenchRequirement = @() }
        $data | Add-Member -NotePropertyName "Recipe" -NotePropertyValue $recipe
    }

    $shape = Parse-Shape $itemId
    $itemClass = $shape[0]
    $subType = $shape[1]
    $idKey = Normalize-Id $itemId

    $newBench = $null
    $source = ""
    $forcedBench = Resolve-BenchOverride -ItemId $itemId -ItemClass $itemClass -SubType $subType -Data $data
    if ($forcedBench) {
        $newBench = $forcedBench
        $source = "forced"
    } elseif ($exactBenchById.ContainsKey($idKey)) {
        $newBench = $exactBenchById[$idKey]
        $source = "exact"
    } else {
        $subKey = "$itemClass|$subType"
        if ($mostCommonBySubtype.ContainsKey($subKey)) {
            $newBench = $mostCommonBySubtype[$subKey]
            $source = "subtype"
        } elseif ($mostCommonByClass.ContainsKey($itemClass)) {
            $newBench = $mostCommonByClass[$itemClass]
            $source = "class"
        } else {
            $newBench = Get-DefaultBench -ItemId $itemId -ItemClass $itemClass -SubType $subType -Data $data
            $source = "default"
        }
    }

    $currentBench = Normalize-BenchRequirement (Get-PropValue -Obj $recipe -Name "BenchRequirement")
    if (-not (Is-AcceptableCraftBench $newBench)) {
        $newBench = Get-DefaultBench -ItemId $itemId -ItemClass $itemClass -SubType $subType -Data $data
        $source = "default"
    }
    $currentSig = Bench-Signature $currentBench
    $newSig = Bench-Signature $newBench
    $shouldReplace = $ForceAll -or (Is-GenericWorkbenchBench $currentBench) -or ($currentSig -ne $newSig)
    if (-not $shouldReplace) {
        $unchanged++
        return
    }

    $recipe.BenchRequirement = @($newBench)
    $changed++
    switch ($source) {
        "exact" { $sourceExact++ }
        "subtype" { $sourceSubtype++ }
        "class" { $sourceClass++ }
        "forced" { $sourceForced++ }
        default { $sourceDefault++ }
    }
    $relative = $_.FullName.Substring((Resolve-Path $ItemsRoot).Path.Length + 1).Replace("\", "/")
    $changedFiles += $relative

    if (-not $DryRun) {
        $json = $data | ConvertTo-Json -Depth 100
        [System.IO.File]::WriteAllText($_.FullName, $json + [Environment]::NewLine, $Utf8NoBomEncoding)
    }
}

$lines = @()
$lines += "# Bench Normalize Report"
$lines += ""
$lines += "Mode: $(if ($DryRun) { 'dry-run' } else { 'write' })"
$lines += "Items root: $ItemsRoot"
$lines += "Assets root: $AssetsRoot"
$lines += ""
$lines += "## Summary"
$lines += "- Changed: $changed"
$lines += "- Unchanged: $unchanged"
$lines += "- Source exact: $sourceExact"
$lines += "- Source subtype: $sourceSubtype"
$lines += "- Source class: $sourceClass"
$lines += "- Source forced overrides: $sourceForced"
$lines += "- Source default: $sourceDefault"
$lines += ""
$lines += "## Changed Files"
foreach ($rel in ($changedFiles | Select-Object -First 500)) {
    $lines += "- $rel"
}

$reportDir = Split-Path $ReportPath -Parent
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}
[System.IO.File]::WriteAllText($ReportPath, (($lines -join [Environment]::NewLine) + [Environment]::NewLine), $Utf8NoBomEncoding)

Write-Output "Bench normalization complete. mode=$(if ($DryRun) { 'dry-run' } else { 'write' }), changed=$changed, report=$ReportPath"
