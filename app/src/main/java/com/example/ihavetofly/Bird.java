package com.example.ihavetofly; // Khai báo package (gói) nơi lớp này thuộc về, giúp tổ chức cấu trúc của dự án.

import android.content.res.Resources; // Nhập (import) lớp Resources, dùng để truy cập vào các tài nguyên của ứng dụng như hình ảnh.
import android.graphics.Bitmap; // Nhập lớp Bitmap, dùng để xử lý dữ liệu hình ảnh.
import android.graphics.BitmapFactory; // Nhập lớp BitmapFactory, giúp tạo ra các đối tượng Bitmap từ nhiều nguồn khác nhau.
import android.graphics.Rect; // Nhập lớp Rect, dùng để biểu diễn một vùng hình chữ nhật (sẽ dùng cho việc xét va chạm).

public class Bird { // Định nghĩa một lớp công khai (public) tên là Bird, đại diện cho đối tượng chim trong game.

    public int x, y, width, height, speed; // Các biến công khai để lưu trữ tọa độ (x, y), kích thước (chiều rộng, chiều cao) và tốc độ của con chim.
    public boolean wasShot = false; // Một cờ (flag) boolean để theo dõi xem con chim đã bị bắn hay chưa.

    private static Bitmap[] birdFrames; // Một mảng Bitmap tĩnh (static) để lưu trữ các khung hình của hoạt ảnh con chim. "Static" nghĩa là mảng này được chia sẻ chung cho tất cả các đối tượng Bird.
    private static int referenceCount = 0; // Một biến đếm tham chiếu tĩnh, dùng để theo dõi có bao nhiêu đối tượng Bird đang tồn tại, giúp quản lý bộ nhớ.
    private int frameIndex = 0; // Chỉ số của khung hình hiện tại đang được hiển thị trong hoạt ảnh.

    private final Rect collisionRect = new Rect(); // Tạo một đối tượng Rect (vùng va chạm) cuối cùng (final), sẽ được tái sử dụng để tránh tạo đối tượng mới liên tục.

    public Bird(Resources res){ // Hàm khởi tạo (constructor) của lớp Bird, được gọi khi một đối tượng Bird mới được tạo.
        if (birdFrames == null) { // Nếu mảng các khung hình của chim chưa được khởi tạo...
            initBirdFrames(res); // ...thì gọi phương thức để khởi tạo chúng.
        }
        referenceCount++; // Tăng biến đếm tham chiếu lên một, vì một đối tượng Bird mới vừa được tạo.

        if (birdFrames != null && birdFrames.length > 0 && birdFrames[0] != null) { // Kiểm tra xem mảng khung hình đã được khởi tạo thành công và có chứa dữ liệu chưa.
            width = birdFrames[0].getWidth(); // Lấy chiều rộng từ khung hình đầu tiên làm chiều rộng của chim.
            height = birdFrames[0].getHeight(); // Lấy chiều cao từ khung hình đầu tiên làm chiều cao của chim.
        } else { // Nếu có lỗi xảy ra và không có khung hình nào...
            width = 1; // ...đặt chiều rộng và chiều cao mặc định là 1 để tránh lỗi.
            height = 1; //
        }
    }

    private synchronized static void initBirdFrames(Resources res) { // Một phương thức tĩnh và đồng bộ (synchronized) để khởi tạo các khung hình của chim. "Synchronized" đảm bảo an toàn khi nhiều luồng cùng cố gắng khởi tạo.
        if (birdFrames != null) return; // Nếu mảng đã được khởi tạo bởi một luồng khác rồi thì thoát ngay.

        int[] BIRD_RES = {R.drawable.bird1, R.drawable.bird2, R.drawable.bird3}; // Khai báo một mảng chứa ID của các tài nguyên hình ảnh (3 khung hình của con chim).
        birdFrames = new Bitmap[BIRD_RES.length]; // Khởi tạo mảng Bitmap với kích thước bằng số lượng khung hình.

        for (int i = 0; i < BIRD_RES.length; i++) { // Lặp qua từng ID tài nguyên hình ảnh.
            Bitmap tmp = BitmapCache.get(res, BIRD_RES[i], 1); // Cố gắng lấy ảnh từ bộ đệm (cache) để tăng hiệu suất.

            if (tmp == null) { // Nếu không có trong cache...
                tmp = BitmapFactory.decodeResource(res, BIRD_RES[i]); // ...thì giải mã hình ảnh từ file tài nguyên.
            }

            if (tmp == null) { // Nếu vẫn không thể tải được ảnh (ví dụ: file bị lỗi)...
                tmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // ...tạo một Bitmap trống 1x1 pixel để tránh lỗi crash.
            }

            int origW = Math.max(1, tmp.getWidth()); // Lấy chiều rộng gốc của ảnh, đảm bảo giá trị ít nhất là 1.
            int origH = Math.max(1, tmp.getHeight()); // Lấy chiều cao gốc của ảnh, đảm bảo giá trị ít nhất là 1.

            int w = origW / 15; // Tính toán chiều rộng mới, giảm kích thước ảnh xuống 15 lần.
            if (w <= 0) w = Math.max(1, origW / 3); // Nếu kết quả quá nhỏ, thử lại với tỷ lệ khác để đảm bảo chiều rộng hợp lệ.
            int h = (int) ((long) origH * w / origW); // Tính chiều cao mới dựa trên chiều rộng mới để giữ nguyên tỷ lệ khung hình.
            if (h <= 0) h = 1; // Đảm bảo chiều cao hợp lệ.

            birdFrames[i] = Bitmap.createScaledBitmap(tmp, w, h, true); // Tạo một Bitmap mới với kích thước đã được thay đổi và lưu vào mảng.
        }
    }

    public void updateFrame() { // Phương thức để cập nhật khung hình cho hoạt ảnh.
        if (birdFrames == null || birdFrames.length == 0) return; // Nếu không có khung hình nào thì không làm gì cả.
        frameIndex = (frameIndex + 1) % birdFrames.length; // Tăng chỉ số khung hình lên, và dùng toán tử modulo (%) để quay vòng về 0 khi hết mảng, tạo hiệu ứng lặp.
    }

    public Bitmap getBird() { // Phương thức để lấy Bitmap của khung hình hiện tại.
        if (birdFrames == null || birdFrames.length == 0) return null; // Nếu không có khung hình, trả về null.
        return birdFrames[frameIndex]; // Trả về Bitmap tại chỉ số khung hình hiện tại.
    }

    public Rect getCollisionShape() { // Phương thức để lấy vùng va chạm của con chim.
        collisionRect.set(x, y, x + width, y + height); // Cập nhật tọa độ của hình chữ nhật va chạm dựa trên vị trí và kích thước hiện tại của con chim.
        return collisionRect; // Trả về đối tượng Rect đã được cập nhật.
    }

    public static void clearCache() { // Một phương thức tĩnh để giải phóng bộ nhớ đã cấp cho các khung hình của chim.
        referenceCount--; // Giảm biến đếm tham chiếu đi một.
        if (referenceCount <= 0) { // Nếu không còn đối tượng Bird nào tồn tại...
            referenceCount = 0; // ...đặt lại biến đếm về 0.
            if (birdFrames != null) { // Kiểm tra xem mảng khung hình có tồn tại không.
                for (Bitmap bmp : birdFrames) { // Lặp qua từng Bitmap trong mảng.
                    if (bmp != null && !bmp.isRecycled()) { // Nếu Bitmap tồn tại và chưa được giải phóng...
                        bmp.recycle(); // ...giải phóng bộ nhớ của Bitmap đó.
                    }
                }
                birdFrames = null; // Đặt mảng về null để hệ thống có thể thu gom rác.
            }
        }
    }
}
