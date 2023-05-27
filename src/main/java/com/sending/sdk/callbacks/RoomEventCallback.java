package com.sending.sdk.callbacks;

import com.sending.sdk.models.RoomEvent;

import java.io.IOException;

public interface RoomEventCallback {
    void onEventReceived(RoomEvent roomEvent) throws IOException;
}
