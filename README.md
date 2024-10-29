# YAPatch

Yet Another Patching Tool for Android to load xposed modules

## Build

use https://github.com/Reginer/aosp-android-jar/tree/main/android-35 android.jar

```shell
./gradlew patch-loader:copyFiles patch:shadowJar
```

## 有什么意义

除了在 Android 15 上 给 qq 加载 qa 外 好像没有什么意义

## 已知问题

存在这个垃圾项目

## 主要感谢
- [Pine](https://github.com/canyie/pine)
- [LSPatch](https://github.com/LSPosed/LSPatch)
- [Xpatch](https://github.com/WindySha/Xpatch)