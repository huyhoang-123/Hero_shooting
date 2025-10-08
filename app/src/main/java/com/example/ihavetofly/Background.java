package com.example.ihavetofly; // Declares the package where this class belongs, organizing it within the project structure.

import android.content.res.Resources; // Imports the Resources class, used to access application resources like images.
import android.graphics.Bitmap; // Imports the Bitmap class, used to handle image data.
import android.graphics.BitmapFactory; // Imports the BitmapFactory class, which helps create Bitmaps from various sources.

public class Background { // Defines a public class named Background, responsible for managing the scrolling background image.

    public Bitmap background; // A public variable to hold the background image itself.
    private int screenY; // A private variable to store the height of the screen in pixels.

    private float y1 = 0; // The vertical (Y) position of the first background image piece.
    private float y2 = 0; // The vertical (Y) position of the second background image piece.
    private float speed = 200; // The speed at which the background scrolls, in pixels per second.

    public Background(Resources res, int resId, int screenY){ // The constructor for the Background class.
        this.screenY = screenY; // Initializes the screen height from the provided argument.

        Bitmap tmp = BitmapFactory.decodeResource(res, resId); // Decodes the image file from resources into a temporary Bitmap object.
        background = Bitmap.createScaledBitmap(tmp, tmp.getWidth(), screenY, true); // Creates a new bitmap, scaled to fit the full height of the screen.
        if(tmp != background) tmp.recycle(); // If a new bitmap was created, recycle the temporary one to free up memory.

        y1 = 0; // Sets the initial position of the first background image to the top of the screen.
        y2 = -screenY; // Sets the initial position of the second background image directly above the first one, off-screen.
    }

    public void update(float deltaTime){ // This method is called repeatedly to update the background's position.
        y1 += speed * deltaTime; // Moves the first background image down based on speed and the time passed since the last update.
        y2 += speed * deltaTime; // Moves the second background image down at the same rate.

        if(y1 >= screenY) { // Checks if the first image has moved completely off the bottom of the screen.
            y1 = y2 - screenY; // If so, it repositions it directly above the second image to create a seamless loop.
        }
        if(y2 >= screenY) { // Checks if the second image has moved completely off the bottom of the screen.
            y2 = y1 - screenY; // If so, it repositions it directly above the first image, continuing the loop.
        }
    }

    public float getY1(){ return y1; } // A public method to get the current Y position of the first background image.
    public float getY2(){ return y2; } // A public method to get the current Y position of the second background image.
}
