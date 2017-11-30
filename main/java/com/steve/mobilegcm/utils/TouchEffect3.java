package com.steve.mobilegcm.utils;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.steve.mobilegcm.R;

public class TouchEffect3 implements View.OnTouchListener {

    private Context context;

    public TouchEffect3(Context context) {
        this.context = context;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            ((Button) v).setTextColor(context.getResources().getColor(R.color.bg_login));
        }
        else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            ((Button) v).setTextColor(context.getResources().getColor(R.color.white));
        }
        return false;
    }

}
