# XML well-formedness check for all project XML files
$ErrorActionPreference = 'Stop'
$files = Get-ChildItem -Recurse -File -Include *.xml | Where-Object { $_.FullName -notmatch '\\\.git\\' }
$bad = 0
foreach ($f in $files) {
    try {
        $x = New-Object System.Xml.XmlDocument
        $x.Load($f.FullName)
    } catch {
        $bad++
        Write-Output ("BROKEN: {0} -> {1}" -f $f.FullName, $_.Exception.Message)
    }
}
Write-Output ("Checked {0} XML files, {1} broken." -f $files.Count, $bad)

# Confirm framework-string usages are fully qualified
Select-String -Path "app\src\main\java\com\rubenotepad\app\*.kt" -Pattern "android\.R\.string\.(ok|cancel)" | ForEach-Object { $_.Line.Trim() }
