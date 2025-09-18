package com.example.ihavetofly;

import static com.example.ihavetofly.GameView.screenRatioX;
import static com.example.ihavetofly.GameView.screenRatioY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Flight {
    public boolean isGoingUp = false;
    public int toShoot = 0;  // số đạn cần bắn
    int x, y, width, height;
    private int frame = 0;
    private final int frameCount = 10; // 10 frame trong sprite sheet hero
    private int spriteWidth, spriteHeight;
    private GameView gameView;

    public boolean movingLeft = false;
    public boolean movingRight = false;
    public boolean movingUp = false;
    public boolean movingDown = false;

    public int speed = 15; // tốc độ di chuyển, có thể thay đổi

    Bitmap runSprite;    // sprite sheet chạy
    Bitmap standing, dead;  // đứng yên và chết

    Flight(GameView gameView, int screenX, int screenY, Resources res) {
        this.gameView = gameView;

        // Hệ số scale để nhân vật nhỏ hơn
        float scaleFactor = 0.35f; // giảm từ 50% → 35%

        // load sprite sheet hero mới
        runSprite = BitmapFactory.decodeResource(res, R.drawable.hero_sprite);
        spriteWidth = runSprite.getWidth() / 5;   // 5 cột
        spriteHeight = runSprite.getHeight() / 2; // 2 hàng

        // scale theo màn hình với hệ số giảm
        width = (int)(spriteWidth * screenRatioX * scaleFactor);
        height = (int)(spriteHeight * screenRatioY * scaleFactor);

        // tạo bitmap đứng yên từ frame đầu tiên của hero sprite
        standing = Bitmap.createBitmap(runSprite, 0, 0, spriteWidth, spriteHeight);
        standing = Bitmap.createScaledBitmap(standing, width, height, false);

        // load bitmap chết từ frame cuối của hero sprite
        dead = Bitmap.createBitmap(runSprite, 4 * spriteWidth, spriteHeight, spriteWidth, spriteHeight);
        dead = Bitmap.createScaledBitmap(dead, width, height, false);

        // vị trí xuất phát
        x = screenX / 2 - width / 2;
        y = screenY - height - 50;
    }

    // cập nhật vị trí theo movement
    public void updatePosition() {
        if (movingLeft && x > 0) {
            x -= speed;
        }
        if (movingRight && x < gameView.screenX - width) {
            x += speed;
        }
        if (movingUp && y > 0) {
            y -= speed;
        }
        if (movingDown && y < gameView.screenY - height) {
            y += speed;
        }
        if (isGoingUp && y > 0) {
            y -= speed;
        }
    }

    // trả về frame hiện tại để vẽ
    Bitmap getFlight() {
        if(toShoot != 0) {
            // gọi Bullet để bắn đạn
            toShoot--;
            gameView.newBullet();
            return standing; // giữ hình đứng khi bắn
        }

        // nếu không di chuyển thì đứng yên
        if (!movingLeft && !movingRight && !movingUp && !movingDown && !isGoingUp) {
            return standing;
        }

        // animation chạy từ sprite sheet hero (2 hàng x 5 cột)
        int row = frame / 5;  // chia cho 5 vì có 5 cột
        int col = frame % 5;  // modulo 5 để lấy cột

        Bitmap currentFrame = Bitmap.createBitmap(
                runSprite,
                col * spriteWidth,
                row * spriteHeight,
                spriteWidth,
                spriteHeight
        );

        // scale frame theo màn hình và scaleFactor
        currentFrame = Bitmap.createScaledBitmap(currentFrame, width, height, false);

        frame++;
        if(frame >= frameCount) frame = 0;

        return currentFrame;
    }

    Rect getCollisionShape() {
        return new Rect(x, y, x + width, y + height);
    }

    Bitmap getDead() {
        return dead;
    }
}