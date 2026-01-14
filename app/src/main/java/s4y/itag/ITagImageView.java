package s4y.itag;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class ITagImageView extends AppCompatImageView {

    private static final String LT = ITagImageView.class.getName();

    public ITagImageView(Context context) {
        super(context);
    }

    public ITagImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ITagImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private static final int CLICK_INTERVAL = ViewConfiguration.getLongPressTimeout();

    private final android.os.Handler clickHandler = new Handler(Looper.getMainLooper());
    private final Runnable waitNext = () -> {
        isLongPress = true;
        Log.d(LT, "waitNext called");
    };

    boolean isLongPress = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clickHandler.postDelayed(waitNext, CLICK_INTERVAL);
                Log.d(LT, "action down");
                startScaleAnimation(this);
                return true;
            case MotionEvent.ACTION_UP:
                clickHandler.removeCallbacks(waitNext);
                cancelScaleAnimation(this);
                Log.d(LT, "callbacks removed");
                if(isLongPress){
                    //performLongClick();
                    Log.d(LT, "is longpress");
                    isLongPress = false;
                } else {
                    Log.d(LT, "call performClick");
                }
                return true;
        }
        return false;
    }

    // Because we call this from onTouchEvent, this code will be executed for both
    // normal touch events and for when the system calls this using Accessibility
    @Override
    public boolean performClick() {
        super.performClick();
        Log.d(LT, "super.performClick()");
        return true;
    }

    private void startScaleAnimation(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f);
        scaleDownX.setDuration(150);
        scaleDownY.setDuration(150);

        scaleDownX.start();
        scaleDownY.start();
    }

    private void cancelScaleAnimation(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f);
        scaleDownX.setDuration(150);
        scaleDownY.setDuration(150);

        scaleDownX.start();
        scaleDownY.start();
    }

}
