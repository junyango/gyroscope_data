package com.example.cs4276;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.CountDownTimer;
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

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //Todo: Implement Firebase to sync this data online

    // Used for logging on logcat
    private static final String TAG = "MainActivity";

    // Variables for Sensors
    private TextView x_gyro, y_gyro, z_gyro;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private SensorEventListener gyroscopeEventListener;

    // Variables for XML elements UI display
    private Button startBtn;
    private Spinner spinner;
    private boolean flagBtn;
    private EditText mEditTextInput;
    private Button mButtonSet;

    private TextView mTextViewCountDown;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mStartTimeInMillis;
    private long mTimeLeftInMillis;


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
                if (sensor.getType() == Sensor.TYPE_GYROSCOPE && flagBtn && mTimerRunning) {
                    Log.d(TAG, "onSensorChanged: X: " + event.values[0] + "Y: " + event.values[1] + "Z: " + event.values[2]);
                    x_gyro.setText("X: " + event.values[0]);
                    y_gyro.setText("Y: " + event.values[1]);
                    z_gyro.setText("Z: " + event.values[2]);
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
                if (flagBtn && mTimerRunning) {
                    flagBtn = false;
                    pauseTimer();
                } else {
                    flagBtn = true;
                    startTimer();
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
                    case "Fastest": // 0 Microseconds delay
                        sensorManager.registerListener(gyroscopeEventListener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
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
            }
        }.start();

        mTimerRunning = true;
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
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
}
