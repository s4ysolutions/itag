package solutions.s4y.itag;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class WaytodayView extends LinearLayout {
    public WaytodayView(Context context) {
        super(context);
        setup();
    }

    public WaytodayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public WaytodayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WaytodayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private void setup() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View root = inflater.inflate(R.layout.waytoday, this);
        root.setOnClickListener(v -> {
            ITagApplication.faWtVisit();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            String tid = sp.getString("tid", "");
            if ("".equals(tid))
                return;
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://way.today/#" + tid));
            getContext().startActivity(browserIntent);
        });
    }
}
