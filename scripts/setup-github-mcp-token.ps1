param(
  [string]$Token
)

if (-not $Token) {
  $secureToken = Read-Host "请输入 GitHub Personal Access Token (不会回显)" -AsSecureString
  $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureToken)
  try {
    $Token = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
  }
  finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
  }
}

if (-not $Token) {
  Write-Error "Token 为空，已取消。"
  exit 1
}

$env:GITHUB_TOKEN = $Token
setx GITHUB_TOKEN $Token | Out-Null

Write-Host "GITHUB_TOKEN 已写入当前会话和用户环境变量。"
Write-Host "请重启 Codex 客户端/终端后再使用 GitHub MCP。"
