# Online Auction System - Hệ thống đấu giá trực tuyến Desktop

Online Auction System là ứng dụng đấu giá trực tuyến dạng Desktop Application được xây dựng bằng Java, JavaFX và mô hình Client-Server. Hệ thống mô phỏng đầy đủ quy trình đấu giá điện tử: người bán đăng sản phẩm, quản trị viên duyệt sản phẩm, người mua tham gia đặt giá theo thời gian thực, hệ thống tự động kết thúc phiên và công bố kết quả.

Dự án được phát triển phục vụ mục tiêu học thuật, thực hành lập trình hướng đối tượng, kiến trúc Client-Server, xử lý đồng thời, kết nối cơ sở dữ liệu, xây dựng giao diện JavaFX và tổ chức mã nguồn theo hướng dễ mở rộng.

---

## Mục lục

- [1. Mục tiêu dự án](#1-mục-tiêu-dự-án)
- [2. Phạm vi hệ thống](#2-phạm-vi-hệ-thống)
- [3. Công nghệ sử dụng](#3-công-nghệ-sử-dụng)
- [4. Kiến trúc tổng quan](#4-kiến-trúc-tổng-quan)
- [5. Cấu trúc thư mục](#5-cấu-trúc-thư-mục)
- [6. Chức năng chính](#6-chức-năng-chính)
- [7. Thiết kế OOP và Design Pattern](#7-thiết-kế-oop-và-design-pattern)
- [8. Cơ chế đấu giá và xử lý đồng thời](#8-cơ-chế-đấu-giá-và-xử-lý-đồng-thời)
- [9. Realtime và đồng bộ Client](#9-realtime-và-đồng-bộ-client)
- [10. Cài đặt và cấu hình](#10-cài-đặt-và-cấu-hình)
- [11. Hướng dẫn chạy chương trình](#11-hướng-dẫn-chạy-chương-trình)
- [12. Tài khoản mặc định](#12-tài-khoản-mặc-định)
- [13. Kiểm thử và CI/CD](#13-kiểm-thử-và-cicd)
- [14. Hình ảnh minh họa](#14-hình-ảnh-minh-họa)
- [15. Thành viên nhóm](#15-thành-viên-nhóm)

---

## 1. Mục tiêu dự án

Mục tiêu của Online Auction System là xây dựng một hệ thống đấu giá trực tuyến hoạt động theo mô hình Client-Server trên nền Desktop. Ứng dụng cho phép nhiều người dùng cùng kết nối đến một Server, cùng xem phiên đấu giá và đặt giá trong thời gian thực.

Các mục tiêu chính:

- Xây dựng ứng dụng Java Desktop có giao diện trực quan bằng JavaFX.
- Tách rõ Client và Server, đảm bảo toàn bộ logic nghiệp vụ quan trọng nằm ở phía Server.
- Áp dụng lập trình hướng đối tượng với các nguyên lý Encapsulation, Inheritance, Polymorphism và Abstraction.
- Tổ chức code theo mô hình Controller - Service - DAO - Model.
- Xử lý nhiều Client đặt giá đồng thời mà không gây sai lệch dữ liệu.
- Cập nhật giá đấu, lịch sử bid, trạng thái phiên và số dư người dùng theo thời gian thực.
- Lưu trữ dữ liệu bền vững bằng MySQL thông qua HikariCP connection pool.
- Bổ sung các chức năng nâng cao như Auto-Bidding, Anti-Sniping, Watchlist, Win List, Sold List, thông báo và lịch sử ví.

---

## 2. Phạm vi hệ thống

Ứng dụng hướng đến mô phỏng hệ thống đấu giá điện tử trong môi trường học thuật.

### Đối tượng sử dụng

- **Admin**: quản lý người dùng, sản phẩm, phiên đấu giá, yêu cầu nạp tiền và kết quả đấu giá.
- **Seller**: đăng sản phẩm, theo dõi sản phẩm đã đăng, xem danh sách sản phẩm đã bán.
- **Bidder**: xem sản phẩm đang đấu giá, đặt bid, dùng auto-bid, theo dõi watchlist, xem danh sách phiên đã thắng.

### Phạm vi triển khai

- Ứng dụng chạy trong môi trường localhost hoặc mạng LAN.
- Server giao tiếp với Client thông qua TCP Socket.
- Database chạy bằng MySQL.
- Hình ảnh sản phẩm được xử lý thông qua đường dẫn hoặc dữ liệu ảnh được đồng bộ từ Server.
- Chưa tích hợp thanh toán thật; nghiệp vụ nạp tiền, trừ tiền, cộng tiền cho Seller được mô phỏng trong hệ thống.

---

## 3. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17 |
| Giao diện Client | JavaFX 21.0.6, FXML, CSS |
| Giao tiếp mạng | TCP Socket |
| Định dạng dữ liệu truyền | JSON |
| JSON Parser | Gson |
| Cơ sở dữ liệu | MySQL |
| Connection Pool | HikariCP |
| Mã hóa mật khẩu | BCrypt |
| Build Tool | Maven |
| Kiểm thử | JUnit 5, Mockito |
| CI/CD | GitHub Actions |
| Quản lý mã nguồn | Git/GitHub |


---

## 4. Kiến trúc tổng quan

Hệ thống được thiết kế theo mô hình Client-Server.

```text
+----------------------+          TCP Socket / JSON          +----------------------+
|      JavaFX Client   |  <------------------------------->  |      Java Server     |
|----------------------|                                     |----------------------|
| FXML Views           |                                     | ClientHandler        |
| Controllers          |                                     | AuctionService       |
| ProductCard UI       |                                     | AutoBidService       |
| SocketClient         |                                     | SettlementService    |
| Session              |                                     | DAO Layer            |
+----------------------+                                     +----------+-----------+
                                                                      |
                                                                      | JDBC / HikariCP
                                                                      v
                                                            +----------------------+
                                                            |       MySQL DB        |
                                                            | users, items, bids,  |
                                                            | auctions, watchlist, |
                                                            | winners, wallets    |
                                                            +----------------------+
```

### Nguyên tắc thiết kế

- Client chỉ hiển thị giao diện và gửi request.
- Server chịu trách nhiệm kiểm tra nghiệp vụ, xử lý bid, cập nhật database và broadcast sự kiện.
- Client không truy cập trực tiếp database.
- Mọi hành động quan trọng như đặt bid, duyệt sản phẩm, kết thúc phiên, cập nhật số dư đều đi qua Server.
- Server quản lý nhiều Client bằng `ClientHandler` chạy trên các luồng riêng.

---

## 5. Cấu trúc thư mục

```text
Online Auction System/
├── backend/
│   ├── src/main/java/
│   │   ├── dao/                 # Truy xuất cơ sở dữ liệu
│   │   ├── dto/                 # Request/Response DTO
│   │   ├── exception/           # Exception nghiệp vụ đấu giá
│   │   ├── model/               # Entity, User, Bidder, Seller, Item, Auction...
│   │   ├── network/             # ServerMain, ClientHandler, AuctionRoomManager
│   │   ├── service/             # AuctionService, AutoBidService, SettlementService...
│   │   └── util/                # Tiện ích thời gian, định dạng...
│   ├── src/test/java/
│   │   ├──dao
│   │   ├──network
│   │   ├──resources
│   │   ├──service
│   └── pom.xml
│
├── frontend/
│   ├── src/main/java/
│   │   ├── Controller/          # JavaFX Controllers
│   │   ├── Model/               # Model phía Client
│   │   ├── Service/             # Service hỗ trợ UI và điều hướng
│   │   ├── Session/             # Lưu phiên đăng nhập hiện tại
│   │   ├── dto/                 # DTO dùng để giao tiếp Server
│   │   ├── network/             # SocketClient
│   │   ├── ui/                  # Component UI như ProductCard
│   │   └── util/                # ImageUtil, NotificationToast...
│   ├── src/test/java/
│   │   ├──Controller
│   │   ├──Model
│   │   ├──network
│   │   ├──Service
│   └── pom.xml
│
├── database/
│   └── schema/                  # Script SQL tạo bảng
│       ├── 01_create_database.sql
│       ├── 02_users.sql
│       ├── 03_items.sql
│       ├── 04_auctions.sql
│       ├── 05_bids.sql
│       ├── 06_auto_bids.sql
│       ├── 07_auction_extensions.sql
│       ├── 08_notifications.sql
│       ├── 10_watchlist.sql
│       ├── 11_deposit_requests.sql
│       └── 12_auction_winners.sql
│
├── .github/workflows/           # GitHub Actions CI/CD
└── README.md
```

---

## 6. Chức năng chính

### 6.1. Quản lý người dùng

- Đăng ký tài khoản.
- Đăng nhập tài khoản.
- Phân quyền người dùng theo vai trò: Admin, Seller, Bidder.
- Mã hóa mật khẩu bằng BCrypt.
- Quản lý trạng thái hoạt động của tài khoản.
- Admin có thể khóa hoặc mở khóa người dùng.

### 6.2. Quản lý sản phẩm

- Seller đăng sản phẩm mới.
- Sản phẩm có các thông tin: tên, mô tả, danh mục, tình trạng, giá khởi điểm, ảnh và bước giá tối thiểu.
- Admin duyệt sản phẩm trước khi xuất hiện trên hệ thống.
- Seller theo dõi danh sách sản phẩm của mình.
- Bidder xem danh sách sản phẩm đang được đấu giá.

### 6.3. Quản lý phiên đấu giá

- Admin chấp nhận/từ chối phiên đấu giá hợp lệ/không hợp lệ.
- Lưu thời gian bắt đầu, thời gian kết thúc, giá hiện tại và người đang dẫn đầu.
- Tự động cập nhật trạng thái phiên: `OPEN`, `RUNNING`, `FINISHED`, `SCHEDULED`.
- Hỗ trợ kết thúc phiên tự động bằng scheduler phía Server.

### 6.4. Đặt giá đấu

- Bidder nhập mức giá muốn đặt.
- Server kiểm tra:
    - phiên đấu giá có tồn tại không;
    - phiên còn thời gian không;
    - giá đặt có lớn hơn giá hiện tại theo bước giá tối thiểu không;
    - người dùng có đủ số dư khả dụng không;
    - người bán không được tự bid sản phẩm của mình.
- Nếu hợp lệ, Server lưu bid vào database, cập nhật người dẫn đầu và broadcast giá mới tới các Client.

### 6.5. Ví người dùng và lịch sử giao dịch

- Mỗi người dùng có `balance` và `availableBalance`.
- Khi Bidder đặt bid, hệ thống giữ lại số dư khả dụng tương ứng.
- Khi phiên kết thúc:
    - người thắng bị trừ tiền thật theo giá thắng;
    - Seller được cộng tiền;
    - người không thắng được giải phóng số dư khả dụng;
    - lịch sử giao dịch ví được ghi nhận minh bạch.

### 6.6. Watchlist

- Bidder và Seller có thể thêm sản phẩm vào danh sách theo dõi.
- Watchlist hiển thị nhiều sản phẩm đã theo dõi.
- Khi có bid mới hoặc trạng thái phiên thay đổi, sản phẩm trong Watchlist được cập nhật realtime.

### 6.7. Win List và Sold List

- Bidder có thể xem danh sách phiên mình đã thắng.
- Seller có thể xem danh sách sản phẩm đã bán.
- Admin có thể xem danh sách người thắng để kiểm soát kết quả phiên đấu giá.

### 6.8. Thông báo

- Hệ thống gửi thông báo khi:
    - người dùng thắng phiên;
    - Seller bán được sản phẩm;
    - phiên kết thúc không có người thắng;

---

## 7. Thiết kế OOP và Design Pattern

### 7.1. Encapsulation - Đóng gói

Các lớp model sử dụng thuộc tính private/protected và truy cập thông qua getter/setter. Điều này giúp bảo vệ dữ liệu nội bộ và kiểm soát thay đổi trạng thái đối tượng.

Ví dụ:

- `User`
- `Auction`
- `Item`
- `BidTransaction`
- `WalletTransaction`

### 7.2. Inheritance - Kế thừa

Hệ thống có cây kế thừa rõ ràng:

```text
User
├── Admin
├── Seller
└── Bidder
```

```text
Item
├── Electronics
├── Art
├── Vehicle
└── OtherItem
```

Cách thiết kế này giúp tái sử dụng các thuộc tính chung, đồng thời cho phép mở rộng hành vi riêng cho từng loại người dùng hoặc từng loại sản phẩm.

### 7.3. Polymorphism - Đa hình

Các đối tượng con có thể được xử lý thông qua kiểu cha như `User` hoặc `Item`. Khi cần mở rộng, hệ thống có thể ghi đè phương thức xử lý hoặc hiển thị thông tin theo từng loại đối tượng cụ thể.

Ví dụ:

- xử lý `Bidder`, `Seller`, `Admin` như các biến thể của `User`;
- xử lý `Electronics`, `Art`, `Vehicle`, `OtherItem` như các biến thể của `Item`.

### 7.4. Abstraction - Trừu tượng

Hệ thống sử dụng abstract class và interface để tách phần khái quát khỏi phần triển khai cụ thể.

Ví dụ:

- `User` là abstract class cho các loại người dùng.
- `Item` là abstract class cho các loại sản phẩm.
- `Entity` là interface nền tảng cho các thực thể.

### 7.5. Design Pattern áp dụng

| Pattern | Vị trí áp dụng | Vai trò |
|---|---|---|
| Singleton | `DatabaseConnection`, `SocketClient`, `AuctionManager` | Quản lý tài nguyên dùng chung, tránh tạo nhiều instance không cần thiết |
| DAO | `UserDAO`, `AuctionDAO`, `AdminDAO`, `WalletTransactionDAO` | Tách logic truy xuất database khỏi service |
| Factory Method | `ItemFactory` | Tạo đúng loại sản phẩm dựa trên dữ liệu đọc từ database |
| Observer/Event | `ServerMain.broadcast`, `AuctionRoomManager`, listener phía Client | Đồng bộ realtime giữa Server và nhiều Client |
| MVC | JavaFX FXML + Controller + Model | Tách giao diện, xử lý sự kiện và dữ liệu phía Client |

---

## 8. Cơ chế đấu giá và xử lý đồng thời

### 8.1. Bài toán đồng thời

Trong hệ thống đấu giá, nhiều người dùng có thể đặt bid cùng lúc vào cùng một phiên. Nếu không xử lý đồng thời đúng cách, hệ thống có thể gặp các lỗi:

- lost update;
- rollback giá;
- hai người cùng trở thành người dẫn đầu;
- ghi lịch sử bid sai thứ tự;
- số dư khả dụng bị tính sai.

### 8.2. Hướng xử lý

Server xử lý bid tập trung trong `AuctionService`. Các thao tác quan trọng được bảo vệ bằng cơ chế khóa và transaction database.

Luồng xử lý bid tổng quát:

```text
Client gửi PLACE_BID
        |
        v
ClientHandler nhận request
        |
        v
AuctionService kiểm tra nghiệp vụ
        |
        v
AuctionDAO thao tác database trong transaction
        |
        v
Cập nhật giá hiện tại, người dẫn đầu, lịch sử bid
        |
        v
Server broadcast NEW_BID_EVENT cho các Client
```

### 8.3. Kiểm tra nghiệp vụ khi bid

Server kiểm tra các điều kiện sau trước khi chấp nhận bid:

- Phiên đấu giá phải tồn tại.
- Phiên chưa kết thúc.
- Người bid không phải Seller của chính sản phẩm đó.
- Giá đặt phải lớn hơn giá hiện tại ít nhất bằng `min_bid_step`(mặc định là 10$).
- Người bid phải có đủ `available_balance`.
- Dữ liệu bid phải hợp lệ và không âm.

### 8.4. Auto-Bidding

Auto-Bidding cho phép người dùng đặt trước mức giá tối đa. Khi có người khác đặt giá, hệ thống tự động tăng giá thay cho người dùng nhưng không vượt quá `maxBid`.

Đặc điểm:

- Tuân thủ bước giá tối thiểu.
- Không vượt quá số dư khả dụng và mức max bid đã cấu hình.
- Hỗ trợ xử lý khi nhiều người cùng bật auto-bid.
- Ưu tiên người đặt auto-bid trước trong trường hợp cùng mức max bid.
- Thứ tự xử lý bắt đầu với `maxBid`, người có `maxBid` lớn hơn sẽ tự động đặt `maxBid` và trở thành người dẫn đầu.

### 8.5. Anti-Sniping

Anti-Sniping giúp tránh tình trạng người dùng chờ sát giờ kết thúc mới đặt bid để người khác không kịp phản ứng.

Cơ chế:

- Nếu có bid hợp lệ trong khoảng thời gian cuối phiên (30 giây trước khi end), Server tự động gia hạn thời gian kết thúc(gia hạn thêm 30 giây, tối đa 10 lần).
- Thông tin gia hạn được lưu vào bảng `auction_extensions`.
- Server gửi sự kiện cập nhật thời gian mới cho các Client đang xem phiên.

### 8.6. Settlement - Kết thúc phiên

Server có scheduler chạy định kỳ để kiểm tra các phiên đã hết hạn.

Khi phiên kết thúc:

- Nếu có người thắng:
    - lưu kết quả vào bảng `auction_winners`;
    - trừ tiền người thắng;
    - cộng tiền cho Seller;
    - ghi lịch sử ví;
    - gửi thông báo cho người thắng và Seller.
- Nếu không có người thắng:
    - thông báo cho Seller;
    - không phát sinh giao dịch thanh toán.

---

## 9. Realtime và đồng bộ Client

Hệ thống dùng TCP Socket để truyền gói tin JSON giữa Client và Server.

### 9.1. Request/Response

Client gửi request theo cấu trúc DTO, Server xử lý và trả về response tương ứng.

Ví dụ các action chính:

- `LOGIN`
- `REGISTER`
- `GET_ACTIVE_AUCTIONS`
- `CREATE_AUCTION`
- `PLACE_BID`
- `GET_BID_HISTORY`
- `ADD_TO_WATCHLIST`
- `GET_WATCHLIST`
- `CREATE_DEPOSIT_REQUEST`
- `GET_WALLET_HISTORY`

### 9.2. Event realtime

Server broadcast sự kiện cho Client khi dữ liệu thay đổi.

Một số event quan trọng:

- `NEW_BID_EVENT`: có bid mới.
- `NEW_AUCTION_EVENT`: có phiên mới hoặc phiên được cập nhật.
- `AUCTION_TIME_EXTENDED`: phiên được gia hạn do anti-sniping.
- `AUCTION_SETTLED_EVENT`: phiên đã kết thúc và được xử lý kết quả.
- `BALANCE_UPDATE_EVENT`: số dư người dùng thay đổi.
- `NOTIFICATION_EVENT`: có thông báo mới.
- `USER_AUCTION_RESULTS_CHANGED`: win list hoặc sold list thay đổi.

---

## 10. Cài đặt và cấu hình

### 10.1. Yêu cầu môi trường

Cần cài đặt:

- JDK 17 trở lên.
- Apache Maven 3.x trở lên.
- MySQL Server.
- Git.
- IntelliJ IDEA hoặc IDE hỗ trợ Maven/JavaFX.

### 10.2. Clone mã nguồn

```bash
git clone https://github.com/25021827-maker/online_auction_system.git
cd online_auction_system
```

Nếu project nằm trong thư mục con sau khi giải nén, hãy mở đúng thư mục chứa `backend/`, `frontend/`, `database/` và `.github/`.

### 10.3. Tạo database

Mở MySQL Workbench hoặc terminal MySQL và chạy lần lượt các script trong thư mục:

```text
database/schema/
```

Thứ tự khuyến nghị:

```text
01_create_database.sql
02_users.sql
03_items.sql
04_auctions.sql
05_bids.sql
06_auto_bids.sql
07_auction_extensions.sql
08_notifications.sql
10_watchlist.sql
11_deposit_requests.sql
12_auction_winners.sql
```

Database mặc định:

```text
auction_db
```

### 10.4. Cấu hình kết nối database

Backend đọc cấu hình database từ biến môi trường. Nếu không cấu hình, hệ thống dùng giá trị mặc định:

```text
AUCTION_DB_URL=jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
AUCTION_DB_USER=root
AUCTION_DB_PASSWORD=1234
```

Có thể cấu hình lại theo máy của bạn.

#### Windows PowerShell

```powershell
$env:AUCTION_DB_URL="jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true"
$env:AUCTION_DB_USER="root"
$env:AUCTION_DB_PASSWORD="your_password"
```

#### macOS/Linux

```bash
export AUCTION_DB_URL="jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true"
export AUCTION_DB_USER="root"
export AUCTION_DB_PASSWORD="your_password"
```

---

## 11. Hướng dẫn chạy chương trình

### 11.1. Build Backend

Tại thư mục gốc project:

```bash
mvn -f main_system/backend/pom.xml clean package
```

### 11.2. Build Frontend

```bash
mvn -f main_system/frontend/pom.xml clean package
```

### 11.3. Chạy Server

Server cần chạy trước Client. Server mặc định lắng nghe ở cổng `8080`.

```bash
mvn -f main_system/backend/pom.xml org.codehaus.mojo:exec-maven-plugin:3.3.0:java -Dexec.mainClass=network.ServerMain
```

Khi chạy thành công, terminal sẽ hiển thị thông báo Server đang lắng nghe tại cổng 8080.

### 11.4. Chạy Client

Mở một terminal mới và chạy:

```bash
mvn -f main_system/frontend/pom.xml javafx:run
```

Có thể mở nhiều cửa sổ Client để kiểm thử realtime bidding.

### 11.5. Chạy nhiều máy trong LAN

Nếu Client chạy ở máy khác, cần đảm bảo:

- Server và Client cùng mạng LAN.
- Firewall trên máy Server cho phép cổng 8080.
- Client trỏ đúng IP của máy Server.

Trên Windows, có thể mở cổng bằng PowerShell với quyền Administrator:

```powershell
New-NetFirewallRule -DisplayName "Auction Server 8080" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080
```

---

## 12. Tài khoản mặc định

Hệ thống có logic tự đảm bảo tài khoản Admin mặc định khi Server khởi động.

```text
Username: admin
Password: admin123
Role: ADMIN
```

Các tài khoản Seller/Bidder có thể được tạo trực tiếp bằng chức năng đăng ký trên giao diện.

---

## 13. Kiểm thử và CI/CD

Dự án sử dụng JUnit 5 và Mockito cho kiểm thử và GitHub Actions để tự động kiểm tra khi push code hoặc tạo pull request.

Workflow CI hiện gồm:

- build và test backend & frontend;
- lưu file chạy của backend & frontend sau khi chạy thành công.

Chạy test backend:

```bash
mvn -f main_system/backend/pom.xml test
```

Build frontend:

```bash
mvn -f main_system/frontend/pom.xml -DskipTests package
```

---


## Tổng kết

online_auction_system là hệ thống đấu giá trực tuyến mô phỏng đầy đủ các nghiệp vụ quan trọng của một nền tảng auction: đăng sản phẩm, duyệt sản phẩm, đặt giá, auto-bid, anti-sniping, cập nhật realtime, xử lý kết thúc phiên, ví người dùng và lịch sử giao dịch.

Dự án thể hiện rõ các nội dung trọng tâm của lập trình nâng cao:

- thiết kế hướng đối tượng;
- áp dụng design pattern;
- kiến trúc Client-Server;
- xử lý đồng thời;
- làm việc với cơ sở dữ liệu;
- xây dựng giao diện JavaFX;
- tổ chức project Maven;
- kiểm thử và CI/CD cơ bản.


## Link PDF và video demo
-PDF : https://drive.google.com/file/d/1sb3SUCrQPz2g3dNTDcIsHUsiH_c4t0O3/view?usp=sharing
-video demo : https://drive.google.com/file/d/1R4Bu6RdPywOHSSyzh5cf_wtQj99gnavf/view?usp=sharing