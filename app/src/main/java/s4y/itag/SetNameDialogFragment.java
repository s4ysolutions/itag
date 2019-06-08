package s4y.itag;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

import s4y.itag.ble.ITagDevice;
import s4y.itag.ble.ITagsDb;
import s4y.itag.preference.AlarmDelayPreference;

public class SetNameDialogFragment extends DialogFragment {
    public static ITagDevice device;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.fragment_set_name, null);
        final TextView textName = view.findViewById(R.id.text_name);
        textName.setText(device.name);
        final RadioGroup grpAlarm = view.findViewById(R.id.alarm_delay);
        final RadioButton btnAlarm0 = view.findViewById(R.id.alarm_delay_0);
        final RadioButton btnAlarm3 = view.findViewById(R.id.alarm_delay_3);
        final RadioButton btnAlarm5 = view.findViewById(R.id.alarm_delay_5);
        final RadioButton btnAlarm10 = view.findViewById(R.id.alarm_delay_10);
        final AlarmDelayPreference alarmDelayPreference =
                new AlarmDelayPreference(this.getContext(), device);
        grpAlarm.clearCheck();
        switch (alarmDelayPreference.get()) {
            case 0:
                btnAlarm0.setChecked(true);
                break;
            case 3:
                btnAlarm3.setChecked(true);
                break;
            case 5:
                btnAlarm5.setChecked(true);
                break;
            case 10:
                btnAlarm10.setChecked(true);
                break;
            default:
                btnAlarm5.setChecked(true);
        }
        builder.setTitle(R.string.change_name)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    device.name = textName.getText().toString();
                    ITagsDb.save(getActivity());
                    ITagsDb.notifyChange();
                    ITagApplication.faNameITag();
                    switch (grpAlarm.getCheckedRadioButtonId()) {
                        case R.id.alarm_delay_0:
                            alarmDelayPreference.set(0);
                            break;
                        case R.id.alarm_delay_3:
                            alarmDelayPreference.set(3);
                            break;
                        case R.id.alarm_delay_5:
                            alarmDelayPreference.set(5);
                            break;
                        default:
                            alarmDelayPreference.set(10);
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    //dialog.cancel();
                });
        return builder.create();
    }
}
