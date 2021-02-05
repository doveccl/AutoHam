package com.aipao.hanmove.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.aipao.hanmove.HanmoveClient;

import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    private IWXAPI api;
    private String code;

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        this.api = WXAPIFactory.createWXAPI(this, HanmoveClient.appID, false);
        this.api.registerApp(HanmoveClient.appID);
        this.api.handleIntent(getIntent(), this);
    }

    protected void onNewIntent(Intent paramIntent) {
        super.onNewIntent(paramIntent);
        setIntent(paramIntent);
        this.api.handleIntent(paramIntent, this);
    }

    public void onReq(BaseReq paramBaseReq) {
        finish();
    }

    public void onResp(BaseResp paramBaseResp) {
        if (paramBaseResp.errCode == 0) {
            HanmoveClient.code = ((SendAuth.Resp)paramBaseResp).code.trim();
        }
        while (true) {
            finish();
            return ;
        }
    }
}
