package com.sending.sdk;

import com.sending.sdk.callbacks.DataCallback;
import com.sending.sdk.exceptions.SDNException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpHelper {
    public static class URLs{
        private static final String root = "_api/";
        public static String client = root+"client/r0/";
        public static String did_address = client+"address/";
        public static String did_pre_login = client+"did/pre_login1";
        public static String did_login = client+"did/login";
        public static String logout = client+"logout";
        public static String rooms = client+"rooms/";
        public static String sync = client+"sync";
        public static String user  = client+"user/";
    }
    private final Logger LOGGER = Logger.getLogger("sdn-sdk-java");
    private String access_token;

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String sendRequest(String host, String path, JSONObject data, boolean useAccessToken, String requestMethod) throws IOException {
        String url = host+path + (useAccessToken ? "?access_token="+access_token  : "");
        URL obj = new URL(url);
        URLConnection con = obj.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod(requestMethod);
        http.setDoOutput(true);
        http.setReadTimeout(60000);

        if(data != null){
            int length = data.toString().length();
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        }

        http.connect();

        if(data != null){
            try(OutputStream os = http.getOutputStream()) {
                os.write(data.toString().getBytes());
            }
        }
        int responseCode = http.getResponseCode();

        String respStr = "";
        try(BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? http.getInputStream() : http.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            respStr = response.toString();
        }catch (IOException e){
            throw new SDNException(Integer.toString(responseCode), e.getMessage());
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            LOGGER.warning(String.format("get response code %d for %s", responseCode, host+path));

            SDNException sdnException;
            try {
                JSONObject respJson = new JSONObject(respStr);
                sdnException = new SDNException(respJson.getString("errcode"), respJson.getString("error"));
            } catch (JSONException e) {
                LOGGER.log(Level.WARNING, "invalid json response", e);
                sdnException = new SDNException(Integer.toString(responseCode), respStr);
            }
            throw sdnException;
        }

        return respStr;
    }

    public String sendStream(String host, String path, String contentType, InputStream data, int contentLength, boolean useAccesstoken, String requestMethod) throws IOException {
        String surl = host+path + (useAccesstoken ? "?access_token="+access_token  : "");

        URL obj = new URL(surl);
        URLConnection con = obj.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod(requestMethod);
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", contentType);
        http.addRequestProperty("Content-Length", Integer.toString(contentLength));

        try(OutputStream os = http.getOutputStream()) {
            int i = 0;
            int bytes = 0;
            while(bytes != -1) {
                byte []buff = new byte[1024];
                bytes = data.read(buff);
                if (bytes != -1) {
                    os.write(buff, 0, bytes);
                    i += bytes;
                }
            }

            os.flush();
            os.close();
        }

        try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }catch (IOException e){
            return "{\n" +
                    "  \"response\":\"error\",\n" +
                    "  \"code\":"+http.getResponseCode()+"\n" +
                    "}";
        }
    }

    public void sendStreamAsync(String host, String path, String contentType, int contentLength, InputStream data, boolean useAccesstoken, String requestMethod, DataCallback callback) throws IOException {
        if(callback == null){
            LOGGER.log(Level.WARNING, "callback must not be null!");
            return;
        }
        new Thread(() -> {
            try {
                String res = sendStream(host,path,contentType,data, contentLength, useAccesstoken,requestMethod);
                callback.onData(res);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "error send stream", e);
            }
        }).start();
    }

    public void sendRequestAsync(String host, String path, JSONObject data, DataCallback callback) throws IOException {
        sendRequestAsync(host, path, data, callback, access_token != null, "POST");
    }

    public void sendRequestAsync(String host, String path, JSONObject data, String requestMethd, DataCallback callback) throws IOException {
        sendRequestAsync(host, path, data, callback, access_token != null, requestMethd);
    }

    public void sendRequestAsync(String host, String path, JSONObject data, DataCallback callback, boolean useAccesstoken, String requestMethod) throws IOException {
        if(callback == null){
            LOGGER.log(Level.WARNING, "callback must not be null!");
            return;
        }
        new Thread(() -> {
            try {
                String res = sendRequest(host,path,data,useAccesstoken,requestMethod);
                callback.onData(res);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "error send stream", e);
            }
        }).start();
    }

}
