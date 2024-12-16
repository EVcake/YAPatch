package android.content.res;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.util.DisplayMetrics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam;
import de.robv.android.xposed.XposedBridge;

/**
 * Provides access to resources from a certain path (usually the module's own path).
 */
public class XModuleResources extends Resources {
	private XModuleResources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
		super(assets, metrics, config);
	}

	/**
	 * Creates a new instance.
	 *
	 * <p>This is usually called with {@link StartupParam#modulePath} from
	 * {@link IXposedHookZygoteInit#initZygote} and {@link InitPackageResourcesParam#res} from
	 * {@link IXposedHookInitPackageResources#handleInitPackageResources} (or {@code null} for
	 * system-wide replacements).
	 *
	 * @param path The path to the APK from which the resources should be loaded.
	 * @param origRes The resources object from which settings like the display metrics and the
	 *                configuration should be copied. May be {@code null}.
	 */
	public static XModuleResources createInstance(String path, XResources origRes) {
		if (path == null)
			throw new IllegalArgumentException("path must not be null");
        try {
			AssetManager assets = getAssetManager(path);
			XModuleResources res;
			if (origRes != null)
				res = new XModuleResources(assets, origRes.getDisplayMetrics(), origRes.getConfiguration());
			else
				res = new XModuleResources(assets, null, null);
			AndroidAppHelper.addActiveResource(path, res);
			return res;
		}
		catch (Exception e) {
            throw new RuntimeException("Failed to create XModuleResources", e);
        }
    }

	private static AssetManager getAssetManager(String path) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		AssetManager assets = new AssetManager();
//		assets.addSharedLibraryPaths(new String[]{path});
//		assets.addAssetPath(path);

		@SuppressLint("SoonBlockedPrivateApi")
//		Method addSharedLibraryPathsMethod = assets.getClass().getDeclaredMethod("addSharedLibraryPaths", String[].class);
//		addSharedLibraryPathsMethod.setAccessible(true);
//		addSharedLibraryPathsMethod.invoke(assets, (Object) new String[]{path});
		Method addAssetPathInternalMethod = assets.getClass().getDeclaredMethod("addAssetPathInternal", String.class, boolean.class, boolean.class);
		addAssetPathInternalMethod.setAccessible(true);
		addAssetPathInternalMethod.invoke(assets, path, false, true);
		return assets;
	}

	/**
	 * Creates an {@link XResForwarder} instance that forwards requests to {@code id} in this resource.
	 */
	public XResForwarder fwd(int id) {
		return new XResForwarder(this, id);
	}
}