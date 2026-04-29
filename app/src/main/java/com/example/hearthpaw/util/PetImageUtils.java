package com.example.hearthpaw.util;

import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.File;

public final class PetImageUtils {

    private PetImageUtils() {
    }

    public static void loadPhoto(String photoPath, ImageView imageView) {
        if (photoPath == null || photoPath.trim().isEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_camera);
            return;
        }

        File photoFile = new File(photoPath);
        if (!photoFile.exists()) {
            imageView.setImageResource(android.R.drawable.ic_menu_camera);
            return;
        }

        imageView.setImageBitmap(BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
    }
}