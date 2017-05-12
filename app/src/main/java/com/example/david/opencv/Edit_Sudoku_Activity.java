package com.example.david.opencv;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.util.ArrayList;
import java.util.List;

public class Edit_Sudoku_Activity extends AppCompatActivity {

    private static final String TAG = "EditSudokuActivity";

    private int[][] array;

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

        array = new int[9][9];
    }

    private class AsyncTaskRunner extends AsyncTask<Mat, Integer, Bitmap> {

        private Mat input;
        ProgressDialog progressDialog;
        private DigitRecognizer mnist;

        @Override
        protected Bitmap doInBackground(Mat... params) {
            mnist = new DigitRecognizer("train-images.idx3-ubyte", "train-labels.idx1-ubyte", getApplicationContext());
            publishProgress(-1); // Calls onProgressUpdate()
            input = params[0].clone();

            //Imgproc.GaussianBlur(input, input, new Size(11, 11), 0);

            Imgproc.dilate(input, input, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
            //Imgproc.resize(input, input, new Size(mnist.getWidth()*9,+mnist.getHeight()*9));
            Imgproc.cvtColor(input, input, Imgproc.COLOR_BGR2GRAY);

            //Imgproc.Canny(input, input, 10, 100);
            //Imgproc.Canny(input, input, 50, 100);

            Imgproc.adaptiveThreshold(input, input, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 101, 30);

            //Imgproc.adaptiveThreshold(input, input,255,Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV,15, 2);

            /*input.convertTo(input, CvType.CV_32FC1, 1.0 / 255.0);
            Mat res = CalcBlockMeanVariance(input, 21);
            Core.subtract(new MatOfDouble(1.0), res, res);
            Imgproc.cvtColor(input, input, Imgproc.COLOR_BGRA2BGR);
            Core.add(input, res, res);
            Imgproc.threshold(res, res, 0.85, 1, Imgproc.THRESH_BINARY);
            res.convertTo(res, CvType.CV_8UC1, 255.0);*/

            //remove grid lines
            /*Mat lines = new Mat();
            int threshold = 10;
            int minLineSize = input.width()/2;
            int lineGap = 2;
            Imgproc.HoughLinesP(input, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);
            for(int i = 0; i < lines.cols(); i++) {
                double[] vec = lines.get(0,i);
                Imgproc.line(input, new Point(vec[0], vec[1]), new Point(vec[2],vec[3]), new Scalar(0), 10, Core.LINE_AA, 0);
            }*/

            //fill lines
            /*double Cellwidth = input.cols()/9;
            Mat mask = Mat.zeros(input.rows() + 2, input.cols() + 2, CvType.CV_8U);
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    Imgproc.floodFill(input, mask, new Point(0,0), new Scalar( 255, 255, 255)) ;
                }
            }*/

            //Imgproc.resize(input, input, new Size(mnist.getWidth()*9,+mnist.getHeight()*9));

            double height = input.rows()/9;
            double width = input.cols()/9;
            publishProgress(-2);
            for (int i = 0; i < 9; i++) {
                int col = (int)Math.round(i*height);
                for (int j = 0; j < 9; j++) {
                    int row = (int)Math.round(j*width);
                    Mat digit = input.submat(row, row+(int)Math.round(width), col, col+(int)Math.round(height));
                    //flood fill edges
                    /*Mat mask = Mat.zeros(digit.rows() + 2, digit.cols() + 2, CvType.CV_8U);
                    for (int k = 0; k < mnist.getHeight(); k++) {
                        if (digit.get(k, 0)[0] != 0) {
                            Imgproc.floodFill(digit, mask, new Point(k, 0), new Scalar(0));
                        }
                        if (digit.get(k, digit.cols()-1)[0] != 0) {
                            Imgproc.floodFill(digit, mask, new Point(k, digit.cols()-1), new Scalar(0));
                        }
                    }
                    for (int k = 0; k < mnist.getWidth(); k++) {
                        if (digit.get(0, k)[0] != 0) {
                            Imgproc.floodFill(digit, mask, new Point(0, k), new Scalar(0));
                        }
                        if (digit.get(digit.rows()-1, k)[0] != 0) {
                            Imgproc.floodFill(digit, mask, new Point(digit.rows()-1, k), new Scalar(0));
                        }
                    }*/
                    //flood fill out everything but what is in the center
                    for (int k = 0; k < (int)Math.round(height); k++) {
                        for (int l = 0; l < (int)Math.round(width); l++) {
                            if (digit.get(k, l)[0] < 128) {
                                digit.put(k, l, new double[]{0});
                            } else {
                                digit.put(k, l, new double[]{255});
                            }
                        }
                    }
                    int count = 0;
                    for (int k = (int)Math.round(height/4.0); k < (int)Math.round(height*3.0/4.0); k++) {
                        for (int l = (int)Math.round(width/4.0); l <(int)Math.round(width*3.0/4.0); l++) {
                            if (digit.get(k, l)[0] == 255) {
                                Mat mask = digit.clone();
                                Core.bitwise_not(mask, mask);
                                Core.copyMakeBorder(mask, mask, 1, 1, 1, 1, Core.BORDER_CONSTANT, new Scalar(0));
                                Imgproc.floodFill(digit, mask, new Point(k, l), new Scalar(100));
                            }
                            if (digit.get(k, l)[0] == 100) {
                                count++;
                            }
                        }
                    }
                    if (count < 7) {
                        publishProgress(j, i, 0);
                        continue;
                    }
                    Imgproc.threshold(digit, digit, 254, 255, Imgproc.THRESH_TOZERO_INV);
                    Imgproc.threshold(digit, digit, 99, 255, Imgproc.THRESH_BINARY);

                    Mat test = new Mat();
                    Imgproc.resize(digit, test, new Size(mnist.getWidth(),mnist.getHeight()));
                    //Core.transpose(test, test);
                    int value = mnist.findValue(test);
                    publishProgress(j, i, value);
                }
            }

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
                    "Complete Sudoku",
                    "Training Classifier");
        }


        @Override
        protected void onProgressUpdate(Integer... update) {
            //show progress;
            if (update[0] == -1) {
                progressDialog.setMessage("Processing Image");
            } else if (update[0] == -2) {
                progressDialog.setMessage("Classifying Digits");
            } else {
                TextView textView = (TextView) findViewById(R.id.textView);
                array[update[0]][update[1]] = update[2];
                textView.setText(getMatrix(array));
                Log.d(TAG, "i: "+update[0]+" j: "+update[1]+" value: "+update[2]);
            }
        }
    }

    private String getMatrix(int[][] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; ++i) {
            if (i % 3 == 0)
                sb.append(" -----------------------").append("\n");
            for (int j = 0; j < 9; ++j) {
                if (j % 3 == 0) sb.append("| ");
                sb.append(a[i][j] == 0 ? " " : Integer.toString(a[i][j]));

                sb.append(' ');
            }
            sb.append("|").append("\n");
        }
        sb.append(" -----------------------").append("\n");

        return sb.toString();
    }
}
