param(
    [string]$AssetsRoot = "C:\\Users\\devin\\Desktop\\HytaleServer\\Assets",
    [string]$OutputRoot = (Join-Path (Resolve-Path ".").Path "src\\main\\resources\\Server\\Item\\Items"),
    [string]$ReportPath = (Join-Path (Resolve-Path ".").Path "codex\\crafting_audit.md")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$itemsRoot = Join-Path $AssetsRoot "Server\\Item\\Items"
if (-not (Test-Path $itemsRoot)) {
    throw "Items path not found: $itemsRoot"
}
$itemsRoot = (Resolve-Path $itemsRoot).Path.TrimEnd('\')

$items = @{}
Get-ChildItem -Recurse -File -Path $itemsRoot -Filter *.json | ForEach-Object {
    try {
        $data = Get-Content $_.FullName -Raw | ConvertFrom-Json
    } catch {
        return
    }
    $id = $_.BaseName
    $relPath = $_.FullName.Substring($itemsRoot.Length + 1)
    $items[$id] = [PSCustomObject]@{
        Id = $id
        IdLower = $id.ToLowerInvariant()
        Path = $_.FullName
        RelPath = $relPath
        Dir = Split-Path $relPath -Parent
        Data = $data
    }
}

function Get-PropValue {
    param($Obj, [string]$Name)
    if (-not $Obj) { return $null }
    $prop = $Obj.PSObject.Properties[$Name]
    if ($prop) { return $prop.Value }
    return $null
}

function Get-CollectionCount {
    param($Value)
    if ($null -eq $Value) { return 0 }
    if ($Value -is [System.Array]) { return $Value.Count }
    return 1
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
    if ($null -eq $Value) { return $null }
    if ($Value -is [System.Array]) { return $Value }
    return @($Value)
}

function Get-EffectiveRecipe {
    param(
        [string]$Id,
        [hashtable]$Seen
    )
    if (-not $Id) { return $null }
    if ($Seen.ContainsKey($Id)) { return $null }
    $Seen[$Id] = $true
    $entry = $items[$Id]
    if (-not $entry) { return $null }
    $data = $entry.Data
    $recipe = Get-PropValue -Obj $data -Name "Recipe"
    if ($recipe) { return $recipe }
    $parent = Get-PropValue -Obj $data -Name "Parent"
    if ($parent) { return Get-EffectiveRecipe -Id $parent -Seen $Seen }
    return $null
}

function Get-RecipeSignature {
    param($BenchRequirement)
    if (-not $BenchRequirement) { return "" }
    $parts = @()
    foreach ($bench in $BenchRequirement) {
        if (-not $bench) { continue }
        $categories = ""
        $benchCategories = Get-PropValue -Obj $bench -Name "Categories"
        if ($benchCategories) { $categories = ($benchCategories -join "/") }
        $benchId = Get-PropValue -Obj $bench -Name "Id"
        $benchType = Get-PropValue -Obj $bench -Name "Type"
        $parts += ("{0}|{1}|{2}" -f $benchId, $benchType, $categories)
    }
    return ($parts -join ";")
}

$groupBench = @{}
$groupRecipe = @{}
$groupCounts = @{}
$typeBenchCounts = @{}
$barMap = @{}

$excludeContains = @(
    "debug_","debug","rock_","_qa_","wood_","plant_","prototype_","ore_","soil_","template_","crystal_","ingredient_",
    "tree","forgotten_","filter_","egg_","stalactite_","editortool_","editor_","test_","spawner_","awn_","hub_",
    "lime_","luid_","mplate_","ock_","ototype_","rubble_","sh_","yzer_","_sap_shunt","utility_","memory_"
)
$excludeStarts = @("co_","deco_","il_","lant_","cloth_roof_","recipe_book_")
$excludeExact = @("recipe_page","launchpad")
$excludeContains += "instance_gateway"
$excludeContains += "weapon_mace_scrap_npc"

function Is-Excluded {
    param($Entry)
    if (-not $Entry) { return $false }
    $id = $Entry.IdLower
    if (-not $id) { return $false }
    foreach ($token in $excludeStarts) {
        if ($id.StartsWith($token)) { return $true }
    }
    foreach ($token in $excludeExact) {
        if ($id -eq $token) { return $true }
    }
    foreach ($token in $excludeContains) {
        if ($id.Contains($token)) { return $true }
    }
    if ($id.Contains("test")) { return $true }
    if ($Entry.RelPath -and $Entry.RelPath -match '(?i)\\Minigames\\') { return $true }
    return $false
}

foreach ($id in $items.Keys) {
    $entry = $items[$id]
    if (Is-Excluded $entry) { continue }
    $data = $entry.Data
    $recipe = Get-PropValue -Obj $data -Name "Recipe"
    $inputs = Get-PropValue -Obj $recipe -Name "Input"
    if ($recipe -and $inputs) {
        foreach ($input in @($inputs)) {
            $itemId = Get-PropValue -Obj $input -Name "ItemId"
            if ($itemId -and $itemId -match '^Ingredient_Bar_(.+)$') {
                $token = $Matches[1].ToLowerInvariant()
                if (-not $barMap.ContainsKey($token)) {
                    $barMap[$token] = $itemId
                }
            }
        }
    }

    $benchReq = Get-PropValue -Obj $recipe -Name "BenchRequirement"
    $validBench = $true
    if (-not $benchReq -or (Get-CollectionCount $benchReq) -eq 0) { $validBench = $false }
    if ($benchReq) {
        foreach ($bench in @($benchReq)) {
            $benchId = Get-PropValue -Obj $bench -Name "Id"
            if (-not $benchId -or $benchId -eq "TODO") { $validBench = $false }
        }
    }
    if (-not $recipe -or -not $validBench) { continue }

    $group = $entry.Dir
    if (-not $groupBench.ContainsKey($group)) {
        $groupBench[$group] = $benchReq
        $groupRecipe[$group] = $recipe
        $groupCounts[$group] = 1
    } else {
        $groupCounts[$group]++
    }

    $tags = Get-PropValue -Obj $data -Name "Tags"
    $tagTypes = Get-PropValue -Obj $tags -Name "Type"
    if ($tagTypes) {
        foreach ($type in $tagTypes) {
            if (-not $typeBenchCounts.ContainsKey($type)) {
                $typeBenchCounts[$type] = @{}
            }
            $sig = Get-RecipeSignature -BenchRequirement $benchReq
            if (-not $typeBenchCounts[$type].ContainsKey($sig)) {
                $typeBenchCounts[$type][$sig] = 0
            }
            $typeBenchCounts[$type][$sig]++
        }
    }
}

function Get-BestBenchForType {
    param([string]$Type)
    if (-not $typeBenchCounts.ContainsKey($Type)) { return $null }
    $bestSig = $null
    $bestCount = -1
    foreach ($sig in $typeBenchCounts[$Type].Keys) {
        $count = $typeBenchCounts[$Type][$sig]
        if ($count -gt $bestCount) {
            $bestCount = $count
            $bestSig = $sig
        }
    }
    if (-not $bestSig) { return $null }
    $benchReq = @()
    foreach ($entry in $bestSig.Split(";")) {
        if (-not $entry) { continue }
        $parts = $entry.Split("|")
        $bench = [ordered]@{
            Id = $parts[0]
        }
        if ($parts.Length -gt 1 -and $parts[1]) { $bench["Type"] = $parts[1] }
        if ($parts.Length -gt 2 -and $parts[2]) { $bench["Categories"] = $parts[2].Split("/") }
        $benchReq += $bench
    }
    return $benchReq
}

function Get-MaterialToken {
    param($data, [string]$id)
    $candidates = @()
    $tags = Get-PropValue -Obj $data -Name "Tags"
    $families = Get-PropValue -Obj $tags -Name "Family"
    if ($families) {
        foreach ($fam in $families) {
            $candidates += $fam.ToString()
        }
    }
    $candidates += $id
    foreach ($candidate in $candidates) {
        foreach ($token in $barMap.Keys) {
            if ($candidate.ToLowerInvariant().Contains($token)) {
                return $token
            }
        }
    }
    return $null
}

function ApplyMaterialSubstitution {
    param($recipe, [string]$targetToken)
    if (-not $recipe -or -not $targetToken -or -not $barMap.ContainsKey($targetToken)) { return $recipe }
    $inputs = Get-PropValue -Obj $recipe -Name "Input"
    if (-not $inputs) { return $recipe }
    foreach ($input in @($inputs)) {
        $itemId = Get-PropValue -Obj $input -Name "ItemId"
        if ($itemId -and $itemId -match '^Ingredient_Bar_(.+)$') {
            $input.ItemId = $barMap[$targetToken]
        }
    }
    return $recipe
}

function Normalize-RecipeArrays {
    param($Recipe)
    if (-not $Recipe) { return $Recipe }
    $inputs = Get-PropValue -Obj $Recipe -Name "Input"
    if ($inputs) { Set-PropValue -Obj $Recipe -Name "Input" -Value (Ensure-Array $inputs) }
    $benches = Get-PropValue -Obj $Recipe -Name "BenchRequirement"
    if ($benches) { Set-PropValue -Obj $Recipe -Name "BenchRequirement" -Value (Ensure-Array $benches) }
    return $Recipe
}

$missing = @()
$benchIssues = @()
$overrides = @()

foreach ($id in ($items.Keys | Sort-Object)) {
    $entry = $items[$id]
    if (Is-Excluded $entry) { continue }
    $data = $entry.Data
    $seen = @{}
    $recipe = Get-EffectiveRecipe -Id $id -Seen $seen
    if (-not $recipe) {
        $missing += $id
        $overrides += $id
        continue
    }
    $benchReq = Get-PropValue -Obj $recipe -Name "BenchRequirement"
    $badBench = $false
    if (-not $benchReq -or (Get-CollectionCount $benchReq) -eq 0) { $badBench = $true }
    if ($benchReq) {
        foreach ($bench in @($benchReq)) {
            $benchId = Get-PropValue -Obj $bench -Name "Id"
            if (-not $benchId -or $benchId -eq "TODO") { $badBench = $true }
        }
    }
    if ($badBench) {
        $benchIssues += $id
        $overrides += $id
    }
}

$overrides = $overrides | Sort-Object -Unique

function Get-TemplateRecipe {
    param($entry, [string]$id)
    $group = $entry.Dir
    if ($groupRecipe.ContainsKey($group)) {
        return $groupRecipe[$group]
    }

    $parent = Get-PropValue -Obj $entry.Data -Name "Parent"
    if ($parent) {
        $seen = @{}
        $parentRecipe = Get-EffectiveRecipe -Id $parent -Seen $seen
        if ($parentRecipe) { return $parentRecipe }
    }

    $tagTypes = Get-PropValue -Obj (Get-PropValue -Obj $entry.Data -Name "Tags") -Name "Type"
    if ($tagTypes) {
        foreach ($type in $tagTypes) {
            $benchReq = Get-BestBenchForType -Type $type
            if ($benchReq) {
                return [PSCustomObject]@{
                    TimeSeconds = 2
                    KnowledgeRequired = $false
                    Input = @(
                        @{ ItemId = "Ingredient_Stick"; Quantity = 2 },
                        @{ ResourceTypeId = "Rubble"; Quantity = 2 }
                    )
                    BenchRequirement = $benchReq
                }
            }
        }
    }

    return [PSCustomObject]@{
        TimeSeconds = 2
        KnowledgeRequired = $false
        Input = @(
            @{ ItemId = "Ingredient_Stick"; Quantity = 2 },
            @{ ResourceTypeId = "Rubble"; Quantity = 2 }
        )
        BenchRequirement = @(
            @{ Id = "Workbench"; Type = "Crafting"; Categories = @("Workbench_Crafting") }
        )
    }
}

function Get-TemplateBench {
    param($entry)
    $group = $entry.Dir
    if ($groupBench.ContainsKey($group)) {
        return $groupBench[$group]
    }
    $tagTypes = Get-PropValue -Obj (Get-PropValue -Obj $entry.Data -Name "Tags") -Name "Type"
    if ($tagTypes) {
        foreach ($type in $tagTypes) {
            $benchReq = Get-BestBenchForType -Type $type
            if ($benchReq) { return $benchReq }
        }
    }
    return @(
        @{ Id = "Workbench"; Type = "Crafting"; Categories = @("Workbench_Crafting") }
    )
}

$written = 0
$skippedExisting = 0
$fixedBench = 0
$addedRecipe = 0

foreach ($id in $overrides) {
    $entry = $items[$id]
    if (-not $entry) { continue }
    $targetPath = Join-Path $OutputRoot $entry.RelPath
    if (Test-Path $targetPath) {
        $skippedExisting++
        continue
    }
    $data = $entry.Data
    $seen = @{}
    $recipe = Get-EffectiveRecipe -Id $id -Seen $seen
    if (-not $recipe) {
        $template = Get-TemplateRecipe -entry $entry -id $id
        $targetToken = Get-MaterialToken -data $data -id $id
        $template = ApplyMaterialSubstitution -recipe $template -targetToken $targetToken
        $template = Normalize-RecipeArrays -Recipe $template
        Set-PropValue -Obj $data -Name "Recipe" -Value $template
        $addedRecipe++
    } else {
        $benchReq = Get-PropValue -Obj $recipe -Name "BenchRequirement"
        $badBench = $false
        if (-not $benchReq -or (Get-CollectionCount $benchReq) -eq 0) { $badBench = $true }
        if ($benchReq) {
            foreach ($bench in @($benchReq)) {
                $benchId = Get-PropValue -Obj $bench -Name "Id"
                if (-not $benchId -or $benchId -eq "TODO") { $badBench = $true }
            }
        }
        if ($badBench) {
            Set-PropValue -Obj $recipe -Name "BenchRequirement" -Value (Ensure-Array (Get-TemplateBench -entry $entry))
            $recipe = Normalize-RecipeArrays -Recipe $recipe
            Set-PropValue -Obj $data -Name "Recipe" -Value $recipe
            $fixedBench++
        } else {
            $recipe = Normalize-RecipeArrays -Recipe $recipe
            Set-PropValue -Obj $data -Name "Recipe" -Value $recipe
        }
    }

    $dir = Split-Path $targetPath -Parent
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    $json = $data | ConvertTo-Json -Depth 100
    $json | Set-Content -Path $targetPath -Encoding ASCII
    $written++
}

$reportLines = @()
$reportLines += "# Crafting Audit (Hytale Base Assets)"
$reportLines += ""
$reportLines += "Assets root: $AssetsRoot"
$reportLines += ""
$reportLines += "## Summary"
$reportLines += "- Items scanned: $($items.Count)"
$reportLines += "- Items with no effective recipe: $($missing.Count)"
$reportLines += "- Items with bench issues: $($benchIssues.Count)"
$reportLines += "- Overrides written: $written"
$reportLines += "- Overrides skipped (already existed in mod): $skippedExisting"
$reportLines += "- Recipes added: $addedRecipe"
$reportLines += "- Bench requirements fixed: $fixedBench"
$reportLines += ""
$reportLines += "## Assumptions"
$reportLines += "- If an item lacks a Recipe, copy a template recipe from the most common recipe in the same directory group; fallback to parent recipe if present."
$reportLines += "- If no group or parent recipe is available, choose the most common bench requirement for the item's Tags.Type and use a basic input fallback (Stick + Rubble)."
$reportLines += '- If a recipe has a BenchRequirement with Id "TODO" or no benches, replace it using the group or type-derived bench.'
$reportLines += "- Material substitution only adjusts Ingredient_Bar_* inputs when the item id or Tags.Family contains a matching material token."
$reportLines += "- Existing mod overrides are not overwritten."
$reportLines += ""
$reportLines += "## Notes"
$reportLines += "- Crafting XP and gating remain driven by item ids and existing CraftingSkillRegistry rules."

$reportDir = Split-Path $ReportPath -Parent
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}
$reportLines | Set-Content -Path $ReportPath -Encoding ASCII

Write-Output \"Audit complete. Overrides written: $written. Report: $ReportPath\"
