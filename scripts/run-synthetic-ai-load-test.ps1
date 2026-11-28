$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$pomPath = Join-Path $projectRoot 'pom.xml'
if (-not (Test-Path -LiteralPath $pomPath)) {
    throw "Could not find the project pom.xml at '$pomPath'."
}

$maven = Get-Command mvn -ErrorAction Stop
$ollamaTags = Invoke-RestMethod -Uri 'http://127.0.0.1:11434/api/tags' -TimeoutSec 3
if (-not ($ollamaTags.models | Where-Object { $_.name -like 'qwen2.5:3b*' })) {
    throw 'The local qwen2.5:3b model is not available. Start Ollama and pull that model first.'
}

$env:AI_JOB_ASSISTANT_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3306/ai_job_assistant_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:AI_JOB_ASSISTANT_TEST_DB_USERNAME = Read-Host 'MySQL username with access to ai_job_assistant_test'
$securePassword = Read-Host 'MySQL password (input stays hidden)' -AsSecureString
$credential = [System.Net.NetworkCredential]::new('', $securePassword)
$env:AI_JOB_ASSISTANT_TEST_DB_PASSWORD = $credential.Password
$env:AI_JOB_ASSISTANT_RUN_SYNTHETIC_LOAD = 'true'

try {
    Write-Host 'Running the synthetic load test. It saves 60 artificial resumes and 1,001 AI questions in ai_job_assistant_test.'
    & $maven.Source '-f' $pomPath '-Dtest=SyntheticAiLoadTest' -B test
    $testExitCode = $LASTEXITCODE
}
finally {
    $env:AI_JOB_ASSISTANT_TEST_DB_URL = $null
    $env:AI_JOB_ASSISTANT_TEST_DB_USERNAME = $null
    $env:AI_JOB_ASSISTANT_TEST_DB_PASSWORD = $null
    $env:AI_JOB_ASSISTANT_RUN_SYNTHETIC_LOAD = $null
    $securePassword.Dispose()
    $credential = $null
}

exit $testExitCode
