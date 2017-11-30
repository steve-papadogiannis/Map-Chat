package com.steve.mobilegcm.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;
import com.steve.mobilegcm.utils.Config;
import com.steve.mobilegcm.R;
import com.steve.mobilegcm.crypto.SHA512;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class Register extends Custom {

	private EditText usernameEditText, passwordEditText, passwordAgainEditText;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        setTouchNClick(R.id.register_button);
        setTouchNClick3(R.id.login_link_button);
		usernameEditText = (EditText) findViewById(R.id.username_edit_text);
		passwordEditText = (EditText) findViewById(R.id.password_edit_text);
        passwordAgainEditText = (EditText) findViewById(R.id.password_again_edit_text);
	}

    public boolean isConnectingToInternet() {
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (NetworkInfo anInfo : info)
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED)
                        return true;
        }
        return false;
    }

    public void showAlertDialog(Context context, String title, String message, Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        if (status != null)
            alertDialog.setIcon((status) ? R.drawable.success : R.drawable.fail);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

            }

        });
        alertDialog.show();
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if ( v.getId() == R.id.register_button ) {
            if ( !isConnectingToInternet() ) {
                showAlertDialog(Register.this, "Δεν υπάρχει σύνδεση στο internet", "Παρακαλώ συνδεθείτε στο internet", false);
            }
            else {
                register();
            }
        }
        else {
            startActivity(new Intent(this, Login.class));
            finish();
        }
    }

    private void register() {
        final String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String passwordAgain = passwordAgainEditText.getText().toString().trim();
        // Validate the register data
        boolean validationError = false;
        StringBuilder validationErrorMessage = new StringBuilder(getString(R.string.error_intro));
        if (username.length() == 0) {
            validationError = true;
            validationErrorMessage.append(getString(R.string.error_blank_username));
        }
        if (password.length() == 0) {
            if (validationError) {
                validationErrorMessage.append(getString(R.string.error_join));
            }
            validationError = true;
            validationErrorMessage.append(getString(R.string.error_blank_password));
        }
        if (!password.equals(passwordAgain)) {
            if (validationError) {
                validationErrorMessage.append(getString(R.string.error_join));
            }
            validationError = true;
            validationErrorMessage.append(getString(R.string.error_mismatched_passwords));
        }
        validationErrorMessage.append(getString(R.string.error_end));
        // If there is a validation error, display the error
        if (validationError) {
            Toast.makeText(Register.this, validationErrorMessage.toString(), Toast.LENGTH_LONG).show();
        }
        else {
            try {
                password = SHA512.sha512(password);
            }
            catch (NoSuchAlgorithmException e) {
                Log.e(Config.TAG, "Ο sha512 δεν μπόρεσε να εκτελεσθεί");
            }
            catch (UnsupportedEncodingException e) {
                Log.e(Config.TAG, "Δεν υποστηρίζεται το encoding του κωδικού");
            }
            final ProgressDialog progressDialog = new ProgressDialog(Register.this);
            progressDialog.setMessage(getString(R.string.progress_register));
            progressDialog.show();
            final ParseUser user = new ParseUser();
            user.setPassword(password);
            user.setUsername(username);
            // Call the Parse signup method
            user.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    progressDialog.dismiss();
                    if (e != null) {
                        // Show the error message
                        Toast.makeText(Register.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Intent intent = new Intent(Register.this, Map.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }
    }
}
