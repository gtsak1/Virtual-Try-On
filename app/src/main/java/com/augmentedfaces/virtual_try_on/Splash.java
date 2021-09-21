package com.augmentedfaces.virtual_try_on;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash);

        new Handler().postDelayed(() -> {
            Intent AugmFacAct = new Intent(this, VirtualTryOnActivity.class);
            startActivity(AugmFacAct);
            finish();
        }, 400);

    }

}
