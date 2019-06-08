package s4y.itag;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import s4y.itag.ble.ITagDevice;
import s4y.itag.ble.ITagGatt;
import s4y.itag.ble.ITagsService;

public class ITagImageView extends ImageView implements GestureDetector.OnGestureListener {
    private GestureDetector mGestureDetector;

    public ITagImageView(Context context) {
        super(context);
        setup();
    }

    public ITagImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public ITagImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ITagImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private void setup() {
        mGestureDetector = new GestureDetector(getContext(), this);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    private MainActivity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof MainActivity) {
                return (MainActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /*
        private void findITag() {
            MainActivity mainActivity = getActivity();
            if (mainActivity != null) {
                if (mainActivity.mITagsServiceBound) {
                    ITagsService service = mainActivity.mITagsService;
                    final ITagDevice device = (ITagDevice) getTag();
                    ITagGatt gatt = service.getGatt(device.addr, true);
                    if (gatt.isFindingITag()) {
                        gatt.stopFindITag();
                    } else {
                        if (gatt.isConnected()) {
                            gatt.findITag();
                        } else {
                            Toast.makeText(getContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    */
/*
    public void onITagClick() {
        final MainActivity mainActivity = getActivity();
        if (mainActivity == null) return;
        if (!mainActivity.mITagsServiceBound) return;

        final ITagsService service = mainActivity.mITagsService;
        final ITagDevice device = (ITagDevice) getTag();
        final ITagGatt gatt = service.getGatt(device.addr, true);

        boolean needNotify = true;
        if (gatt.isFindingITag()) {
            gatt.stopFindITag();
            needNotify = false;
        }
        if (gatt.isError()) {
            gatt.connect(getContext());
            needNotify = false;
        }
        if (gatt.isFindingPhone()) {
            gatt.stopFindPhone();
        }
        if (MediaPlayerUtils.getInstance().isSound()) {
            MediaPlayerUtils.getInstance().stopSound(getContext());
        }
        if (needNotify) {
            gatt.findITag();
            // Toast.makeText(this, R.string.help_longpress, Toast.LENGTH_SHORT).show();
        }
    }
    */
    @Override
    public void onLongPress(MotionEvent e) {
        performClick();
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            float move = Math.abs(e2.getX() - e1.getX());
            if (move > 8) {
                final ITagDevice device = (ITagDevice) getTag();
                MainActivity mainActivity = getActivity();
                if (mainActivity != null && mainActivity.mITagsServiceBound) {
                    ITagsService service = mainActivity.mITagsService;
                    ITagGatt gatt = service.getGatt(device.addr, false);
                    if (gatt.isConnected() || gatt.isConnecting()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage(R.string.confirm_disconnect)
                                .setTitle(R.string.confirm_title)
                                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                    gatt.disconnect(true);
                                    Toast.makeText(getContext(), R.string.disconnect, Toast.LENGTH_SHORT).show();
                                    ITagApplication.faITagDisconnected();
                                })
                                .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.cancel())
                                .show();
                    } else {
                        gatt.connect(service);
                        Toast.makeText(getContext(), R.string.connect, Toast.LENGTH_SHORT).show();
                        ITagApplication.faITagConnected();
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performLongClick() {
        return super.performLongClick();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
