package com.core.InstantMessaging;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ruby on 8/1/2014.
 */
public class UserListActivity extends ListActivity {
    String signUpUserName;
    String TAG = "UserListActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_list_activity);
        Intent i = getIntent();
        signUpUserName = i.getStringExtra("signUpUserName");
        Log.d(TAG,"Signed up user : " + signUpUserName);
        setTitle("Welcome " + signUpUserName);

        List<String> users = new ArrayList<String>();
        users.add("Ruby");
        users.add("Nitin");
       // users.add("Prof1");
       // users.add("Prof2");

        String [] usersArr = new String [users.size()-1];
        int cnt = 0;
        for (String u : users) {
            if(!signUpUserName.equalsIgnoreCase(u)) {
                usersArr[cnt++] = u;
            }
        }
       // usersArr = users.toArray(usersArr);

        ListView userListView = (ListView) findViewById(android.R.id.list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, usersArr);
        userListView.setAdapter(adapter);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String  toUser = (String) l.getItemAtPosition(position);
        Intent i = new Intent(getApplicationContext(),ChatActivity.class);
        i.putExtra("toUser", toUser);
        i.putExtra("signUpUser",signUpUserName);
        startActivity(i);
        finish();
    }
}
