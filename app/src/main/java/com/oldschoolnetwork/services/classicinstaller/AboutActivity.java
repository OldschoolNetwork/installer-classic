package com.oldschoolnetwork.services.classicinstaller;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Убираем системный Action Bar / Title Bar, чтобы освободить место сверху
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_about);
        ((android.widget.ImageView)findViewById(R.id.aboutBrandLogo)).setImageDrawable(androidx.vectordrawable.graphics.drawable.VectorDrawableCompat.create(getResources(),R.drawable.on_logo_full,getTheme()));
        ((android.widget.ImageView)findViewById(R.id.aboutServicesLogo)).setImageDrawable(androidx.vectordrawable.graphics.drawable.VectorDrawableCompat.create(getResources(),R.drawable.ic_services_logo_notext,getTheme()));

        findViewById(R.id.btn_website).setOnClickListener(v -> { try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://oldschoolnetwork.online/"))); } catch (android.content.ActivityNotFoundException ignored) {} });
        // Bind Back Button
        // REFERENCE: res/layout/activity_about.xml
        Button btnBack = findViewById(R.id.btn_back);

        // Handle click event to close this Activity and return
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Closes AboutActivity and returns to MainActivity
            }
        });
    }
}
