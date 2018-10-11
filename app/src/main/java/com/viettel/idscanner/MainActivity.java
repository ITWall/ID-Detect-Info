package com.viettel.idscanner;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnChoosingNameListener, OnGettingInfoListener {

    private ImageView mIvPickImage;
    private TextView mTvResultScan;
    private Map<String, List<String>> dictionary = new TreeMap<>();
    private static final int REQUEST_PICK_IMAGE = 232;
    private static final String DICT_PATH = "dict.txt";
    private final String error = "UNKNOWN";
    private Map<String, String> infoMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
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
    }

    @Override
    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.iv_pick_image:
//                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
//                startActivityForResult(intent, REQUEST_PICK_IMAGE);
//        }
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
        infoMap = info;
        String allName = infoMap.get("Name");
        if (allName != null && !allName.equals("")) {
            String[] possibleName = allName.split("@");
            if (possibleName.length == 1) {
                infoMap.put("Name", possibleName[0]);
                showInfo(infoMap);
            } else {
                ArrayList<String> allPossibleName = new ArrayList<>();
                Collections.addAll(allPossibleName, possibleName);
                ChooseNameDialog chooseNameDialog = new ChooseNameDialog(this, allPossibleName, this);
                chooseNameDialog.show();
            }
        } else {
            showInfo(infoMap);
        }

    }
}
