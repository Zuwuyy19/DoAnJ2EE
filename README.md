# Hướng dẫn chạy và test Backend

## 1. Chạy Backend

### Cách 1 — Terminal

```bash
cd /Users/tenthumuc/DoAnJ2EE
./mvnw spring-boot:run
```

> Backend chạy tại: **http://localhost:8081**

### Cách 2 — IntelliJ IDEA

1. Mở IntelliJ → File → Open → chọn thư mục `DoAnJ2EE`
2. Đợi Maven load xong
3. Tìm file `DoAnJ2EeApplication.java` trong `src/main/java`
4. Click chuột phải → **Run**

---

## 2. Test API bằng Swagger UI

### Truy cập Swagger

Mở trình duyệt: **http://localhost:8081/swagger-ui/index.html**

### Bước 1 — Đăng nhập lấy JWT Token

1. Mở **AuthController** → `POST /auth/api/login`
2. Nhấn **Try it out**
3. Điền body:
   ```json
   {
     "email": "test01@gmail.com",
     "password": "123456"
   }
   ```
4. Nhấn **Execute**
5. Copy **token** từ response (chuỗi bắt đầu bằng `eyJ...`)

### Bước 2 — Xác thực token

1. Nhấn nút **Authorize** (🔓) ở góc trên bên phải Swagger
2. Dán token đã copy vào ô `Bearer <token>`
3. Nhấn **Authorize** → **Close**

### Bước 3 — Test API mua khóa học

1. Mở **OrderController** → `POST /api/orders`
2. Nhấn **Try it out**
3. Điền body:
   ```json
   {
     "courseId": 1
   }
   ```
4. Nhấn **Execute**
5. Xem kết quả:
   - ✅ `201` → Mua thành công
   - ⚠️ `404` → Khóa học không tồn tại
   - ⚠️ `409` → Đã mua khóa học này rồi

### Bước 4 — Test API xem khóa học đã mua

1. Mở **OrderController** → `GET /api/orders/my-courses`
2. Nhấn **Try it out**
3. Điền `Authorization` header (đã tự động dùng token ở Bước 2)
4. Nhấn **Execute**
5. Xem danh sách khóa học đã mua

---

## 3. Đăng ký tài khoản mới

Truy cập trình duyệt: **http://localhost:8081/register**

Điền email + password để đăng ký.

---

## 4. Tài khoản test

| Email | Password |
|-------|---------|
| test01@gmail.com | 123456 |

---

## 5. Các API chính

| Method | URL | Mô tả |
|--------|-----|--------|
| POST | `/auth/api/login` | Đăng nhập lấy JWT token |
| POST | `/api/orders` | Mua khóa học |
| GET | `/api/orders/my-courses` | Xem khóa học đã mua |