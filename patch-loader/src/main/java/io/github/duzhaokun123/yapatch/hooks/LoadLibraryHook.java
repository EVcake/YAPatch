package io.github.duzhaokun123.yapatch.hooks;

import android.content.Context;
import android.util.Log;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

public class LoadLibraryHook {
    static String TAG = "YAPatch-LoadLibraryHook";
    public static void hook(Context context, String[] modules) throws NoSuchMethodException {
        Log.d(TAG, "Hooking loadLibrary");
        var pm = context.getPackageManager();
        Pine.hook(System.class.getDeclaredMethod("loadLibrary", String.class), new MethodHook() {
            @Override
            public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                callFrame.setResult(null);
                Log.d(TAG, "loadLibrary called" + callFrame.args[0], callFrame.getThrowable());
                String libName = (String) callFrame.args[0];
                Log.d(TAG, "Loading library " + libName);

                var thisSourceDir = pm.getApplicationInfo(context.getPackageName(), 0).sourceDir;
                var thisSoPath = thisSourceDir + "!/lib/arm64-v8a/lib" + libName + ".so";
                Log.d(TAG, "Trying to load from " + thisSoPath);
                try {
                    System.load(thisSoPath);
                    Log.d(TAG, "Loaded library " + thisSoPath);
                    return;
                } catch (UnsatisfiedLinkError ignored) {
                }

                for (String module : modules) {
                    String sourceDir;
                    try {
                        sourceDir = pm.getApplicationInfo(module, 0).sourceDir;
                    } catch (Exception e) {
                        continue;
                    }
                    sourceDir = sourceDir + "!/lib/arm64-v8a";
                    var soPatch = sourceDir + "/lib" + libName + ".so";
                    Log.d(TAG, "Trying to load from " + soPatch);
                    try {
                        System.load(soPatch);
                        Log.d(TAG, "Loaded library " + soPatch);
                        return;
                    } catch (UnsatisfiedLinkError ignored) {
                    }
                }
                Log.d(TAG, "Failed to load library " + libName);
                callFrame.setThrowable(new UnsatisfiedLinkError("Failed to load library " + libName));
            }
        });
    }
}
