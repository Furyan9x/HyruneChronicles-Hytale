param(
    [string]$ItemsRoot = (Join-Path (Resolve-Path ".").Path "src\\main\\resources\\Server\\Item\\Items"),
    [string]$AssetsRoot = "C:\\Users\\devin\\Desktop\\HytaleServer\\Assets",
    [string]$ReportPath = (Join-Path (Resolve-Path ".").Path "codex\\recipe_refine_report.md"),
    [switch]$DryRun,
    [switch]$Force
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

function Write-JsonNoBom {
    param([string]$Path, $Object)
    $json = $Object | ConvertTo-Json -Depth 100
    [System.IO.File]::WriteAllText($Path, $json + [Environment]::NewLine, $Utf8NoBomEncoding)
}

function Write-LinesNoBom {
    param([string]$Path, [string[]]$Lines)
    [System.IO.File]::WriteAllText($Path, (($Lines -join [Environment]::NewLine) + [Environment]::NewLine), $Utf8NoBomEncoding)
}

# ===========================
# User-Editable Configuration
# ===========================
#
# Priority order at runtime:
# 1) CustomRecipeOverrides (prefix rule match)
# 2) Exact source recipe from base assets
# 3) Subtype+material source recipe from base assets
# 4) Profile-based fallback (SubtypeRecipeProfiles + material scaling)
#
# Tip:
# - Keep ItemId values as canonical IDs.
# - Inputs are clamped to 3..5 ingredients.
# - You can set ResourceTypeId instead of ItemId when needed.

$CustomRecipeOverrides = @(
    # Example:
    # @{
    #     MatchPrefix = "weapon_staff_cobalt"
    #     TimeSeconds = 4
    #     Inputs = @(
    #         @{ ItemId = "Ingredient_Bar_Cobalt"; Quantity = 4 },
    #         @{ ItemId = "Ingredient_Plank_Oak"; Quantity = 3 },
    #         @{ ItemId = "Ingredient_Fabric_Scrap_Shadoweave"; Quantity = 2 }
    #     )
    #     Bench = @{ Id = "Weapon_Bench"; Type = "Crafting"; Categories = @("Weapon_Staff") }
    # }
)


# Per-subtype quantity curves. Edit these to tune recipe weight by subtype.
# Fields:
# - primary: multiplier for primary material quantity
# - secondary: quantity for secondary ingredient
# - tertiary: quantity for tertiary ingredient
# - timeSeconds: crafting time override
# - secondaryItemId / tertiaryItemId: optional ingredient substitutions
$SubtypeRecipeProfiles = @{
    "weapon_sword"      = @{ primary = 1.00; secondary = 2; tertiary = 1; timeSeconds = 2.5; secondaryItemId = "Ingredient_Leather_Medium"; tertiaryItemId = "Ingredient_Fabric_Scrap_Linen" }
    "weapon_longsword"  = @{ primary = 1.35; secondary = 3; tertiary = 2; timeSeconds = 3.5; secondaryItemId = "Ingredient_Leather_Heavy"; tertiaryItemId = "Ingredient_Fabric_Scrap_Cindercloth" }
    "weapon_mace"       = @{ primary = 1.15; secondary = 2; tertiary = 1; timeSeconds = 3.0; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Fibre" }
    "weapon_staff"      = @{ primary = 0.90; secondary = 3; tertiary = 2; timeSeconds = 3.0; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Fabric_Scrap_Shadoweave" }
    "weapon_wand"       = @{ primary = 0.65; secondary = 2; tertiary = 1; timeSeconds = 2.0; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Fabric_Scrap_Linen" }
    "weapon_daggers"    = @{ primary = 0.80; secondary = 2; tertiary = 1; timeSeconds = 2.0; secondaryItemId = "Ingredient_Leather_Light"; tertiaryItemId = "Ingredient_Fabric_Scrap_Linen" }
    "weapon_shortbow"   = @{ primary = 0.85; secondary = 3; tertiary = 1; timeSeconds = 2.5; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Fabric_Scrap_Linen" }
    "weapon_spear"      = @{ primary = 1.00; secondary = 3; tertiary = 1; timeSeconds = 2.8; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Fibre" }
    "weapon_shield"     = @{ primary = 1.20; secondary = 3; tertiary = 2; timeSeconds = 3.0; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Leather_Heavy" }
    "armor_generic"     = @{ primary = 1.20; secondary = 2; tertiary = 2; timeSeconds = 2.5; secondaryItemId = "Ingredient_Leather_Medium"; tertiaryItemId = "Ingredient_Fabric_Scrap_Linen" }
    "tool_pickaxe"      = @{ primary = 1.10; secondary = 2; tertiary = 1; timeSeconds = 2.5; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Leather_Light" }
    "tool_hatchet"      = @{ primary = 1.00; secondary = 2; tertiary = 1; timeSeconds = 2.2; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Leather_Light" }
    "tool_shovel"       = @{ primary = 0.85; secondary = 2; tertiary = 1; timeSeconds = 2.0; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Fibre" }
    "tool_rod"          = @{ primary = 0.70; secondary = 2; tertiary = 1; timeSeconds = 2.0; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Fibre" }
    "tool_generic"      = @{ primary = 0.90; secondary = 2; tertiary = 1; timeSeconds = 2.0; secondaryItemId = "Ingredient_Plank_Oak"; tertiaryItemId = "Ingredient_Leather_Light" }
}

$eligiblePrefixes = @("weapon_", "armor_", "tool_")
$blockedPrefixes = @(
    "weapon_bomb_",
    "weapon_grenade_",
    "weapon_poison_flask_",
    "weapon_dev_",
    "armor_dev_",
    "tool_dev_",
    "test_",
    "special_"
)
$knownMaterials = @(
    "crude","wood","copper","bronze","iron","steel","cobalt","thorium","adamantite","mithril","onyxium",
    "bone","stone","scrap","rusty","leaf","silver","gold","obsidian","praetorian"
)

function Is-Eligible {
    param([string]$ItemId)
    $id = Normalize-Id $ItemId
    $ok = $false
    foreach ($prefix in $eligiblePrefixes) {
        if ($id.StartsWith($prefix)) { $ok = $true; break }
    }
    if (-not $ok) { return $false }
    foreach ($prefix in $blockedPrefixes) {
        if ($id.StartsWith($prefix)) { return $false }
    }
    return $true
}

function Parse-Shape {
    param([string]$ItemId)
    $tokens = (Normalize-Id $ItemId).Split("_")
    if ($tokens.Length -lt 2) { return @("", "", "") }
    $itemClass = $tokens[0]
    $subType = $tokens[1]
    $material = ""
    if ($tokens.Length -gt 2) {
        for ($i = $tokens.Length - 1; $i -ge 2; $i--) {
            if ($knownMaterials -contains $tokens[$i]) {
                $material = $tokens[$i]
                break
            }
        }
        if (-not $material) { $material = $tokens[$tokens.Length - 1] }
    }
    return @($itemClass, $subType, $material)
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

function Get-MaterialTier {
    param([string]$Material)
    $tiers = @{
        "crude" = 1; "wood" = 1; "scrap" = 1; "rusty" = 1; "bone" = 1; "leaf" = 1;
        "copper" = 2; "bronze" = 2;
        "iron" = 3; "steel" = 3;
        "cobalt" = 4; "thorium" = 4;
        "adamantite" = 5; "mithril" = 5;
        "onyxium" = 6;
        "silver" = 4; "gold" = 4; "obsidian" = 5; "praetorian" = 6
    }
    if ($tiers.ContainsKey($Material)) { return [int]$tiers[$Material] }
    return 3
}

function Clamp-Inputs {
    param([array]$Inputs)
    $out = @($Inputs)
    $fillers = @(
        [PSCustomObject]@{ ItemId = "Ingredient_Stick"; ResourceTypeId = $null; Quantity = 2 },
        [PSCustomObject]@{ ItemId = "Ingredient_Leather_Light"; ResourceTypeId = $null; Quantity = 1 },
        [PSCustomObject]@{ ItemId = $null; ResourceTypeId = "Rubble"; Quantity = 2 }
    )
    $idx = 0
    while ($out.Count -lt 3) {
        $pick = $fillers[[Math]::Min($idx, $fillers.Count - 1)]
        $out += [PSCustomObject]@{
            ItemId = $pick.ItemId
            ResourceTypeId = $pick.ResourceTypeId
            Quantity = $pick.Quantity
        }
        $idx++
    }
    if ($out.Count -gt 5) { $out = @($out | Select-Object -First 5) }
    return $out
}

function Default-Bench {
    param([string]$ItemId, [string]$ItemClass, [string]$SubType, $Data)
    $forced = Resolve-BenchOverride -ItemId $ItemId -ItemClass $ItemClass -SubType $SubType -Data $Data
    if ($forced) { return $forced }
    $sub = if ($SubType) { ($SubType.Substring(0,1).ToUpperInvariant() + $SubType.Substring(1)) } else { "Generic" }
    if ($ItemClass -eq "weapon") {
        return @([PSCustomObject]@{ Id = "Weapon_Bench"; Type = "Crafting"; Categories = @("Weapon_$sub") })
    }
    if ($ItemClass -eq "armor") {
        $slot = Get-ArmorSlot -ItemId $ItemId -Data $Data
        return @([PSCustomObject]@{ Id = "Armor_Bench"; Type = "Crafting"; Categories = @(Get-ArmorCategoryForSlot -Slot $slot) })
    }
    return @([PSCustomObject]@{ Id = "Workbench"; Type = "Crafting"; Categories = @("Workbench_Tools") })
}

function Normalize-Recipe {
    param($Recipe, [string]$ItemClass, [string]$SubType, [string]$ItemId = "", $Data = $null)
    if ($null -eq $Recipe) { $Recipe = [PSCustomObject]@{} }

    $inputs = @()
    foreach ($input in (Ensure-Array (Get-PropValue -Obj $Recipe -Name "Input"))) {
        if ($null -eq $input) { continue }
        $itemId = Get-PropValue -Obj $input -Name "ItemId"
        $resourceId = Get-PropValue -Obj $input -Name "ResourceTypeId"
        if ([string]::IsNullOrWhiteSpace($itemId) -and [string]::IsNullOrWhiteSpace($resourceId)) { continue }
        $qty = 1
        $inputQty = Get-PropValue -Obj $input -Name "Quantity"
        if ($null -ne $inputQty) { $qty = [int]$inputQty }
        if ($qty -le 0) { $qty = 1 }
        $inputs += [PSCustomObject]@{ ItemId = $itemId; ResourceTypeId = $resourceId; Quantity = $qty }
    }
    $inputs = Clamp-Inputs $inputs

    $benches = @()
    foreach ($bench in (Ensure-Array (Get-PropValue -Obj $Recipe -Name "BenchRequirement"))) {
        if ($null -eq $bench) { continue }
        $benchId = Get-PropValue -Obj $bench -Name "Id"
        if ([string]::IsNullOrWhiteSpace($benchId) -or $benchId -eq "TODO") { continue }
        $benchType = Get-PropValue -Obj $bench -Name "Type"
        $benchCategories = Ensure-Array (Get-PropValue -Obj $bench -Name "Categories")
        $requiredTierLevel = Get-PropValue -Obj $bench -Name "RequiredTierLevel"
        $normalized = [ordered]@{
            Id = $benchId
            Type = $(if ([string]::IsNullOrWhiteSpace($benchType)) { "Crafting" } else { $benchType })
            Categories = @($benchCategories)
        }
        if ($null -ne $requiredTierLevel) { $normalized["RequiredTierLevel"] = [int]$requiredTierLevel }
        $benches += [PSCustomObject]$normalized
    }
    if ($benches.Count -eq 0) { $benches = Default-Bench -ItemId $ItemId -ItemClass $ItemClass -SubType $SubType -Data $Data }

    $timeSeconds = 2
    $recipeTimeSeconds = Get-PropValue -Obj $Recipe -Name "TimeSeconds"
    if ($null -ne $recipeTimeSeconds) { $timeSeconds = $recipeTimeSeconds }
    return [PSCustomObject]@{
        TimeSeconds = $timeSeconds
        KnowledgeRequired = $false
        Input = $inputs
        BenchRequirement = $benches
    }
}

function Material-Primary {
    param([string]$Material, [string]$ItemClass)
    $barMap = @{
        "copper" = "Ingredient_Bar_Copper"; "bronze" = "Ingredient_Bar_Bronze"; "iron" = "Ingredient_Bar_Iron";
        "steel" = "Ingredient_Bar_Steel"; "cobalt" = "Ingredient_Bar_Cobalt"; "thorium" = "Ingredient_Bar_Thorium";
        "adamantite" = "Ingredient_Bar_Adamantite"; "mithril" = "Ingredient_Bar_Mithril"; "onyxium" = "Ingredient_Bar_Onyxium";
        "silver" = "Ingredient_Bar_Silver"; "gold" = "Ingredient_Bar_Gold"
    }
    if ($barMap.ContainsKey($Material)) { return [PSCustomObject]@{ ItemId = $barMap[$Material]; ResourceTypeId = $null; Quantity = 5 } }
    if ($Material -eq "wood" -or $Material -eq "leaf") { return [PSCustomObject]@{ ItemId = "Ingredient_Plank_Oak"; ResourceTypeId = $null; Quantity = 4 } }
    if ($Material -eq "bone") { return [PSCustomObject]@{ ItemId = "Ingredient_Bone"; ResourceTypeId = $null; Quantity = 4 } }
    if ($Material -eq "scrap" -or $Material -eq "rusty") { return [PSCustomObject]@{ ItemId = "Ingredient_Bar_Iron"; ResourceTypeId = $null; Quantity = 3 } }
    if ($ItemClass -eq "armor") { return [PSCustomObject]@{ ItemId = "Ingredient_Leather_Medium"; ResourceTypeId = $null; Quantity = 4 } }
    return [PSCustomObject]@{ ItemId = "Ingredient_Bar_Iron"; ResourceTypeId = $null; Quantity = 4 }
}

function Get-SubtypeProfile {
    param([string]$ItemClass, [string]$SubType)
    $keySpecific = "$ItemClass`_$SubType"
    if ($SubtypeRecipeProfiles.ContainsKey($keySpecific)) { return $SubtypeRecipeProfiles[$keySpecific] }
    $keyGeneric = "$ItemClass`_generic"
    if ($SubtypeRecipeProfiles.ContainsKey($keyGeneric)) { return $SubtypeRecipeProfiles[$keyGeneric] }
    return @{
        primary = 1.0; secondary = 2; tertiary = 1; timeSeconds = 2.0;
        secondaryItemId = "Ingredient_Leather_Light"; tertiaryItemId = "Ingredient_Fabric_Scrap_Linen"
    }
}

function Build-ProfileFallbackRecipe {
    param([string]$ItemId)
    $shape = Parse-Shape $ItemId
    $itemClass = $shape[0]
    $subType = $shape[1]
    $material = $shape[2]
    $tier = Get-MaterialTier -Material $material
    $primary = Material-Primary -Material $material -ItemClass $itemClass
    $profile = Get-SubtypeProfile -ItemClass $itemClass -SubType $subType

    $basePrimary = [int](Get-PropValue -Obj $primary -Name "Quantity")
    if ($basePrimary -le 0) { $basePrimary = 4 }
    $scale = [double](Get-PropValue -Obj $profile -Name "primary")
    $primaryQty = [Math]::Max(1, [int][Math]::Round($basePrimary * $scale + (($tier - 1) * 0.8)))

    $secondaryQty = [Math]::Max(1, [int](Get-PropValue -Obj $profile -Name "secondary"))
    $tertiaryQty = [Math]::Max(1, [int](Get-PropValue -Obj $profile -Name "tertiary"))
    $timeSeconds = [double](Get-PropValue -Obj $profile -Name "timeSeconds")
    if ($timeSeconds -le 0) { $timeSeconds = 2.0 }
    $secondaryItemId = Get-PropValue -Obj $profile -Name "secondaryItemId"
    $tertiaryItemId = Get-PropValue -Obj $profile -Name "tertiaryItemId"

    $inputs = @(
        [PSCustomObject]@{ ItemId = (Get-PropValue -Obj $primary -Name "ItemId"); ResourceTypeId = (Get-PropValue -Obj $primary -Name "ResourceTypeId"); Quantity = $primaryQty },
        [PSCustomObject]@{ ItemId = $secondaryItemId; ResourceTypeId = $null; Quantity = $secondaryQty },
        [PSCustomObject]@{ ItemId = $tertiaryItemId; ResourceTypeId = $null; Quantity = $tertiaryQty }
    )

    return [PSCustomObject]@{
        TimeSeconds = $timeSeconds
        KnowledgeRequired = $false
        Input = (Clamp-Inputs $inputs)
        BenchRequirement = (Default-Bench -ItemId $ItemId -ItemClass $itemClass -SubType $subType -Data $null)
    }
}

function Get-CustomRecipeOverride {
    param([string]$ItemId, [string]$ItemClass, [string]$SubType)
    $id = Normalize-Id $ItemId
    foreach ($rule in $CustomRecipeOverrides) {
        $matchPrefix = Normalize-Id (Get-PropValue -Obj $rule -Name "MatchPrefix")
        if (-not $matchPrefix) { continue }
        if (-not $id.StartsWith($matchPrefix)) { continue }

        $inputs = @()
        foreach ($input in (Ensure-Array (Get-PropValue -Obj $rule -Name "Inputs"))) {
            if ($null -eq $input) { continue }
            $itemRef = Get-PropValue -Obj $input -Name "ItemId"
            $resourceRef = Get-PropValue -Obj $input -Name "ResourceTypeId"
            if ([string]::IsNullOrWhiteSpace($itemRef) -and [string]::IsNullOrWhiteSpace($resourceRef)) { continue }
            $qty = [int](Get-PropValue -Obj $input -Name "Quantity")
            if ($qty -le 0) { $qty = 1 }
            $inputs += [PSCustomObject]@{ ItemId = $itemRef; ResourceTypeId = $resourceRef; Quantity = $qty }
        }

        $bench = Get-PropValue -Obj $rule -Name "Bench"
        $benchList = @()
        if ($bench) {
            $benchList += [PSCustomObject]@{
                Id = Get-PropValue -Obj $bench -Name "Id"
                Type = Get-PropValue -Obj $bench -Name "Type"
                Categories = @(Ensure-Array (Get-PropValue -Obj $bench -Name "Categories"))
            }
        } else {
            $benchList = Default-Bench -ItemId $ItemId -ItemClass $ItemClass -SubType $SubType -Data $null
        }

        $timeSeconds = Get-PropValue -Obj $rule -Name "TimeSeconds"
        if ($null -eq $timeSeconds) { $timeSeconds = 2 }
        return [PSCustomObject]@{
            TimeSeconds = $timeSeconds
            KnowledgeRequired = $false
            Input = (Clamp-Inputs $inputs)
            BenchRequirement = $benchList
        }
    }
    return $null
}

function Recipe-Signature {
    param($Recipe, [string]$ItemClass, [string]$SubType)
    $normalized = Normalize-Recipe -Recipe $Recipe -ItemClass $ItemClass -SubType $SubType
    return ($normalized | ConvertTo-Json -Depth 20 -Compress)
}

function Is-GenericPlaceholderRecipe {
    param($Recipe, [string]$ItemClass, [string]$SubType)
    $normalized = Normalize-Recipe -Recipe $Recipe -ItemClass $ItemClass -SubType $SubType
    $inputs = @(Ensure-Array (Get-PropValue -Obj $normalized -Name "Input"))
    if ($inputs.Count -ne 3) { return $false }
    $a = $inputs[0]
    $b = $inputs[1]
    $c = $inputs[2]
    $isGenericInputs = (Normalize-Id (Get-PropValue -Obj $a -Name "ItemId")) -eq "ingredient_stick" `
        -and (Normalize-Id (Get-PropValue -Obj $b -Name "ItemId")) -eq "ingredient_leather_light" `
        -and (Normalize-Id (Get-PropValue -Obj $c -Name "ResourceTypeId")) -eq "rubble"
    if (-not $isGenericInputs) { return $false }
    $benches = @(Ensure-Array (Get-PropValue -Obj $normalized -Name "BenchRequirement"))
    if ($benches.Count -ne 1) { return $false }
    $bench = $benches[0]
    return (Normalize-Id (Get-PropValue -Obj $bench -Name "Id")) -eq "workbench"
}

$assetsItemsRoot = Join-Path $AssetsRoot "Server\\Item\\Items"
if (-not (Test-Path $assetsItemsRoot)) { throw "Assets items path not found: $assetsItemsRoot" }
if (-not (Test-Path $ItemsRoot)) { throw "Items path not found: $ItemsRoot" }

$catalogExact = @{}
$catalogTriplet = @{}

Get-ChildItem -Recurse -File -Path $assetsItemsRoot -Filter *.json | ForEach-Object {
    $itemId = $_.BaseName
    if (-not (Is-Eligible $itemId)) { return }
    try {
        $data = Get-Content -Raw $_.FullName | ConvertFrom-Json
    } catch {
        return
    }
    if (-not $data) { return }
    $baseRecipe = Get-PropValue -Obj $data -Name "Recipe"
    if (-not $baseRecipe) { return }
    $shape = Parse-Shape $itemId
    $itemClass = $shape[0]
    $subType = $shape[1]
    $material = $shape[2]
    $recipe = Normalize-Recipe -Recipe $baseRecipe -ItemClass $itemClass -SubType $subType -ItemId $itemId -Data $data
    if (Is-GenericPlaceholderRecipe -Recipe $recipe -ItemClass $itemClass -SubType $subType) { return }
    $exactKey = Normalize-Id $itemId
    $catalogExact[$exactKey] = $recipe
    $tripletKey = "$itemClass|$subType|$material"
    if (-not $catalogTriplet.ContainsKey($tripletKey)) { $catalogTriplet[$tripletKey] = $recipe }
}

$eligibleScanned = 0
$changed = 0
$unchanged = 0
$sourceCustom = 0
$sourceExact = 0
$sourceTriplet = 0
$sourceFallback = 0
$changedFiles = @()

Get-ChildItem -Recurse -File -Path $ItemsRoot -Filter *.json | Sort-Object FullName | ForEach-Object {
    $itemId = $_.BaseName
    if (-not (Is-Eligible $itemId)) { return }
    $eligibleScanned++
    try {
        $data = Get-Content -Raw $_.FullName | ConvertFrom-Json
    } catch {
        return
    }
    if (-not $data) { return }
    $shape = Parse-Shape $itemId
    $itemClass = $shape[0]
    $subType = $shape[1]
    $material = $shape[2]

    $newRecipe = $null
    $source = ""
    $custom = Get-CustomRecipeOverride -ItemId $itemId -ItemClass $itemClass -SubType $subType
    if ($custom) {
        $newRecipe = $custom
        $source = "custom"
    } else {
        $exactKey = Normalize-Id $itemId
        if ($catalogExact.ContainsKey($exactKey)) {
            $newRecipe = $catalogExact[$exactKey]
            $source = "exact"
        } else {
            $tripletKey = "$itemClass|$subType|$material"
            if ($catalogTriplet.ContainsKey($tripletKey)) {
                $newRecipe = $catalogTriplet[$tripletKey]
                $source = "triplet"
            } else {
                $newRecipe = Build-ProfileFallbackRecipe -ItemId $itemId
                $source = "fallback"
            }
        }
    }

    $newRecipe = Normalize-Recipe -Recipe $newRecipe -ItemClass $itemClass -SubType $subType -ItemId $itemId -Data $data
    $forcedBench = Resolve-BenchOverride -ItemId $itemId -ItemClass $itemClass -SubType $subType -Data $data
    if ($forcedBench) {
        $newRecipe.BenchRequirement = @($forcedBench)
    }
    $oldSig = Recipe-Signature -Recipe (Get-PropValue -Obj $data -Name "Recipe") -ItemClass $itemClass -SubType $subType
    $newSig = Recipe-Signature -Recipe $newRecipe -ItemClass $itemClass -SubType $subType
    if (-not $Force -and $oldSig -eq $newSig) {
        $unchanged++
        return
    }

    $data.Recipe = $newRecipe
    $data.Quality = "Common"
    $changed++
    switch ($source) {
        "custom" { $sourceCustom++ }
        "exact" { $sourceExact++ }
        "triplet" { $sourceTriplet++ }
        default { $sourceFallback++ }
    }
    $relative = $_.FullName.Substring((Resolve-Path $ItemsRoot).Path.Length + 1).Replace("\", "/")
    $changedFiles += $relative
    if (-not $DryRun) {
        Write-JsonNoBom -Path $_.FullName -Object $data
    }
}

$lines = @()
$lines += "# Recipe Refinement Report"
$lines += ""
$lines += "Mode: $(if ($DryRun) { 'dry-run' } else { 'write' })"
$lines += "Items root: $ItemsRoot"
$lines += "Assets root: $AssetsRoot"
$lines += ""
$lines += "## Summary"
$lines += "- Eligible scanned: $eligibleScanned"
$lines += "- Changed: $changed"
$lines += "- Unchanged: $unchanged"
$lines += "- Source custom overrides: $sourceCustom"
$lines += "- Source exact matches: $sourceExact"
$lines += "- Source subtype+material matches: $sourceTriplet"
$lines += "- Source profile fallback: $sourceFallback"
$lines += ""
$lines += "## Changed Files"
foreach ($rel in ($changedFiles | Select-Object -First 500)) {
    $lines += "- $rel"
}
$reportDir = Split-Path $ReportPath -Parent
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}
Write-LinesNoBom -Path $ReportPath -Lines $lines

Write-Output "Recipe refinement complete. mode=$(if ($DryRun) { 'dry-run' } else { 'write' }), changed=$changed, report=$ReportPath"
