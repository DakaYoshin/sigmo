$files = Get-ChildItem -Path "F:\sigmo\GameServer\java" -Recurse -Filter *.java

foreach ($file in $files) {
    try {
        $content = Get-Content $file.FullName -Raw
        $originalContent = $content

        # Commons Logging -> SLF4J
        $content = $content -replace 'import org.apache.commons.logging.Log;', 'import org.slf4j.Logger;'
        $content = $content -replace 'import org.apache.commons.logging.LogFactory;', 'import org.slf4j.LoggerFactory;'
        
        # Replace variable declarations (handling typical modifiers)
        # Matches "private/protected/public static final Log " or just "Log " preceded by usage context is hard regex.
        # We rely on the convention that these are usually fields.
        $content = $content -replace '(\b(private|protected|public)\s+(static\s+)?(final\s+)?)Log(\s+\w+\s*=\s*LogFactory\.getLog)', '$1Logger$5'
        $content = $content -replace '(\b(private|protected|public)\s+(static\s+)?(final\s+)?)Log(\s+\w+\s*=\s*LoggerFactory\.getLogger)', '$1Logger$5'
        
        # Fallback for splitting declaration and assignment if any (rare in L2J)
        # Just replace LogFactory.getLog first
        $content = $content -replace 'LogFactory\.getLog', 'LoggerFactory.getLogger'
        
        # Replace .fatal()
        $content = $content -replace '\.fatal\(', '.error('

        # Commons Lang -> Lang3
        $content = $content -replace 'import org.apache.commons.lang\.', 'import org.apache.commons.lang3.'

        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8
            Write-Host "Updated $($file.Name)"
        }
    }
    catch {
        Write-Error "Failed to process $($file.Name): $_"
    }
}
