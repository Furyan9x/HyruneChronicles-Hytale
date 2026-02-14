param(
    [string]$ConfigPath = (Join-Path (Resolve-Path ".").Path "hyrune_data\\gameplay_config.json"),
    [string]$Source = "crafted",
    [string]$ProfessionSkill = "WEAPONSMITHING",
    [int]$ProfessionLevel = 50,
    [int]$BenchTier = 3,
    [int]$Trials = 200000
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path $ConfigPath)) {
    throw "Config not found: $ConfigPath"
}

$cfg = Get-Content -Raw $ConfigPath | ConvertFrom-Json
$model = $cfg.itemizationRarityModel
if (-not $model) {
    throw "itemizationRarityModel missing from config."
}

$weights = $null
if ($model.baseWeightsBySource -and $model.baseWeightsBySource.PSObject.Properties[$Source]) {
    $weights = $model.baseWeightsBySource.$Source
}
if (-not $weights) {
    throw "No base weights found for source '$Source'."
}

$perLevel = 0.0
if ($model.professionBonusPerLevel -and $model.professionBonusPerLevel.PSObject.Properties[$ProfessionSkill]) {
    $perLevel = [double]$model.professionBonusPerLevel.$ProfessionSkill
}
$maxLevel = [Math]::Max(1, [int]$model.maxProfessionLevel)
$clampedLevel = [Math]::Max(0, [Math]::Min($maxLevel, $ProfessionLevel))
$professionBonus = $perLevel * $clampedLevel

$benchBonus = 0.0
$benchKey = [string]$BenchTier
if ($model.benchTierBonus -and $model.benchTierBonus.PSObject.Properties[$benchKey]) {
    $benchBonus = [double]$model.benchTierBonus.$benchKey
}

$scoreRaw = $professionBonus + $benchBonus
$score = [Math]::Max([double]$model.minRarityScore, [Math]::Min([double]$model.maxRarityScore, $scoreRaw))
$shift = [double]$model.rarityShiftStrength

$rarities = @("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC")
$base = @(
    [Math]::Max(0.0, [double]$weights.common),
    [Math]::Max(0.0, [double]$weights.uncommon),
    [Math]::Max(0.0, [double]$weights.rare),
    [Math]::Max(0.0, [double]$weights.epic),
    [Math]::Max(0.0, [double]$weights.legendary),
    [Math]::Max(0.0, [double]$weights.mythic)
)

$mid = 2.5
$adjusted = @()
for ($i = 0; $i -lt $base.Count; $i++) {
    $bias = $i - $mid
    $factor = 1.0 + ($score * $shift * $bias)
    if ($factor -lt 0.05) { $factor = 0.05 }
    $adjusted += ($base[$i] * $factor)
}

$sum = ($adjusted | Measure-Object -Sum).Sum
if ($sum -le 0) { throw "Adjusted weights sum to zero." }
$normalized = @($adjusted | ForEach-Object { $_ / $sum })

$counts = @{}
foreach ($r in $rarities) { $counts[$r] = 0 }

for ($t = 0; $t -lt $Trials; $t++) {
    $roll = Get-Random -Minimum 0.0 -Maximum 1.0
    $cursor = 0.0
    for ($i = 0; $i -lt $normalized.Count; $i++) {
        $cursor += $normalized[$i]
        if ($roll -le $cursor) {
            $counts[$rarities[$i]]++
            break
        }
    }
}

Write-Output "Simulation input:"
Write-Output "  source=$Source skill=$ProfessionSkill level=$ProfessionLevel benchTier=$BenchTier trials=$Trials"
Write-Output "  scoreRaw=$([Math]::Round($scoreRaw, 4)) scoreClamped=$([Math]::Round($score, 4))"
Write-Output ""
Write-Output "Expected probabilities:"
for ($i = 0; $i -lt $rarities.Count; $i++) {
    Write-Output ("  {0,-10} {1,8:P3}" -f $rarities[$i], $normalized[$i])
}
Write-Output ""
Write-Output "Simulated distribution:"
foreach ($r in $rarities) {
    $pct = $counts[$r] / [double]$Trials
    Write-Output ("  {0,-10} {1,10:N0} ({2,8:P3})" -f $r, $counts[$r], $pct)
}
