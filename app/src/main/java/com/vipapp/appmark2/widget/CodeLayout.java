package com.vipapp.appmark2.widget;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;

import com.vipapp.appmark2.R;
import com.vipapp.appmark2.callback.PushCallback;
import com.vipapp.appmark2.item.ScrollChange;
import com.vipapp.appmark2.util.MathUtils;
import com.vipapp.appmark2.util.TextUtils;
import com.vipapp.appmark2.util.ThrowableUtils;
import com.vipapp.appmark2.util.Toast;
import com.vipapp.appmark2.util.wrapper.Res;
import com.vipapp.appmark2.util.wrapper.Window;
import com.vipapp.appmark2.util.wrapper.mLayoutInflater;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.view.Gravity.NO_GRAVITY;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.vipapp.appmark2.menu.HintsMenu.ARRAY_PUSHED;
import static com.vipapp.appmark2.menu.HintsMenu.SHOW_POPUP;
import static com.vipapp.appmark2.menu.HintsMenu.TEXT_CHANGED;
import static com.vipapp.appmark2.menu.HintsMenu.TEXT_INSERTED;
import static com.vipapp.appmark2.util.Const.DISTANCE_TO_ZOOM;
import static com.vipapp.appmark2.util.Const.JAVA_KEYWORDS;
import static com.vipapp.appmark2.util.Const.JAVA_LANGUAGE;
import static java.lang.Math.abs;

public class CodeLayout extends ScrollView {
    private HashMap<Integer, ArrayList<Hint>> hints = new HashMap<Integer, ArrayList<Hint>>(){{
        put(JAVA_LANGUAGE, new ArrayList<>());
    }};

    PushCallback<ScrollChange> scroll;

    View popupView;
    RecyclerView popupRecycler;
    PopupWindow hintsPopup;
    int popupY = -1;
    
    private boolean firstOnLayout = true;
    private LinearLayout container;
    private CodeText codeText;

    int first_pointer_id = 0;
    int second_pointer_id = 1;

    double startDistance = -1;
    boolean canTouch = true;

    public CodeLayout(Context context) {
        super(context);
        init(context, null);
    }

    public CodeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CodeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void pushScroll(int vert, int horiz, int oldVert, int oldHoriz){
        if(scroll != null)
            scroll.onComplete(new ScrollChange(vert, horiz, oldVert, oldHoriz));
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        pushScroll(t, l, oldt, oldl);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(firstOnLayout) {
            firstOnLayout = false;
            if (getChildCount() > 0) {
                View child = getChildAt(0);
                if (child instanceof CodeText) {
                    setupWithCodeText((CodeText) child);
                } else {
                    throw new UnsupportedChildException();
                }
            } else {
                setupWithCodeText(new CodeText(getContext()));
            }
        }
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs){
        setupHintsList();
        setupHintsPopup();
        setupTouchZoom();
    }

    private void setupHintsList(){
        ArrayList<Hint> javaHints = hints.get(JAVA_LANGUAGE);
        for(String keyword: JAVA_KEYWORDS) {
            assert javaHints != null;
            javaHints.add(new KeywordHint(keyword));
        }
    }

    private void setupWithCodeText(CodeText text){
        codeText = text;
        codeText.addTextChangedListener(new TextWatcher() {
           public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
           public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
               if(popupRecycler != null)
                   popupRecycler.pushValue(TEXT_CHANGED, TextUtils.getCurrentWord(codeText).trim());
           }
           public void afterTextChanged(Editable editable) {}
        });
        // creating horizontal scroll
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(getContext()){
            @Override
            protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);
                pushScroll(t, l, oldt, oldl);
            }
        };
        removeAllViews();
        addView(horizontalScrollView);
        // creating container
        container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        horizontalScrollView.addView(container);
        container.addView(codeText);
        // adding space to scroll
        View space = new View(getContext());
        container.addView(space);
        space.setLayoutParams(new LinearLayout.LayoutParams(Res.get().getDisplayMetrics().widthPixels, Res.get().getDisplayMetrics().heightPixels));
        // code text setup
        codeText.setMinWidth(Res.get().getDisplayMetrics().widthPixels);
    }

    @SuppressLint("InflateParams")
    private void setupHintsPopup() {
        popupView = mLayoutInflater.get().inflate(R.layout.hints_popup, null);
        popupRecycler = popupView.findViewById(R.id.recycler);
        popupRecycler.addOnPushCallback(item -> {
            if (item.getType() == TEXT_INSERTED) {
                if (codeText.getSelectionStart() > 0) {
                    TextUtils.deleteWordAtPosition(codeText.getText(), codeText.getSelectionStart() - 1);
                    codeText.getText().insert(codeText.getSelectionStart(), (String) item.getInstance());
                }
            }
            if (item.getType() == SHOW_POPUP) {
                if ((Boolean) item.getInstance()) {
                    Layout layout = codeText.getLayout();
                    if (layout != null) {
                        int current_line = layout.getLineForOffset(codeText.getSelectionStart());
                        Rect currentLineBounds = new Rect();
                        codeText.getLineBounds(current_line, currentLineBounds);
                        int current_baseline = layout.getLineBaseline(current_line);
                        int current_ascent = layout.getLineAscent(current_line);
                        float cursor_y = current_baseline + current_ascent + currentLineBounds.bottom - currentLineBounds.top;
                        setHintsPopupPosition((int) cursor_y);
                    }
                } else {
                    hidePopup();
                }
            }
        });
    }

    private void setupTouchZoom(){
        setOnTouchListener((view, motionEvent) -> {
            // FIXME: 29.03.2020 Remove try-catch block and find the reason of random exceptions
            try {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        startDistance = -1;
                        canTouch = true;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (motionEvent.getPointerCount() == 2) {
                            int first_pointer_index = motionEvent.findPointerIndex(first_pointer_id);
                            int second_pointer_index = motionEvent.findPointerIndex(second_pointer_id);

                            float x1 = motionEvent.getX(first_pointer_index);
                            float y1 = motionEvent.getY(first_pointer_index);
                            float x2 = motionEvent.getX(second_pointer_index);
                            float y2 = motionEvent.getY(second_pointer_index);

                            double distance = MathUtils.getDistanceBetween(x1, y1, x2, y2);
                            startDistance = startDistance == -1 ? distance : startDistance;

                            double difference = distance - startDistance;

                            if (abs(difference) >= DISTANCE_TO_ZOOM) {
                                float k = (float) difference / DISTANCE_TO_ZOOM;
                                startDistance = distance;
                                int linesAbove = getScrollY() / codeText.getLineHeight();
                                int textHeightBefore = codeText.getLineHeight() * linesAbove;
                                codeText.upTextSize(k);
                                int textHeightAfter = codeText.getLineHeight() * linesAbove;
                                scrollBy(0, textHeightAfter - textHeightBefore);
                            }
                            canTouch = false;
                        }
                        break;
                }
                performClick();
                return !canTouch;
            } catch (Exception e) {
                // Toast.show(ThrowableUtils.toString(e));
                return true;
            }
        });
    }

    private void setHintsPopupPosition(int y){
        Rect frame = new Rect();
        getWindowVisibleDisplayFrame(frame);
        Rect lineBounds = new Rect();
        codeText.getLineBounds(0, lineBounds);
        int scrollY = y;
        y = Math.min(y, 2 * codeText.getLineHeight());
        if(y != popupY || hintsPopup == null){
            hidePopup();
            setScrollY(scrollY - codeText.getLineHeight() * 2);
            if(popupView.getParent() != null)
                ((ViewGroup)popupView.getParent()).removeAllViews();
            hintsPopup = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hintsPopup.setOutsideTouchable(true);
            hintsPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
            hintsPopup.setOnDismissListener(() -> hintsPopup = null);
            popupY = y;
            hintsPopup.showAsDropDown(this, 0, popupY - getHeight());
        }
    }
    
    public void setLanguage(int language){
        codeText.setLanguage(language);
        setupHints();
    }

    private void setupHints(){
        if(hints.containsKey(codeText.getLanguage()))
            popupRecycler.pushValue(ARRAY_PUSHED, hints.get(codeText.getLanguage()));
    }
    
    public void hidePopup(){
        if(hintsPopup != null) {
            hintsPopup.dismiss();
            hintsPopup = null;
        }
    }

    public CodeText getCodeText(){
        return codeText;
    }

    public LinearLayout getContainer(){
        return container;
    }

    public void setOnScrollChangeListener(PushCallback<ScrollChange> listener){
        this.scroll = listener;
    }

    private static class UnsupportedChildException extends RuntimeException{
        UnsupportedChildException(){ super("CodeLayout can have only CodeText as child"); }
    }

    public static class Hint{
        @DrawableRes
        private int image;
        private String body;
        private String insertValue;

        Hint(int image, String body, String insertValue) {
            this.image = image;
            this.body = body;
            this.insertValue = insertValue;
        }

        private Hint(int image, String body) {
            this.image = image;
            this.body = body;
            this.insertValue = body + " ";
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getInsertValue() {
            return insertValue;
        }

        public void setInsertValue(String insertValue) {
            this.insertValue = insertValue;
        }

        public int getImage() {
            return image;
        }

        public void setImage(int image) {
            this.image = image;
        }
    }

    public static class KeywordHint extends Hint{

        public KeywordHint(String body, String insertValue) {
            super(R.drawable.keyword, body, insertValue);
        }

        KeywordHint(String body){
            super(R.drawable.keyword, body);
        }
    }

}
