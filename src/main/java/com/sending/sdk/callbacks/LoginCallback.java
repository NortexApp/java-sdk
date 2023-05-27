package com.sending.sdk.callbacks;

import com.sending.sdk.models.LoginData;
import java.io.IOException;

public interface LoginCallback {
    void onResponse(LoginData data) throws IOException;
}
