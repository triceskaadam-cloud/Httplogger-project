package com.httplogger;

import android.util.Log;
import java.io.IOException;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "HttpLogger";
    private static final String TARGET = "com.chaineapp.driver";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET.equals(lpparam.packageName)) return;
        Log.d(TAG, "Loaded in ChainDriver!");

        // Hook OkHttp3 Response
        try {
            Class<?> responseClass = XposedHelpers.findClass(
                "okhttp3.Response", lpparam.classLoader);
            
            XposedHelpers.findAndHookMethod(responseClass, "body",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object response = param.thisObject;
                        try {
                            // Get URL
                            Object request = XposedHelpers.callMethod(response, "request");
                            Object url = XposedHelpers.callMethod(request, "url");
                            String urlStr = url.toString();
                            
                            if (urlStr.contains("chaineapp")) {
                                int code = (int) XposedHelpers.callMethod(response, "code");
                                Log.d(TAG, "=== CHAINEAPP RESPONSE ===");
                                Log.d(TAG, "URL: " + urlStr);
                                Log.d(TAG, "Code: " + code);
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "Error: " + t.getMessage());
                        }
                    }
                });
            Log.d(TAG, "OkHttp Response.body() hooked!");
        } catch (Throwable t) {
            Log.e(TAG, "OkHttp hook failed: " + t.getMessage());
        }

        // Hook OkHttp3 ResponseBody.string()
        try {
            Class<?> responseBodyClass = XposedHelpers.findClass(
                "okhttp3.ResponseBody", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(responseBodyClass, "string",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String body = (String) param.getResult();
                        if (body != null && body.contains("trust")) {
                            Log.d(TAG, "=== CHAINEAPP BODY ===");
                            Log.d(TAG, body);
                        }
                    }
                });
            Log.d(TAG, "OkHttp ResponseBody.string() hooked!");
        } catch (Throwable t) {
            Log.e(TAG, "ResponseBody hook failed: " + t.getMessage());
        }

        // Hook URLConnection for non-OkHttp requests  
        try {
            XposedHelpers.findAndHookMethod(
                "java.net.URL", lpparam.classLoader,
                "openConnection",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object url = param.thisObject;
                        String urlStr = url.toString();
                        if (urlStr.contains("chaineapp")) {
                            Log.d(TAG, "=== URL.openConnection ===");
                            Log.d(TAG, "URL: " + urlStr);
                        }
                    }
                });
        } catch (Throwable t) {
            Log.e(TAG, "URLConnection hook failed: " + t.getMessage());
        }
    }
}
