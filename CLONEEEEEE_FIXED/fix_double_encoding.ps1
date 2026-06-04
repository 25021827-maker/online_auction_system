$file = "c:\Users\ADMIN\Downloads\CLONEEEEEE_FIXED\CLONEEEEEE_FIXED\backend\src\main\java\network\ClientHandler.java"
$bytes = [System.IO.File]::ReadAllBytes($file)
$content = [System.Text.Encoding]::UTF8.GetString($bytes)

# The file has double-encoded UTF-8: UTF-8 bytes were read as Windows-1252 (cp1252) then saved as UTF-8 again.
# Fix: convert back: read each "garbled" char as cp1252 byte, then decode those bytes as UTF-8.

$lines = $content -split "`n"

function Fix-DoubleEncoding($text) {
    # Find runs of high-byte characters and try to decode them
    $cp1252 = [System.Text.Encoding]::GetEncoding(1252)
    $utf8 = [System.Text.Encoding]::UTF8
    
    # Convert each char back to its cp1252 byte, then decode as UTF-8
    $charArray = $text.ToCharArray()
    $byteList = New-Object System.Collections.Generic.List[byte]
    
    foreach ($c in $charArray) {
        $code = [int]$c
        if ($code -lt 256) {
            $byteList.Add([byte]$code)
        } else {
            # Characters above 255 cannot be cp1252 encoded, write as UTF-8 bytes
            $charBytes = $utf8.GetBytes([string]$c)
            foreach ($b in $charBytes) {
                $byteList.Add($b)
            }
        }
    }
    
    return $utf8.GetString($byteList.ToArray())
}

# Only fix lines that contain garbled text (non-ASCII that looks like double-encoded)
$fixedCount = 0
for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    # Check if this line contains the garbled pattern (chars in the C0-FF range that form UTF-8 sequences)
    $hasGarbled = $false
    foreach ($c in $line.ToCharArray()) {
        $code = [int]$c
        # These are typical double-encoding markers: C3, C4, C6, etc. followed by bytes in 80-BF range
        if ($code -ge 0xC0 -and $code -le 0xFF) {
            $hasGarbled = $true
            break
        }
    }
    
    if ($hasGarbled) {
        $fixed = Fix-DoubleEncoding $line
        if ($fixed -ne $line) {
            $lines[$i] = $fixed
            $fixedCount++
            Write-Output "Fixed line $($i+1): $($fixed.Trim())"
        }
    }
}

$newContent = $lines -join "`n"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($file, $newContent, $utf8NoBom)
Write-Output ""
Write-Output "Done! Fixed $fixedCount lines in ClientHandler.java"
