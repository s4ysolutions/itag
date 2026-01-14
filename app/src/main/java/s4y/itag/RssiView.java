package s4y.itag;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class RssiView extends LinearLayout {
    public RssiView(Context context) {
        super(context);
        setup();
    }

    public RssiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public RssiView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RssiView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private View l1;
    private View l2;
    private View l3;
    private View l4;
    private View l5;
    private View l6;
    private View l7;
    private View l8;
    private View l9;
    private View l10;
    private View l11;
    private View l12;

    static private final float BG_ON=1f;
    static private final float BG_OFF=0.3f;

    public void setRssi(int level) {
        // -999 indicates no signal
        // theoretical min level -115
        // highly likely disconnection -90
        l1.setAlpha(level > -115 ? BG_ON : BG_OFF);
        l2.setAlpha(level > -92 ? BG_ON : BG_OFF);
        l3.setAlpha(level > -90 ? BG_ON : BG_OFF);
        l4.setAlpha(level > -89 ? BG_ON : BG_OFF);
        l5.setAlpha(level > -87 ? BG_ON : BG_OFF);
        l6.setAlpha(level > -86 ? BG_ON : BG_OFF);
        l7.setAlpha(level > -85 ? BG_ON : BG_OFF);
        l8.setAlpha(level > -84 ? BG_ON : BG_OFF);
        l9.setAlpha(level > -83? BG_ON : BG_OFF);
        l10.setAlpha(level > -81 ? BG_ON : BG_OFF);
        l11.setAlpha(level > -78 ? BG_ON : BG_OFF);
        l12.setAlpha(level > -74 ? BG_ON : BG_OFF); // disconnection is highly likely
        // real life nearest signal level -70
        // theoretical max level -65
    }

    private void setup() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View root=inflater.inflate(R.layout.rssi, this);
        l1=root.findViewById(R.id.level1);
        l2=root.findViewById(R.id.level2);
        l3=root.findViewById(R.id.level3);
        l4=root.findViewById(R.id.level4);
        l5=root.findViewById(R.id.level5);
        l6=root.findViewById(R.id.level6);
        l7=root.findViewById(R.id.level7);
        l8=root.findViewById(R.id.level8);
        l9=root.findViewById(R.id.level9);
        l10=root.findViewById(R.id.level10);
        l11=root.findViewById(R.id.level11);
        l12=root.findViewById(R.id.level12);
    }

}
