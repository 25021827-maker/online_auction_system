$file = "c:\Users\ADMIN\Downloads\CLONEEEEEE_FIXED\CLONEEEEEE_FIXED\backend\src\main\java\network\ClientHandler.java"
$bytes = [System.IO.File]::ReadAllBytes($file)
$content = [System.Text.Encoding]::UTF8.GetString($bytes)

# Build replacement map: garbled -> clean ASCII Vietnamese
$map = @{}

# Login messages
$map[[char]0x0110 + [char]0x0103 + "ng nh" + [char]0x1EAD + "p th" + [char]0x00E0 + "nh c" + [char]0x00F4 + "ng"] = "Dang nhap thanh cong"
$map["Sai t" + [char]0x00EA + "n " + [char]0x0111 + [char]0x0103 + "ng nh" + [char]0x1EAD + "p ho" + [char]0x1EB7 + "c m" + [char]0x1EAD + "t kh" + [char]0x1EA9 + "u"] = "Sai ten dang nhap hoac mat khau"

# Register messages  
$map[[char]0x0110 + [char]0x0103 + "ng k" + [char]0x00FD + " th" + [char]0x00E0 + "nh c" + [char]0x00F4 + "ng!"] = "Dang ky thanh cong!"
$map[[char]0x0110 + [char]0x0103 + "ng k" + [char]0x00FD + " th" + [char]0x1EA5 + "t b" + [char]0x1EA1 + "i. T" + [char]0x00EA + "n " + [char]0x0111 + [char]0x0103 + "ng nh" + [char]0x1EAD + "p/email c" + [char]0x00F3 + " th" + [char]0x1EC3 + " " + [char]0x0111 + [char]0x00E3 + " t" + [char]0x1ED3 + "n t" + [char]0x1EA1 + "i."] = "Dang ky that bai. Ten dang nhap/email co the da ton tai."

# Bid messages
$map["Gi" + [char]0x00E1 + " " + [char]0x0111 + [char]0x1EB7 + "t kh" + [char]0x00F4 + "ng h" + [char]0x1EE3 + "p l" + [char]0x1EC7 + " ho" + [char]0x1EB7 + "c phi" + [char]0x00EA + "n " + [char]0x0111 + [char]0x00E3 + " " + [char]0x0111 + [char]0x00F3 + "ng."] = "Gia dat khong hop le hoac phien da dong."
$map[[char]0x0110 + [char]0x1EB7 + "t gi" + [char]0x00E1 + " th" + [char]0x00E0 + "nh c" + [char]0x00F4 + "ng!"] = "Dat gia thanh cong!"

# Thanh cong (appears multiple times)
$map["Th" + [char]0x00E0 + "nh c" + [char]0x00F4 + "ng"] = "Thanh cong"

# Create auction
$map["T" + [char]0x1EA1 + "o phi" + [char]0x00EA + "n " + [char]0x0111 + [char]0x1EA5 + "u gi" + [char]0x00E1 + " th" + [char]0x00E0 + "nh c" + [char]0x00F4 + "ng"] = "Tao phien dau gia thanh cong"
$map["C" + [char]0x00F3 + " s" + [char]0x1EA3 + "n ph" + [char]0x1EA9 + "m m" + [char]0x1EDB + "i"] = "Co san pham moi"
$map["Kh" + [char]0x00F4 + "ng th" + [char]0x1EC3 + " t" + [char]0x1EA1 + "o phi" + [char]0x00EA + "n " + [char]0x0111 + [char]0x1EA5 + "u gi" + [char]0x00E1 + "."] = "Khong the tao phien dau gia."
$map["D" + [char]0x1EEF + " li" + [char]0x1EC7 + "u t" + [char]0x1EA1 + "o phi" + [char]0x00EA + "n kh" + [char]0x00F4 + "ng h" + [char]0x1EE3 + "p l" + [char]0x1EC7 + ": "] = "Du lieu tao phien khong hop le: "

# Update auction
$map["C" + [char]0x1EAD + "p nh" + [char]0x1EAD + "t th" + [char]0x00E0 + "nh c" + [char]0x00F4 + "ng!"] = "Cap nhat thanh cong!"
$map["S" + [char]0x1EA3 + "n ph" + [char]0x1EA9 + "m " + [char]0x0111 + [char]0x01B0 + [char]0x1EE3 + "c c" + [char]0x1EAD + "p nh" + [char]0x1EAD + "t"] = "San pham duoc cap nhat"
$map["Kh" + [char]0x00F4 + "ng th" + [char]0x1EC3 + " c" + [char]0x1EAD + "p nh" + [char]0x1EAD + "t phi" + [char]0x00EA + "n " + [char]0x0111 + [char]0x1EA5 + "u gi" + [char]0x00E1 + "."] = "Khong the cap nhat phien dau gia."
$map["D" + [char]0x1EEF + " li" + [char]0x1EC7 + "u c" + [char]0x1EAD + "p nh" + [char]0x1EAD + "t kh" + [char]0x00F4 + "ng h" + [char]0x1EE3 + "p l" + [char]0x1EC7 + ": "] = "Du lieu cap nhat khong hop le: "

# Delete
$map[[char]0x0110 + [char]0x00E3 + " x" + [char]0x00F3 + "a"] = "Da xoa"
$map["S" + [char]0x1EA3 + "n ph" + [char]0x1EA9 + "m b" + [char]0x1ECB + " x" + [char]0x00F3 + "a"] = "San pham bi xoa"
$map["Kh" + [char]0x00F4 + "ng th" + [char]0x1EC3 + " x" + [char]0x00F3 + "a s" + [char]0x1EA3 + "n ph" + [char]0x1EA9 + "m."] = "Khong the xoa san pham."

# Error messages
$map["L" + [char]0x1ED7 + "i Server: "] = "Loi Server: "

# Client disconnect (already fixed but just in case)
$map["Client ng" + [char]0x1EAF + "t k" + [char]0x1EBF + "t n" + [char]0x1ED1 + "i: "] = "Client disconnected: "

$count = 0
foreach ($key in $map.Keys) {
    if ($content.Contains($key)) {
        $content = $content.Replace($key, $map[$key])
        $count++
        Write-Output "Replaced: $($map[$key])"
    }
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($file, $content, $utf8NoBom)
Write-Output "Done! Fixed $count replacements in ClientHandler.java"
