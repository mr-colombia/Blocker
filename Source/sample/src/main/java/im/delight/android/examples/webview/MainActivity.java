package im.delight.android.examples.webview;

import im.delight.android.webview.AdvancedWebView;

import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.widget.Toast;
import android.webkit.WebView;
import android.view.View;
import android.webkit.WebViewClient;
import android.graphics.Bitmap;
import android.content.Intent;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.app.Activity;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements AdvancedWebView.Listener {

    private static final String TEST_PAGE_URL = "https://www.google.com/";
    private AdvancedWebView mWebView;
    private String url = null;
    private Holder holder = new Holder();
    private boolean reload = false;
    Object mLock = new Object();
    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
// Instantiate the cache
        final Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

// Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

// Instantiate the RequestQueue with the cache and network.
        requestQueue = new RequestQueue(cache, network);

// Start the queue
        requestQueue.start();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        url = getIntent().getDataString();

        mWebView = (AdvancedWebView) findViewById(R.id.webview);
        mWebView.setListener(this, this);
        mWebView.setGeolocationEnabled(false);
        mWebView.setMixedContentAllowed(true);
        mWebView.setCookiesEnabled(true);
        mWebView.setThirdPartyCookiesEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                //Toast.makeText(MainActivity.this, "Finished loading", Toast.LENGTH_SHORT).show();
            }

        });
        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                //Toast.makeText(MainActivity.this, title, Toast.LENGTH_SHORT).show();
            }

        });
        mWebView.addHttpHeader("X-Requested-With", "");
        mWebView.loadUrl(url);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
        url = getIntent().getDataString();
        Log.v(TAG, "resume url= " + url);
        mWebView.loadUrl(url);
        String source = getIntent().getStringExtra("Source");

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mWebView.onResume();
        url = getIntent().getDataString();
        Log.v(TAG, "restart url= " + url);
        mWebView.loadUrl(url);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        mWebView.onPause();
        super.onPause();
        mWebView.destroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        mWebView.destroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onDestroy() {
        mWebView.onDestroy();
        // ...

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mWebView.onActivityResult(requestCode, resultCode, intent);
        // ...
    }

    @Override
    public void onBackPressed() {
        if (!mWebView.onBackPressed()) {
            return;
        }
        // ...
        super.onBackPressed();
    }

    class Holder{
        boolean val;
    }


    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        if (!url.equals("about:blank")) {
            Log.v(TAG, "url= " + url);
            //Toast.makeText(MainActivity.this, "url = " + url, Toast.LENGTH_SHORT).show();
            if (url.equals("https://www.google.com/")) {
                mWebView.loadHtml("<h1>Te la creíste prro</h1>");
            }
            urlGetToken(url, holder);
            synchronized (mLock) {
                //SystemClock.sleep(5 * 1000);
                Log.v(TAG, "url= f " + holder.val);
                /*
                if (!holder.val) {
                    String tes = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">";
                    mWebView.loadHtml("<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "        \n"
                            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                            "<head>\n" +
                            "<style>\n" +
                            "\n" +
                            "body {\n" +
                            "    background-color: #f44336;\n" +
                            "    font-family: Arial;\n" +
                            "    color: white;\n" +
                            "}    \n" +
                            ".center {\n" +
                            "  margin: 0 auto;\n" +
                            "  border-radius:10px;\n" +
                            "  border-color: red;\n" +
                            "  padding: 20px;\n" +
                            "  position: relative;\n" +
                            "\n" +
                            "}\n" +
                            "h3{\n" +
                            "    font-size: 20px;\n" +
                            "    text-align: center;    \n" +
                            "}\n" +
                            "h6{\n" +
                            "    font-size: 6px;\n" +
                            "}\n" +
                            "hr{\n" +
                            "    background-color: white;\n" +
                            "}\n" +
                            "h5{\n" +
                            "    position: fixed;\n" +
                            "    bottom: 0;\n" +
                            "    font-size: 6px;\n" +
                            "    text-align: center;\n" +
                            "    font-style: italic;\n" +
                            "}\n" +
                            "</style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "\n" +
                            "\n" +
                            "\n" +
                            "<div class=\"center\">\n" +
                            "  <h3> <b>URL bloqueada por protección Banorte - BlockFish</b> </h3>\n" +
                            "  <hr>\n" +
                            "  <h6>El sitio web es: </h6>\n" +
                            "  <h6>Y representa una amenaza de seguridad.</h6>\n" +
                            "  <h5> Recuerda revisar con cuidado las URLs antes de seguirlas</h5>\n" +
                            "</div>\n" +
                            "\n" +
                            "</body>\n" +
                            "</html>");
                }
                */
                mWebView.setVisibility(View.INVISIBLE);
            }
        }
    }

    public boolean urlGetToken(final String url, final Holder holder) {

        final String req_url = "http://54.198.112.41:16084/api/v1.0/token";
        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, req_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Do something with the response
                        //Log.v(TAG, "Req= " + response);
                        urlOk(url, response, holder);
                        Log.v(TAG,"Req= f "+holder.val);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v(TAG, "Req= Err=" + error);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String credentials = "santi:semueretony";
                String auth = "Basic "
                        + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", auth);

                return headers;
            }
        };

// Add the request to the RequestQueue.
        requestQueue.add(stringRequest);
        Log.v(TAG,"Req= f2"+holder.val);;
        return holder.val;
    }

    public boolean urlOk(final String url, final String token, final Holder holder) {
        requestQueue.start();
        final String req_url = "http://54.198.112.41:16084/api/v1.0/domain?url="+url;
        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, req_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Do something with the response
                        synchronized (mLock) {
                            System.out.println();


                            Log.v(TAG, "Req= " + response);
                            JSONObject jsonObject = null;
                            try {
                                jsonObject = new JSONObject(response);
                                boolean aJsonString = jsonObject.getBoolean("is_safe");
                                holder.val = aJsonString;
                                Log.v(TAG, "Req= h " + holder.val + " url: "+url);
                                if (!holder.val) {
                                    String tes = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">";
                                    mWebView.loadHtml("<!DOCTYPE html>\n" +
                                            "<html>\n" +
                                            "        \n"
                                            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                                            "<head>\n" +
                                            "<style>\n" +
                                            "\n" +
                                            "body {\n" +
                                            "    background-color: #f44336;\n" +
                                            "    font-family: Arial;\n" +
                                            "    color: white;\n" +
                                            "}    \n" +
                                            ".center {\n" +
                                            "  margin: 0 auto;\n" +
                                            "  border-radius:10px;\n" +
                                            "  border-color: red;\n" +
                                            "  padding: 25px;\n" +
                                            "  position: relative;\n" +
                                            "\n" +
                                            "}\n" +
                                            "h3{\n" +
                                            "    font-size: 30px;\n" +
                                            "    text-align: center;    \n" +
                                            "}\n" +
                                            "h6{\n" +
                                            "    font-size: 12px;\n" +
                                            "}\n" +
                                            "hr{\n" +
                                            "    background-color: white;\n" +
                                            "}\n" +
                                            "h5{\n" +
                                            "    position: fixed;\n" +
                                            "    bottom: 0;\n" +
                                            "    font-size: 12px;\n" +
                                            "    text-align: center;\n" +
                                            "    font-style: italic;\n" +
                                            "}\n" +
                                            "</style>\n" +
                                            "</head>\n" +
                                            "<body>\n" +
                                            "\n" +
                                            "\n" +
                                            "\n" +
                                            "<div class=\"center\">\n" +
                                            "  <h3> <b>URL bloqueada por protección Banorte - BlockFish</b> </h3>\n" +
                                            "  <hr>\n" +
                                            "  <h6>El sitio web es:"+url+" </h6>\n" +
                                            "  <h6>Y representa una amenaza de seguridad.</h6>\n" +
                                            "  <h5> Recuerda revisar con cuidado las URLs antes de seguirlas</h5>\n" +
                                            "</div>\n" +
                                            "\n" +
                                            "</body>\n" +
                                            "</html>");
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mLock.notify();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v(TAG, "Req= Err=" + error);
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> headers = new HashMap<>();
                String tok = "";
                try {
                    JSONObject jsonObject = new JSONObject(token);
                    String aJsonString = jsonObject.getString("token");
                    tok=aJsonString;
                    Log.v(TAG,"Req= Tok="+tok);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String credentials = tok+":";
                String auth = "Basic "
                        + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Content-Type", "application/json");
                headers.put("Authorization",auth);
                return headers;
            }
        };

        requestQueue.add(stringRequest);
        Log.v(TAG,"Req= f3"+holder.val);
        //SystemClock.sleep(5*1000);
        return holder.val;
    }

    @Override
    public void onPageFinished(String url) {
        //Toast.makeText(MainActivity.this, "url = " + url, Toast.LENGTH_SHORT).show();
        mWebView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        //Toast.makeText(MainActivity.this, "onPageError(errorCode = " + errorCode + ",  description = " + description + ",  failingUrl = " + failingUrl + ")", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
        Toast.makeText(MainActivity.this, "onDownloadRequested(url = " + url + ",  suggestedFilename = " + suggestedFilename + ",  mimeType = " + mimeType + ",  contentLength = " + contentLength + ",  contentDisposition = " + contentDisposition + ",  userAgent = " + userAgent + ")", Toast.LENGTH_LONG).show();

		/*if (AdvancedWebView.handleDownload(this, url, suggestedFilename)) {
			// download successfully handled
		}
		else {
			// download couldn't be handled because user has disabled download manager app on the device
		}*/
    }

    @Override
    public void onExternalPageRequest(String url) {
        //Toast.makeText(MainActivity.this, "onExternalPageRequest(url = " + url + ")", Toast.LENGTH_SHORT).show();
    }

}
