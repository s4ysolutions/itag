package solutions.s4y.itag;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import solutions.s4y.itag.ble.Db;
import solutions.s4y.itag.ble.Device;

public class SetNameDialogFragment extends DialogFragment {
    public static Device device;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_set_name, null);
        final TextView textName = view.findViewById(R.id.text_name);
        textName.setText(device.name);
        builder.setTitle(R.string.change_name)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    device.name=textName.getText().toString();
                    Db.save(getActivity());
                    Db.subject.onNext(device);
                    // dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    //dialog.cancel();
                });
        return builder.create();
    }
}
