package com.viettel.idscanner;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnChoosingNameListener, OnGettingInfoListener {

    private ImageView mIvPickImage;
    private TextView mTvResultScan;
    private Map<String, List<String>> dictionary = new TreeMap<>();
    private static final int REQUEST_PICK_IMAGE = 232;
    private static final String DICT_PATH = "dict.txt";
    private final String error = "UNKNOWN";
    private Map<String, String> infoMap;
    private ChooseNameDialogSample chooseNameDialogSample;
    private ArrayList<String> allPossibleName;
    private final static String TAG = "dialogTag";
    private final static String INFO = "infoMap";
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        if (savedInstanceState != null) {
            chooseNameDialogSample = (ChooseNameDialogSample) getSupportFragmentManager().findFragmentByTag(TAG);
            if (chooseNameDialogSample != null) {
                chooseNameDialogSample.setOnChoosingNameListener(this);
            }
            infoMap = (Map<String, String>) savedInstanceState.get(INFO);
        }
    }

    private void initData() {
        try {
            initDict(loadDictNameList(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        mIvPickImage = findViewById(R.id.iv_pick_image);
        mTvResultScan = findViewById(R.id.tv_result_scan);
        mIvPickImage.setOnClickListener(this);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Đang xử lý ảnh");
        mProgressDialog.setCancelable(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putSerializable(INFO, (Serializable) infoMap);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_pick_image:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGE) {
                if (data != null) {
                    Uri uri = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        GetInfoID getInfoID = new GetInfoID(bitmap, this, this, dictionary);
                        getInfoID.getInfo();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void initDict(List<String> lines) {
        for (String line : lines) {
            line = line.trim();
            String[] names = line.split("\\s");
            List<String> listName = Arrays.asList(names);
            dictionary.put(listName.get(0), listName.subList(1, listName.size()));
        }
    }

    private List<String> loadDictNameList(Activity activity) throws IOException {
        List<String> dictNameList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(DICT_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            dictNameList.add(line);
        }
        reader.close();
        return dictNameList;
    }

    private void showInfo(Map<String, String> infoMap) {
        mTvResultScan.setText("\nName: " + infoMap.get("Name"));
        mTvResultScan.append("\nID number: " + infoMap.get("ID"));
        mTvResultScan.append("\nGender: " + infoMap.get("Gender"));
        mTvResultScan.append("\nBirthday: " + infoMap.get("Birthday"));
    }

    @Override
    public void onChoosing(String name) {
        infoMap.put("Name", name);
        showInfo(infoMap);
    }

    @Override
    public void onSuccess(Map<String, String> info) {
        Toast.makeText(this, ""+info.get("Type"), Toast.LENGTH_SHORT).show();
        mProgressDialog.dismiss();
        infoMap = info;
        String allName = infoMap.get("Name");
        if (allName != null && !allName.equals("")) {
            String[] possibleName = allName.split("@");
            if (possibleName.length == 1) {
                infoMap.put("Name", possibleName[0]);
                showInfo(infoMap);
            } else {
                allPossibleName = new ArrayList<>();
                Collections.addAll(allPossibleName, possibleName);
                chooseNameDialogSample = new ChooseNameDialogSample()
                        .setAllPossibleName(allPossibleName)
                        .setOnChoosingNameListener(this);
                chooseNameDialogSample.setCancelable(false);
                chooseNameDialogSample.show(getSupportFragmentManager(), TAG);
            }
        } else {
            showInfo(infoMap);
        }
    }

    @Override
    public void onFailed(Map<String, String> info) {
        Toast.makeText(this, ""+info.get("Type"), Toast.LENGTH_SHORT).show();
        mProgressDialog.dismiss();
    }

    @Override
    public void onProcessing() {
        mProgressDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mProgressDialog.dismiss();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (chooseNameDialogSample != null && chooseNameDialogSample.getDialog() != null) {
            chooseNameDialogSample.onResume();
        }
    }
}
