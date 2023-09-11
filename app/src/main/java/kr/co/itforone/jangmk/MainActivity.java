package kr.co.itforone.jangmk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.icu.text.UnicodeSetSpanner;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;
//import butterknife.BindView;
//import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.File;

import kr.co.itforone.jangmk.databinding.ActivityMainBinding;
import mc.kr.cross.sdk.CrossSdk;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding biding;

    WebView webView;
    public SwipeRefreshLayout refreshlayout = null;
    public CookieManager cookieManager;

    private long backPrssedTime = 0;
    public int flg_refresh = 1;

    final int FILECHOOSER_NORMAL_REQ_CODE = 1200, FILECHOOSER_LOLLIPOP_REQ_CODE = 1300;
    public ValueCallback<Uri> filePathCallbackNormal;
    public ValueCallback<Uri[]> filePathCallbackLollipop;
    public  Uri mCapturedImageURI;
    public String loadUrl = "";

    public static String TOKEN = ""; // 푸시토큰
    public ImageView imgGif;    // 로딩이미지

    public SharedPreferences preferences;  // 로그인데이터저장 ㅅㄷㄴㅅ
    public SharedPreferences.Editor pEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 데이터바인딩
        biding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        biding.setMainData(this);

        CrossSdk.INSTANCE.init(this, "10000132", "0cac525d83404fd081a2c65223ba9e69");
        CrossSdk.INSTANCE.openPopup(this);


        webView = biding.webview;
        refreshlayout = biding.refreshlayout;

        imgGif = biding.imgGif; // 로딩이미지
        Glide.with(this).asGif()    // GIF 로딩
                .load( R.raw.loading_spin )
                .override(200, 200)
                .diskCacheStrategy( DiskCacheStrategy.RESOURCE )    // Glide에서 캐싱한 리소스와 로드할 리소스가 같을때 캐싱된 리소스 사용
                .into( imgGif );

        setTOKEN(this);

        // 쿠키매니저
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);
        }
        setCookieAllow(cookieManager, webView);

        // 로그인데이터저장
        preferences = getSharedPreferences("member", Activity.MODE_PRIVATE);
        if(preferences!=null) {
            pEditor = preferences.edit();
        }

        Intent splash = new Intent(MainActivity.this,SplashActivity.class);
        startActivity(splash);

        // 버전코드
        int versionCode = 1;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        webView.addJavascriptInterface(new WebviewJavainterface(this),"Android");
        webView.setWebViewClient(new ClientManager(this));
        webView.setWebChromeClient(new ChoromeManager(this, this));
        WebSettings settings = webView.getSettings();
        settings.setUserAgentString(settings.getUserAgentString() + "INAPP/APP_VER="+ versionCode);
        settings.setTextZoom(100);
        settings.setJavaScriptEnabled(true);

        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {    // 뒤로가기 net::ERR_CACHE_MISS
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        settings.setAppCacheMaxSize(1024 * 1024 * 8); //8mb
        File dir = getCacheDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        settings.setAppCachePath(dir.getPath());
        settings.setAllowFileAccess(true);
        settings.setAppCacheEnabled(true);  // 앱내부캐시사용여부

        // 크롬디버깅
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setWebContentsDebuggingEnabled(true);
        }

        refreshlayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.clearCache(true);
                webView.reload();
                refreshlayout.setRefreshing(false);
            }
        });

        refreshlayout.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if(webView.getScrollY() == 0 && flg_refresh==1){
                    refreshlayout.setEnabled(true);
                } else{
                    refreshlayout.setEnabled(false);
                }
            }
        });

        // push&공유하기 url체크
        loadUrl = getString(R.string.index);
        try {
            Intent intent = getIntent();
            Uri uriData = intent.getData();
            String idx = uriData.getQueryParameter("idx");
            if (!idx.equals("")) {
                loadUrl = uriData.getQueryParameter("url").toString() + "?idx=" + uriData.getQueryParameter("idx").toString();
            } else if (!intent.getExtras().getString("goUrl").equals("")) {
                loadUrl = intent.getExtras().getString("goUrl");
            }

        } catch (Exception e) {
        }

        // 로그인데이터 전달..ㅎ
        loadUrl += (loadUrl.contains("?"))? "&" : "?";
        loadUrl += "app_mb_id=" + preferences.getString("appLoginId", "");
        Log.d("로그:onCreate", loadUrl);

        webView.loadUrl(loadUrl);
        webView.clearCache(true);
    }

    //뒤로가기이벤트
    @Override
    public void onBackPressed(){
        WebBackForwardList historyList = webView.copyBackForwardList();
        String currentUrl = webView.getUrl();

        if (currentUrl.equals(getString(R.string.index)) || currentUrl.equals(getString(R.string.domain)) || currentUrl.equals(getString(R.string.domain)+"/")) {
            long tempTime = System.currentTimeMillis();
            long intervalTime = tempTime - backPrssedTime;

            if (0 <= intervalTime && 2000 >= intervalTime) {
                finish();
            } else {
                backPrssedTime = tempTime;
                Toast.makeText(getApplicationContext(), "한번 더 뒤로가기 누를시 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if(webView.canGoBack()){
            String backTargetUrl = historyList.getItemAtIndex(historyList.getCurrentIndex() - 1).getUrl();

            if(backTargetUrl.equals(getString(R.string.login)) || historyList.getCurrentItem().getUrl().equals(getString(R.string.login)) || backTargetUrl.equals(getString(R.string.loginchk))) {
                webView.clearHistory();
                webView.loadUrl(getString(R.string.index));
                /*
                long tempTime = System.currentTimeMillis();
                long intervalTime = tempTime - backPrssedTime;
                backPrssedTime = tempTime;
                Toast.makeText(getApplicationContext(), "한번 더 뒤로가기 누를시 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
                return;
                */
            } else if (currentUrl.contains("order_form_result.php")) {
                webView.clearHistory();
                webView.loadUrl(getString(R.string.index));
            }

            webView.goBack();

        }else{
            long tempTime = System.currentTimeMillis();
            long intervalTime = tempTime - backPrssedTime;
            if (0 <= intervalTime && 2000 >= intervalTime){
                finish();
            }
            else
            {
                if (currentUrl.contains("market_view.php") || currentUrl.contains("order_form_result.php")) {
                    // 공유하기 타고왔을경우 or 결제완료 후 주문내역 이동시
                    webView.clearHistory();
                    webView.loadUrl(getString(R.string.index));
                } else {
                    backPrssedTime = tempTime;
                    Toast.makeText(getApplicationContext(), "한번 더 뒤로가기 누를시 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void Norefresh(){
        refreshlayout.setEnabled(false);
    }
    public void Yesrefresh(){
        refreshlayout.setEnabled(true);
    }
    public void setCookieAllow(CookieManager cookieManager, WebView webView) {
        try {
            cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                cookieManager.setAcceptThirdPartyCookies(webView, true);
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == FILECHOOSER_NORMAL_REQ_CODE) {
                if (filePathCallbackNormal == null) return;
                Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                filePathCallbackNormal.onReceiveValue(result);
                filePathCallbackNormal = null;

            } else if (requestCode == FILECHOOSER_LOLLIPOP_REQ_CODE) {
                Uri[] result = new Uri[0];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // 카메라/갤러리 선택
                    if (resultCode == RESULT_OK) {
                        result = (data == null) ? new Uri[]{mCapturedImageURI} : WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                    }
                    filePathCallbackLollipop.onReceiveValue(result);
                    filePathCallbackLollipop = null;
                }
            }
        } else {
            try {
                if (filePathCallbackLollipop != null) {
                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                    //webView.loadUrl("javascript:removeInputFile()");
                }
            } catch (Exception e) {
            }
        }
    }

    // 푸시..
    public static void setTOKEN(Activity mActivity){
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    return;
                }

                // Get new Instance ID token
                TOKEN = task.getResult().getToken();

                // Log and toast
                //String msg = mActivity.getString(R.string.msg_token_fmt, TOKEN);
                //Log.d("로그:token", msg);
            }
        });
    }
}
