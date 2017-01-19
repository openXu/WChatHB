package com.openxu.hb;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhb);

        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    public void start(View v) {
        if (AirAccessibilityService.ALL) {
            AirAccessibilityService.ALL = false;
            ((Button) v).setText("对话内监控+关");
        } else {
            AirAccessibilityService.ALL = true;
            ((Button) v).setText("对话内监控+开");
        }
    }
}
