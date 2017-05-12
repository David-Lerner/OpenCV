package com.example.david.opencv;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.util.ArrayList;
import java.util.List;

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
        int width = bitMap.getWidth();
        int height = bitMap.getHeight();
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(width,height);
        iv.setLayoutParams(params);
        AsyncTaskRunner ocr = new AsyncTaskRunner();
        ocr.execute(img);
    }

    private class AsyncTaskRunner extends AsyncTask<Mat, String, Bitmap> {

        private Mat input;
        ProgressDialog progressDialog;

        @Override
        protected Bitmap doInBackground(Mat... params) {
            publishProgress("Sleeping..."); // Calls onProgressUpdate()

            input = params[0].clone();
            //Imgproc.GaussianBlur(input, input, new Size(11, 11), 0);

            Imgproc.cvtColor(input, input, Imgproc.COLOR_BGR2GRAY);

            //Imgproc.Canny(input, input, 10, 100);
            //Imgproc.Canny(input, input, 50, 100);
            Imgproc.adaptiveThreshold(input, input, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 101, 30);
            //Imgproc.dilate(input, input, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));

            /*input.convertTo(input, CvType.CV_32FC1, 1.0 / 255.0);
            Mat res = CalcBlockMeanVariance(input, 21);
            Core.subtract(new MatOfDouble(1.0), res, res);
            Imgproc.cvtColor(input, input, Imgproc.COLOR_BGRA2BGR);
            Core.add(input, res, res);
            Imgproc.threshold(res, res, 0.85, 1, Imgproc.THRESH_BINARY);
            res.convertTo(res, CvType.CV_8UC1, 255.0);*/

            Mat res = input;
            Bitmap bitMap = Bitmap.createBitmap(res.cols(), res.rows(),Bitmap.Config.RGB_565);
            Utils.matToBitmap(res, bitMap);
            return bitMap;
        }

        private Mat CalcBlockMeanVariance (Mat Img, int blockSide) {
            Mat I = new Mat();
            Mat ResMat;
            Mat inpaintmask = new Mat();
            Mat patch;
            Mat smallImg = new Mat();
            MatOfDouble mean = new MatOfDouble();
            MatOfDouble stddev = new MatOfDouble();

            Img.convertTo(I, CvType.CV_32FC1);
            ResMat = Mat.zeros(Img.rows() / blockSide, Img.cols() / blockSide, CvType.CV_32FC1);

            for (int i = 0; i < Img.rows() - blockSide; i += blockSide)
            {
                for (int j = 0; j < Img.cols() - blockSide; j += blockSide)
                {
                    patch = new Mat(I,new Rect(j,i, blockSide, blockSide));
                    Core.meanStdDev(patch, mean, stddev);

                    if (stddev.get(0,0)[0] > 0.01)
                        ResMat.put(i / blockSide, j / blockSide, mean.get(0,0)[0]);
                    else
                        ResMat.put(i / blockSide, j / blockSide, 0);
                }
            }

            Imgproc.resize(I, smallImg, ResMat.size());
            Imgproc.threshold(ResMat, inpaintmask, 0.02, 1.0, Imgproc.THRESH_BINARY);

            Mat inpainted = new Mat();
            Imgproc.cvtColor(smallImg, smallImg, Imgproc.COLOR_RGBA2BGR);
            smallImg.convertTo(smallImg, CvType.CV_8UC1, 255.0);

            inpaintmask.convertTo(inpaintmask, CvType.CV_8UC1);
            Photo.inpaint(smallImg, inpaintmask, inpainted, 5, Photo.INPAINT_TELEA);

            Imgproc.resize(inpainted, ResMat, Img.size());
            ResMat.convertTo(ResMat, CvType.CV_32FC1, 1.0 / 255.0);

            return ResMat;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // execution of result of Long time consuming operation
            ImageView iv = (ImageView) findViewById(R.id.imageView);
            iv.setImageBitmap(result);
            progressDialog.dismiss();

        }


        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(Edit_Sudoku_Activity.this,
                    "ProgressDialog",
                    "Proccessing image");
        }


        @Override
        protected void onProgressUpdate(String... text) {
            //show progress;

        }
    }
}
