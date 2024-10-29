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
            public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                String libName = (String) callFrame.args[0];
                if (!callFrame.hasThrowable()) return;
                if (!(callFrame.getThrowable() instanceof UnsatisfiedLinkError)) return;
                Log.d(TAG, "Loading library " + libName);
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
                        callFrame.setThrowable(null);
                        return;
                    } catch (UnsatisfiedLinkError ignored) {
                    }
                }
            }
        });
    }
}
