$files = Get-ChildItem -Path "F:\sigmo\GameServer\java" -Recurse -Filter *.java

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content

    # Check if the file is using SLF4J
    if ($content -match "import org.slf4j.LoggerFactory;") {
        
        # Replace variable type assertions
        # Use regex to find "Log _log" declarations and change to "Logger _log"
        # We need to be careful not to replace "Logic" or "Login" etc.
        # \bLog\b matches exact word Log.
        
        # Pattern 1: private static final Log _log = ...
        $content = $content -replace '(\b(private|protected|public)\s+(static\s+)?(final\s+)?)Log(\s+_log\s*=\s*LoggerFactory)', '$1Logger$5'
        
        # Pattern 2: static Log _log = ... (without access modifier)
        $content = $content -replace '(\bstatic\s+(final\s+)?)Log(\s+_log\s*=\s*LoggerFactory)', '$1Logger$3'
        
        # Pattern 3: private final static Log _log (order swapped)
        $content = $content -replace '(\b(private|protected|public)\s+(final\s+)?(static\s+)?)Log(\s+_log\s*=\s*LoggerFactory)', '$1Logger$5'

        # Pattern 4: Just "Log _log =" if previous failed but context is clear
        $content = $content -replace '\bLog\s+_log\s*=\s*LoggerFactory', 'Logger _log = LoggerFactory'

        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8
            Write-Host "Fixed Log type in $($file.Name)"
        }
    }
}
