package com.example.david.opencv;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.opencv.core.Core.bitwise_not;


/**
 * Created by David on 5/9/2017.
 */

public class TestActivity extends Activity implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "TestActivity";

    private Mat cameraImage;
    private List<MatOfPoint> grid;
    private final Object cameraImageLock = new Object();
    private final Object gridLock = new Object();
    private CameraBridgeViewBase mOpenCvCameraView;
    private DigitRecognizer mnist;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(TestActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.test_view);

        mOpenCvCameraView = new JavaCameraView(this, 0);
        mOpenCvCameraView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        LinearLayout myLayout = (LinearLayout) findViewById(R.id.test_linear_layout);
        myLayout.addView(mOpenCvCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mnist = null;
        Runnable runnable = new Runnable() {
            public void run() {
                //mnist = new DigitRecognizer("train-images.idx3-ubyte", "train-labels.idx1-ubyte", getApplicationContext());
                Log.d(TAG, "Digit recognizer created");
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "camera started");
        cameraImage = new Mat(height, width, CvType.CV_8UC4);
        grid = new ArrayList<MatOfPoint>();
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Mat input;
                synchronized (cameraImageLock) {
                    input = cameraImage.clone();
                }
                Mat outerBox = new Mat(input.size(), CvType.CV_8UC1);

                Imgproc.GaussianBlur(input, input, new Size(11, 11), 0);

                Imgproc.cvtColor(input, input, Imgproc.COLOR_BGR2GRAY);

                //Imgproc.Canny(input, input, 10, 100);

                Imgproc.adaptiveThreshold(input, input, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 2);

                bitwise_not(input, input);

                Imgproc.dilate(input, input, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));

                List<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(input, contourList, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                int max = 0;
                int index = 0;
                for (int i = 0; i < contourList.size(); i++) {
                    Rect bounding_rect = Imgproc.boundingRect(contourList.get(i));
                    int area = bounding_rect.width*bounding_rect.height;
                    if (area > max) {
                        max = area;
                        index = i;
                    }
                }

                if (contourList.size() > 0) {
                    MatOfPoint2f contour2f = new MatOfPoint2f();
                    MatOfPoint2f approxContour2f = new MatOfPoint2f();

                    MatOfPoint approxContour = new MatOfPoint();
                    contourList.get(index).convertTo(contour2f, CvType.CV_32FC2);
                    Imgproc.approxPolyDP(contour2f, approxContour2f, 4, true);
                    approxContour2f.convertTo(approxContour, CvType.CV_32S);
                    synchronized (gridLock) {
                        grid.clear();
                        if (approxContour.size().height == 4) {
                            grid.add(approxContour);
                        }
                    }
                    contour2f.release();
                    approxContour2f.release();
                }
                hierarchy.release();
                input.release();
            }
        }, 100, 1);
    }

    @Override
    public void onCameraViewStopped() {
        cameraImage.release();
        for (MatOfPoint point : grid) {
            point.release();
        }
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat temp;
        synchronized (cameraImageLock) {
            cameraImage = inputFrame.rgba();
            temp = cameraImage.clone();
        }

        //digit ocr
        /*Imgproc.rectangle(temp, new Point(temp.cols()/2 - 200, temp.rows() / 2 - 200), new Point(temp.cols() / 2 + 200, temp.rows() / 2 + 200), new Scalar(255,255,255),1);
        Mat digit = temp.submat(temp.rows()/2 - 180, temp.rows() / 2 + 180, temp.cols() / 2 - 180, temp.cols() / 2 + 180).clone();
        Core.transpose(digit,digit);
        if (mnist != null) {
            mnist.FindMatch(digit);
        }*/

        synchronized (gridLock) {
            if (!grid.isEmpty()) {
                Imgproc.drawContours(temp, grid, 0, new Scalar(255, 255, 255), -1);
            }
        }

        return temp;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        /*
        //fill holes
        Mat edgesNeg = outerBox.clone();
        Mat mask = Mat.zeros(outerBox.rows() + 2, outerBox.cols() + 2, CvType.CV_8U);
        Imgproc.floodFill(edgesNeg, mask, new Point(0,0), new Scalar( 255, 255, 255)) ;
        int ch = outerBox.channels();
        //bitwise_not(edgesNeg, edgesNeg);
        for(int i=0;i<outerBox.rows();i++) {
            for(int j=0;j<outerBox.cols();j++) {
                double[] data = outerBox.get(i, j); //Stores element in an array
                double[] reverse = edgesNeg.get(i, j);
                for (int k = 0; k < ch; k++) //Runs for the available number of channels
                {
                    if (reverse[k] == 0) {
                        data[k] = 255; //Pixel modification done here
                    }
                }
            }
        */
        return false;
    }
}
