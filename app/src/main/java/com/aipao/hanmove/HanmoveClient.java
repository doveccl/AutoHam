package com.aipao.hanmove;

import com.loopj.android.http.*;

import java.util.Random;

public class HanmoveClient {
    public static String appID = "wx893d8d75f27b7348";
    public static String imei = "";
    public static boolean delay = false;

    public static String code = "";
    public static String IMEICode = "";
    public static String token = "";
    public static String nick = "";
    public static int len = 0;
    public static int sign = 0;
    public static int uid = 0;

    public String rid = "";
    public String length = "";
    public String time = "";
    public String lat = "";
    public String lng = "";

    private String api = "http://client2.aipao.me/api/";
    private AsyncHttpClient client = new AsyncHttpClient();
    private Random myRandom = new Random();

    HanmoveClient() {
        client.setTimeout(3000);
        client.setMaxRetriesAndTimeout(3, 1000);

        client.addHeader("auth", "authValue");
        client.addHeader("Connection", "Keep-Alive");
    }

    private int rand(int min, int max) {
        return myRandom.nextInt(max - min) + min;
    }

    private String encryptKey() {
        String map = "abcdefghijklmnopqrstuvwxyz";
        String key = "";
        for (int i = 0; i < 10; i++) {
            int j = myRandom.nextInt(map.length());
            key = key + map.charAt(j);
            map = map.replace(String.valueOf(map.charAt(j)), "");
        }
        return key;
    }

    private String encrypt(String key, String val) {
        String s = "";
        for (int i = 0; i < val.length(); i++) {
            int num = Integer.parseInt(String.valueOf(val.charAt(i)));
            s = s + key.charAt(num);
        }
        return s;
    }

    public void loginByCode(AsyncHttpResponseHandler handler) {
        String url = api + code + "/QM_Users/Login_Android";
        RequestParams params = new RequestParams();
        params.put("wxCode", code);
        params.put("IMEI", imei);
        client.get(url, params, handler);
    }

    public void loginByIMEICode(AsyncHttpResponseHandler handler) {
        String url = api + "%7Btoken%7D/QM_Users/Login_Android";
        RequestParams params = new RequestParams("IMEICode", IMEICode);
        client.get(url, params, handler);
    }

    public void getUserInfo(AsyncHttpResponseHandler handler) {
        String url = api + token + "/QM_Users/GetLoginInfoByUserId";
        client.get(url, handler);
    }

    public void checkUser(AsyncHttpResponseHandler handler) {
        String url = "https://test.doveccl.com/";
        RequestParams params = new RequestParams("uid", uid);
        client.get(url, params, handler);
    }

    public void setLatLng(AsyncHttpResponseHandler handler) {
        String url = api + token + "/QM_User_Field/SetLastLatLngByField";
        RequestParams params = new RequestParams();
        params.put("UserId", uid);
        params.put("FieldId", 0);
        params.put("Lat", String.valueOf(rand(30533393, 30534676) / 1000000.0));
        params.put("Lng", String.valueOf(rand(114367152, 114368055) / 1000000.0));
        client.get(url, params, handler);
    }

    public void sign(AsyncHttpResponseHandler handler) {
        String url = api + token + "/QM_Users/GetSignReward";
        client.get(url, handler);
    }

    public void startRunForSchool(AsyncHttpResponseHandler handler) {
        String url = api + token + "/QM_Runs/StartRunForSchool";
        RequestParams params = new RequestParams();
        params.put("Lat", lat);
        params.put("Lng", lng);
        params.put("RunType", 1);
        params.put("RunMode", 1);
        params.put("FUserId", 0);
        params.put("Level_Length", len);
        params.put("IsSchool", 1);
        client.get(url, params, handler);
    }

    public void endRunForSchool(AsyncHttpResponseHandler handler) {
        String url = api + token + "/QM_Runs/EndRunForSchool";
        String key = encryptKey();
        RequestParams params = new RequestParams();
        params.put("S1", rid);
        params.put("S2", encrypt(key, "5000"));
        params.put("S3", encrypt(key, len + ""));
        params.put("S4", encrypt(key, time));
        params.put("S5", encrypt(key, length));
        params.put("S6", "");
        params.put("S7", 1);
        params.put("S8", key);
        client.get(url, params, handler);
    }
}
