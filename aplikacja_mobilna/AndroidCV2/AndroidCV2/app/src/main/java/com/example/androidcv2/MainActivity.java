package com.example.androidcv2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import static com.example.androidcv2.BluetoothService.getIsConnected;

public class MainActivity extends AppCompatActivity {

    Mat frame, result;
    private static final int FILE_SELECT_CODE = 1;
    Button pick_video, send_button;
    ImageView image_view;
    SeekBar hue_min, sat_min, val_min, hue_max, sat_max, val_max;
    Bitmap b;
    Scalar scalar_low, scalar_high;
    static BluetoothAdapter adapter;
    static BluetoothDevice bluetoothDevice;
    BluetoothService service;
    int sendCounter = 0;
    CheckBox checkBox;
    Uri path;
    static Boolean isSend = false;
    static Boolean response = false;
    Boolean sending_error = false;
    ProgressBar progressBar;
    ProgressBar circleBar;
    static Boolean isResult = false;
    ArrayList<String> ball_lost1;
    ArrayList<String> ball_lost2;
    public static Activity MA;
    Boolean pickVideo = false;
    public static Boolean fatalError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        MA = this;
        pick_video = findViewById(R.id.pick_button);
        image_view = findViewById(R.id.imageView);
        hue_min = findViewById(R.id.hue_min);
        hue_max = findViewById(R.id.hue_max);
        sat_min = findViewById(R.id.sat_min);
        sat_max = findViewById(R.id.sat_max);
        val_min = findViewById(R.id.val_min);
        val_max = findViewById(R.id.val_max);
        send_button = findViewById(R.id.bluetooth_button);
        checkBox = findViewById(R.id.checkBox);
        progressBar = findViewById(R.id.progressBar);
        circleBar = findViewById(R.id.circleBar);

        scalar_low = new Scalar(hue_min.getProgress(), sat_min.getProgress(), val_min.getProgress());
        scalar_high = new Scalar(hue_max.getProgress(), sat_max.getProgress(), val_max.getProgress());
        OpenCVLoader.initDebug();

        pick_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    return;
                }

                showFileChooser();
                pickVideo = true;
            }
        });
        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(send_button.getText().equals("Search")){
                    open_bluetooth_activity();
                }
                else if(send_button.getText().equals("Connect")){
                    pick_video.setVisibility(View.INVISIBLE);
                    UUID uuid = UUID.fromString("58723436-5452-11eb-ae93-0242ac130002");
                    service = new BluetoothService(getApplicationContext(), adapter);
                    service.startClient(bluetoothDevice, uuid);
                    long startTime = System.currentTimeMillis();
                    while(!getIsConnected()){
                        double difference = (System.currentTimeMillis() - startTime)/1000;
                        if(difference > 10){
                            Toast.makeText(getApplicationContext(), "Connection timeout", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                    if(getIsConnected()){
                        send_button.setText("Send");
                    }
                    Toast.makeText(getApplicationContext(), "Connect", Toast.LENGTH_SHORT).show();
                }
                else{

                    sendCounter = sendCounter + 1;

                    if (sendCounter >= 2){
                        checkBox.setVisibility(View.VISIBLE);
                        pick_video.setVisibility(View.INVISIBLE);
                    }
                    else{
                        checkBox.setVisibility(View.INVISIBLE);
                    }

                    if(sendCounter >= 3){
                        String plainColor;
                        if(checkBox.isChecked()){
                            plainColor = "001";
                        }
                        else{
                            plainColor = "000";
                        }
                        service.write(plainColor.getBytes(StandardCharsets.UTF_8));
                        checkSendingStatus();
                    }

                    if(sendCounter <= 4) {

                        String hue_m = format(String.valueOf(hue_min.getProgress()));
                        String sat_m = format(String.valueOf(sat_min.getProgress()));
                        String val_m = format(String.valueOf(val_min.getProgress()));
                        String hue_M = format(String.valueOf(hue_max.getProgress()));
                        String sat_M = format(String.valueOf(sat_max.getProgress()));
                        String val_M = format(String.valueOf(val_max.getProgress()));

                        hue_min.setProgress(0);
                        sat_min.setProgress(0);
                        val_min.setProgress(0);
                        hue_max.setProgress(359);
                        sat_max.setProgress(255);
                        val_max.setProgress(255);

                        image_view.setImageBitmap(b);

                        service.write(hue_m.getBytes(StandardCharsets.UTF_8));
                        checkSendingStatus();
                        service.write(sat_m.getBytes(StandardCharsets.UTF_8));
                        checkSendingStatus();
                        service.write(val_m.getBytes(StandardCharsets.UTF_8));
                        checkSendingStatus();
                        service.write(hue_M.getBytes(StandardCharsets.UTF_8));
                        checkSendingStatus();
                        service.write(sat_M.getBytes(StandardCharsets.UTF_8));
                        checkSendingStatus();
                        service.write(val_M.getBytes(StandardCharsets.UTF_8));
                        checkSendingStatus();

                        if (!sending_error) {
                            Toast.makeText(getApplicationContext(), "Write successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Write error, try again or check program for video analysis", Toast.LENGTH_SHORT).show();
                            sendCounter = sendCounter - 1;
                            sending_error = false;
                        }
                    }

                    if(sendCounter == 4){
                        send_button.setText("Send video");
                    }

                    if(sendCounter > 4){
                        image_view.setVisibility(View.GONE);
                        progressBar.setProgress(20);
                        progressBar.setVisibility(View.VISIBLE);
                        sendCounter = 0;

                        BluetoothThread bThread = new BluetoothThread();
                        bThread.start();
                    }
                }

            }
        });

        hue_min.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scalar_low = new Scalar(progress, sat_min.getProgress(), val_min.getProgress());
                image_change();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        hue_max.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scalar_high = new Scalar(progress, sat_max.getProgress(), val_max.getProgress());
                image_change();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        sat_min.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scalar_low = new Scalar(hue_min.getProgress(), progress, val_min.getProgress());
                image_change();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        sat_max.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scalar_high = new Scalar(hue_max.getProgress(), progress, val_max.getProgress());
                image_change();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        val_min.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scalar_low = new Scalar(hue_min.getProgress(), sat_min.getProgress(), progress);
                image_change();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        val_max.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scalar_high = new Scalar(hue_max.getProgress(), sat_max.getProgress(), progress);
                image_change();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private ArrayList<String> receiveResults() {
        byte[] buffer = new byte[1024];
        int receivedBytes = 0;
        while(receivedBytes == 0){
            receivedBytes = service.read(buffer);
        }
        System.out.println("out");
        service.write("1".getBytes(StandardCharsets.UTF_8));
        String messageLength = new String(buffer, 0, receivedBytes);
        System.out.println(messageLength);
        int amountOfElements = Integer.parseInt(messageLength);

        ArrayList<String> stringArrayList = new ArrayList<String>();
        String position = "";

        for(int i = 0; i < amountOfElements; i++) {
            buffer = new byte[10];
            receivedBytes = 0;
            while (receivedBytes == 0) {
                receivedBytes = service.read(buffer);
            }
            service.write("1".getBytes(StandardCharsets.UTF_8));
            String message = new String(buffer, 0, receivedBytes);
            System.out.println(message);
            if((i+1)%3==0){
                position = position + message;
                stringArrayList.add(position);
                System.out.println("sal " + stringArrayList);
                position = "";
            }
            else{
                position = position + message + " ";
            }

        }
        return stringArrayList;
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/mp4");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                path = data.getData();
                MediaMetadataRetriever med = new MediaMetadataRetriever();
                med.setDataSource(this, path);
                b = med.getFrameAtTime(1);
            }
        }
    }

    public void open_bluetooth_activity(){
        Intent intent = new Intent(getApplicationContext(), BluetoothActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        image_view.setImageBitmap(b);
        hue_min.setProgress(0);
        sat_min.setProgress(0);
        val_min.setProgress(0);
        hue_max.setProgress(359);
        sat_max.setProgress(255);
        val_max.setProgress(255);
        Boolean connected = getIntent().getBooleanExtra("CONNECTED", false);
        if(connected){
            send_button.setText("Connect");
            pick_video.setVisibility(View.VISIBLE);
            if(!pickVideo) {
                send_button.setVisibility(View.INVISIBLE);
            }
            else{
                send_button.setVisibility(View.VISIBLE);
            }
        }
    }

    private void image_change() {
        if(b != null){
            frame = new Mat(image_view.getHeight(),  image_view.getWidth(), CvType.CV_8UC3);
            result = new Mat(image_view.getHeight(),  image_view.getWidth(), CvType.CV_8UC3);
            Utils.bitmapToMat(b, frame);

            Imgproc.cvtColor(frame, result, Imgproc.COLOR_RGB2HSV_FULL, 3);
            Core.inRange(result, scalar_low, scalar_high, result);

            Bitmap bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(result, bitmap);
            image_view.setImageBitmap(bitmap);

            System.out.println(scalar_low);
            System.out.println(scalar_high);
        }
    }
    public static void setAdapter(BluetoothAdapter bluetoothAdapter){
        adapter = bluetoothAdapter;
    }

    public static void setBluetoothDevice(BluetoothDevice device){
        bluetoothDevice = device;
    }

    String format(String string){
        if(string.length() == 2){
            string = "0" + string;
        }
        if(string.length() == 1){
            string = "00" + string;
        }
        return string;
    }

    public static void setIsSend(Boolean b){
        isSend = b;
        response = true;
    }

    public static void setIsResult(Boolean b){
        isResult = b;
    }


    public void checkSendingStatus(){
        long startTime = System.currentTimeMillis();
        while(!response){
            double difference = (System.currentTimeMillis() - startTime)/1000;
            if(difference > 2){
                Toast.makeText(getApplicationContext(), "Send timeout", Toast.LENGTH_SHORT).show();
                sending_error = true;
                break;
            }
        }
        response = false;
        if(!isSend){
            sending_error = true;
        }
    }

    private class BluetoothThread extends Thread {

        public void run() {
            try {
                InputStream inputStream = getContentResolver().openInputStream(path);
                int bytesAvailable = inputStream.available();
                int allBytes = bytesAvailable;
                byte[] buffer = String.valueOf(allBytes).getBytes(StandardCharsets.UTF_8);
                Boolean success = false;
                while (!success) {
                    service.write(buffer);
                    long startTime = System.currentTimeMillis();
                    while (!response) {
                        double difference = (System.currentTimeMillis() - startTime) / 1000;
                        if (difference > 10) {
                            Toast.makeText(getApplicationContext(), "Send timeout", Toast.LENGTH_SHORT).show();
                            isSend = false;
                            break;
                        }
                    }
                    response = false;
                    if (isSend) {
                        success = true;
                    }
                }

                while (bytesAvailable > 0) {
                    int buffSize = Math.min(1024, bytesAvailable);
                    buffer = new byte[buffSize];
                    int readBytes = 0;
                    readBytes = inputStream.read(buffer);
                    service.write(buffer);
                    long startTime = System.currentTimeMillis();
                    while (!response) {

                        double difference = (System.currentTimeMillis() - startTime) / 1000;
                        if (difference > 10) {
                            Toast.makeText(getApplicationContext(), "Send timeout", Toast.LENGTH_SHORT).show();
                            isSend = false;
                            break;
                        }
                    }
                    if (isSend) {
                        bytesAvailable = bytesAvailable - readBytes;
                        final float percentage = ((float) bytesAvailable / (float) allBytes) * 100;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress((int) percentage);
                            }
                        });
                        System.out.println("ProgressBar");
                        System.out.println("percentage " + percentage + " allBytes " + allBytes + " bytesAvailable " + bytesAvailable);
                    }
                    response = false;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Sending complete", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Send video error", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Send video error", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.GONE);
                    circleBar.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(), "Match is analyzing, please be patient", Toast.LENGTH_LONG).show();
                }
            });

            // waiting for results
            while (!isResult) {
                if(fatalError){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Fatal error", Toast.LENGTH_SHORT).show();
                            try {
                                Thread.sleep(1000); // time to user can read toast
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            System.exit(1);
                        }
                    });
                }
            }
            service.write("1".getBytes(StandardCharsets.UTF_8));
            ball_lost1 = receiveResults();
            ball_lost2 = receiveResults();
            openDisplayActivity();
            return;
        }
    }

    public void openDisplayActivity(){
        Intent intent = new Intent(getApplicationContext(), DisplayActivity.class);
        intent.putExtra("ball_lost1", ball_lost1);
        intent.putExtra("ball_lost2", ball_lost2);
        startActivity(intent);
    }

}
