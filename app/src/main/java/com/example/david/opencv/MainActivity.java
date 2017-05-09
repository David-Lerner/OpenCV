package com.example.david.opencv;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Example of a call to a native method
        String text = "";
        TextView tv = (TextView) findViewById(R.id.sample_text);
        text += stringFromJNI();
        tv.setText(text);

        Button BrightnessActivity = (Button) findViewById(R.id.bt_brightness_activity);
        BrightnessActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                launchBrightnessActivity();
            }
        });

        Button Puzzle15Activity = (Button) findViewById(R.id.bt_puzzle15_activity);
        Puzzle15Activity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                launchPuzzle15Activity();
            }
        });

        Button CameraCalibrationActivity = (Button) findViewById(R.id.bt_calibration_activity);
        CameraCalibrationActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                launchCameraCalibrationActivity();
            }
        });

        Button ColorBlobDetectionActivity = (Button) findViewById(R.id.bt_color_blob_activity);
        ColorBlobDetectionActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                launchColorBlobDetectionActivity();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchBrightnessActivity() {

        Intent intent = new Intent(this, ImageBrightness.class);
        startActivity(intent);
    }

    private void launchPuzzle15Activity() {

        Intent intent = new Intent(this, Puzzle15Activity.class);
        startActivity(intent);
    }

    private void launchCameraCalibrationActivity() {

        Intent intent = new Intent(this, CameraCalibrationActivity.class);
        startActivity(intent);
    }

    private void launchColorBlobDetectionActivity() {

        Intent intent = new Intent(this, ColorBlobDetectionActivity.class);
        startActivity(intent);
    }

    private static final String TAG = "MainActivity";

    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");

        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }
}
