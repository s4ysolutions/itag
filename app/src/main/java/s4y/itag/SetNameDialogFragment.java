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

import s4y.itag.itag.ITagInterface;

public class SetNameDialogFragment extends DialogFragment {
    public static ITagInterface iTag;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.fragment_set_name, null);
        final TextView textName = view.findViewById(R.id.text_name);
        textName.setText(iTag.name());
        final RadioGroup grpAlarm = view.findViewById(R.id.alarm_delay);
        final RadioButton btnAlarm0 = view.findViewById(R.id.alarm_delay_0);
        final RadioButton btnAlarm3 = view.findViewById(R.id.alarm_delay_3);
        final RadioButton btnAlarm5 = view.findViewById(R.id.alarm_delay_5);
        final RadioButton btnAlarm10 = view.findViewById(R.id.alarm_delay_10);
        /*
        final AlarmDelayPreference alarmDelayPreference =
                new AlarmDelayPreference(this.getContext(), device);
                *
         */
        grpAlarm.clearCheck();
        int alarm =iTag.alertDelay();
        if (alarm<3) {
            btnAlarm0.setChecked(true);
        }else if (alarm < 5){
            btnAlarm3.setChecked(true);
        }else if (alarm <10) {
            btnAlarm5.setChecked(true);
        }else {
            btnAlarm10.setChecked(true);
        }

        builder.setTitle(R.string.change_name)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    iTag.setName(textName.getText().toString());
                    ITagApplication.faNameITag();
                    switch (grpAlarm.getCheckedRadioButtonId()) {
                        case R.id.alarm_delay_0:
                            iTag.setAlertDelay(0);
                            break;
                        case R.id.alarm_delay_3:
                            iTag.setAlertDelay(3);
                            break;
                        case R.id.alarm_delay_5:
                            iTag.setAlertDelay(5);
                            break;
                        default:
                            iTag.setAlertDelay(10);
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    //dialog.cancel();
                });
        return builder.create();
    }
}
