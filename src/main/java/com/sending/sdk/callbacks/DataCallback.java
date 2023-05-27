package com.sending.sdk.callbacks;

import java.io.IOException;

public interface DataCallback {
    void onData(Object data) throws IOException;
}
