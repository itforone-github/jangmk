package kr.co.itforone.jangmk;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import mc.kr.cross.sdk.CrossSdk;
import util.LocationPosition;

class WebviewJavainterface {
    Activity activity;
    MainActivity mainActivity;

    WebviewJavainterface(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        
    }
    WebviewJavainterface(Activity activity) {
        this.activity = activity;
    }

    // 기기에서 제공하는 공유하기 사용
    @JavascriptInterface
    public void doShare(final String arg1, final String arg2) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, arg1);       // 제목
                shareIntent.putExtra(Intent.EXTRA_TEXT, arg2);          // 내용

                Intent chooser = Intent.createChooser(shareIntent, "공유하기");
                mainActivity.startActivity(chooser);
            }
        });
    }

    // 로그인데이터저장/삭제
    @JavascriptInterface
    public void updateLoginInfo(final String mb_id) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mainActivity.pEditor.putString("appLoginId", mb_id);
                mainActivity.pEditor.apply();

                Log.d("로그:updateLoginInfo()", mb_id);
            }
        });
    }
    //멀티앱크로스 회원가입 여부 확인하기
    @JavascriptInterface
    public void setSdkJoin(String mb_id){
        try {
            Log.d("sdk",mb_id);
            CrossSdk.INSTANCE.join(mb_id);
        }catch (Exception e){
            Toast.makeText(mainActivity.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }

    }
}
