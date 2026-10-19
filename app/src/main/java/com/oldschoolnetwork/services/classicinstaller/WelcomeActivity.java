package com.oldschoolnetwork.services.classicinstaller;
import android.app.Activity;import android.content.Intent;import android.os.Bundle;
public class WelcomeActivity extends Activity { public void onCreate(Bundle state){super.onCreate(state);startActivity(new Intent(this,MainActivity.class));finish();} }
