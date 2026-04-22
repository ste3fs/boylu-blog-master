param(
  [switch]$Headed,
  [switch]$Ui
)

$e2ePath = Join-Path $PSScriptRoot "..\\e2e"
Push-Location $e2ePath

try {
  if ($Ui) {
    npm run test:ui
  }
  elseif ($Headed) {
    npm run test:headed
  }
  else {
    npm test
  }
}
finally {
  Pop-Location
}
