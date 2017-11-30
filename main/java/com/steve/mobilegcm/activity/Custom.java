package com.steve.mobilegcm.activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import com.steve.mobilegcm.R;
import com.steve.mobilegcm.utils.TouchEffect;
import com.steve.mobilegcm.utils.TouchEffect2;
import com.steve.mobilegcm.utils.TouchEffect3;

public class Custom extends FragmentActivity implements OnClickListener {

	public static final TouchEffect TOUCH = new TouchEffect();
    public static TouchEffect2 TOUCH2;
    public static TouchEffect3 TOUCH3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TOUCH2 = new TouchEffect2(getApplicationContext());
        TOUCH3 = new TouchEffect3(getApplicationContext());
    }

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
	}

	public View setTouchNClick(int id) {
		View v = setClick(id);
		if (v != null)
			v.setOnTouchListener(TOUCH);
		return v;
	}

    public View setTouchNClick2(int id) {
        View v = setClick(id);
        if (v != null)
            v.setOnTouchListener(TOUCH2);
        return v;
    }

    public View setTouchNClick3(int id) {
        View v = setClick(id);
        if (v != null)
            v.setOnTouchListener(TOUCH3);
        return v;
    }

	public View setClick(int id) {
		View v = findViewById(id);
		if (v != null)
			v.setOnClickListener(this);
		return v;
	}

	@Override
	public void onClick(View v)
	{

	}

}
