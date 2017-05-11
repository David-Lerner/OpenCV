package com.example.david.opencv;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;



/**
 * Created by David on 5/9/2017.
 */

public class TestActivity extends Activity implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "TestActivity";

    private Mat cameraImage;
    private Mat gridImage;
    private double[][] points;
    private final Object cameraImageLock = new Object();
    private final Object gridLock = new Object();
    private Timer timer;
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
        timer = new Timer(true);
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
        gridImage = new Mat(height, width, CvType.CV_8UC4);
        points = null;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Mat input;

                synchronized (cameraImageLock) {
                    input = cameraImage.clone();
                }

                Mat toGrid = input.clone();

                Imgproc.GaussianBlur(input, input, new Size(11, 11), 0);

                Imgproc.cvtColor(input, input, Imgproc.COLOR_BGR2GRAY);

                //Imgproc.Canny(input, input, 10, 100);

                Imgproc.adaptiveThreshold(input, input, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 2);

                Core.bitwise_not(input, input);

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
                    contourList.get(index).convertTo(contour2f, CvType.CV_32FC2);
                    Imgproc.approxPolyDP(contour2f, approxContour2f, Imgproc.arcLength(contour2f, true) * 0.02, true);
                    double newPoints[][] = null;
                    if (approxContour2f.total() == 4) {
                        newPoints = new double[4][2];
                        List<Point> dots = approxContour2f.toList();
                        for (int i = 0; i < 4; i++) {
                            newPoints[i][0] = dots.get(i).x;
                            newPoints[i][1] = dots.get(i).y;
                        }
                    }
                    synchronized (gridLock) {
                        gridImage = toGrid;
                        points = newPoints;
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
        timer.cancel();
        cameraImage.release();
        gridImage.release();
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

        double newPoints[][] = null;
        synchronized (gridLock) {
            if (points != null) {
                newPoints = new double[4][2];
                for (int i = 0; i < 4; i++) {
                    System.arraycopy(points[i], 0, newPoints[i], 0, 2);
                }
            }
        }
        if (newPoints != null) {
            Imgproc.fillConvexPoly(temp, new MatOfPoint(new Point(newPoints[0]), new Point(newPoints[1]),
                    new Point(newPoints[2]), new Point(newPoints[3])), new Scalar(255, 255, 255, .5));
        }

        //show rectangle (test)
        /*try {
            findRectangle(temp);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }*/

        //show hough line count
        /*Mat gray = new Mat();
        Imgproc.cvtColor(temp, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(15,15), 0);

        Mat edgeImage=new Mat();
        Imgproc.Canny(gray, edgeImage, 150, 300);

        temp = edgeImage;
        Mat lines = new Mat();
        int threshold = 200;
        Imgproc.HoughLinesP(edgeImage, lines, 1, Math.PI/180, threshold,100,60);
        ArrayList<Point> flexCorners=new ArrayList<Point>();

        Log.d(TAG, "lines.size"+lines.size());
        //Find the intersection of the four lines to get the four corners
        for (int i = 0; i < lines.cols(); i++)
        {
            for (int j = i+1; j < lines.cols(); j++)
            {
                Point intersectionPoint=getLinesIntersection(lines.get(0, i), lines.get(0, j))	;
                if(intersectionPoint!=null)
                {
                    Log.i(TAG, "intersectionPoint: " + intersectionPoint.x+" "+intersectionPoint.y);
                    flexCorners.add(intersectionPoint);
                }
            }
        }

        MatOfPoint2f cornersMat=new MatOfPoint2f();
        cornersMat.convertTo(cornersMat, CvType.CV_32S);
        cornersMat.fromList(flexCorners);
        Log.i(TAG, "cornersMat.size:"+cornersMat.size()+"flex size:"+flexCorners.size());
        Log.i(TAG, "cornersMat.depth: "+ cornersMat.depth()+" CV_32F:"+CvType.CV_32F+" CV_32S:"+CvType.CV_32S);*/

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
        
        Mat sampledImage = null;
        double[][] gridPoints = null;
        synchronized (gridLock) {
            if (points != null) {
                gridPoints = new double[4][2];
                for (int i = 0; i < 4; i++) {
                    System.arraycopy(points[i], 0, gridPoints[i], 0, 2);
                }
                sampledImage = gridImage.clone();
            }
        }
        if (sampledImage == null) {
            return false;
        }
        
        List<Point> flexCorners = new ArrayList<>();
        for (double[] point : gridPoints) {
            flexCorners.add(new Point(point[0], point[1]));
        }
        Point centroid = new Point(0,0);

        for(Point point : flexCorners)
        {
            Log.i(TAG, "Point x: "+ point.x+  " Point y: "+ point.y);
            centroid.x+=point.x;
            centroid.y+=point.y;
        }
        centroid.x/=((double)flexCorners.size());
        centroid.y/=((double)flexCorners.size());

        sortCorners(flexCorners,centroid);

        for(Point point:flexCorners)
        {
            Log.i(TAG, "PointAfterSort x: "+ point.x+  " PointAfterSort y: "+ point.y);
            Imgproc.circle(sampledImage, point, (int) 10, new Scalar(0,0,255),2);
        }

        Mat correctedImage = new Mat(sampledImage.rows(), sampledImage.cols(), sampledImage.type());
        Mat srcPoints=Converters.vector_Point2f_to_Mat(flexCorners);

        Mat destPoints=Converters.vector_Point2f_to_Mat(Arrays.asList(
                new Point(30, 30),
                new Point(correctedImage.cols()-30, 30),
                new Point(correctedImage.cols()-30,correctedImage.rows()-30),
                new Point(30,correctedImage.rows()-30)));

        Mat transformation = Imgproc.getPerspectiveTransform(srcPoints, destPoints);
        Imgproc.warpPerspective(sampledImage, correctedImage, transformation, correctedImage.size());
        long addr = correctedImage.getNativeObjAddr();
        Intent intent = new Intent(this, Edit_Sudoku_Activity.class);
        intent.putExtra( "myImg", addr );
        startActivity( intent );
        
        return false;
    }
    
    private void sortCorners(List<Point> corners, Point center)
    {
        ArrayList<Point> top=new ArrayList<Point>();
        ArrayList<Point> bottom=new ArrayList<Point>();

        for (int i = 0; i < corners.size(); i++)
        {
            if (corners.get(i).y < center.y)
                top.add(corners.get(i));
            else
                bottom.add(corners.get(i));
        }

        double topLeft=top.get(0).x;
        int topLeftIndex=0;
        for(int i=1;i<top.size();i++)
        {
            if(top.get(i).x<topLeft)
            {
                topLeft=top.get(i).x;
                topLeftIndex=i;
            }
        }

        double topRight=0;
        int topRightIndex=0;
        for(int i=0;i<top.size();i++)
        {
            if(top.get(i).x>topRight)
            {
                topRight=top.get(i).x;
                topRightIndex=i;
            }
        }

        double bottomLeft=bottom.get(0).x;
        int bottomLeftIndex=0;
        for(int i=1;i<bottom.size();i++)
        {
            if(bottom.get(i).x<bottomLeft)
            {
                bottomLeft=bottom.get(i).x;
                bottomLeftIndex=i;
            }
        }

        double bottomRight=bottom.get(0).x;
        int bottomRightIndex=0;
        for(int i=1;i<bottom.size();i++)
        {
            if(bottom.get(i).x>bottomRight)
            {
                bottomRight=bottom.get(i).x;
                bottomRightIndex=i;
            }
        }

        Point topLeftPoint = top.get(topLeftIndex);
        Point topRightPoint = top.get(topRightIndex);
        Point bottomLeftPoint = bottom.get(bottomLeftIndex);
        Point bottomRightPoint = bottom.get(bottomRightIndex);

        corners.clear();
        corners.add(topLeftPoint);
        corners.add(topRightPoint);
        corners.add(bottomRightPoint);
        corners.add(bottomLeftPoint);
    }

    private void findRectangle(Mat src) throws Exception {
        Mat blurred = src.clone();
        Imgproc.medianBlur(src, blurred, 9);

        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        List<Mat> blurredChannel = new ArrayList<Mat>();
        blurredChannel.add(blurred);
        List<Mat> gray0Channel = new ArrayList<Mat>();
        gray0Channel.add(gray0);

        MatOfPoint2f approxCurve;

        double maxArea = 0;
        int maxId = -1;

        for (int c = 0; c < 3; c++) {
            int ch[] = { c, 0 };
            Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

            int thresholdLevel = 1;
            for (int t = 0; t < thresholdLevel; t++) {
                if (t == 0) {
                    Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                    // ?
                } else {
                    Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                            Imgproc.THRESH_BINARY,
                            (src.width() + src.height()) / 200, t);
                }

                Imgproc.findContours(gray, contours, new Mat(),
                        Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                for (MatOfPoint contour : contours) {
                    MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                    double area = Imgproc.contourArea(contour);
                    approxCurve = new MatOfPoint2f();
                    Imgproc.approxPolyDP(temp, approxCurve,
                            Imgproc.arcLength(temp, true) * 0.02, true);

                    if (approxCurve.total() == 4 && area >= maxArea) {
                        double maxCosine = 0;

                        List<Point> curves = approxCurve.toList();
                        for (int j = 2; j < 5; j++) {

                            double cosine = Math.abs(angle(curves.get(j % 4),
                                    curves.get(j - 2), curves.get(j - 1)));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.3) {
                            maxArea = area;
                            maxId = contours.indexOf(contour);
                        }
                    }
                }
            }
        }

        if (maxId >= 0) {
            Imgproc.drawContours(src, contours, maxId, new Scalar(255, 0, 0,
                    .8), 8);

        }
    }

    private double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }

    private Point getLinesIntersection(double [] firstLine, double [] secondLine)
    {
        double FX1=firstLine[0],FY1=firstLine[1],FX2=firstLine[2],FY2=firstLine[3];
        double SX1=secondLine[0],SY1=secondLine[1],SX2=secondLine[2],SY2=secondLine[3];
        Point intersectionPoint=null;
        //Make sure the we will not divide by zero
        double denominator=(FX1-FX2)*(SY1-SY2)-(FY1-FY2)*(SX1-SX2);
        if(denominator!=0)
        {
            intersectionPoint=new Point();
            intersectionPoint.x=((FX1*FY2-FY1*FX2)*(SX1-SX2)-(FX1-FX2)*(SX1*SY2-SY1*SX2))/denominator;
            intersectionPoint.y=((FX1*FY2-FY1*FX2)*(SY1-SY2)-(FY1-FY2)*(SX1*SY2-SY1*SX2))/denominator;
            if(intersectionPoint.x<0 || intersectionPoint.y<0)
                return null;
        }
        return intersectionPoint;
    }

}
