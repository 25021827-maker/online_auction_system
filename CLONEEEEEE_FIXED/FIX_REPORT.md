# Báo cáo fix bug theo rubric Bài tập lớn

## Căn cứ chấm điểm
Rubric yêu cầu bắt buộc: OOP + design pattern, quản lý user/sản phẩm, đấu giá, xử lý lỗi, concurrency an toàn, realtime Observer/Socket, Client–Server + MVC, Maven/Gradle, JUnit, CI/CD. Phần nâng cao gồm auto-bidding, anti-sniping và biểu đồ lịch sử bid realtime.

## Các lỗi trọng yếu đã sửa

### 1. Luồng tạo phiên đấu giá bị sai schema DB
- `ClientHandler` trước đây gọi trực tiếp `AuctionDAO.createAuction`, không đi qua `AuctionManager`, làm phiên mới không được đưa vào `activeServices`.
- `AuctionDAO.createAuction` insert cột `starting_price` vào bảng `auctions` trong khi schema thật không có cột này.
- `AuctionDAO.createAuction` dùng status `SCHEDULED` trong khi enum schema chỉ có `OPEN`, `RUNNING`, `FINISHED`, `PAID`, `CANCELED`.

Đã sửa:
- `CREATE_AUCTION` đi qua `AuctionManager.createNewAuction(...)`.
- SQL insert phiên đấu giá chỉ dùng các cột thật: `item_id`, `start_time`, `end_time`, `current_price`, `status`.
- Phiên mới dùng `PENDING` và chỉ chuyển `OPEN` sau khi admin duyệt.

### 2. Realtime và bid history chưa khép kín
- Frontend gửi `GET_BID_HISTORY` nhưng backend chưa xử lý action này.
- `AuctionRoomController` đã có line chart nhưng thiếu dữ liệu lịch sử thật từ DB.

Đã sửa:
- Thêm `AuctionDAO.getBidHistory(Long auctionId)`.
- Thêm action `GET_BID_HISTORY` trong `ClientHandler`.
- Backend trả lịch sử bid để frontend nạp vào list và line chart.

### 3. Xử lý request JSON bị parse sai
- Nhiều case dùng `gson.fromJson(gson.toJson(request.getData()), JsonObject.class)` trong khi `request.getData()` đã là JSON string. Cách cũ biến JSON object thành JSON string literal, dễ gây lỗi parse.

Đã sửa:
- Viết helper `parseObject(String jsonData)` và parse trực tiếp `gson.fromJson(jsonData, JsonObject.class)`.
- Áp dụng cho `ADD_BALANCE`, `GET_MY_PRODUCTS`, `DELETE_PRODUCT`, `GET_WATCHLIST`, `GET_BID_HISTORY`.

### 4. Đấu giá bị báo thành công dù DAO trả false
- `processBid` gọi `auctionManager.processBid(...)` nhưng không kiểm tra kết quả boolean.
- Nếu bid thấp hoặc phiên đóng mà DAO trả `false`, server vẫn có thể broadcast `NEW_BID_EVENT`.

Đã sửa:
- `AuctionService.placeBid` ném `InvalidBidException` khi DAO từ chối.
- `ClientHandler.processBid` kiểm tra kết quả trước khi broadcast.
- Chỉ bid hợp lệ mới được gửi realtime cho client khác.

### 5. Concurrency và trạng thái phiên
- DAO đã có `SELECT ... FOR UPDATE`, nhưng chưa kiểm tra `end_time` ở tầng DAO trước khi insert.
- Phiên hết hạn không tự chuyển `FINISHED` khi danh sách được tải.

Đã sửa:
- `AuctionDAO.placeBid` khóa dòng auction bằng `FOR UPDATE`, kiểm tra status và `end_time` trong transaction.
- Thêm `finishExpiredAuctions()` và gọi trước khi lấy danh sách phiên đang hoạt động.

### 6. Status frontend không khớp backend
- Backend dùng `RUNNING`/`FINISHED`, nhưng nhiều chỗ frontend chỉ cho bid khi status `OPEN` hoặc xem `SOLD` là kết thúc.

Đã sửa:
- `AuctionRoomController` và `ProductController` cho phép đặt giá khi status là `OPEN` hoặc `RUNNING`.
- Frontend nhận `FINISHED` như trạng thái đã kết thúc.
- `Product.getStatus()` fallback trả `FINISHED` thay vì `SOLD` để khớp backend.

### 7. Item category OTHER làm backend crash
- Schema và frontend có `OTHER`, nhưng `ItemFactory` không xử lý `OTHER`, dẫn đến lỗi khi map dữ liệu từ DB.

Đã sửa:
- Thêm `OtherItem`.
- `ItemFactory` trả `OtherItem` cho `OTHER` và category không nhận diện được.

### 8. Xóa sản phẩm trong màn My Products chưa có action
- Nút Delete được tạo nhưng chưa gửi request.

Đã sửa:
- Gắn `deleteBtn.setOnAction(...)` để gửi `DELETE_PRODUCT` lên server.

### 9. Unit test phụ thuộc MySQL
- `AuctionServiceTest` dùng `AuctionService` mặc định, khiến test cần DB thật nên CI dễ fail.

Đã sửa:
- Thêm constructor `AuctionService(Auction, AuctionDAO)`.
- Unit test truyền `null` DAO để test logic in-memory.
- Test vẫn kiểm tra bid hợp lệ, bid thấp và bid khi phiên đóng.

### 10. CI/CD và build config
- Frontend `pom.xml` đặt source/target = 25, không ổn với môi trường Java phổ biến.
- JavaFX plugin trỏ main class không tồn tại.

Đã sửa:
- Frontend compile target về Java 21.
- Main class trỏ về `demo/Main.MainApp`.
- Thêm `.github/workflows/ci.yml` chạy backend test và compile frontend.

### 11. Cấu hình database an toàn hơn
- Mật khẩu MySQL bị hard-code trong source.

Đã sửa:
- `DatabaseConnection` đọc `AUCTION_DB_URL`, `AUCTION_DB_USER`, `AUCTION_DB_PASSWORD` từ biến môi trường, fallback về localhost/root/mật khẩu rỗng.

## File chính đã thay đổi
- `backend/src/main/java/network/ClientHandler.java`
- `backend/src/main/java/dao/AuctionDAO.java`
- `backend/src/main/java/service/AuctionService.java`
- `backend/src/test/java/service/AuctionServiceTest.java`
- `backend/src/main/java/model/ItemFactory.java`
- `backend/src/main/java/model/OtherItem.java`
- `backend/src/main/java/dao/DatabaseConnection.java`
- `frontend/src/main/java/Controller/product/AuctionRoomController.java`
- `frontend/src/main/java/Controller/product/ProductController.java`
- `frontend/src/main/java/Controller/product/MyProductsController.java`
- `frontend/src/main/java/Model/Product.java`
- `frontend/src/main/java/ui/product/ProductCardFactory.java`
- `frontend/pom.xml`
- `.github/workflows/ci.yml`
- `database/schema/10_watchlist.sql`

## Lưu ý kiểm thử
Do môi trường hiện tại không truy cập Internet nên không tải được Maven dependencies từ Maven Central. Tôi đã kiểm tra biên dịch cú pháp các lớp backend quan trọng bằng `javac` với classpath sẵn có/stub Gson. Khi chạy ở máy có Maven/Internet, dùng:

```bash
mvn -f backend/pom.xml test
mvn -f frontend/pom.xml -DskipTests package
```

Trước khi chạy app, tạo database bằng các file trong `database/schema/` theo thứ tự 01 đến 12, rồi set biến môi trường nếu MySQL có mật khẩu:

```bash
export AUCTION_DB_URL='jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true'
export AUCTION_DB_USER='root'
export AUCTION_DB_PASSWORD='your_password'
```
