package com.steve.mobilegcm.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.steve.mobilegcm.R;
import com.steve.mobilegcm.utils.Config;

public class UserList extends Custom {

    private ArrayList<ParseUser> userList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_list);
        setTitle("Εγγεγραμμένοι Χρήστες");
	}

    @Override
    protected void onResume() {
        super.onResume();
        loadUserList();
    }

    private void loadUserList() {
        final ProgressDialog progressDialog = ProgressDialog.show(this, null,
                getString(R.string.alert_loading));
        ParseUser.getQuery().whereNotEqualTo("username", ParseUser.getCurrentUser().getUsername()).findInBackground(new FindCallback<ParseUser>() {

            @Override
            public void done(List<ParseUser> li, ParseException e) {
                progressDialog.dismiss();
                if ( li != null ) {
                    if ( li.size() == 0 )
                        Toast.makeText(UserList.this, R.string.msg_no_user_found, Toast.LENGTH_SHORT).show();

                    userList = new ArrayList<ParseUser>(li);
                    ListView list = (ListView) findViewById(R.id.list);
                    list.setAdapter(new UserAdapter());
                    list.setOnItemClickListener(new OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                            startActivity(new Intent(UserList.this, Chat.class).putExtra("username", userList.get(pos).getUsername()));
                        }
                    });
                }
                else {
                    Toast.makeText(getApplicationContext(), "Σφάλμα στην φόρτωση των χρηστών", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class UserAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return userList.size();
        }

        @Override
        public ParseUser getItem(int arg0) {
            return userList.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int pos, View v, ViewGroup arg2) {
            if (v == null)
                v = getLayoutInflater().inflate(R.layout.chat_item, null);
            ParseUser c = getItem(pos);
            TextView lbl = (TextView) v;
            lbl.setText(c.getUsername());
            lbl.setCompoundDrawablesWithIntrinsicBounds(
                    c.getBoolean("online") ? R.drawable.ic_online : R.drawable.ic_offline, 0, R.drawable.arrow, 0);
            return v;
        }

    }

}