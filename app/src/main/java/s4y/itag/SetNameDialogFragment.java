package s4y.itag;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import s4y.itag.itag.ITag;
import s4y.itag.itag.ITagInterface;
import s4y.itag.itag.TagAlertMode;
import s4y.itag.itag.TagConnectionMode;

public class SetNameDialogFragment extends DialogFragment {
    static ITagInterface iTag;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.fragment_set_name, null);
        final TextView textName = view.findViewById(R.id.text_name);
        textName.setText(iTag.name());

        final CheckBox reconnect_checkbox = view.findViewById(R.id.reconnect_checkbox);
        reconnect_checkbox.setChecked(iTag.reconnectMode());

        final Spinner alarmDelaySpinner = view.findViewById(R.id.alarm_delay_spinner);
        ArrayAdapter<CharSequence> alarmDelayAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.itag_alarm_delays,
                android.R.layout.simple_spinner_item
        );
        // Specify the layout to use when the list of choices appears.
        alarmDelayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        alarmDelaySpinner.setAdapter(alarmDelayAdapter);

        final Spinner alarmModeSpinner = view.findViewById(R.id.alarm_mode_spinner);
        ArrayAdapter<CharSequence> alarmModeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.itag_alarm_modes,
                android.R.layout.simple_spinner_item
        );
        // Specify the layout to use when the list of choices appears.
        alarmModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        alarmModeSpinner.setAdapter(alarmModeAdapter);

        TagAlertMode alertMode = iTag.alertMode();
        switch(alertMode){
            case noAlarm:
                alarmModeSpinner.setSelection(0);
                break;
            case alertOnDisconnect:
                alarmModeSpinner.setSelection(1);
                break;
            case alertOnConnect:
                alarmModeSpinner.setSelection(2);
                break;
            case alertOnBoth:
                alarmModeSpinner.setSelection(3);
                break;
        }

        // TODO: passive mode would let the phone detect iTag without connecting to it
        /*final Spinner connectionModeSpinner = view.findViewById(R.id.connection_mode_spinner);
        ArrayAdapter<CharSequence> connectionModeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.itag_connection_modes,
                android.R.layout.simple_spinner_item
        );
        // Specify the layout to use when the list of choices appears.
        connectionModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        connectionModeSpinner.setAdapter(connectionModeAdapter);

        TagConnectionMode connectionMode = iTag.connectionMode();
        switch(connectionMode){
            case connect:
                connectionModeSpinner.setSelection(0);
                break;
            case dontConnect:
                connectionModeSpinner.setSelection(1);
                break;
        }*/

        int alarm =iTag.alertDelay();
        if (alarm<3) {
            alarmDelaySpinner.setSelection(0);
        }else if (alarm < 5){
            alarmDelaySpinner.setSelection(1);
        }else if (alarm <10) {
            alarmDelaySpinner.setSelection(2);
        }else {
            alarmDelaySpinner.setSelection(3);
        }

        builder.setTitle(R.string.change_name)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if(!iTag.name().equals(textName.getText().toString())) {
                        ITag.store.setName(iTag.id(), textName.getText().toString());
                        ITagApplication.faNameITag();
                    }
                    ITag.store.setReconnectMode(iTag.id(), reconnect_checkbox.isChecked());
                    switch (alarmDelaySpinner.getSelectedItemPosition()) {
                        case 0:
                            ITag.store.setAlertDelay(iTag.id(), 0);
                            break;
                        case 1:
                            ITag.store.setAlertDelay(iTag.id(), 3);
                            break;
                        case 2:
                            ITag.store.setAlertDelay(iTag.id(), 5);
                            break;
                        default:
                            ITag.store.setAlertDelay(iTag.id(), 10);
                            break;
                    }
                    // TODO: passive vs active mode
                    /*switch (connectionModeSpinner.getSelectedItemPosition()) {
                        case 0:
                            ITag.store.setConnectionMode(iTag.id(), TagConnectionMode.connect);
                            break;
                        case 1:
                            ITag.store.setConnectionMode(iTag.id(), TagConnectionMode.dontConnect);
                            break;
                    }*/
                    switch (alarmModeSpinner.getSelectedItemPosition()) {
                        case 0:
                            ITag.store.setAlertMode(iTag.id(), TagAlertMode.noAlarm);
                            break;
                        case 1:
                            ITag.store.setAlertMode(iTag.id(), TagAlertMode.alertOnDisconnect);
                            break;
                        case 2:
                            ITag.store.setAlertMode(iTag.id(), TagAlertMode.alertOnConnect);
                            break;
                        default:
                            ITag.store.setAlertMode(iTag.id(), TagAlertMode.alertOnBoth);
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    //dialog.cancel();
                });
        return builder.create();
    }
}
