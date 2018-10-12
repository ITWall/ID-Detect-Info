package com.viettel.idscanner;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChooseNameDialogSample extends DialogFragment implements View.OnClickListener, DialogInterface.OnDismissListener, DialogInterface.OnShowListener {

    private List<String> allPossibleName;
    private RadioGroup mRgResult;
    private Button mBtnOk;
    private Button mBtnCancel;
    private OnChoosingNameListener onChoosingNameListener;
    private View v;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        v = inflater.inflate(R.layout.layout_dialog_choose_result, container, false);
        return v;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        Toast.makeText(this.getActivity(), "onCreateDialog", Toast.LENGTH_SHORT).show();
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            allPossibleName = savedInstanceState.getStringArrayList("names");
        }
        setup();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (allPossibleName != null) {
            outState.putStringArrayList("names", (ArrayList<String>) allPossibleName);
        }
    }

    private void setup() {
        mBtnCancel = v.findViewById(R.id.btn_cancel);
        mBtnOk = v.findViewById(R.id.btn_ok);
        mRgResult = v.findViewById(R.id.rg_result);
        mBtnCancel.setOnClickListener(this);
        mBtnOk.setOnClickListener(this);
        this.getDialog().setOnDismissListener(this);
        this.getDialog().setOnShowListener(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 18, 0, 18);
        mRgResult.clearCheck();
        int id = 0;
        for (int i = 0; i < allPossibleName.size(); i++) {
            RadioButton mRadioButtonResult = new RadioButton(this.getActivity());
            mRadioButtonResult.setText(allPossibleName.get(i));
            mRadioButtonResult.setTextSize(16);
            mRadioButtonResult.setLayoutParams(layoutParams);
            mRgResult.addView(mRadioButtonResult);
            if (i == 0) {
                id = mRadioButtonResult.getId();
            }
        }
        mRgResult.check(id);
//        this.getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {

    }

    @Override
    public void onShow(DialogInterface dialog) {

    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.btn_ok) {
            int selection = mRgResult.indexOfChild(v.findViewById(mRgResult.getCheckedRadioButtonId()));
            if (selection < 0) {
                Toast.makeText(this.getActivity(), "Hãy chọn một kết quả", Toast.LENGTH_SHORT).show();
            } else {
                onChoosingNameListener.onChoosing(allPossibleName.get(selection));
                this.dismiss();
            }

        } else if (i == R.id.btn_cancel) {
            this.dismiss();
        }
    }

    public ChooseNameDialogSample setAllPossibleName(List<String> allPossibleName) {
        this.allPossibleName = allPossibleName;
        return this;
    }

    public ChooseNameDialogSample setOnChoosingNameListener(OnChoosingNameListener onChoosingNameListener) {
        this.onChoosingNameListener = onChoosingNameListener;
        return this;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupLayout();
    }

    private void setupLayout() {
        ViewGroup.LayoutParams params = Objects.requireNonNull(getDialog().getWindow()).getAttributes();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Objects.requireNonNull(getActivity()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int orientation = getActivity().getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.width = Math.round(displayMetrics.widthPixels * 0.75f);
            params.height = Math.round(displayMetrics.heightPixels * 0.5f);
            Toast.makeText(getActivity(), "Potrait", Toast.LENGTH_SHORT).show();
        } else {
            params.width = Math.round(displayMetrics.widthPixels / 2.7f);
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            Toast.makeText(getActivity(), "Landscape", Toast.LENGTH_SHORT).show();
        }
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }
}
