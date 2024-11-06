package io.github.duzhaokun123.yapatch;

import android.app.ActivityThread;
import android.app.Application;
import android.app.LoadedApk;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.CompatibilityInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import de.robv.android.xposed.XposedHelpers;
import io.github.duzhaokun123.yapatch.hooks.SigBypass;
import io.github.duzhaokun123.yapatch.utils.Utils;
import top.canyie.pine.Pine;
import top.canyie.pine.PineConfig;
import top.canyie.pine.xposed.PineXposed;

public class LoaderMain {
    static String TAG = "YAPatch";
    private static final Map<String, String> archToLib = new HashMap<>(4);

    private static LoadedApk appLoadedApk;
    private static ActivityThread activityThread;

    private static PackageManager pm;

    public static void load() throws PackageManager.NameNotFoundException, JSONException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        archToLib.put("arm", "armeabi-v7a");
        archToLib.put("arm64", "arm64-v8a");
        archToLib.put("x86", "x86");
        archToLib.put("x86_64", "x86_64");

        PineConfig.debug = false;
        PineConfig.debuggable = false;
        PineConfig.disableHiddenApiPolicy = true;
        PineConfig.disableHiddenApiPolicyForPlatformDomain = true;

        HiddenApiBypass.addHiddenApiExemptions("");
        activityThread = ActivityThread.currentActivityThread();
        Log.d(TAG, "load on: " + activityThread.getProcessName());
        var context = createLoadedApkWithContext();
        if (context == null) {
            Log.e(TAG, "Error when creating context");
            return;
        }
        pm = context.getPackageManager();
        var applicationInfo = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        var config = new JSONObject(applicationInfo.metaData.getString("yapatch"));
        Class<?> VMRuntime = Class.forName("dalvik.system.VMRuntime");
        Method getRuntime = VMRuntime.getDeclaredMethod("getRuntime");
        getRuntime.setAccessible(true);
        Method vmInstructionSet = VMRuntime.getDeclaredMethod("vmInstructionSet");
        vmInstructionSet.setAccessible(true);
        String arch = (String) vmInstructionSet.invoke(getRuntime.invoke(null));
        String libName = archToLib.get(arch);

        PineConfig.libLoader = new Pine.LibLoader() {
            @Override
            public void loadLib() {
                try {
                    loadLibrary();
                } catch (ClassNotFoundException | NoSuchMethodException |
                         InvocationTargetException | IllegalAccessException |
                         PackageManager.NameNotFoundException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            public void loadLibrary() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, PackageManager.NameNotFoundException, JSONException {
                String sourceDir;
                try {
                   sourceDir = pm.getApplicationInfo(config.getString("manager"), 0).sourceDir;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Manager not found: " + config.getString("manager"));
                    Toast.makeText(context, "[YAPatch] Manager not found", Toast.LENGTH_LONG).show();
                    throw e;
                }
                var pineSo = sourceDir + "!/assets/yapatch/" + libName + "/libpine.so";
                try {
                    System.load(pineSo);
                } catch (UnsatisfiedLinkError e) {
                    Log.e(TAG, "Failed to load libpine.so: " + e.getMessage());
                    Toast.makeText(context, "[YAPatch] Unsupported " + libName, Toast.LENGTH_LONG).show();
                    throw e;
                }
            }
        };

        SigBypass.doSigBypass(context, config.getInt("sigbypassLevel"));

        var modules = Utils.fromJsonArray(config.getJSONArray("modules"));
        if (modules.length == 0) {
            Log.w(TAG, "No module to load");
            return;
        }
        for (String module : modules) {
            loadModule(module, libName);
        }
        PineXposed.onPackageLoad(context.getPackageName(), Application.getProcessName(), applicationInfo, true, context.getClassLoader());

        String originalAppComponentFactory = null;
        try {
            originalAppComponentFactory = config.getString("originalAppComponentFactory");
        } catch (JSONException ignored) {
        }
        Log.d(TAG, "originalAppComponentFactory: " + originalAppComponentFactory);
        if (originalAppComponentFactory != null) {
            try {
                context.getClassLoader().loadClass(originalAppComponentFactory);
            } catch (ClassNotFoundException e) { // This will happen on some strange shells like 360
                Log.w(TAG, "Original AppComponentFactory not found: " + originalAppComponentFactory);
            }
        }
    }

    private static Context createLoadedApkWithContext() {
        try {
            var mBoundApplication = XposedHelpers.getObjectField(activityThread, "mBoundApplication");
            appLoadedApk = (LoadedApk) XposedHelpers.getObjectField(mBoundApplication, "info");
            Log.i(TAG, "hooked app initialized: " + appLoadedApk);
            return (Context) XposedHelpers.callStaticMethod(Class.forName("android.app.ContextImpl"), "createAppContext", activityThread, appLoadedApk);
        } catch (Throwable e) {
            Log.e(TAG, "createLoadedApk", e);
            return null;
        }
    }

    private static void loadModule(String module, String libName) {
        ApplicationInfo moduleInfo;
        try {
            moduleInfo = pm.getApplicationInfo(module, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Module not found: " + module);
            return;
        }

        var modulePath = moduleInfo.sourceDir;
        var librarySearchPath = modulePath + "!/lib/" + libName;
        PineXposed.loadModule(new File(modulePath), librarySearchPath, false);
        Log.d(TAG, "Module loaded: " + module);
    }
}
