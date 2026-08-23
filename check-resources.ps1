# Resource consistency checker for Rube Note Pad
$ErrorActionPreference = 'Stop'
$res  = "app\src\main\res"
$java = "app\src\main\java"

$defined = @{}
Get-ChildItem $res -Recurse -File | ForEach-Object {
    $base = $_.BaseName
    switch -Regex ($_.DirectoryName) {
        'layout$'        { $defined["layout/$base"] = 1 }
        'menu$'          { $defined["menu/$base"] = 1 }
        'drawable.*'     { if ($_.Extension -ne '.xml' -or (Select-String -Path $_.FullName -Pattern '<bitmap|<shape|<vector|<layer-list|<ripple') ) { $defined["drawable/$base"] = 1 } }
        'mipmap.*'       { $defined["mipmap/$base"] = 1 }
    }
}

# strings / plurals / colors / styles from values
[xml]$strings = Get-Content "$res\values\strings.xml" -Raw
$strings.resources.string | ForEach-Object { $defined["string/$($_.name)"] = 1 }
$strings.resources.plurals | ForEach-Object { $defined["plurals/$($_.name)"] = 1 }
[xml]$colors = Get-Content "$res\values\colors.xml" -Raw
$colors.resources.color | ForEach-Object { $defined["color/$($_.name)"] = 1 }
[xml]$themes = Get-Content "$res\values\themes.xml" -Raw
foreach ($s in $themes.resources.style) {
    if ($s.name -like 'Theme.*' -or $s.name -like 'TextAppearance.RubeNotePad*') { $defined["style/$($s.name)"] = 1 }
}
# Material library styles used
'Material3.CardView.Elevated','Material3.Button.TextButton','Material3.Toolbar','Material3.TitleLarge','Material3.HeadlineMedium','Material3.TitleMedium','Material3.TitleSmall','Material3.BodyMedium','Material3.BodySmall','Material3.LabelSmall' | ForEach-Object { $defined["style/Widget.$_"] = 1; $defined["style/TextAppearance.$_"] = 1 }

$missing = New-Object System.Collections.Generic.List[string]
function Check-Refs([string]$path, [string[]]$patterns) {
    $content = Get-Content $path -Raw
    foreach ($p in $patterns) {
        foreach ($m in [regex]::Matches($content, $p)) {
            $type = $m.Groups[1].Value; $name = $m.Groups[2].Value
            if ($name -match '^(android|app|material)$') { continue }
            if (-not $defined.ContainsKey("$type/$name")) { $missing.Add("$path -> @$type/$name") }
        }
    }
}

Get-ChildItem $res -Recurse -File -Include *.xml | ForEach-Object {
    Check-Refs $_.FullName @('@(string|plurals|color|drawable|mipmap|layout|menu|style)/([A-Za-z0-9_.]+)')
}

# Kotlin references: R.string.x, R.drawable.x, R.layout.x, R.menu.x, R.id.x, R.plurals.x
$idDefs = [regex]::Matches((Get-ChildItem $res -Recurse -File -Include *.xml | ForEach-Object { Get-Content $_.FullName -Raw }), '\+id/([A-Za-z0-9_]+)') |
    ForEach-Object { "id/$($_.Groups[1].Value)" }
$idDefs | ForEach-Object { $defined[$_] = 1 }

Get-ChildItem $java -Recurse -File -Include *.kt | ForEach-Object {
    Check-Refs $_.FullName @('R\.(string|drawable|layout|menu|plurals|color)\.([A-Za-z0-9_]+)')
}

if ($missing.Count -eq 0) { "OK: all resource references resolve." }
else { "MISSING REFERENCES:"; $missing | ForEach-Object { "  $_" } }
