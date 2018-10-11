package com.viettel.idscanner;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.List;

public class ChooseNameDialog extends Dialog implements View.OnClickListener, DialogInterface.OnDismissListener, DialogInterface.OnShowListener {

    private List<String> allPossibleName;
    private AppCompatActivity mActivity;
    private RadioGroup mRgResult;
    private Button mBtnOk;
    private Button mBtnCancel;
    private OnChoosingNameListener onChoosingNameListener;

    public ChooseNameDialog(@NonNull AppCompatActivity activity, List<String> allPossibleName, OnChoosingNameListener onChoosingNameListener) {
        super(activity);
        this.mActivity = activity;
        this.allPossibleName = allPossibleName;
        this.onChoosingNameListener = onChoosingNameListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.layout_dialog_choose_result);
        setup();
    }

    private void setup() {
        mBtnCancel = findViewById(R.id.btn_cancel);
        mBtnOk = findViewById(R.id.btn_ok);
        mRgResult = findViewById(R.id.rg_result);
        mBtnCancel.setOnClickListener(this);
        mBtnOk.setOnClickListener(this);
        this.setOnDismissListener(this);
        this.setOnShowListener(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 18, 0, 18);
        mRgResult.clearCheck();
        int id = 0;
        for (int i = 0; i < allPossibleName.size(); i++) {
            RadioButton mRadioButtonResult = new RadioButton(mActivity);
            mRadioButtonResult.setText(allPossibleName.get(i));
            mRadioButtonResult.setTextSize(16);
            mRadioButtonResult.setLayoutParams(layoutParams);
            mRgResult.addView(mRadioButtonResult);
            if (i == 0) {
                id = mRadioButtonResult.getId();
            }
        }
        mRgResult.check(id);
        mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
            int selection = mRgResult.indexOfChild(findViewById(mRgResult.getCheckedRadioButtonId()));
            if (selection < 0) {
                Toast.makeText(mActivity, "Hãy chọn một kết quả", Toast.LENGTH_SHORT).show();
            } else {
                onChoosingNameListener.onChoosing(allPossibleName.get(selection));
                this.dismiss();
            }

        } else if (i == R.id.btn_cancel) {
            this.dismiss();
        }
    }
}
