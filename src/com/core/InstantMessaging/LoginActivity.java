package com.core.InstantMessaging;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    String signUpUserName;
    String TAG = "LoginActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);


        final EditText signUpUserField = (EditText) findViewById(R.id.signUpUser);

        Button loginButton = (Button) findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
                signUpUserName = signUpUserField.getText().toString();
               Log.d(TAG,"Logged in user : "+signUpUserName);
               Intent i = new Intent(getApplicationContext(),UserListActivity.class);
               i.putExtra("signUpUserName",signUpUserName);
               startActivity(i);
               finish();
           }
       });

    }
}
