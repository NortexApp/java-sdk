package com.sending.sdk.callbacks;

import com.sending.sdk.models.Member;

import java.io.IOException;
import java.util.List;

public interface MemberCallback {
    void onResponse(List<Member> roomMember) throws IOException;
}
