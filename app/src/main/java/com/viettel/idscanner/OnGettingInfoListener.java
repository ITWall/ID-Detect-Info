package com.viettel.idscanner;

import java.util.Map;

public interface OnGettingInfoListener {
    void onSuccess(Map<String, String> info);
    void onFailed(Map<String, String> info);
}
