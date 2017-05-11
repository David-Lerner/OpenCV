package com.example.david.opencv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class Edit_Sudoku_Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        long addr = intent.getLongExtra("myImg", 0);
        Mat tempImg = new Mat( addr );
        Mat img = tempImg.clone();

        setContentView(R.layout.activity_edit__sudoku_);

        Bitmap bitMap = Bitmap.createBitmap(img.cols(), img.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(img, bitMap);

        // find the imageview and draw it!
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(bitMap);
    }
}
