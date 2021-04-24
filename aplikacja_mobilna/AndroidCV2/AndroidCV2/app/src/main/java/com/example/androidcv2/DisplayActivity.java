package com.example.androidcv2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class DisplayActivity extends AppCompatActivity {

    ListView listViewSide1, listViewZone1, listViewTime1, listViewSide2, listViewZone2, listViewTime2;
    Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        listViewSide1 = findViewById(R.id.side1);
        listViewZone1 = findViewById(R.id.zone1);
        listViewTime1 = findViewById(R.id.time1);
        listViewSide2 = findViewById(R.id.side2);
        listViewZone2 = findViewById(R.id.zone2);
        listViewTime2 = findViewById(R.id.time2);
        exitButton = findViewById(R.id.exitButton);

        ArrayList<String> ball_lost1 = (ArrayList<String>) getIntent().getSerializableExtra("ball_lost1");
        ArrayList<String> ball_lost2 = (ArrayList<String>) getIntent().getSerializableExtra("ball_lost2");

        ArrayList<String> listSide1 = new ArrayList<String>();
        ArrayList<String> listSide2 = new ArrayList<String>();
        ArrayList<String> listZone1 = new ArrayList<String>();
        ArrayList<String> listZone2 = new ArrayList<String>();
        ArrayList<String> listTime1 = new ArrayList<String>();
        ArrayList<String> listTime2 = new ArrayList<String>();

        for(int i=0; i < ball_lost1.size(); i++){
            String[] properties = ball_lost1.get(i).split(" ");
            properties[0] = formatZone(properties[0]);
            listZone1.add(properties[0]);
            properties[1] = formatSide(properties[1]);
            listSide1.add(properties[1]);
            listTime1.add(properties[2]);
        }

        for(int i=0; i < ball_lost2.size(); i++){
            String[] properties = ball_lost2.get(i).split(" ");
            properties[0] = formatZone(properties[0]);
            listZone2.add(properties[0]);
            properties[1] = formatSide(properties[1]);
            listSide2.add(properties[1]);
            listTime2.add(properties[2]);
        }

        ArrayAdapter<String> arrayAdapterZone1 = new ArrayAdapter<String>(getApplicationContext(), R.layout.custom_simple_list_item, listZone1);
        listViewZone1.setAdapter(arrayAdapterZone1);

        ArrayAdapter<String> arrayAdapterSide1 = new ArrayAdapter<String>(getApplicationContext(), R.layout.custom_simple_list_item, listSide1);
        listViewSide1.setAdapter(arrayAdapterSide1);

        ArrayAdapter<String> arrayAdapterTime1 = new ArrayAdapter<String>(getApplicationContext(), R.layout.custom_simple_list_item, listTime1);
        listViewTime1.setAdapter(arrayAdapterTime1);

        ArrayAdapter<String> arrayAdapterZone2 = new ArrayAdapter<String>(getApplicationContext(), R.layout.custom_simple_list_item, listZone2);
        listViewZone2.setAdapter(arrayAdapterZone2);

        ArrayAdapter<String> arrayAdapterSide2 = new ArrayAdapter<String>(getApplicationContext(), R.layout.custom_simple_list_item, listSide2);
        listViewSide2.setAdapter(arrayAdapterSide2);

        ArrayAdapter<String> arrayAdapterTime2 = new ArrayAdapter<String>(getApplicationContext(), R.layout.custom_simple_list_item, listTime2);
        listViewTime2.setAdapter(arrayAdapterTime2);

        MainActivity.MA.finish();
        BluetoothActivity.BA.finish();

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(1);
            }
        });
    }

    private String formatZone(String zone){
        if(zone.equals("-1")){
            zone = "down";
        }
        else if(zone.equals("1")){
            zone = "up";
        }
        else{
            zone = "center";
        }
        return zone;
    }

    private String formatSide(String side){
        if(side.equals("-1")){
            side = "left";
        }
        else{
            side = "right";
        }
        return side;
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(getApplicationContext(),"There is no back action",Toast.LENGTH_LONG).show();
        return;
    }

}
