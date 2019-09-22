package com.example.cs4276;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    
    //Todo: Implement a timer to capture traffic
    //Todo: Implement Firebase to sync this data online

    // Used for logging on logcat
    private static final String TAG = "MainActivity";

    private TextView x_gyro, y_gyro, z_gyro;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private SensorEventListener gyroscopeEventListener;
    private Button startBtn;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        // Initializing TextView
        x_gyro =  findViewById(R.id.xGyro);
        y_gyro =  findViewById(R.id.yGyro);
        z_gyro =  findViewById(R.id.zGyro);

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
                if(sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    Log.d(TAG, "onSensorChanged: X: " + event.values[0] + "Y: " + event.values[1] + "Z: " + event.values[2]);
                    x_gyro.setText("X: "+ event.values[0]);
                    y_gyro.setText("Y: "+ event.values[1]);
                    z_gyro.setText("Z: "+ event.values[2]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        // Setting up for buttons
        startBtn = findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Toast.makeText(getApplicationContext(), "The button works!", Toast.LENGTH_SHORT).show();
            }
        });

        // Setting up for spinner
        spinner = findViewById(R.id.refresh_spinner);
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
