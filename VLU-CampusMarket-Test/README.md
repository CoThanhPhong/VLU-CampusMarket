# LapTrinhDiDong_0101
Đồ án nhóm lớp 0101

> [!CAUTION]
>**RULES**
>- Không commit trực tiếp vào branch `main`.
>- Không merge branch main khi chưa test kỹ
>- Hãy làm việc ở branch Test về rồi push lên nhánh Test
>- Cùng review file để xem còn lỗi hay không mới nhờ leader merge vào nhánh main

***Members:***
- Cổ Thanh Phong - 197CT31643 (leader)
- Phạm Hoàng An - 2174802010168
- Nguyễn Trương Xuân Phước - 2474802010322
- Lê Võ Hải Khánh - 197ct09781

---

## Cấu hình Firebase Authentication (bắt buộc trước khi build)
1. Vào https://console.firebase.google.com → tạo project → **Add app → Android**.
2. **Package name phải đúng `com.example.vlu_campusmarket`** (trùng `applicationId` trong `app/build.gradle.kts`).
3. Tải `google-services.json` → đặt vào thư mục `app/` (cùng cấp `app/build.gradle.kts`).
4. **Build → Authentication → Sign-in method → Email/Password → Enable**.
5. Sync Gradle và chạy. Thiếu `google-services.json` thì build sẽ báo lỗi.

Email đăng ký bị giới hạn theo `AuthValidator.ALLOWED_DOMAIN` (mặc định `@vanlanguni.vn`);
đặt thành `""` nếu muốn test bằng email khác.
