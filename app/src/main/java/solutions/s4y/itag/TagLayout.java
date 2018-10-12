package solutions.s4y.itag;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TagLayout extends FrameLayout {
    public TagLayout(@NotNull Context context) {
        super(context);
        setup();
    }

    public TagLayout(@NotNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public TagLayout(@NotNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TagLayout(@NotNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private void setup() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.itag, this);
    }
}
