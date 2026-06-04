$file = "c:\Users\ADMIN\Downloads\CLONEEEEEE_FIXED\CLONEEEEEE_FIXED\backend\src\main\java\network\ClientHandler.java"
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

# Line 61
$content = $content.Replace("Client ng" + [char]0x1EAF + "t k" + [char]0x1EBF + "t n" + [char]0x1ED1 + "i: ", "Client ng" + [char]0x1EAF + "t k" + [char]0x1EBF + "t n" + [char]0x1ED1 + "i: ")

# Instead of trying to match garbled chars, let's find and replace by surrounding context
# Read line by line and fix
$lines = $content -split "`n"

for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    $lineNum = $i + 1
    
    # Line 61: Client disconnect message
    if ($lineNum -eq 61) {
        $lines[$i] = '            System.out.println("Client ng' + [char]0x1EAF + 't k' + [char]0x1EBF + 't n' + [char]0x1ED1 + 'i: " + e.getMessage());'
    }
    # Line 127: Invalid action
    if ($lineNum -eq 127) {
        $lines[$i] = '                default -> sendResponse(ResponsePayload.fail(request.getAction() + "_RESPONSE", "H' + [char]0x00E0 + 'nh ' + [char]0x0111 + [char]0x1ED9 + 'ng kh' + [char]0x00F4 + 'ng h' + [char]0x1EE3 + 'p l' + [char]0x1EC7 + ': " + request.getAction()));'
    }
    # Line 130: Server error
    if ($lineNum -eq 130) {
        $lines[$i] = '            sendResponse(ResponsePayload.fail(request.getAction() + "_RESPONSE", "L' + [char]0x1ED7 + 'i Server: " + e.getMessage()));'
    }
    # Line 224-225: Login
    if ($lineNum -eq 224) {
        $lines[$i] = '                ? ResponsePayload.success("LOGIN_RESPONSE", "' + [char]0x0110 + [char]0x0103 + 'ng nh' + [char]0x1EAD + 'p th' + [char]0x00E0 + 'nh c' + [char]0x00F4 + 'ng", loggedInUser)'
    }
    if ($lineNum -eq 225) {
        $lines[$i] = '                : ResponsePayload.fail("LOGIN_RESPONSE", "Sai t' + [char]0x00EA + 'n ' + [char]0x0111 + [char]0x0103 + 'ng nh' + [char]0x1EAD + 'p ho' + [char]0x1EB7 + 'c m' + [char]0x1EAD + 't kh' + [char]0x1EA9 + 'u"));'
    }
    # Line 232-233: Register
    if ($lineNum -eq 232) {
        $lines[$i] = '                ? ResponsePayload.success("REGISTER_RESPONSE", "' + [char]0x0110 + [char]0x0103 + 'ng k' + [char]0x00FD + ' th' + [char]0x00E0 + 'nh c' + [char]0x00F4 + 'ng!", null)'
    }
    if ($lineNum -eq 233) {
        $lines[$i] = '                : ResponsePayload.fail("REGISTER_RESPONSE", "' + [char]0x0110 + [char]0x0103 + 'ng k' + [char]0x00FD + ' th' + [char]0x1EA5 + 't b' + [char]0x1EA1 + 'i. T' + [char]0x00EA + 'n ' + [char]0x0111 + [char]0x0103 + 'ng nh' + [char]0x1EAD + 'p/email c' + [char]0x00F3 + ' th' + [char]0x1EC3 + ' ' + [char]0x0111 + [char]0x00E3 + ' t' + [char]0x1ED3 + 'n t' + [char]0x1EA1 + 'i."));'
    }
    # Line 246: Invalid bid
    if ($lineNum -eq 246) {
        $lines[$i] = '                sendResponse(ResponsePayload.fail("PLACE_BID_RESPONSE", "Gi' + [char]0x00E1 + ' ' + [char]0x0111 + [char]0x1EB7 + 't kh' + [char]0x00F4 + 'ng h' + [char]0x1EE3 + 'p l' + [char]0x1EC7 + ' ho' + [char]0x1EB7 + 'c phi' + [char]0x00EA + 'n ' + [char]0x0111 + [char]0x00E3 + ' ' + [char]0x0111 + [char]0x00F3 + 'ng."));'
    }
    # Line 254: Bid success
    if ($lineNum -eq 254) {
        $lines[$i] = '            sendResponse(ResponsePayload.success("PLACE_BID_RESPONSE", "' + [char]0x0110 + [char]0x1EB7 + 't gi' + [char]0x00E1 + ' th' + [char]0x00E0 + 'nh c' + [char]0x00F4 + 'ng!", null));'
    }
    # Line 285: Get active auctions success
    if ($lineNum -eq 285) {
        $lines[$i] = '        sendResponse(ResponsePayload.success("GET_ACTIVE_AUCTIONS_RESPONSE", "Th' + [char]0x00E0 + 'nh c' + [char]0x00F4 + 'ng", toAuctionDTOs(activeAuctions)));'
    }
    # Line 301: Create auction success
    if ($lineNum -eq 301) {
        $lines[$i] = '                sendResponse(ResponsePayload.success("CREATE_AUCTION_RESPONSE", "T' + [char]0x1EA1 + 'o phi' + [char]0x00EA + 'n ' + [char]0x0111 + [char]0x1EA5 + 'u gi' + [char]0x00E1 + ' th' + [char]0x00E0 + 'nh c' + [char]0x00F4 + 'ng", null));'
    }
    # Line 302: New product event
    if ($lineNum -eq 302) {
        $lines[$i] = '                ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "C' + [char]0x00F3 + ' s' + [char]0x1EA3 + 'n ph' + [char]0x1EA9 + 'm m' + [char]0x1EDB + 'i", null));'
    }
    # Line 304: Cannot create auction
    if ($lineNum -eq 304) {
        $lines[$i] = '                sendResponse(ResponsePayload.fail("CREATE_AUCTION_RESPONSE", "Kh' + [char]0x00F4 + 'ng th' + [char]0x1EC3 + ' t' + [char]0x1EA1 + 'o phi' + [char]0x00EA + 'n ' + [char]0x0111 + [char]0x1EA5 + 'u gi' + [char]0x00E1 + '."));'
    }
    # Line 307: Invalid create data
    if ($lineNum -eq 307) {
        $lines[$i] = '            sendResponse(ResponsePayload.fail("CREATE_AUCTION_RESPONSE", "D' + [char]0x1EEF + ' li' + [char]0x1EC7 + 'u t' + [char]0x1EA1 + 'o phi' + [char]0x00EA + 'n kh' + [char]0x00F4 + 'ng h' + [char]0x1EE3 + 'p l' + [char]0x1EC7 + ': " + e.getMessage()));'
    }
    # Line 325: Update success
    if ($lineNum -eq 325) {
        $lines[$i] = '                sendResponse(ResponsePayload.success("UPDATE_AUCTION_RESPONSE", "C' + [char]0x1EAD + 'p nh' + [char]0x1EAD + 't th' + [char]0x00E0 + 'nh c' + [char]0x00F4 + 'ng!", null));'
    }
    # Line 326: Product updated event
    if ($lineNum -eq 326) {
        $lines[$i] = '                ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "S' + [char]0x1EA3 + 'n ph' + [char]0x1EA9 + 'm ' + [char]0x0111 + [char]0x01B0 + [char]0x1EE3 + 'c c' + [char]0x1EAD + 'p nh' + [char]0x1EAD + 't", req.auctionId));'
    }
    # Line 328: Cannot update
    if ($lineNum -eq 328) {
        $lines[$i] = '                sendResponse(ResponsePayload.fail("UPDATE_AUCTION_RESPONSE", "Kh' + [char]0x00F4 + 'ng th' + [char]0x1EC3 + ' c' + [char]0x1EAD + 'p nh' + [char]0x1EAD + 't phi' + [char]0x00EA + 'n ' + [char]0x0111 + [char]0x1EA5 + 'u gi' + [char]0x00E1 + '."));'
    }
    # Line 331: Invalid update data
    if ($lineNum -eq 331) {
        $lines[$i] = '            sendResponse(ResponsePayload.fail("UPDATE_AUCTION_RESPONSE", "D' + [char]0x1EEF + ' li' + [char]0x1EC7 + 'u c' + [char]0x1EAD + 'p nh' + [char]0x1EAD + 't kh' + [char]0x00F4 + 'ng h' + [char]0x1EE3 + 'p l' + [char]0x1EC7 + ': " + e.getMessage()));'
    }
    # Line 357: My products success
    if ($lineNum -eq 357) {
        $lines[$i] = '        sendResponse(ResponsePayload.success("GET_MY_PRODUCTS_RESPONSE", "Th' + [char]0x00E0 + 'nh c' + [char]0x00F4 + 'ng", toAuctionDTOs(new AuctionDAO().getAuctionsBySeller(sellerId))));'
    }
    # Line 368: Deleted
    if ($lineNum -eq 368) {
        $lines[$i] = '            sendResponse(ResponsePayload.success("DELETE_PRODUCT_RESPONSE", "' + [char]0x0110 + [char]0x00E3 + ' x' + [char]0x00F3 + 'a", null));'
    }
    # Line 369: Product deleted event
    if ($lineNum -eq 369) {
        $lines[$i] = '            ServerMain.broadcast(ResponsePayload.success("NEW_AUCTION_EVENT", "S' + [char]0x1EA3 + 'n ph' + [char]0x1EA9 + 'm b' + [char]0x1ECB + ' x' + [char]0x00F3 + 'a", null));'
    }
    # Line 371: Cannot delete
    if ($lineNum -eq 371) {
        $lines[$i] = '            sendResponse(ResponsePayload.fail("DELETE_PRODUCT_RESPONSE", "Kh' + [char]0x00F4 + 'ng th' + [char]0x1EC3 + ' x' + [char]0x00F3 + 'a s' + [char]0x1EA3 + 'n ph' + [char]0x1EA9 + 'm."));'
    }
    # Line 381: Watchlist success
    if ($lineNum -eq 381) {
        $lines[$i] = '        sendResponse(ResponsePayload.success("GET_WATCHLIST_RESPONSE", "Th' + [char]0x00E0 + 'nh c' + [char]0x00F4 + 'ng", toAuctionDTOs(new AuctionDAO().getWatchlist(userId))));'
    }
    # Line 513: Bid history success
    if ($lineNum -eq 513) {
        $lines[$i] = '        sendResponse(ResponsePayload.success("GET_BID_HISTORY_RESPONSE", "Th' + [char]0x00E0 + 'nh c' + [char]0x00F4 + 'ng", history));'
    }
}

$newContent = $lines -join "`n"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($file, $newContent, $utf8NoBom)
Write-Output "Done! Fixed all Vietnamese strings in ClientHandler.java"
