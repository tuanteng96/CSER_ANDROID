# cser21_android

for Android

Cách tạo App

- Clone source code

- Đổi tên Package name

- Tạo Icon

- Tạo Google Firebase => GoogleService-Info.plist

- Đổi đường link "https://cser.vn/app/index21.aspx" thành link mới trong embed21.html

- Chạy test 

# Fix ANDROID
# 1. Update kotlin 1.8.0 Update
- Kiểm tra các file MainActivity, build.gradle (APP, Project), gradle-wrapper, local.properties
- Remove setting.setAppCacheEnabled(true) ở MainActivity
- Ở hàm onRequestPermissionsResult thêm super.onRequestPermissionsResult(requestCode, permissions, grantResults); ở đầu hàm file MainActivity
- Mở lại Android Studio

# 2. Lỗi mất noti kotlin 1.8.0
- Xin thêm quyền `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`
- Thêm hàm dưới trên hàm onNewIntent :
public void getNotificationPermission(){
        try {
            if (Build.VERSION.SDK_INT > 32) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        202);
            }
        }catch (Exception e){

        }
    }
- Bỏ hàm getNotificationPermission () vào cuối hàm onCreate()

# 3. Lỗi auto bật QR code
- Ở hàm onPermissionsGranted thêm if (requestCode == 202) return;


