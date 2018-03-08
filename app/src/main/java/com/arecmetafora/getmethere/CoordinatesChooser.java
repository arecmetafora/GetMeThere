package com.arecmetafora.getmethere;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.telecom.Call;
import android.view.View;
import android.widget.EditText;

public class CoordinatesChooser extends DialogFragment {

    public interface Callback {
        void onCancel();
        void onChoose(Location location);
    }

    private Callback mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Callback) {
            mListener = (Callback) getActivity();
        } else {
            throw new ClassCastException(context.toString() + " must implement CoordinatesChooser.Callback");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.coordinates_chooser_title)
            .setView(View.inflate(getContext(), R.layout.activity_main, null))
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(mListener != null) {
                        //mListener.onChoose();
                    }
                    dismiss();
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(mListener != null) {
                        mListener.onCancel();
                    }
                    dismiss();
                }
            })
            .create();
    }
}
