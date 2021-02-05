package com.aipao.hanmove;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.loopj.android.http.TextHttpResponseHandler;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private EditText lat, lng, time, length;
    private Button random, run;
    private TextView logs, wait;

    private Random myRandom = new Random();

    private static final String configFile = "IMEICode";

    private HanmoveClient ham = new HanmoveClient();

    private AlertDialog confirm, retry;

    private boolean updateCode = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lat = (EditText) findViewById(R.id.lat);
        lng = (EditText) findViewById(R.id.lng);
        time = (EditText) findViewById(R.id.time);
        length = (EditText) findViewById(R.id.length);
        random = (Button) findViewById(R.id.random);
        run = (Button) findViewById(R.id.run);
        logs = (TextView) findViewById(R.id.logs);
        wait = (TextView) findViewById(R.id.count_down);

        logs.setMovementMethod(new ScrollingMovementMethod());
        logs.setFocusableInTouchMode(true);
        logs.setFocusable(true);
        logs.requestFocus();

        String [] permissions = {
                Manifest.permission.READ_PHONE_STATE
        };

        List<String> not_permit = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23) {
            for (String x : permissions)
                if (checkSelfPermission(x) != PackageManager.PERMISSION_GRANTED)
                    not_permit.add(x);
            if (not_permit.isEmpty())
                init();
            else
                requestPermissions(not_permit.toArray(new String[0]), 233);
        } else init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 233) {
            init();
        } else System.exit(0);
    }

    private void init() {
        HanmoveClient.imei = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId();

        random.setVisibility(View.INVISIBLE);
        run.setVisibility(View.INVISIBLE);

        random.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRandom();
            }
        });
        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRun();
            }
        });

        HanmoveClient.IMEICode = readIMEICode();
        if (!empty(HanmoveClient.IMEICode))
            ham.loginByIMEICode(loginHandler);
        else goWeChat();

        confirm = new AlertDialog.Builder(this).create();
        retry = new AlertDialog.Builder(this).create();
    }

    private void hideBtn() {
        random.setVisibility(View.INVISIBLE);
        run.setVisibility(View.INVISIBLE);
        wait.setText("您没有智商使用该软件");
        logs.setText("中国有句古话：闷声发大财\n");
    }

    private void showBtn() {
        random.setVisibility(View.VISIBLE);
        run.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog isExit = new AlertDialog.Builder(this).create();
            isExit.setTitle("提示");
            isExit.setMessage("你确定要退出吗？");
            isExit.setButton(DialogInterface.BUTTON_POSITIVE, "退出", clickListener);
            isExit.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", clickListener);
            isExit.show();
        }
        return false;
    }

    DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:
                    System.exit(0);
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    break;
                default:
                    break;
            }
        }
    };

    public void writeIMEICode(String message) {
        try {
            FileOutputStream fout = openFileOutput(configFile, MODE_PRIVATE);
            byte[] bytes = message.getBytes();
            fout.write(bytes);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readIMEICode() {
        String result = "";
        try {
            FileInputStream fin = openFileInput(configFile);
            int lenght = fin.available();
            byte[] buffer = new byte[lenght];
            fin.read(buffer);
            result = new String(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private class countDown extends AsyncTask <String, String, Object> {
        protected Object doInBackground(String... o) {
            int tot = Integer.parseInt(o[0]);
            String w = o[1];
            long s = System.currentTimeMillis() / 1000;
            long c = System.currentTimeMillis() / 1000;
            while (c <= tot + s) {
                long x = tot + s - c;
                publishProgress("请等待 " + x + " 秒...");
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                c = System.currentTimeMillis() / 1000;
            }
            publishProgress("");
            if ("sign".equals(w)) {
                ham.sign(signHandler);
            } else if ("enable".equals(w)) {
                publishProgress("enable");
            } else if ("end".equals(w)) {
                ham.endRunForSchool(endHandler);
            }
            return null;
        }

        protected void onProgressUpdate(String... o) {
            if ("enable".equals(o[0])) {
                random.setEnabled(true);
                run.setEnabled(true);
            } else
                setState(o[0]);
        }
    }

    public boolean empty(String s) {
        return s == null || "".equals(s);
    }

    public int rand(int min, int max) {
        return myRandom.nextInt(max - min) + min;
    }

    public void setState(String s) {
        wait.setText(s);
    }

    public void setLogs(String s) {
        logs.setText(s);
    }

    public void addLogs(String s) {
        s = logs.getText() + s;
        logs.setText(s);
    }

    public void setRandom() {
        ham.lat = String.valueOf(rand(30533393, 30534676) / 1000000.0);
        ham.lng = String.valueOf(rand(114367152, 114368055) / 1000000.0);
        ham.time = String.valueOf(rand(9 * 60, 17 * 60));
        ham.length = String.valueOf(HanmoveClient.len + rand(1, 10));

        lat.setText(ham.lat);
        lng.setText(ham.lng);
        time.setText(ham.time);
        length.setText(ham.length);
    }

    public void goWeChat() {
        IWXAPI api;
        HanmoveClient.code = "";
        api = WXAPIFactory.createWXAPI(MainActivity.this, HanmoveClient.appID, false);
        api.registerApp(HanmoveClient.appID);
        SendAuth.Req wxReq = new SendAuth.Req();
        wxReq.scope = "snsapi_userinfo";
        wxReq.state = "wechat_sdk_demo_test";

        if (api.sendReq(wxReq)) {
            updateCode = true;
            Toast.makeText(this, "登陆微信 ...", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(this, "调用微信接口失败", Toast.LENGTH_LONG).show();
    }

    protected void onRestart() {
        super.onRestart();
        if (!empty(HanmoveClient.code) && updateCode) {
            updateCode = false;
            Toast.makeText(this, "start req", Toast.LENGTH_SHORT);
            ham.loginByCode(loginHandler);
        }
    }

    TextHttpResponseHandler loginHandler = new TextHttpResponseHandler() {
        @Override
        public void onSuccess(int i, Header[] headers, String s) {
            try {
                JSONObject res = new JSONObject(s);
                if (res.getBoolean("Success")) {
                    JSONObject data = res.getJSONObject("Data");
                    HanmoveClient.token = data.getString("Token");
                    HanmoveClient.IMEICode = data.getString("IMEICode");
                    HanmoveClient.sign = data.getInt("SingleReward");
                    setLogs("Token:" + HanmoveClient.token + "\n");
                    ham.getUserInfo(infoHandler);
                } else {
                    setState("登录失败，请重启软件");
                    writeIMEICode("");
                    setLogs("错误代码:" + res.getString("ErrCode") + "\n");
                    addLogs("错误信息:" + res.getString("ErrMsg") + "\n");
                }
            } catch (JSONException e) {
                setState("JSON解析出错");
                setLogs(e.getMessage());
            }
        }

        @Override
        public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
            setState("HTTP请求错误");
            setLogs(s);
        }
    };

    TextHttpResponseHandler infoHandler = new TextHttpResponseHandler() {
        @Override
        public void onSuccess(int i, Header[] headers, String s) {
            try {
                JSONObject res = new JSONObject(s);
                if (res.getBoolean("Success")) {
                    JSONObject data = res.getJSONObject("Data");
                    HanmoveClient.uid = data.getJSONObject("User").getInt("UserID");
                    HanmoveClient.nick = data.getJSONObject("User").getString("NickName");
                    HanmoveClient.len = data.getJSONObject("SchoolRun").getInt("Lengths");
                    addLogs("用户ID:" + HanmoveClient.uid + "\n");
                    addLogs("用户名:" + HanmoveClient.nick + "\n");
                    addLogs("单次需跑长度:" + HanmoveClient.len + "\n");
                    addLogs("剩余体力值:" + data.getJSONObject("UserStatic").getInt("Powers") + "\n");
                    if (HanmoveClient.uid == 4551)
                        permitUsage();
                    else ham.checkUser(checkhandler);
                } else {
                    setState("获取信息失败");
                    setLogs("错误代码:" + res.getString("ErrCode") + "\n");
                    addLogs("错误信息:" + res.getString("ErrMsg") + "\n");
                }
            } catch (JSONException e) {
                setState("JSON解析出错");
                setLogs(e.getMessage());
            }
        }

        @Override
        public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
            setState("HTTP请求错误");
            setLogs(s);
        }
    };

    private void permitUsage() {
        showBtn();
        setRandom();
        confirm.setTitle("询问");
        confirm.setMessage("你确定要以" + HanmoveClient.nick + "的身份继续吗？");
        confirm.setButton(DialogInterface.BUTTON_POSITIVE, "继续", confirmListener);
        confirm.setButton(DialogInterface.BUTTON_NEGATIVE, "用微信登陆", confirmListener);
        confirm.setCancelable(false);
        if (empty(HanmoveClient.code))
            confirm.show();
        else {
            writeIMEICode(HanmoveClient.IMEICode);
            HanmoveClient.delay = HanmoveClient.uid == 433 * 25;
            ham.setLatLng(setHandler);
        }
    }

    TextHttpResponseHandler checkhandler = new TextHttpResponseHandler() {
        @Override
        public void onSuccess(int i, Header[] headers, String s) {
            try {
                JSONObject res = new JSONObject(s);
                if (res.getBoolean("enable"))
                    permitUsage();
                else {
                    hideBtn();
                    addLogs(HanmoveClient.uid + "\n");
                }
            } catch (JSONException e) {
                hideBtn();
                setState("JSON解析出错");
                setLogs(String.valueOf(e.getMessage()));
            }
        }

        @Override
        public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
                hideBtn();
                setState("HTTP请求错误");
                setLogs(headers[0].toString());
        }
    };

    DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:
                    writeIMEICode(HanmoveClient.IMEICode);
                    ham.setLatLng(setHandler);
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    goWeChat();
                    break;
                default:
                    break;
            }
        }
    };

    TextHttpResponseHandler setHandler = new TextHttpResponseHandler() {
        @Override
        public void onSuccess(int i, Header[] headers, String s) {
            try {
                JSONObject res = new JSONObject(s);
                if (res.getBoolean("Success")) {
                    addLogs("设置初始位置成功 !\n");
                } else {
                    addLogs("设置初始位置失败 !\n");
                }

                if (HanmoveClient.sign > 0) {
                    addLogs("今天是连续第 " + HanmoveClient.sign + " 天签到 ...\n");
                    if (HanmoveClient.delay) {
                        new countDown().execute(String.valueOf(rand(1, 3)), "sign");
                    } else
                        ham.sign(signHandler);
                } else {
                    addLogs("今天已签到，跳过签到 ...\n");
                    new countDown().execute("4", "enable");
                }
            } catch (JSONException e) {
                setState("JSON解析出错");
                setLogs(e.getMessage());
            }
        }

        @Override
        public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
            setState("HTTP请求错误");
            setLogs(s);
        }
    };

    TextHttpResponseHandler signHandler = new TextHttpResponseHandler() {
        @Override
        public void onSuccess(int i, Header[] headers, String s) {
            try {
                JSONObject res = new JSONObject(s);
                if (res.getBoolean("Success")) {
                    addLogs("签到成功 !\n");
                } else {
                    addLogs("签到失败 !\n");
                }
            } catch (JSONException e) {
                setState("JSON解析出错");
                setLogs(e.getMessage());
            }
            new countDown().execute("4", "enable");
        }

        @Override
        public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
            setState("HTTP请求错误");
            setLogs(s);
        }
    };

    public void startRun() {
        ham.lat = lat.getText().toString();
        ham.lng = lng.getText().toString();
        ham.time = time.getText().toString();
        ham.length = length.getText().toString();
        if (empty(ham.lat)) {
            Toast.makeText(this, "纬度不可为空", Toast.LENGTH_LONG).show();
            lat.requestFocus();
            return ;
        }
        if (empty(ham.lng)) {
            Toast.makeText(this, "经度不可为空", Toast.LENGTH_LONG).show();
            lng.requestFocus();
            return ;
        }
        if (empty(ham.time)) {
            Toast.makeText(this, "时间不可为空", Toast.LENGTH_LONG).show();
            time.requestFocus();
            return ;
        }
        if (empty(ham.length)) {
            Toast.makeText(this, "路程不可为空", Toast.LENGTH_LONG).show();
            length.requestFocus();
            return ;
        }
        ham.startRunForSchool(startHandler);
    }

    TextHttpResponseHandler startHandler = new TextHttpResponseHandler() {
        @Override
        public void onSuccess(int i, Header[] headers, String s) {
            try {
                JSONObject res = new JSONObject(s);
                if (res.getBoolean("Success")) {
                    addLogs("成功开始环跑 !\n");
                    ham.rid = res.getJSONObject("Data").getString("RunId");
                    addLogs("Run ID:" + ham.rid + "\n");
                    run.setEnabled(false);
                    random.setEnabled(false);
                    int cd_time = Integer.valueOf(ham.time);
                    if (HanmoveClient.delay) {
                        cd_time += rand(10, 12);
                    }
                    new countDown().execute(String.valueOf(cd_time), "end");
                } else {
                    setState("开始环跑失败 !\n");
                    setLogs("错误代码:" + res.getString("ErrCode") + "\n");
                    addLogs("错误信息:" + res.getString("ErrMsg") + "\n");
                }
            } catch (JSONException e) {
                setState("JSON解析出错");
                setLogs(e.getMessage());
            }
        }

        @Override
        public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
            setState("HTTP请求错误");
            setLogs(s);
        }
    };

    TextHttpResponseHandler endHandler = new TextHttpResponseHandler() {
        @Override
        public void onSuccess(int i, Header[] headers, String s) {
            try {
                JSONObject res = new JSONObject(s);
                if (res.getBoolean("Success")) {
                    setState("成功结束环跑 !\n");
                    setLogs("返回信息：" + s);
                } else {
                    retry.setTitle("结束环跑失败 !");
                    retry.setMessage("是否尝试重新发送数据包？\n不重试将会丢失本次成绩！");
                    retry.setButton(DialogInterface.BUTTON_POSITIVE, "重试", retryListener);
                    retry.setButton(DialogInterface.BUTTON_NEGATIVE, "不重试", retryListener);
                    retry.setCancelable(false);
                    retry.show();
                }
            } catch (JSONException e) {
                setState("JSON解析出错");
                setLogs(e.getMessage());
            }
        }

        @Override
        public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
            setState("HTTP请求错误");
            retry.setTitle("结束环跑失败 !");
            retry.setMessage("HTTP请求错误，请检查网络连接\n是否尝试重新发送中指数据包？\n不重试将会丢失本次成绩！");
            retry.setButton(DialogInterface.BUTTON_POSITIVE, "重试", retryListener);
            retry.setButton(DialogInterface.BUTTON_NEGATIVE, "不重试", retryListener);
            retry.setCancelable(false);
            retry.show();
            setLogs(s);
        }
    };

    DialogInterface.OnClickListener retryListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:
                    ham.endRunForSchool(endHandler);
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    break;
                default:
                    break;
            }
        }
    };
}