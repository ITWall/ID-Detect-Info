package com.viettel.idscanner;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetInfoID {
    private Bitmap bitmap;
    private Activity mActivity;
    private Map<String, List<String>> dictionary;
    private static final String DICT_PATH = "dict.txt";
    private OnGettingInfoListener onGettingInfoListener;
    private ProgressDialog mProgressDialog;

    public GetInfoID(Bitmap bitmap, Activity activity, OnGettingInfoListener mOnGettingInfoListener, Map<String, List<String>> dictionary) {
        this.bitmap = bitmap;
        this.mActivity = activity;
        this.bitmap = rotateBitmap(bitmap, 90);
        this.onGettingInfoListener = mOnGettingInfoListener;
        this.dictionary = dictionary;
    }

    public void getInfo() {
        try {
            final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
            final ImageClassifier classifier = new ImageClassifier(mActivity);
            final Bitmap finalBitmap = bitmap;
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setMessage("Đang xử lý ảnh");
            mProgressDialog.setCancelable(false);
            detectInfo(classifier, bitmap, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void detectInfo(final ImageClassifier classifier, final Bitmap bitmap, final int degree) {
        final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        final Bitmap finalBitmap = rotateBitmap(bitmap, degree);
        Toast.makeText(mActivity, "" + degree, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 30; i++) {
                    final String textToShow = classifier.classifyFrame(scaledBitmap);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (degree == 0) {
                                mProgressDialog.show();
                            }
                        }
                    });
                    if (i == 29) {
                        if (textToShow.split("\n")[1].startsWith("the can cuoc")) {
                            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(finalBitmap);
                            FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
                                    .getOnDeviceTextRecognizer();
                            textRecognizer.processImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText visionText) {
                                    Map<String, String> infoMap = getInfoMap(visionText);
                                    if (degree == 270) {
                                        mProgressDialog.dismiss();
                                        onGettingInfoListener.onSuccess(infoMap);
                                        return;
                                    }
                                    for (Map.Entry<String, String> entry: infoMap.entrySet()) {
                                        if (entry.getKey().equalsIgnoreCase("Gender")) {
                                            continue;
                                        }
                                        if (!entry.getValue().equalsIgnoreCase("")) {
                                            mProgressDialog.dismiss();
                                            onGettingInfoListener.onSuccess(infoMap);
                                            return;
                                        }
                                    }
                                    int copyDegree = degree;
                                    detectInfo(classifier, bitmap, copyDegree+=90);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                        else {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.dismiss();
                                    Toast.makeText(mActivity, "Bạn phải đặt thẻ căn cước vào", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                }
            }
        }).start();
    }

    private Map<String, String> getInfoMap(FirebaseVisionText visionText) {
        Map<String, String> infoMap = new HashMap<>();
        List<String> dates = new ArrayList<>();
        String[] keys = new String[] {"Name", "ID", "Gender", "Birthday"};
        // Khởi tạo giá trị phần tử hashmap
        for (String key: keys) {
            infoMap.put(key, "");
        }
        for (FirebaseVisionText.TextBlock block : visionText.getTextBlocks()) {
            for (FirebaseVisionText.Line line : block.getLines()) {
                String textLine = line.getText();
                String idDetected = getIDNumber(textLine);
                if (!idDetected.equalsIgnoreCase("")) {
                    infoMap.put("ID", idDetected);
                    continue;
                }
                if (infoMap.get("Name").equalsIgnoreCase("")) {
                    List<String> allPossibleName = null;
                    try {
                        allPossibleName = detectName(textLine);
                    } catch (Exception e) {
                        infoMap.put("Name", "");
                        e.printStackTrace();
                    }
                    if (allPossibleName != null) {
                        if(allPossibleName.size() > 1) {
                            String allPossibleNameString = "";
                            for (String possibleName: allPossibleName) {
                                allPossibleNameString += possibleName + "@";
                            }
                            infoMap.put("Name", allPossibleNameString);
                        } else {
                            infoMap.put("Name", allPossibleName.get(0));
                        }

                    } else {
                        infoMap.put("Name", "");
                    }
                }
                if (infoMap.get("Gender").equalsIgnoreCase("") || infoMap.get("Gender").equalsIgnoreCase("Nam")) {
                    infoMap.put("Gender", getGender(textLine));
                }
                String dateDetected = detectDate(textLine);
                if (!dateDetected.equalsIgnoreCase("")) {
                    dates.add(dateDetected);
                }
            }
        }
        if (dates.size() > 0) {
            infoMap.put("Birthday", getBirthday(dates));
        }
        return infoMap;
    }

    private String formatToDetectname(String textLine) {
        String result = "";
        if (isUppercaseString(textLine)) {
            result += covertStringToURL(textLine);
        }
        return result;
    }

    private String getIDNumber(String textLine) {
        String result = "";
        Pattern p = Pattern.compile("(\\d){12}");
        textLine = covertStringToURL(textLine).replaceAll("\\s+", "");
        Matcher m = p.matcher(textLine);
        if (m.find()) {
            result += m.group(0);
        }
        return result;
    }

    private String getGender(String textLine) {
        Pattern p = Pattern.compile("NU");
        textLine = covertStringToURL(textLine).replaceAll("\\s+", "");
        Matcher m = p.matcher(textLine);
        if (m.find()) {
            return "Nữ";
        }
        return "Nam";
    }

    private String detectDate(String textLine) {
        Pattern p = Pattern.compile("(\\d){2}/(\\d){2}/(\\d){4}");
        Matcher m = p.matcher(textLine);
        if (m.find()) {
            return m.group();
        }
        return "";
    }

    private String getBirthday(List<String> dates) {
        if (dates.size() == 1) {
            return dates.get(0);
        } else {
            String firstDate = dates.get(0);
            String secondDate = dates.get(1);
            if (firstDate.substring(firstDate.length() - 4, firstDate.length()).compareTo(secondDate.substring(secondDate.length() - 4, secondDate.length())) < 0) {
                return firstDate;
            } else {
                return secondDate;
            }
        }
    }

    public String covertStringToURL(String str) {
        try {
            String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
            Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
            return pattern.matcher(temp).replaceAll("")
                    .toUpperCase()
                    .replaceAll("đ", "d");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    private int countUppercaseLetter(String string) {
        int count = 0;
        for (int i = 0; i < string.length(); i++) {
            if (Character.isUpperCase(string.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private boolean isUppercaseString(String string) {
        // remove all space in string
        string = string.replaceAll("\\s+", "");
        if ((countUppercaseLetter(string) * 1.0f / string.length()) > 0.8f) {
            return true;
        }
        return false;
    }

    private List<String> detectName(String textLine) {
        List<List> listNameComponent = new ArrayList<>();
        List<String> listPossibleName = new ArrayList<>();
        String formattedTextLine = formatToDetectname(textLine);
        if (formattedTextLine.length() <= 7) {
            return null;
        }
        if (formattedTextLine.startsWith("VIET NAM") || formattedTextLine.startsWith("CONG HOA") || formattedTextLine.startsWith("ONG HOA") || formattedTextLine.startsWith("IET NAM") || formattedTextLine.startsWith("CAN CUOC")) {
            return null;
        } else {
            String[] nameParts = formattedTextLine.split("\\s");
            for (String part : nameParts) {
                try {
                    List<String> listWordInPart = dictionary.get(part);
                    listNameComponent.add(listWordInPart);
                } catch (NullPointerException ex) {
                    return null;
                }
            }
            listPossibleName = getAllPossibleName(listNameComponent);
            return listPossibleName;
        }
    }

    private int[] next(int array[], int limit[]) {
        int arrayLength = array.length;
        for (int i = arrayLength - 1; i >= 0; i--) {
            if (array[i] == limit[i]) {
                int posNotLimited = Math.abs(i - 1);
                while (array[posNotLimited] == limit[posNotLimited]) {
                    posNotLimited--;
                    if (posNotLimited == -1) {
                        return new int[]{-1};
                    }
                }
                array[posNotLimited]++;
                for (int j = posNotLimited + 1; j < arrayLength; j++) {
                    array[j] = 0;
                }
                break;
            } else {
                array[i]++;
                break;
            }
        }
        return array;
    }

    private List<String> getAllPossibleName(List<List> listNameComponent) {
        if (listNameComponent == null) {
            return new ArrayList<>();
        }
        List<String> fullnameList = new ArrayList<>();
        int[] limit = new int[listNameComponent.size()];
        int[] state = new int[listNameComponent.size()];
        for (int i = 0; i < limit.length; i++) {
            limit[i] = listNameComponent.get(i).size() - 1;
            state[i] = 0;
        }
        while (state.length != 1) {
            String fullname = "";
            for (int posList = 0; posList < listNameComponent.size(); posList++) {
                fullname += listNameComponent.get(posList).get(state[posList]) + " ";
            }
            fullnameList.add(fullname);
            state = next(state, limit);
        }
        return fullnameList;
    }
}
