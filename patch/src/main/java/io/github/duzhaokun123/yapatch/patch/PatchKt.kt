package io.github.duzhaokun123.yapatch.patch

import com.android.tools.build.apkzlib.sign.SigningExtension
import com.android.tools.build.apkzlib.sign.SigningOptions
import com.android.tools.build.apkzlib.zip.AlignmentRules
import com.android.tools.build.apkzlib.zip.ZFile
import com.android.tools.build.apkzlib.zip.ZFileOptions
import com.google.gson.Gson
import com.wind.meditor.core.ManifestEditor
import com.wind.meditor.property.AttributeItem
import com.wind.meditor.property.ModificationProperty
import com.wind.meditor.utils.NodeValue
import io.github.duzhaokun123.yapatch.patch.utils.ApkSignatureHelper
import io.github.duzhaokun123.yapatch.patch.utils.Logger
import io.github.duzhaokun123.yapatch.patch.utils.ManifestParser
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File
import java.io.FilenameFilter
import java.security.KeyStore
import java.security.cert.X509Certificate


class PatchKt(logger: Logger, vararg args: String) : Main.Patch(logger, *args) {
    val gson = Gson()

    val zFileOptions = ZFileOptions().apply {
        setAlignmentRule(
            AlignmentRules.compose(
                AlignmentRules.constantForSuffix(".so", 4096),
                AlignmentRules.constantForSuffix("resources.arsc", 4096),
            )
        )
    }

    override fun run() {
        logger.info("PatchKt run")
        logger.info("Patching $apk")
        val apkFile = File(apk)
        val outputFileName = apkFile.nameWithoutExtension + "_yapatched.apk"
        val outputDir = File(outputPath)
        logger.info("Output to ${outputDir.absolutePath}")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, outputFileName)
        logger.info("Output file: ${outputFile.name}")
        logger.info("Load ${modules.size} module(s)")
        modules.forEach {
            logger.info("Module: $it")
        }
        if (modules.isEmpty()) {
            logger.warn("No module loaded")
        }
        patch(apkFile, outputFile)

        if (splitApks != null && splitApks.isNotEmpty()) {
            logger.info("Resign split apks")
            splitApks.forEach { apkPatch ->
                val apkFile = File(apkPatch)
                val outputFileFile = File(outputPath, apkFile.nameWithoutExtension + "_yapatched.apk")
                outputFileFile.delete()
                logger.info("Resigning $apkPatch")
                logger.info("Output to ${outputFileFile.absolutePath}")
                val srcZFile = ZFile.openReadOnly(apkFile)
                val dstZFile = ZFile.openReadWrite(outputFileFile, zFileOptions)
                srcZFile.use {
                    dstZFile.use {
                        resign(srcZFile, dstZFile)
                    }
                }
            }
        }
    }

    fun patch(srcApk: File, outputFile: File) {
        logger.info("PatchKt patch")
        outputFile.delete()
        val tempDir = File(outputFile.parent, "${srcApk.nameWithoutExtension}_temp")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        logger.info("Extracting apk to $tempDir")
        val srcZipFile = ZipFile(srcApk)
        srcZipFile.isRunInThread = true
        val srcZipFileMonitor = srcZipFile.progressMonitor
        srcZipFile.extractAll(tempDir.absolutePath)
        while (srcZipFileMonitor.state != ProgressMonitor.State.READY) {
            logger.onProgress(srcZipFileMonitor.percentDone, 100)
            Thread.sleep(100)
        }
        logger.onProgress(srcZipFileMonitor.percentDone, 100)
        logger.onProgress(0, 0)
        logger.info("Extracted")

        val dexFileCount = tempDir.listFiles(object : FilenameFilter {
            override fun accept(dir: File, name: String): Boolean {
                return name.endsWith(".dex")
            }
        })!!.size
        logger.info("Found ${dexFileCount} dex file(s)")

        val manifestFile = File(tempDir, "AndroidManifest.xml")
        if (!manifestFile.exists()) {
            throw RuntimeException("AndroidManifest.xml not found")
        }
        logger.info("Found AndroidManifest.xml")
        val pair = ManifestParser.parseManifestFile(manifestFile.absolutePath)
        if (pair == null) {
            throw RuntimeException("Parse AndroidManifest.xml failed")
        }
        val appComponentFactory = pair.appComponentFactory
        val minSdkVersion = pair.minSdkVersion
        logger.info("AppComponentFactory: $appComponentFactory")
        logger.info("MinSdkVersion: $minSdkVersion")
        val originalSignature = ApkSignatureHelper.getApkSignInfo(srcApk.absolutePath)
        logger.info("Original signature: $originalSignature")
        logger.info("Sigbypass level: $sigbypassLevel")
        patchManifest(manifestFile.absolutePath, gson.toJson(Metadata(appComponentFactory, modules, originalSignature, sigbypassLevel,
            Versions.loader)), minSdkVersion)
        logger.info("Patched AndroidManifest.xml")

        logger.info("Adding loader dex")
        val patchDex = File(tempDir, "classes${dexFileCount + 1}.dex")
        patchDex.createNewFile()
        this.javaClass.getResourceAsStream("/assets/yapatch/loader.dex")!!.use { input ->
            patchDex.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        logger.info("Added")

        logger.info("Repackaging apk")
        val tempApk = File(outputFile.parent, outputFile.nameWithoutExtension + "_temp.apk")
        tempApk.delete()
        val tempZipFile = ZipFile(tempApk)
        tempZipFile.isRunInThread = true
        val tempZipFileMonitor = tempZipFile.progressMonitor
        tempZipFile.addFolder(tempDir, ZipParameters().apply {
            isIncludeRootFolder = false
            compressionLevel = CompressionLevel.NO_COMPRESSION
        })
        while (tempZipFileMonitor.state != ProgressMonitor.State.READY) {
            logger.onProgress(tempZipFileMonitor.percentDone, 100)
            Thread.sleep(100)
        }
        logger.onProgress(tempZipFileMonitor.percentDone, 100)
        logger.onProgress(0, 0)
        logger.info("Repackaged")
        val tempZFIle = ZFile.openReadOnly(tempApk)
        val dstZFile = ZFile.openReadWrite(outputFile, zFileOptions)
        tempZFIle.use {
            dstZFile.use {
                resign(tempZFIle, dstZFile)
            }
        }

        clean(tempDir, tempApk)
    }

    fun patchManifest(manifestPath: String, metadata: String, minSdkVersion: Int) {
        val modificationProperty = ModificationProperty()
        if (minSdkVersion < 33)
            modificationProperty.addUsesSdkAttribute(AttributeItem(NodeValue.UsesSDK.MIN_SDK_VERSION, 33));
        modificationProperty.addApplicationAttribute(AttributeItem("appComponentFactory", "io.github.duzhaokun123.yapatch.AppComponentFactory"))
        modificationProperty.addApplicationAttribute(AttributeItem(NodeValue.Application.DEBUGGABLE, debuggable))
        modificationProperty.addMetaData(ModificationProperty.MetaData("yapatch", metadata))
        modificationProperty.addUsesPermission("android.permission.QUERY_ALL_PACKAGES")
        ManifestEditor(manifestPath, manifestPath + "_new", modificationProperty).processManifest()
        assert(File(manifestPath + "_new").renameTo(File(manifestPath).also { it.delete() }))
    }

    fun clean(tempDir: File, tempApk: File? = null) {
        logger.info("Cleaning")
        tempDir.deleteRecursively()
        tempApk?.delete()
    }

    fun resign(srcZFile: ZFile, dstZFile: ZFile) {
        logger.info("Signing apk")
        val defaultType = KeyStore.getDefaultType().lowercase()
        logger.info("Default keystore type: $defaultType")
        val keyStore = KeyStore.getInstance(defaultType)
        if (keystoreArgs[0] == null) {
            this.javaClass.getResourceAsStream("/assets/lspatch/keystore_$defaultType").use {
                keyStore.load(it, keystoreArgs[1].toCharArray())
            }
        } else {
            File(keystoreArgs[0]).inputStream().use {
                keyStore.load(it, keystoreArgs[1].toCharArray())
            }
        }
        val entry = keyStore.getEntry(
            keystoreArgs[2],
            KeyStore.PasswordProtection(keystoreArgs[3].toCharArray())
        ) as KeyStore.PrivateKeyEntry
        SigningExtension(
            SigningOptions.builder()
                .setMinSdkVersion(28)
                .setV2SigningEnabled(true)
                .setCertificates(*(entry.certificateChain as Array<X509Certificate>))
                .setKey(entry.privateKey)
                .build()
        ).register(dstZFile)
        val entries = srcZFile.entries()
        val total = entries.size
        entries.forEachIndexed { i, entry ->
            logger.onProgress(i + 1, total)
            val name = entry.centralDirectoryHeader.name
            if (name.startsWith("META-INF") && (name.endsWith(".SF") || name.endsWith(".MF") || name.endsWith(".RSA"))) return@forEachIndexed
            if (name == "resources.arsc" || name.endsWith(".so")) {
                dstZFile.add(name, entry.open(), false)
                return@forEachIndexed
            }
            dstZFile.add(name, entry.open())
        }
        dstZFile.realign()
        logger.onProgress(0, 0)
        logger.info("Signed")
    }
}