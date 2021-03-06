package com.vipapp.appmark2.widget;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import android.util.AttributeSet;

import com.vipapp.appmark2.R;
import com.vipapp.appmark2.util.FontUtils;

public class TextView extends AppCompatTextView {

    @Override
    public boolean isInEditMode() {
        return false;
    }

    public TextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context, attrs);
    }
    public TextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }
    public TextView(Context context) {
        super(context);
        setup(context, null);
    }

    //SET TEXT FONT
    public void setup(Context context, AttributeSet attrs) {
        if (context != null) {
            //DEFAULT VALUES
            String font_name = null;

            if (attrs != null) {
                //GETTING TYPED ARRAY
                TypedArray a = context.getTheme().obtainStyledAttributes(
                        attrs, R.styleable.TextView, 0, 0);

                //GETTING VALUES
                try {
                    font_name = getFontName(a);
                } finally {
                    a.recycle();
                }
            }

            font_name = font_name == null? "main.ttf": font_name;

            //SETUP WIDGET
            FontUtils.setFont(font_name, this);
        }
    }

    public String getFontName(TypedArray array) {
        return array.getString(R.styleable.TextView_font_name);
    }

}
