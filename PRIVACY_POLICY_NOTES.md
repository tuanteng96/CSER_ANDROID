# Hoàn thiện chính sách quyền riêng tư cho Cser Beauty

File chính: `privacy-policy.html` — HTML tiếng Việt, hiển thị trên điện thoại, không dùng JavaScript, font ngoài hoặc công cụ theo dõi. Đây là bản nháp dựa trên mã Android, chưa phải chính sách đã xác nhận của đơn vị vận hành.

## Thông tin cần hoàn thiện

Tìm `[CẦN BỔ SUNG` trong HTML và thay bằng thông tin vận hành thực tế: đơn vị phát hành, email liên hệ, dữ liệu biểu mẫu/tài khoản, đối tác nhận dữ liệu, bảo mật, thời hạn lưu/xóa, quy trình xóa tài khoản và nhóm tuổi. Sau khi kiểm tra toàn bộ, bỏ khối `aside` cảnh báo bản nháp và các đánh dấu còn lại. Không chỉ xóa đánh dấu để tạo cảm giác tài liệu đã đầy đủ.

Mã Android tải nội dung từ `https://cserbeauty.com/app2021/index.aspx`. Repository không chứa toàn bộ chức năng web và backend, nên không thể suy ra đầy đủ dữ liệu về khách hàng, lịch hẹn, thanh toán, sức khỏe, tài khoản hoặc các SDK web. Cần kiểm tra các phần này trước khi công bố; không tự tuyên bố “không thu thập dữ liệu”, “không chia sẻ dữ liệu” hoặc “không quảng cáo”.

## Căn cứ trong project

| Nguồn | Điều xác định được |
| --- | --- |
| `app/src/main/res/values/strings.xml`, `app/build.gradle` | Tên Cser Beauty, mã ứng dụng `vn.cservn2020`, tên miền nền `https://cser.vn` |
| `app/src/main/assets/embed21.html`, `MainActivity.java` | Nội dung web từ cserbeauty.com, WebView có JavaScript và lưu trữ cục bộ |
| `App21.java` | Camera, chọn tệp, ghi âm/video, số SIM, gọi điện, mở trình soạn SMS, thông tin thiết bị, thông báo và dữ liệu cục bộ |
| `QRCodeFragment.java` | Quét QR từ camera hoặc ảnh chọn; chuyển kết quả cho chức năng ứng dụng |
| `Record21.java`, `PostFileToServer.java` | Ghi âm thành tệp; tải tệp đến địa chỉ do chức năng web cung cấp |
| `MyFirebaseMessagingService.java`, `App21.java`, `app/build.gradle` | Firebase Cloud Messaging, lưu/lấy token và đăng ký chủ đề |
| `Loction21.java`, `SERVER_NOTI.java` | Có mã lấy vị trí và gửi tọa độ tới địa chỉ máy chủ chỉ định trong luồng chạy nền |
| `app/src/main/AndroidManifest.xml` | Các quyền nhạy cảm và `allowBackup="true"` |

Các tên lớp Java ở bảng nằm trong `app/src/main/java/vn/cser21/`.

## Điểm cần đối chiếu trước khi phát hành

- **Vị trí:** Manifest khai báo `ACCESS_BACKGROUND_LOCATION`, nhưng `ACCESS_FINE_LOCATION` và `ACCESS_COARSE_LOCATION` đang bị comment. Không đủ cơ sở nói vị trí đang hoạt động, cũng không thể khẳng định ứng dụng không có luồng gửi vị trí nền. Quyết định có sử dụng tính năng này không; nếu có, xác minh mục đích và bên nhận, hoàn thiện quyền và thông báo nổi bật/xin đồng ý trong ứng dụng. Chính sách trên website không thay thế thông báo đó.
- **Số điện thoại:** `GET_PHONE` gọi `getLine1Number()` sau yêu cầu `READ_PHONE_STATE`; khả năng đọc còn phụ thuộc phiên bản Android/quyền. Cần xác định chức năng web nào gọi và dùng số này vào việc gì; quyền khai báo không chứng minh dữ liệu luôn được thu thập.
- **SMS:** `SEND_SMS` mở trình soạn qua `ACTION_SENDTO`; quyền đọc/nhận/gửi SMS trong Manifest đang bị comment. Không khai báo ứng dụng đọc SMS hoặc tự gửi SMS chỉ dựa trên tên hàm.
- **Mã hóa:** Các URL chính là HTTPS, nhưng địa chỉ tải tệp và nhận vị trí có thể được truyền động. Phải kiểm tra mọi endpoint trước khi xác nhận “tất cả dữ liệu được mã hóa khi truyền” trong Data safety.
- **Xóa tài khoản:** Chưa xác minh được luồng xóa tài khoản máy chủ. `CLEAR_WEBVIEW_DATA` xóa dữ liệu WebView và gọi xóa token FCM; việc này không chứng minh tài khoản hay dữ liệu backend đã bị xóa. Nếu cho phép tạo tài khoản, cần luồng yêu cầu xóa trong ứng dụng và tài nguyên web bên ngoài hoạt động thật.
- **SDK:** Gradle được đọc có Firebase Messaging, không thấy khai báo trực tiếp Firebase Analytics/Crashlytics hoặc SDK quảng cáo. Điều này không chứng minh backend, dependency bắc cầu và nội dung web không xử lý dữ liệu tương ứng.

## Cách sử dụng trên Google Play

1. Hoàn thiện nội dung HTML theo bản phát hành và cách vận hành thực tế.
2. Đăng file lên website dưới URL HTTPS công khai, ví dụ `https://cserbeauty.com/privacy-policy.html` **chỉ là URL đề xuất, chưa được triển khai**. Trang phải truy cập được không cần đăng nhập, không giới hạn địa lý và không cho khách truy cập chỉnh sửa; không dùng PDF.
3. Điền URL thực tế vào trường Chính sách quyền riêng tư của Play Console; thêm liên kết hoặc nội dung chính sách vào vị trí dễ tìm trong ứng dụng.
4. Khai báo An toàn dữ liệu (Data safety) nhất quán với bản phát hành, backend, web và SDK. Phân biệt truy cập/xử lý tại thiết bị với thu thập hoặc chia sẻ ra ngoài thiết bị theo định nghĩa của biểu mẫu.
5. Nếu có tạo tài khoản, triển khai và kiểm tra kênh xóa ở cả ứng dụng và web; khai báo URL yêu cầu xóa riêng trong Play Console. Mục `#xoa-tai-khoan` hiện là bản nháp, chưa phải một kênh tiếp nhận yêu cầu hoạt động.

Tài liệu này không triển khai website, thay đổi quyền ứng dụng hoặc tạo chức năng xóa dữ liệu máy chủ.

## Tài liệu tham chiếu

- [Google Play: Dữ liệu người dùng và chính sách quyền riêng tư](https://support.google.com/googleplay/android-developer/answer/10144311?hl=vi)
- [Google Play: Yêu cầu xóa tài khoản](https://support.google.com/googleplay/android-developer/answer/13327111?hl=vi)
- [Firebase: Quyền riêng tư và bảo mật](https://firebase.google.com/support/privacy)

Đối chiếu ngày 24/09/2026. Chính sách và biểu mẫu có thể được cập nhật; kiểm tra lại khi gửi bản phát hành.
