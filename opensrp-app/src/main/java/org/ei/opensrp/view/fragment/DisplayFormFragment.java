package org.ei.opensrp.view.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;

import org.ei.opensrp.util.FormUtils;
import org.xwalk.core.JavascriptInterface;

import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ei.opensrp.R;
import org.ei.opensrp.view.activity.SecuredNativeSmartRegisterActivity;
import org.json.JSONObject;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkView;

import java.io.IOException;
import java.io.InputStream;
/**
 * Created by Geoffrey Koros on 9/12/2015.
 */
public class DisplayFormFragment extends Fragment {

    public static final String TAG = "DisplayFormFragment";
    XWalkView XwebView;
//    WebView webView;
    ProgressBar progressBar;
    public static String formInputErrorMessage = "Form contains errors please try again";// externalize this
    
    private static final String headerTemplate = "web/forms/header";
    private static final String footerTemplate = "web/forms/footer";
    private static final String scriptFile = "web/forms/js_include.js";

    private String formName;
    public static String okMessage = "ok";
    Dialog progressDialog;

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    private String recordId;

    private boolean javascriptLoaded = false;

    private JSONObject fieldOverides = new JSONObject();

    public JSONObject getFieldOverides() {
        return fieldOverides;
    }

    public void setFieldOverides(String overrides) {
        try{
            //get the field overrides map
            if (overrides != null){
                JSONObject json = new JSONObject(overrides);
                String overridesStr = json.getString("fieldOverrides");
                this.fieldOverides = new JSONObject(overridesStr);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.display_form_fragment, container, false);
//        webView = (WebView)view.findViewById(R.id.webview);
        XWalkPreferences.setValue("enable-javascript", true);
        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
        XWalkPreferences.setValue(XWalkPreferences.JAVASCRIPT_CAN_OPEN_WINDOW, true);

        XwebView = (XWalkView)view.findViewById(R.id.xwalkWebView);
        progressBar = (ProgressBar)view.findViewById(R.id.progressBar);
        initWebViewSettings();
        loadHtml();
        initProgressDialog();
        return view;
    }

    private void initWebViewSettings(){
//        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.getSettings().setGeolocationEnabled(true);
//        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        XwebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
//        XwebView.
//
//        webView.setWebViewClient(new AppWebViewClient(progressBar));
//        webView.setWebChromeClient(new WebChromeClient() {
//            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
//                callback.invoke(origin, true, false);
//            }
//        });
//
//        webView.getSettings().setGeolocationDatabasePath(getActivity().getFilesDir().getPath());
//        webView.getSettings().setDefaultTextEncodingName("utf-8");
               final MyJavaScriptInterface myJavaScriptInterface = new MyJavaScriptInterface(getActivity());
//        webView.addJavascriptInterface(myJavaScriptInterface, "Android");
        XwebView.addJavascriptInterface(myJavaScriptInterface,"Android");
    }

    public void loadHtml(){
//        showProgressDialog();
        String header = readFileAssets(headerTemplate);

        String script = readFileAssets(scriptFile);
        String modelString = readFileAssets("www/form/" + formName + "/model.xml").replaceAll("\"", "\\\\\"").replaceAll("\n", "").replaceAll("\r", "").replaceAll("/","\\\\/");;
        String form = readFileAssets("www/form/" + formName + "/form.xml");
        String footer = readFileAssets(footerTemplate);

        // inject the model and form into html template
        script = script.replace("$model_string_placeholder", modelString);
        footer = footer.replace("<!-- $script_placeholder >", script);

        StringBuilder sb = new StringBuilder();
        sb.append(header).append(form).append(footer);
//        webView.loadDataWithBaseURL("file:///android_asset/web/forms/", sb.toString(), "text/html", "utf-8", null);
        XwebView.load("file:///android_asset/web/forms/", sb.toString());
//        resizeForm();
        dismissProgressDialog();
    }

    public String readFileAssets(String fileName) {
        String fileContents = null;
        try {
            InputStream is = getActivity().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            fileContents = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        //Log.d("File", fileContents);
        return fileContents;
    }

    private void showProgressDialog(){
        XwebView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void dismissProgressDialog(){
        //dialog.dismiss();
        XwebView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        //loadFormData();
    }

    public void showTranslucentProgressDialog(){
        XwebView.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.show();
            }
        });
    }

    public void hideTranslucentProgressDialog(){
        XwebView.post(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.hide();
                }
            }
        });
    }

    String formData;


    public void loadFormData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    while (!javascriptLoaded){
                        Thread.sleep(100);
                    }

                    formData = formData != null && !formData.isEmpty() ? formData.replaceAll("\"","\\\"") : "";
                    XwebView.post(new Runnable() {
                        @Override
                        public void run() {
                            XwebView.load("javascript:loadDraft('" + formData + "')",null);
                            Log.e("posting data", formData);
                        }
                    });

                }catch(Exception doNothing){}
            }
        }).start();

    }

    //override this on tha child classes to override specific fields
    public JSONObject getFormFieldsOverrides(){
        return fieldOverides;
    }

    public void saveCurrentFormData() {

        XwebView.load("javascript:savePartialData()",null);
    }

    public void reloadDateWidget() {
        XwebView.post(new Runnable() {
            @Override
            public void run() {
                XwebView.load("javascript:refreshDateFields()",null);
                Log.d(TAG, "date widgets reloaded");
            }
        });
    }
//
//    public class AppWebViewClient extends WebViewClient {
//        private View progressBar;
//
//        public AppWebViewClient(ProgressBar progressBar) {
//            this.progressBar = progressBar;
//            progressBar.setVisibility(View.VISIBLE);
//        }
//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            // TODO Auto-generated method stub
//            view.loadUrl(url);
//            return true;
//        }
//
//        @Override
//        public void onPageFinished(WebView view, String url) {
//            // TODO Auto-generated method stub
//            super.onPageFinished(view, url);
//            progressBar.setVisibility(View.GONE);
//            dismissProgressDialog();
//        }
//    }

    public class MyJavaScriptInterface {
        Context mContext;

        MyJavaScriptInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void showFormErrorToast(){
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

            builder.setMessage(formInputErrorMessage)
                    .setCancelable(false)
                    .setPositiveButton(okMessage, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things

                        }
                    });

//            AlertDialog alert = builder.create();
//
//            alert.show();
            AlertDialog dialog = builder.show();
            TextView messageText = (TextView)dialog.findViewById(android.R.id.message);
            messageText.setGravity(Gravity.CENTER);
            messageText.setPadding(20, 20, 20, 20);
            dialog.show();
            dialog.setCanceledOnTouchOutside(true);
//            Toast.makeText(mContext, formInputErrorMessage, Toast.LENGTH_LONG).show();
        }

        @JavascriptInterface
        public void processFormSubmission(String formSubmission){
            showTranslucentProgressDialog();
            ((SecuredNativeSmartRegisterActivity)getActivity()).saveFormSubmission(formSubmission, recordId, formName, getFormFieldsOverrides());
        }

        @JavascriptInterface
        public void javascriptLoaded(){
            //Toast.makeText(mContext, "Javascript loaded", Toast.LENGTH_LONG).show();
            javascriptLoaded = true;
        }

        @JavascriptInterface
        public void savePartialFormData(String partialData){
            //Toast.makeText(mContext, "saving un-submitted form data", Toast.LENGTH_LONG).show();
            ((SecuredNativeSmartRegisterActivity)getActivity()).savePartialFormData(partialData, recordId, formName, getFormFieldsOverrides());
        }

    }
    public void showForm(final ViewPager viewPager, final int formIndex, String entityId, final String metaData, boolean loadPrevious){
        viewPager.setCurrentItem(formIndex, false); //Don't animate the view on orientation change the view disapears

        showTranslucentProgressDialog();

        if (entityId != null || metaData != null){
            String data = null;
            //check if there is previously saved data for the form
            if(loadPrevious){
                data = FormUtils.getInstance(getActivity()).getPreviouslySavedDataForForm(formName, metaData, entityId);
            }

            if (data == null){
                data = FormUtils.getInstance(getActivity()).generateXMLInputForFormWithEntityId(entityId, formName, metaData);
            }

            setRecordId(entityId);
            setFieldOverides(metaData);

            final String finalData = data;
            Thread tm = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Wait for the page to initialize
                        while (!javascriptLoaded) {
                            Thread.sleep(100);
                        }

                        if (finalData != null && !finalData.isEmpty()) {
                            postXmlDataToFormWWait(finalData);
                        } else {
                            resetForm();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.toString(), e);
                    }
                }
            });
            tm.start();
        }
    }

    public void setFormData(final String data){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    // Wait for the page to initialize
                    while (!javascriptLoaded){
                        Thread.sleep(100);
                    }

                    if (data != null && !data.isEmpty()){
                        postXmlDataToForm(data);
                    }else{
                        resetForm();
                    }
                }catch(Exception e){
                    Log.e(TAG, e.toString(), e);
                }
            }
        }).start();
    }
    public void resetForm(){
        XwebView.post(new Runnable() {
            @Override
            public void run() {
                XwebView.load("javascript:resetForm()",null);
                Log.d(TAG, "reseting form");
            }
        });
    }

    private void postXmlDataToFormWWait(final String data) throws InterruptedException {
        Runnable t = new Runnable() {
            @Override
            public void run() {
                try {
                    XwebView.evaluateJavascript("javascript:loadDraft('" + data + "')", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            Log.d("RECEIVED VALUE : ", s); // Returns the value from the function
                            hideTranslucentProgressDialog();
                        }
                    });
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        XwebView.post(t);
    }

    private void postXmlDataToForm(final String data){
        XwebView.post(new Runnable() {
            @Override
            public void run() {
                XwebView.load("javascript:loadDraft('" + data + "')",null);
                Log.d("posting data", data);
            }
        });
    }

    private void resizeForm() {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int landWidthPixels = 0;
                int landHeightPixels = 0;

                WindowManager w = getActivity().getWindowManager();
                Display d = w.getDefaultDisplay();

                if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
                    try {
                        landWidthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                        landHeightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if(Build.VERSION.SDK_INT > 17) {
                    try {
                        Point realSize = new Point();
                        Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                        landWidthPixels = realSize.x;
                        landHeightPixels = realSize.y;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                XwebView.setLayoutParams(new RelativeLayout.LayoutParams(landHeightPixels, landWidthPixels));
            }
        });
    }

    private void initProgressDialog() {
        progressDialog = new Dialog(getActivity(), R.style.progress_dialog_theme);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setContentView(R.layout.progress_dialog);
    }
}