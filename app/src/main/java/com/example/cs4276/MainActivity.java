package com.example.cs4276;

import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    // Used for logging on logcat
    private static final String TAG = "MainActivity";

    // Try to write to a file
    FileWriter writer;
    String fileDir;

    // Variables for Sensors
    private TextView x_gyro, y_gyro, z_gyro;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private SensorEventListener gyroscopeEventListener;

    // Variables for XML elements UI display
    private Button startBtn;
    private Spinner spinner;
    private boolean isStartPressed;
    private boolean isSetPressed;
    private EditText mEditTextInput;
    private Button mButtonSet;

    private TextView mTextViewCountDown;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mStartTimeInMillis;
    private long mTimeLeftInMillis;

    // Firebase storage
    private StorageReference mStorageRef;
    private Button selectBtn;
    private Button sendBtn;
    private Uri filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        x_gyro = findViewById(R.id.xGyro);
        y_gyro = findViewById(R.id.yGyro);
        z_gyro = findViewById(R.id.zGyro);
        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mEditTextInput = findViewById(R.id.mEditTimer);
        mButtonSet = findViewById(R.id.setBtn);
        spinner = findViewById(R.id.refresh_spinner);
        startBtn = findViewById(R.id.startBtn);

        // Firebase storage
        mStorageRef = FirebaseStorage.getInstance("gs://cs4276-6a376.appspot.com").getReference();

        mButtonSet.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String input = mEditTextInput.getText().toString();
                if (input.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please enter a value", Toast.LENGTH_SHORT).show();
                    return;
                }

                long millisInput = Long.parseLong(input) * 1000;
                if (millisInput == 0) {
                    Toast.makeText(MainActivity.this, "Please enter positive number", Toast.LENGTH_SHORT).show();
                    return;
                }
                isSetPressed = true;
                setTimer(millisInput);
                mEditTextInput.setText("");
                closeKeyboard();


            }
        });

        // Initializing gyroscope components
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        try {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        } catch (NullPointerException npe) {
            Toast toast = Toast.makeText(this, "This device does not support Gyroscope", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        gyroscopeEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                if (sensor.getType() == Sensor.TYPE_GYROSCOPE && isStartPressed && mTimerRunning && isSetPressed) {
                    Log.d(TAG, "onSensorChanged: X: " + event.values[0] + "Y: " + event.values[1] + "Z: " + event.values[2]);
                    x_gyro.setText("X: " + event.values[0]);
                    y_gyro.setText("Y: " + event.values[1]);
                    z_gyro.setText("Z: " + event.values[2]);

                    try {
                        writer.write(String.format("%s, %f, %f, %f\n", SystemClock.elapsedRealtimeNanos(), event.values[0], event.values[1], event.values[2]));
                        writer.flush();
                    } catch (IOException io) {
                        Log.d(TAG, "Input output exception!" + io);
                    }
                } else {
                    // Display error. Click on start button to use
                    x_gyro.setText("");
                    y_gyro.setText("Please press start button to use");
                    z_gyro.setText("");
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        // Setting up for buttons
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(isStartPressed && mTimerRunning && isSetPressed)) {
                    Log.d(TAG, "Writing to " + getStorageDir());
                    // Creating date format
                    DateFormat simple = new SimpleDateFormat("ddMMyyyy_HHmmss");
                    Date currDate = new Date(System.currentTimeMillis());
                    try {
                        fileDir = getStorageDir() + "/gyro_100hz" + simple.format(currDate) + ".csv";
                        Log.d(TAG, "This is the filedir to be saved: " + fileDir);
                        writer = new FileWriter(new File(fileDir));
                        Log.d(TAG, "Successfully created writer");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    isStartPressed = true;
                    startTimer();
                    mButtonSet.setEnabled(false);
                    startBtn.setEnabled(false);
                }

            }
        });

        // Setting up for spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(MainActivity.this, R.array.refresh_rate, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String option = parent.getItemAtPosition(position).toString();
                // Unregistering the listener before setting a new refresh rate
                sensorManager.unregisterListener(gyroscopeEventListener);

                // Todo: Add customized frequency based on requirements
                switch (option) {
                    case "UI": // 16.667 Hz
                        sensorManager.registerListener(gyroscopeEventListener, gyroscope, SensorManager.SENSOR_DELAY_UI);
                        break;
                    case "Normal": // 5Hz
                        sensorManager.registerListener(gyroscopeEventListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
                    case "Game": // 50hz
                        sensorManager.registerListener(gyroscopeEventListener, gyroscope, SensorManager.SENSOR_DELAY_GAME);
                    case "Fastest": // 100hz
                        sensorManager.registerListener(gyroscopeEventListener, gyroscope, 10000);
                    default:
                        sensorManager.registerListener(gyroscopeEventListener, gyroscope, SensorManager.SENSOR_DELAY_UI);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

    }

    private void resetTimer() {
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
    }

    private void setTimer(long milliseconds) {
        mStartTimeInMillis = milliseconds;
        resetTimer();
    }
    private void startTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                isSetPressed = false;
                isStartPressed = false;
                startBtn.setText("Start");
                mButtonSet.setEnabled(true);
                startBtn.setEnabled(true);
                try {
                    writer.close();
                } catch (IOException io) {
                    Log.e(TAG, "Error in IO when closing writer");
                }
                Log.d(TAG, "This is my filedir " + fileDir);
                Uri file = Uri.fromFile(new File(fileDir));
                StorageReference csvRef = mStorageRef.child("blue_huawei/" + "100hz/" + file.getLastPathSegment());
                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setTitle("Progress...");

                csvRef.putFile(file)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Toast.makeText(MainActivity.this, "Successful", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                Toast.makeText(MainActivity.this, "Not Successful", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                //calculating progress percentage
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                                //displaying percentage in progress dialog
                                progressDialog.setMessage("Uploaded " + (int)(progress) + "%...");
                                progressDialog.show();
                            }
                        });
            }
        }.start();

        mTimerRunning = true;
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        mTextViewCountDown.setText(timeLeftFormatted);
    }
    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // DELAY refers to the refresh rate
    // Default = UI setting
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(gyroscopeEventListener, gyroscope, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(gyroscopeEventListener);
    }

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
    }
}
