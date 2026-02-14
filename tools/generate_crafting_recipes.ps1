param(
    [string]$AssetsRoot = "C:\\Users\\devin\\Desktop\\HytaleServer\\Assets",
    [string]$OutputRoot = (Join-Path (Resolve-Path ".").Path "src\\main\\resources\\Server\\Item\\Items"),
    [string]$ReportPath = (Join-Path (Resolve-Path ".").Path "codex\\crafting_generation_report.md"),
    [string]$ExclusionListPath = "",
    [string[]]$ExcludeIds = @(),
    [switch]$DryRun,
    [switch]$Overwrite,
    [switch]$PruneExcluded
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

$itemsRoot = Join-Path $AssetsRoot "Server\\Item\\Items"
if (-not (Test-Path $itemsRoot)) {
    throw "Items path not found: $itemsRoot"
}
$itemsRoot = (Resolve-Path $itemsRoot).Path.TrimEnd('\')

function To-NormalizedId {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return "" }
    return $Value.Trim().ToLowerInvariant()
}

function Is-EligibleItemId {
    param([string]$ItemId)
    $id = To-NormalizedId $ItemId
    return $id.StartsWith("weapon_") -or $id.StartsWith("armor_") -or $id.StartsWith("tool_")
}

function Parse-ItemShape {
    param([string]$ItemId)
    $tokens = (To-NormalizedId $ItemId).Split("_")
    if ($tokens.Length -lt 2) { return @("", "") }
    return @($tokens[0], $tokens[1])
}

function Is-ExcludedByPrefix {
    param([string]$ItemId)
    $id = To-NormalizedId $ItemId
    $blockedPrefixes = @(
        "weapon_bomb",
        "weapon_grenade_",
        "weapon_poison_flask_",
        "weapon_dev_",
        "armor_dev_",
        "armor_kweebec_",
        "armor_QA_",
        "armor_trooper_",
        "armor_trork_",
        "weapon_assault_",
        "weapon_handgun",
        "weapon_gun",
        "tool_dev_",
        "tool_bark_scraper",
        "tool_pickaxe_scrap",
        "tool_growth_potion",
        "tool_map",
        "tool_sap_shunt",
        "tool_trap_bait",
        "tool_sickle_steel_rusty",
        "test_",
        "_rusty",
        "bandage_",
        "_trork",
        "_scrap",
        "_claws_",
        "_turret",
        "_npc",
        "_spiked",
        "_cutlass",
        "special_"
    )
    foreach ($prefixRaw in $blockedPrefixes) {
        $prefix = To-NormalizedId $prefixRaw
        if (-not $prefix) { continue }
        if ($prefix.Contains("*")) {
            if ($id -like $prefix) { return $true }
            continue
        }
        # Tokens that start with "_" are treated as contains-match (e.g. "_rusty", "_scrap", "_npc").
        if ($prefix.StartsWith("_")) {
            if ($id.Contains($prefix)) { return $true }
            continue
        }
        if ($id.StartsWith($prefix)) {
            return $true
        }
    }
    return $false
}

function Load-ExcludedIds {
    param([string]$FilePath, [string[]]$CliIds)
    $ids = New-Object System.Collections.Generic.HashSet[string]([System.StringComparer]::OrdinalIgnoreCase)

    foreach ($id in $CliIds) {
        $normalized = To-NormalizedId $id
        if ($normalized) { $null = $ids.Add($normalized) }
    }

    if (-not [string]::IsNullOrWhiteSpace($FilePath)) {
        if (-not (Test-Path $FilePath)) {
            throw "Exclusion list file not found: $FilePath"
        }
        Get-Content $FilePath | ForEach-Object {
            $line = $_.Trim()
            if (-not $line -or $line.StartsWith("#")) { return }
            $normalized = To-NormalizedId $line
            if ($normalized) { $null = $ids.Add($normalized) }
        }
    }

    return $ids
}

function Get-PropValue {
    param($Obj, [string]$Name)
    if (-not $Obj) { return $null }
    $prop = $Obj.PSObject.Properties[$Name]
    if ($prop) { return $prop.Value }
    return $null
}

function Set-PropValue {
    param($Obj, [string]$Name, $Value)
    if (-not $Obj) { return }
    $prop = $Obj.PSObject.Properties[$Name]
    if ($prop) {
        $Obj.$Name = $Value
    } else {
        $Obj | Add-Member -NotePropertyName $Name -NotePropertyValue $Value
    }
}

function Ensure-Array {
    param($Value)
    if ($null -eq $Value) { return @() }
    if ($Value -is [System.Array]) { return $Value }
    return @($Value)
}

function Get-ArmorSlot {
    param([string]$ItemId, $Data)
    $id = To-NormalizedId $ItemId
    if ($id.EndsWith("_head")) { return "head" }
    if ($id.EndsWith("_hands")) { return "hands" }
    if ($id.EndsWith("_legs")) { return "legs" }
    if ($id.EndsWith("_chest")) { return "chest" }

    $armor = Get-PropValue -Obj $Data -Name "Armor"
    $slot = To-NormalizedId (Get-PropValue -Obj $armor -Name "ArmorSlot")
    if ($slot -eq "head" -or $slot -eq "hands" -or $slot -eq "legs" -or $slot -eq "chest") { return $slot }
    return "chest"
}

function Get-ArmorCategoryForSlot {
    param([string]$Slot)
    if ([string]::IsNullOrWhiteSpace($Slot)) { return "Armor_Chest" }
    return "Armor_" + ($Slot.Substring(0,1).ToUpperInvariant() + $Slot.Substring(1))
}

function Resolve-BenchOverride {
    param([string]$ItemId, $Data)
    $shape = Parse-ItemShape $ItemId
    $itemClass = $shape[0]
    $subType = $shape[1]
    $id = To-NormalizedId $ItemId

    if ($itemClass -eq "weapon") {
        if ($ForcedWeaponBenchRouting.ContainsKey($subType)) {
            $route = $ForcedWeaponBenchRouting[$subType]
            return @([PSCustomObject]@{
                Id = $route.BenchId
                Type = "Crafting"
                Categories = @($route.Category)
            })
        }
        return $null
    }

    if ($itemClass -eq "armor") {
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

function Write-TextNoBom {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content + [Environment]::NewLine, $Utf8NoBomEncoding)
}

function Write-LinesNoBom {
    param([string]$Path, [string[]]$Lines)
    [System.IO.File]::WriteAllText($Path, (($Lines -join [Environment]::NewLine) + [Environment]::NewLine), $Utf8NoBomEncoding)
}

function Normalize-Recipe {
    param($Recipe, [string]$ItemId = "", $Data = $null)
    if (-not $Recipe) {
        $Recipe = [PSCustomObject]@{}
    }

    $inputs = Ensure-Array (Get-PropValue -Obj $Recipe -Name "Input")
    $benches = Ensure-Array (Get-PropValue -Obj $Recipe -Name "BenchRequirement")
    $timeSeconds = Get-PropValue -Obj $Recipe -Name "TimeSeconds"
    if ($null -eq $timeSeconds) { $timeSeconds = 2 }

    $normalizedInputs = @()
    foreach ($input in $inputs) {
        if (-not $input) { continue }
        $itemId = Get-PropValue -Obj $input -Name "ItemId"
        $resourceTypeId = Get-PropValue -Obj $input -Name "ResourceTypeId"
        $quantity = Get-PropValue -Obj $input -Name "Quantity"
        if ($null -eq $quantity) { $quantity = 1 }
        $normalizedInputs += [PSCustomObject]@{
            ItemId = $itemId
            ResourceTypeId = $resourceTypeId
            Quantity = [int]$quantity
        }
    }
    if (@($normalizedInputs).Count -eq 0) {
        $normalizedInputs = @(
            [PSCustomObject]@{ ItemId = "Ingredient_Stick"; ResourceTypeId = $null; Quantity = 2 },
            [PSCustomObject]@{ ItemId = "Ingredient_Leather_Light"; ResourceTypeId = $null; Quantity = 1 },
            [PSCustomObject]@{ ItemId = $null; ResourceTypeId = "Rubble"; Quantity = 2 }
        )
    }
    while (@($normalizedInputs).Count -lt 3) {
        $next = switch (@($normalizedInputs).Count) {
            0 { [PSCustomObject]@{ ItemId = "Ingredient_Stick"; ResourceTypeId = $null; Quantity = 2 } }
            1 { [PSCustomObject]@{ ItemId = "Ingredient_Leather_Light"; ResourceTypeId = $null; Quantity = 1 } }
            default { [PSCustomObject]@{ ItemId = $null; ResourceTypeId = "Rubble"; Quantity = 2 } }
        }
        $normalizedInputs += $next
    }
    if (@($normalizedInputs).Count -gt 5) {
        $normalizedInputs = @($normalizedInputs | Select-Object -First 5)
    }

    $normalizedBenches = @()
    foreach ($bench in $benches) {
        if (-not $bench) { continue }
        $benchId = Get-PropValue -Obj $bench -Name "Id"
        if ([string]::IsNullOrWhiteSpace($benchId) -or $benchId -eq "TODO") { continue }
        $benchType = Get-PropValue -Obj $bench -Name "Type"
        $benchCategories = Ensure-Array (Get-PropValue -Obj $bench -Name "Categories")
        $requiredTier = Get-PropValue -Obj $bench -Name "RequiredTierLevel"
        $normalizedBench = [ordered]@{
            Id = $benchId
            Type = $(if ([string]::IsNullOrWhiteSpace($benchType)) { "Crafting" } else { $benchType })
            Categories = $benchCategories
        }
        if ($null -ne $requiredTier) {
            $normalizedBench["RequiredTierLevel"] = [int]$requiredTier
        }
        $normalizedBenches += [PSCustomObject]$normalizedBench
    }
    if (@($normalizedBenches).Count -eq 0) {
        $forcedBench = Resolve-BenchOverride -ItemId $ItemId -Data $Data
        if ($forcedBench) {
            $normalizedBenches = @($forcedBench)
        } else {
            $shape = Parse-ItemShape $ItemId
            $itemClass = $shape[0]
            $subType = $shape[1]
            if ($itemClass -eq "weapon") {
                $cat = if ($subType) { "Weapon_" + $subType.Substring(0,1).ToUpperInvariant() + $subType.Substring(1) } else { "Weapon_Generic" }
                $normalizedBenches = @([PSCustomObject]@{ Id = "Weapon_Bench"; Type = "Crafting"; Categories = @($cat) })
            } elseif ($itemClass -eq "armor") {
                $slot = Get-ArmorSlot -ItemId $ItemId -Data $Data
                $normalizedBenches = @([PSCustomObject]@{ Id = "Armor_Bench"; Type = "Crafting"; Categories = @(Get-ArmorCategoryForSlot -Slot $slot) })
            } else {
                $normalizedBenches = @([PSCustomObject]@{ Id = "Workbench"; Type = "Crafting"; Categories = @("Workbench_Tools") })
            }
        }
    }

    return [PSCustomObject]@{
        TimeSeconds = $timeSeconds
        KnowledgeRequired = $false
        Input = $normalizedInputs
        BenchRequirement = $normalizedBenches
    }
}

function Get-JsonCanonical {
    param($Obj)
    return (($Obj | ConvertTo-Json -Depth 100) -replace "`r`n", "`n")
}

function Get-ExistingJsonCanonical {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return $null }
    try {
        $obj = Get-Content -Raw $Path | ConvertFrom-Json
        return Get-JsonCanonical $obj
    } catch {
        return $null
    }
}

function Validate-Recipe {
    param([string]$ItemId, $Recipe)

    $issues = @()
    if (-not $Recipe) {
        $issues += "${ItemId}: missing Recipe object"
        return $issues
    }

    $inputs = Ensure-Array (Get-PropValue -Obj $Recipe -Name "Input")
    if (@($inputs).Count -eq 0) {
        $issues += "${ItemId}: missing recipe inputs"
    }
    if (@($inputs).Count -lt 3 -or @($inputs).Count -gt 5) {
        $issues += "${ItemId}: ingredient count must be between 3 and 5"
    }
    foreach ($input in $inputs) {
        if (-not $input) {
            $issues += "${ItemId}: null input entry"
            continue
        }
        $itemRef = Get-PropValue -Obj $input -Name "ItemId"
        $resourceRef = Get-PropValue -Obj $input -Name "ResourceTypeId"
        $quantity = Get-PropValue -Obj $input -Name "Quantity"
        if ([string]::IsNullOrWhiteSpace($itemRef) -and [string]::IsNullOrWhiteSpace($resourceRef)) {
            $issues += "${ItemId}: input missing ItemId/ResourceTypeId"
        }
        if ($null -eq $quantity -or [int]$quantity -le 0) {
            $issues += "${ItemId}: input quantity must be > 0"
        }
    }

    $output = Get-PropValue -Obj $Recipe -Name "Output"
    if ($output) {
        $outItemId = Get-PropValue -Obj $output -Name "ItemId"
        $outQuantity = Get-PropValue -Obj $output -Name "Quantity"
        if ([string]::IsNullOrWhiteSpace($outItemId)) {
            $issues += "${ItemId}: malformed recipe output missing ItemId"
        }
        if ($null -ne $outQuantity -and [int]$outQuantity -le 0) {
            $issues += "${ItemId}: malformed recipe output quantity must be > 0"
        }
    }

    return $issues
}

$excludedIds = Load-ExcludedIds -FilePath $ExclusionListPath -CliIds $ExcludeIds
if ($null -eq $excludedIds) {
    $excludedIds = New-Object System.Collections.Generic.HashSet[string]([System.StringComparer]::OrdinalIgnoreCase)
}

$allItems = @{}
Get-ChildItem -Recurse -File -Path $itemsRoot -Filter *.json | ForEach-Object {
    $id = $_.BaseName
    try {
        $data = Get-Content -Raw $_.FullName | ConvertFrom-Json
    } catch {
        return
    }
    $allItems[$id] = [PSCustomObject]@{
        Id = $id
        IdLower = To-NormalizedId $id
        Path = $_.FullName
        RelPath = $_.FullName.Substring($itemsRoot.Length + 1)
        Data = $data
    }
}

$eligibleItems = @()
foreach ($entry in $allItems.Values) {
    if (-not (Is-EligibleItemId $entry.Id)) { continue }
    if (Is-ExcludedByPrefix $entry.Id) { continue }
    if ($excludedIds.Contains($entry.IdLower)) { continue }
    $eligibleItems += $entry
}

$recipeIdsSeen = @{}
$duplicateRecipeIds = @()
$missingIngredientRefs = @()
$ingredientCountViolations = @()
$malformedOutputs = @()
$validationIssues = @()

$generatedCount = 0
$changedCount = 0
$newCount = 0
$unchangedCount = 0
$skippedExistingCount = 0
$writeCount = 0
$prunedExcludedCount = 0
$diffRows = @()

if ($PruneExcluded -and -not $DryRun -and (Test-Path $OutputRoot)) {
    Get-ChildItem -Recurse -File -Path $OutputRoot -Filter *.json | ForEach-Object {
        $outputItemId = $_.BaseName
        $outputItemIdLower = To-NormalizedId $outputItemId
        if (-not (Is-EligibleItemId $outputItemId)) { return }
        if (-not (Is-ExcludedByPrefix $outputItemId) -and -not $excludedIds.Contains($outputItemIdLower)) { return }
        Remove-Item -Path $_.FullName -Force
        $prunedExcludedCount++
    }
}

foreach ($entry in ($eligibleItems | Sort-Object IdLower)) {
    $sourceData = $entry.Data
    $recipe = Normalize-Recipe (Get-PropValue -Obj $sourceData -Name "Recipe") -ItemId $entry.Id -Data $sourceData
    $forcedBench = Resolve-BenchOverride -ItemId $entry.Id -Data $sourceData
    if ($forcedBench) {
        $recipe.BenchRequirement = @($forcedBench)
    }
    Set-PropValue -Obj $sourceData -Name "Recipe" -Value $recipe
    Set-PropValue -Obj $sourceData -Name "Quality" -Value "Common"

    $recipeId = Get-PropValue -Obj $recipe -Name "Id"
    if ([string]::IsNullOrWhiteSpace($recipeId)) {
        $recipeId = "generated:$($entry.IdLower)"
    }
    if ($recipeIdsSeen.ContainsKey($recipeId)) {
        $duplicateRecipeIds += "$recipeId -> $($recipeIdsSeen[$recipeId]), $($entry.Id)"
    } else {
        $recipeIdsSeen[$recipeId] = $entry.Id
    }

    $issues = Validate-Recipe -ItemId $entry.Id -Recipe $recipe
    foreach ($issue in $issues) {
        $validationIssues += $issue
        if ($issue.Contains("missing recipe inputs") -or $issue.Contains("input missing ItemId/ResourceTypeId")) {
            $missingIngredientRefs += $issue
        }
        if ($issue.Contains("ingredient count must be between 3 and 5")) {
            $ingredientCountViolations += $issue
        }
        if ($issue.Contains("malformed recipe output")) {
            $malformedOutputs += $issue
        }
    }

    $targetPath = Join-Path $OutputRoot $entry.RelPath
    $generatedJson = Get-JsonCanonical $sourceData
    $existingJson = Get-ExistingJsonCanonical $targetPath

    $status = "new"
    if ($null -ne $existingJson) {
        if ($existingJson -eq $generatedJson) {
            $status = "unchanged"
        } else {
            $status = "changed"
        }
    }

    switch ($status) {
        "new" { $newCount++ }
        "changed" { $changedCount++ }
        "unchanged" { $unchangedCount++ }
    }
    $generatedCount++
    $diffRows += "- [$status] $($entry.RelPath)"

    if ($DryRun) { continue }

    if ((Test-Path $targetPath) -and -not $Overwrite -and $status -eq "changed") {
        $skippedExistingCount++
        continue
    }
    if ($status -eq "unchanged") { continue }

    $dir = Split-Path $targetPath -Parent
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    Write-TextNoBom -Path $targetPath -Content $generatedJson
    $writeCount++
}

$reportLines = @()
$reportLines += "# RNG Crafting Recipe Generation Report"
$reportLines += ""
$reportLines += "Run mode: $(if ($DryRun) { "dry-run" } else { "write" })"
$reportLines += "Assets root: $AssetsRoot"
$reportLines += "Output root: $OutputRoot"
$reportLines += "Eligible scope: item ids starting with weapon_/armor_/tool_"
$reportLines += ""
$reportLines += "## Summary"
$reportLines += "- Eligible items discovered: $($eligibleItems.Count)"
$reportLines += "- Overrides generated: $generatedCount"
$reportLines += "- Diff new: $newCount"
$reportLines += "- Diff changed: $changedCount"
$reportLines += "- Diff unchanged: $unchangedCount"
$reportLines += "- Writes applied: $writeCount"
$reportLines += "- Existing changed overrides skipped (no -Overwrite): $skippedExistingCount"
$reportLines += "- Excluded ids (explicit): $($excludedIds.Count)"
$reportLines += "- Excluded files pruned from output: $prunedExcludedCount"
$reportLines += ""
$reportLines += "## Baseline Defaults Enforced"
$reportLines += "- Recipe.KnowledgeRequired = false"
$reportLines += "- Quality = Common"
$reportLines += "- Recipe arrays normalized (Input/BenchRequirement)"
$reportLines += ""
$reportLines += "## Sanity Checks"
$reportLines += "- Missing ingredient refs: $($missingIngredientRefs.Count)"
$reportLines += "- Ingredient count violations (must be 3..5): $($ingredientCountViolations.Count)"
$reportLines += "- Malformed outputs: $($malformedOutputs.Count)"
$reportLines += "- Duplicate recipe ids: $($duplicateRecipeIds.Count)"
$reportLines += ""
if ($validationIssues.Count -gt 0) {
    $reportLines += "### Validation Issues"
    $reportLines += ($validationIssues | Sort-Object -Unique)
    $reportLines += ""
}
if ($duplicateRecipeIds.Count -gt 0) {
    $reportLines += "### Duplicate Recipe IDs"
    $reportLines += ($duplicateRecipeIds | Sort-Object -Unique)
    $reportLines += ""
}
$reportLines += "## Dry-Run Diff"
$reportLines += ($diffRows | Sort-Object)

$reportDir = Split-Path $ReportPath -Parent
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}
Write-LinesNoBom -Path $ReportPath -Lines $reportLines

Write-Output "Recipe generation complete. Mode=$(if ($DryRun) { "dry-run" } else { "write" }), eligible=$($eligibleItems.Count), report=$ReportPath"
