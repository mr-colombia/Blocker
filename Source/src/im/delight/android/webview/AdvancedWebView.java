package im.delight.android.webview;

import android.os.Message;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebStorage.QuotaUpdater;
import android.app.Fragment;
import android.util.Base64;
import android.os.Build;
import android.webkit.DownloadListener;
import android.graphics.Bitmap;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import java.util.MissingResourceException;
import java.util.Locale;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Advanced WebView component for Android that works as intended out of the box
 *
 * @author delight.im <info@delight.im>
 * @see <a href="https://github.com/delight-im/Android-AdvancedWebView">Android-AdvancedWebView on GitHub</a>
 * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>
 */
@SuppressWarnings("deprecation")
public class AdvancedWebView extends WebView {

	public static interface Listener {
		public void onPageStarted(String url, Bitmap favicon);
		public void onPageFinished(String url);
		public void onPageError(int errorCode, String description, String failingUrl);
		public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength);
		public void onExternalPageRequest(String url);
	}

	protected static final int REQUEST_CODE_FILE_PICKER = 51426;
	protected static final String DATABASES_SUB_FOLDER = "/databases";
	protected static final String LANGUAGE_DEFAULT_ISO3 = "eng";
	protected static final String CHARSET_DEFAULT = "UTF-8";
	protected WeakReference<Activity> mActivity;
	protected WeakReference<Fragment> mFragment;
	protected Listener mListener;
	protected List<String> mPermittedHostnames;
	/** File upload callback for platform versions prior to Android 5.0 */
	protected ValueCallback<Uri> mFileUploadCallbackFirst;
	/** File upload callback for Android 5.0+ */
	protected ValueCallback<Uri[]> mFileUploadCallbackSecond;
	protected long mLastError;
	protected String mLanguageIso3;
	protected int mRequestCodeFilePicker = REQUEST_CODE_FILE_PICKER;
	protected WebChromeClient mCustomWebChromeClient;

	public AdvancedWebView(Context context) {
		super(context);
		init(context);
	}

	public AdvancedWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public AdvancedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public void setListener(final Activity activity, final Listener listener) {
		setListener(activity, listener, REQUEST_CODE_FILE_PICKER);
	}

	public void setListener(final Activity activity, final Listener listener, final int requestCodeFilePicker) {
		if (activity != null) {
			mActivity = new WeakReference<Activity>(activity);
		}
		else {
			mActivity = null;
		}

		setListener(listener, requestCodeFilePicker);
	}

	public void setListener(final Fragment fragment, final Listener listener) {
		setListener(fragment, listener, REQUEST_CODE_FILE_PICKER);
	}

	public void setListener(final Fragment fragment, final Listener listener, final int requestCodeFilePicker) {
		if (fragment != null) {
			mFragment = new WeakReference<Fragment>(fragment);
		}
		else {
			mFragment = null;
		}

		setListener(listener, requestCodeFilePicker);
	}

	protected void setListener(final Listener listener, final int requestCodeFilePicker) {
		mListener = listener;
		mRequestCodeFilePicker = requestCodeFilePicker;
	}

	@Override
	public void setWebChromeClient(WebChromeClient client) {
		mCustomWebChromeClient = client;
	}

	public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		if (requestCode == mRequestCodeFilePicker) {
			if (resultCode == Activity.RESULT_OK) {
				if (intent != null) {
					if (mFileUploadCallbackFirst != null) {
						mFileUploadCallbackFirst.onReceiveValue(intent.getData());
						mFileUploadCallbackFirst = null;
					}
					else if (mFileUploadCallbackSecond != null) {
						Uri[] dataUris;
						try {
							dataUris = new Uri[] { Uri.parse(intent.getDataString()) };
						}
						catch (Exception e) {
							dataUris = null;
						}

						mFileUploadCallbackSecond.onReceiveValue(dataUris);
						mFileUploadCallbackSecond = null;
					}
				}
			}
			else {
				if (mFileUploadCallbackFirst != null) {
					mFileUploadCallbackFirst.onReceiveValue(null);
					mFileUploadCallbackFirst = null;
				}
				else if (mFileUploadCallbackSecond != null) {
					mFileUploadCallbackSecond.onReceiveValue(null);
					mFileUploadCallbackSecond = null;
				}
			}
		}
	}

	public void addPermittedHostname(String hostname) {
		mPermittedHostnames.add(hostname);
	}

	public void addPermittedHostnames(Collection<? extends String> collection) {
		mPermittedHostnames.addAll(collection);
	}

	public List<String> getPermittedHostnames() {
		return mPermittedHostnames;
	}

	public void removePermittedHostname(String hostname) {
		mPermittedHostnames.remove(hostname);
	}

	public void clearPermittedHostnames() {
		mPermittedHostnames.clear();
	}

	public boolean onBackPressed() {
		if (canGoBack()) {
			goBack();
			return false;
		}
		else {
			return true;
		}
	}

	@SuppressLint({ "SetJavaScriptEnabled", "NewApi" })
	protected void init(Context context) {
		if (context instanceof Activity) {
			mActivity = new WeakReference<Activity>((Activity) context);
		}

		mLanguageIso3 = getLanguageIso3();

		mPermittedHostnames = new LinkedList<String>();

		setFocusable(true);
		setFocusableInTouchMode(true);

		setSaveEnabled(true);

		final String filesDir = context.getFilesDir().getPath();
		final String databaseDir = filesDir.substring(0, filesDir.lastIndexOf("/")) + DATABASES_SUB_FOLDER;

		final WebSettings webSettings = getSettings();
		webSettings.setAllowFileAccess(false);
		if (Build.VERSION.SDK_INT >= 16) {
			webSettings.setAllowFileAccessFromFileURLs(false);
			webSettings.setAllowUniversalAccessFromFileURLs(false);
		}
		webSettings.setBuiltInZoomControls(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setDomStorageEnabled(true);
		if (Build.VERSION.SDK_INT < 18) {
			webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
		}
		webSettings.setDatabaseEnabled(true);
		if (Build.VERSION.SDK_INT < 19) {
			webSettings.setDatabasePath(databaseDir);
		}

		setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if (!hasError()) {
					if (mListener != null) {
						mListener.onPageStarted(url, favicon);
					}
				}
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				if (!hasError()) {
					if (mListener != null) {
						mListener.onPageFinished(url);
					}
				}
			}

			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				setLastError();

				if (mListener != null) {
					mListener.onPageError(errorCode, description, failingUrl);
				}
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (isHostnameAllowed(url)) {
					return false;
				}
				else {
					if (mListener != null) {
						mListener.onExternalPageRequest(url);
					}

					return true;
				}
			}

		});

		super.setWebChromeClient(new WebChromeClient() {

			// file upload callback (Android 2.2 -- 4.3) (hidden method)
			@SuppressWarnings("unused")
			public void openFileChooser(ValueCallback<Uri> uploadMsg) {
				openFileChooser(uploadMsg, null);
			}

			// file upload callback (Android 2.2 -- 4.3) (hidden method)
			public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
				openFileChooser(uploadMsg, acceptType, null);
			}

			// file upload callback (Android 2.2 -- 4.3) (hidden method)
			@SuppressWarnings("unused")
			public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
				openFileInput(uploadMsg, null);
			}

			// file upload callback (Android 5.0+)
			@SuppressWarnings("all")
			public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
				openFileInput(null, filePathCallback);
				return true;
			}

			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onProgressChanged(view, newProgress);
				}
				else {
					super.onProgressChanged(view, newProgress);
				}
			}

			@Override
			public void onReceivedTitle(WebView view, String title) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onReceivedTitle(view, title);
				}
				else {
					super.onReceivedTitle(view, title);
				}
			}

			@Override
			public void onReceivedIcon(WebView view, Bitmap icon) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onReceivedIcon(view, icon);
				}
				else {
					super.onReceivedIcon(view, icon);
				}
			}

			@Override
			public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
				}
				else {
					super.onReceivedTouchIconUrl(view, url, precomposed);
				}
			}

			@Override
			public void onShowCustomView(View view, CustomViewCallback callback) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onShowCustomView(view, callback);
				}
				else {
					super.onShowCustomView(view, callback);
				}
			}

			@SuppressWarnings("all")
			public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onShowCustomView(view, requestedOrientation, callback);
				}
				else {
					super.onShowCustomView(view, requestedOrientation, callback);
				}
			}

			@Override
			public void onHideCustomView() {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onHideCustomView();
				}
				else {
					super.onHideCustomView();
				}
			}

			@Override
			public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
				}
				else {
					return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
				}
			}

			@Override
			public void onRequestFocus(WebView view) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onRequestFocus(view);
				}
				else {
					super.onRequestFocus(view);
				}
			}

			@Override
			public void onCloseWindow(WebView window) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onCloseWindow(window);
				}
				else {
					super.onCloseWindow(window);
				}
			}

			@Override
			public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsAlert(view, url, message, result);
				}
				else {
					return super.onJsAlert(view, url, message, result);
				}
			}

			@Override
			public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsConfirm(view, url, message, result);
				}
				else {
					return super.onJsConfirm(view, url, message, result);
				}
			}

			@Override
			public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsPrompt(view, url, message, defaultValue, result);
				}
				else {
					return super.onJsPrompt(view, url, message, defaultValue, result);
				}
			}

			@Override
			public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsBeforeUnload(view, url, message, result);
				}
				else {
					return super.onJsBeforeUnload(view, url, message, result);
				}
			}

			@Override
			public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
				}
				else {
					super.onGeolocationPermissionsShowPrompt(origin, callback);
				}
			}

			@Override
			public void onGeolocationPermissionsHidePrompt() {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onGeolocationPermissionsHidePrompt();
				}
				else {
					super.onGeolocationPermissionsHidePrompt();
				}
			}

			@SuppressWarnings("all")
			public void onPermissionRequest(PermissionRequest request) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onPermissionRequest(request);
				}
				else {
					super.onPermissionRequest(request);
				}
			}

			@SuppressWarnings("all")
			public void onPermissionRequestCanceled(PermissionRequest request) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onPermissionRequestCanceled(request);
				}
				else {
					super.onPermissionRequestCanceled(request);
				}
			}

			@Override
			public boolean onJsTimeout() {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsTimeout();
				}
				else {
					return super.onJsTimeout();
				}
			}

			@Override
			public void onConsoleMessage(String message, int lineNumber, String sourceID) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onConsoleMessage(message, lineNumber, sourceID);
				}
				else {
					super.onConsoleMessage(message, lineNumber, sourceID);
				}
			}

			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onConsoleMessage(consoleMessage);
				}
				else {
					return super.onConsoleMessage(consoleMessage);
				}
			}

			@Override
			public Bitmap getDefaultVideoPoster() {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.getDefaultVideoPoster();
				}
				else {
					return super.getDefaultVideoPoster();
				}
			}

			@Override
			public View getVideoLoadingProgressView() {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.getVideoLoadingProgressView();
				}
				else {
					return super.getVideoLoadingProgressView();
				}
			}

			@Override
			public void getVisitedHistory(ValueCallback<String[]> callback) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.getVisitedHistory(callback);
				}
				else {
					super.getVisitedHistory(callback);
				}
			}

			@Override
			public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota, long estimatedDatabaseSize, long totalQuota, QuotaUpdater quotaUpdater) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
				}
				else {
					super.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
				}
			}

			@Override
			public void onReachedMaxAppCacheSize(long requiredStorage, long quota, QuotaUpdater quotaUpdater) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
				}
				else {
					super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
				}
			}

		});

		setDownloadListener(new DownloadListener() {

			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
				if (mListener != null) {
					mListener.onDownloadRequested(url, userAgent, contentDisposition, mimetype, contentLength);
				}
			}

		});
	}

	public void loadUrl(String url, final boolean preventCaching) {
		if (preventCaching) {
			url = makeUrlUnique(url);
		}

		loadUrl(url);
	}

	public void loadUrl(String url, final boolean preventCaching, final Map<String,String> additionalHttpHeaders) {
		if (preventCaching) {
			url = makeUrlUnique(url);
		}

		loadUrl(url, additionalHttpHeaders);
	}

	protected static String makeUrlUnique(final String url) {
		StringBuilder unique = new StringBuilder();
		unique.append(url);

		if (url.contains("?")) {
			unique.append('&');
		}
		else {
			if (url.lastIndexOf('/') <= 7) {
				unique.append('/');
			}
			unique.append('?');
		}

		unique.append(System.currentTimeMillis());
		unique.append('=');
		unique.append(1);

		return unique.toString();
	}

	protected boolean isHostnameAllowed(String url) {
		if (mPermittedHostnames.size() == 0) {
			return true;
		}

		url = url.replace("http://", "");
		url = url.replace("https://", "");

		for (String hostname : mPermittedHostnames) {
			if (url.startsWith(hostname)) {
				return true;
			}
		}

		return false;
	}

	protected void setLastError() {
		mLastError = System.currentTimeMillis();
	}

	protected boolean hasError() {
		return (mLastError + 500) >= System.currentTimeMillis();
	}

	protected static String getLanguageIso3() {
		try {
			return Locale.getDefault().getISO3Language().toLowerCase(Locale.US);
		}
		catch (MissingResourceException e) {
			return LANGUAGE_DEFAULT_ISO3;
		}
	}

	/** Provides localizations for the 25 most widely spoken languages that have a ISO 639-2/T code */
	protected String getFileUploadPromptLabel() {
		try {
			if (mLanguageIso3.equals("zho")) return decodeBase64("6YCJ5oup5LiA5Liq5paH5Lu2");
			else if (mLanguageIso3.equals("spa")) return "Elija un archivo";
			else if (mLanguageIso3.equals("hin")) return decodeBase64("4KSP4KSVIOCkq+CkvOCkvuCkh+CksiDgpJrgpYHgpKjgpYfgpII=");
			else if (mLanguageIso3.equals("ben")) return decodeBase64("4KaP4KaV4Kaf4Ka/IOCmq+CmvuCmh+CmsiDgpqjgpr/gprDgp43gpqzgpr7gpprgpqg=");
			else if (mLanguageIso3.equals("ara")) return decodeBase64("2KfYrtiq2YrYp9ixINmF2YTZgSDZiNin2K3Yrw==");
			else if (mLanguageIso3.equals("por")) return "Escolha um arquivo";
			else if (mLanguageIso3.equals("rus")) return decodeBase64("0JLRi9Cx0LXRgNC40YLQtSDQvtC00LjQvSDRhNCw0LnQuw==");
			else if (mLanguageIso3.equals("jpn")) return decodeBase64("MeODleOCoeOCpOODq+OCkumBuOaKnuOBl+OBpuOBj+OBoOOBleOBhA==");
			else if (mLanguageIso3.equals("pan")) return decodeBase64("4KiH4Kmx4KiVIOCoq+CovuCoh+CosiDgqJrgqYHgqKPgqYs=");
			else if (mLanguageIso3.equals("deu")) return "W�hle eine Datei";
			else if (mLanguageIso3.equals("jav")) return "Pilih siji berkas";
			else if (mLanguageIso3.equals("msa")) return "Pilih satu fail";
			else if (mLanguageIso3.equals("tel")) return decodeBase64("4LCS4LCVIOCwq+CxhuCxluCwsuCxjeCwqOCxgSDgsI7gsILgsJrgsYHgsJXgsYvgsILgsKHgsL8=");
			else if (mLanguageIso3.equals("vie")) return decodeBase64("Q2jhu41uIG3hu5l0IHThuq1wIHRpbg==");
			else if (mLanguageIso3.equals("kor")) return decodeBase64("7ZWY64KY7J2YIO2MjOydvOydhCDshKDtg50=");
			else if (mLanguageIso3.equals("fra")) return "Choisissez un fichier";
			else if (mLanguageIso3.equals("mar")) return decodeBase64("4KSr4KS+4KSH4KSyIOCkqOCkv+CkteCkoeCkvg==");
			else if (mLanguageIso3.equals("tam")) return decodeBase64("4K6S4K6w4K+BIOCuleCvh+CuvuCuquCvjeCuquCviCDgrqTgr4fgrrDgr43grrXgr4E=");
			else if (mLanguageIso3.equals("urd")) return decodeBase64("2KfbjNqpINmB2KfYptmEINmF24zauiDYs9uSINin2YbYqtiu2KfYqCDaqdix24zaug==");
			else if (mLanguageIso3.equals("fas")) return decodeBase64("2LHYpyDYp9mG2KrYrtin2Kgg2qnZhtuM2K8g24zaqSDZgdin24zZhA==");
			else if (mLanguageIso3.equals("tur")) return "Bir dosya se�in";
			else if (mLanguageIso3.equals("ita")) return "Scegli un file";
			else if (mLanguageIso3.equals("tha")) return decodeBase64("4LmA4Lil4Li34Lit4LiB4LmE4Lif4Lil4LmM4Lir4LiZ4Li24LmI4LiH");
			else if (mLanguageIso3.equals("guj")) return decodeBase64("4KqP4KqVIOCqq+CqvuCqh+CqsuCqqOCrhyDgqqrgqrjgqoLgqqY=");
		}
		catch (Exception e) { }

		// return English translation by default
		return "Choose a file";
	}

	protected static String decodeBase64(final String base64) throws IllegalArgumentException, UnsupportedEncodingException {
		final byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
		return new String(bytes, CHARSET_DEFAULT);
	}

	@SuppressLint("NewApi")
	protected void openFileInput(final ValueCallback<Uri> fileUploadCallbackFirst, final ValueCallback<Uri[]> fileUploadCallbackSecond) {
		if (mFileUploadCallbackFirst != null) {
			mFileUploadCallbackFirst.onReceiveValue(null);
		}
		mFileUploadCallbackFirst = fileUploadCallbackFirst;

		if (mFileUploadCallbackSecond != null) {
			mFileUploadCallbackSecond.onReceiveValue(null);
		}
		mFileUploadCallbackSecond = fileUploadCallbackSecond;

		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		i.setType("*/*");

		if (mFragment != null && mFragment.get() != null && Build.VERSION.SDK_INT >= 11) {
			mFragment.get().startActivityForResult(Intent.createChooser(i, getFileUploadPromptLabel()), mRequestCodeFilePicker);
		}
		else if (mActivity != null && mActivity.get() != null) {
			mActivity.get().startActivityForResult(Intent.createChooser(i, getFileUploadPromptLabel()), mRequestCodeFilePicker);
		}
	}

}