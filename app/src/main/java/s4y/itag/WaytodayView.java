package s4y.itag;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import s4y.itag.waytoday.WayToday;

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

            if (WayToday.getInstance().wtClient.hasTrackerId()) {
                Intent browserIntent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://way.today/#" + WayToday.getInstance().wtClient.getCurrentTrackerId()));
                getContext().startActivity(browserIntent);
            }
        });
    }
}
