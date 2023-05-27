package com.sending.sdk.callbacks;

import com.sending.sdk.models.RoomEvent;

import java.io.IOException;
import java.util.List;

public interface RoomEventsCallback {
    void onEventReceived(List<RoomEvent> roomEvents) throws IOException;
}
