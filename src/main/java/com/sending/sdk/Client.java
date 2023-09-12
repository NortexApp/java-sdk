package com.sending.sdk;

import com.sending.sdk.models.LoginData;
import com.sending.sdk.models.*;
import com.sending.sdk.callbacks.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.*;
import okio.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Client {
    private String host;
    private LoginData loginData;
    private boolean isLoggedIn = false;
    private final HttpHelper httpHelper;
    private final Syncee syncee;

    public LoginData loginDID(String address, String privateKey) throws IOException {

        // query did list
        String queryDidRespRaw = httpHelper.sendRequest(host, HttpHelper.URLs.did_address + address, null, false, "GET");
        JSONObject queryDidResp = new JSONObject(queryDidRespRaw);
        JSONArray didList = queryDidResp.getJSONArray("data");

        // pre login
        JSONObject preLoginReq = new JSONObject();
        if (didList.length() > 0) {
            preLoginReq.put("did", didList.getString(0));
        } else {
            preLoginReq.put("address", address);
        }
        String preLoginResp = httpHelper.sendRequest(host, HttpHelper.URLs.did_pre_login, preLoginReq, false, "POST");
        JSONObject resp = new JSONObject(preLoginResp);
        if (resp.has("response") && resp.getString("response").equals("error")) {
            System.err.println("preLogin error: " + resp.getInt("code"));
            return null;
        }

        // sign message
        String did = resp.getString("did");
        String message = resp.getString("message");
        String randomServer = resp.getString("random_server");
        String updatedTime = resp.getString("updated");
        ECKeyPair keyPair = ECKeyPair.create(ByteString.decodeHex(privateKey).toByteArray());
        Sign.SignatureData ethSignature = Sign.signPrefixedMessage(message.getBytes(), keyPair);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(ethSignature.getR());
        outputStream.write(ethSignature.getS());
        outputStream.write(ethSignature.getV());
        String sig = bytesToHex(outputStream.toByteArray());

        // login
        JSONObject loginIdentifier = new JSONObject();
        loginIdentifier.put("did", did);
        loginIdentifier.put("address", address);
        loginIdentifier.put("message", message);
        loginIdentifier.put("token", sig);
        JSONObject loginReq = new JSONObject();
        loginReq.put("type", "m.login.did.identity");
        loginReq.put("random_server", randomServer);
        loginReq.put("updated", updatedTime);
        loginReq.put("identifier", loginIdentifier);

        String loginResp = httpHelper.sendRequest(host, HttpHelper.URLs.did_login, loginReq, false, "POST");
        JSONObject loginRespObj = new JSONObject(loginResp);
        LoginData loginData = new LoginData();
        if (loginRespObj.has("response") && loginRespObj.getString("response").equals("error")) {
            loginData.setSuccess(false);
            System.err.println("login error: " + loginRespObj.getInt("code"));
        } else {
            loginData.setSuccess(true);
            loginData.setAccess_token(loginRespObj.getString("access_token"));
            loginData.setDevice_id(loginRespObj.getString("device_id"));
            loginData.setHome_server(host);
            loginData.setUser_id(loginRespObj.getString("user_id"));
            setLoginData(loginData);
        }
        return loginData;
    }

    public void setLoginData(LoginData loginData) {
        this.loginData = loginData;
        httpHelper.setAccess_token(loginData.getAccess_token());
        if(loginData.isSuccess()) {
            isLoggedIn = true;
        }
    }

    public void startSync() {
        syncee.startSyncee();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void logout() throws IOException {
        if (!isLoggedIn)
            return;

        httpHelper.sendRequestAsync(host, HttpHelper.URLs.logout, null, data -> {
            this.isLoggedIn = false;
        });
    }

    public void registerRoomEventListener(RoomEventsCallback event) {
        syncee.addRoomEventListener(event);
    }

    public void removeRoomEventListener(RoomEventsCallback event) {
        syncee.removeRoomEventListener(event);
    }

    public void joinRoom(String roomID) throws IOException {
        if (!isLoggedIn)
            return;

        httpHelper.sendRequest(host, HttpHelper.URLs.rooms + roomID + "/join", null, true, "POST");
    }

    public void leaveRoom(String roomID) throws IOException {
        if (!isLoggedIn)
            return;

        httpHelper.sendRequest(host, HttpHelper.URLs.rooms + roomID + "/leave", null, true, "POST");
    }

    public String sendMessage(String roomID, String message) throws IOException {
        if (!isLoggedIn)
            return null;

        JSONObject data = new JSONObject();
        data.put("msgtype", "m.text");
        data.put("body", message);
        return sendRoomEvent("m.room.message", roomID, data);
    }

    public String sendRoomEvent(String event, String roomID, JSONObject content) throws
            IOException {
        String eventId = "";
        String resp = httpHelper.sendRequest(host, HttpHelper.URLs.rooms + roomID + "/send/" + event + "/" + System.currentTimeMillis(), content, true, "PUT");
        try {
            eventId = new JSONObject(resp).getString("event_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return eventId;
    }

    public void inviteUser(String roomID, String userId, String reason) throws IOException {
        if (!isLoggedIn)
            return;

        JSONObject ob = new JSONObject();
        ob.put("user_id", userId);
        if (reason != null) {
            ob.put("reason", reason);
        }

        httpHelper.sendRequest(host, HttpHelper.URLs.rooms + roomID + "/invite", ob, true, "POST");
    }

    public void kickUser(String roomID, String userID, String reason) throws IOException {
        if (!isLoggedIn)
            return;

        JSONObject ob = new JSONObject();
        ob.put("reason", reason);
        ob.put("user_id", userID);

        httpHelper.sendRequest(host, HttpHelper.URLs.rooms + roomID + "/kick", ob, true, "POST");
    }

    public List<Room> getRooms() throws IOException {
        if (!isLoggedIn)
            return null;
        List<Room> rooms = new ArrayList<>();
        String resp = httpHelper.sendRequest(host, HttpHelper.URLs.client + "joined_rooms", null, true, "GET");
        try {
            JSONArray object = new JSONObject(resp).getJSONArray("joined_rooms");
            for(Object roomId : object) {
                rooms.add(new Room((String)roomId));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    public List<Member> getRoomMembers(String roomID) throws IOException {
        if (!isLoggedIn)
            return null;
        List<Member> members = new ArrayList<>();
        String resp = httpHelper.sendRequest(host, HttpHelper.URLs.rooms + roomID + "/joined_members", null, true, "GET");
        try {
            JSONObject object = new JSONObject(resp).getJSONObject("joined");
            Iterator<String> keys = object.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject user = object.getJSONObject(key);
                String avatar = "";
                if (user.has("avatar_url") && user.get("avatar_url") != null && user.get("avatar_url") instanceof String) {
                    avatar = user.getString("avatar_url");
                }
                members.add(new Member(
                        key,
                        user.getString("display_name"),
                        avatar
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return members;
    }

    public String createRoom(String name, String topic, List<String> invitations) throws IOException {
        if (!isLoggedIn)
            return null;

        JSONObject object = new JSONObject();
        object.put("name", name);
        if(topic != null){
            object.put("topic", topic);
        }
        if(invitations != null){
            JSONArray inviteUser = new JSONArray();
            for(String user : invitations){
                inviteUser.put(user);
            }
            object.put("invite", inviteUser);
        }
        return createRoom(object);
    }

    public String createRoom(JSONObject data) throws IOException {
        String roomId = "";
        String resp = httpHelper.sendRequest(host, HttpHelper.URLs.client + "createRoom", data, true, "POST");
        try {
            JSONObject object = new JSONObject(resp);
            if(object.has("room_id")) {
                roomId = object.getString("room_id");
            }
        } catch (JSONException ee) {
            ee.printStackTrace();
        }
        return roomId;
    }

    public Client(String host) {
        this.host = host;
        this.httpHelper = new HttpHelper();
        this.syncee = new Syncee(this, httpHelper);
        if (!host.endsWith("/"))
            this.host += "/";
    }

    /**
     * For testing only.
     */
    public Client(HttpHelper httpHelper, boolean isLoggedIn) {
        this.httpHelper = httpHelper;
        this.syncee = new Syncee(this, httpHelper);
        this.isLoggedIn = isLoggedIn;
    }

    public String getHost() {
        return host;
    }

    public LoginData getLoginData() {
        return loginData;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public String getDisplayname() throws IOException {
        String url = HttpHelper.URLs.client + "profile/" + this.loginData.getUser_id() + "/displayname";
        String resp = httpHelper.sendRequest(host, url, null, true, "GET");

        String displayname = "";
        try {
            displayname = new JSONObject(resp).getString("displayname");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return displayname;
    }

    public void setDisplayname(String displayname) throws IOException {
        JSONObject reqObj = new JSONObject();
        reqObj.put("displayname", displayname);
        String url = HttpHelper.URLs.client + "profile/" + this.loginData.getUser_id() + "/displayname";
        httpHelper.sendRequest(host, url, reqObj, true, "PUT");
    }
}