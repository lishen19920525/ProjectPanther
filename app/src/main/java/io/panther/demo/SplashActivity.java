package io.panther.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Project: ProjectPanther
 * Author: LiShen
 * Time: 2019/4/3 16:57
 */
public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    public void onClick(View v) {
        startActivity(new Intent(this, MainActivity.class));
    }
}