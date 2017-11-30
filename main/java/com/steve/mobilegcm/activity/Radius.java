package com.steve.mobilegcm.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.parse.ParseUser;
import com.steve.mobilegcm.app.ChattApp;
import com.steve.mobilegcm.utils.NumberPicker;
import com.steve.mobilegcm.R;

public class Radius extends Custom {

    private NumberPicker numberPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radius);
        setTouchNClick(R.id.map);
        numberPicker = (NumberPicker) findViewById(R.id.numberPicker);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(5000);
        numberPicker.setValue(150);
        numberPicker.setWrapSelectorWheel(false);
    }

    @Override
    public void onClick(View v) {
        ChattApp.setSearchDistance(numberPicker.getValue());
        Intent intent = new Intent(getApplicationContext(), Map.class);
        startActivity(intent);
    }

}
