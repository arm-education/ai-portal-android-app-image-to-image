package com.arm.learningpath.imagetoimage.storage

import android.content.Context
import com.arm.learningpath.imagetoimage.catalog.ModelConfig
import java.io.File

object BundledModelInstaller {
    fun state(context: Context, config: ModelConfig, modelDir: File): BundledModelState {
        val modelFile = File(modelDir, config.modelFile)
        return when {
            modelFile.isFile -> BundledModelState.FOUND_IN_APP_STORAGE
            isBundled(context, config) -> BundledModelState.BUNDLED_WITH_APP
            hasPlaceholder(context, config) -> BundledModelState.PLACEHOLDER_BUNDLED
            else -> BundledModelState.MISSING
        }
    }

    fun installIfAvailable(context: Context, config: ModelConfig, modelDir: File): Boolean {
        if (File(modelDir, config.modelFile).isFile) {
            return false
        }
        if (isBundled(context, config)) {
            copyAsset(context, assetPath(config, config.modelFile), File(modelDir, config.modelFile))
            config.externalDataFiles.forEach { relativePath ->
                copyAsset(context, assetPath(config, relativePath), File(modelDir, relativePath))
            }
            return true
        }

        require(!hasPlaceholder(context, config)) {
            "The app contains a placeholder model asset for '${config.id}'. Replace the placeholder with ${config.modelFile}, or copy the model into app-private storage."
        }
        return false
    }

    fun isBundled(context: Context, config: ModelConfig): Boolean {
        return assetExists(context, assetPath(config, config.modelFile))
    }

    private fun hasPlaceholder(context: Context, config: ModelConfig): Boolean {
        val marker = "MOCK_REPLACE_ME_${config.modelFile}"
        return assetExists(context, "models/${config.id}/$marker")
    }

    private fun assetPath(config: ModelConfig, relativePath: String): String {
        require(!File(relativePath).isAbsolute) {
            "Bundled model asset paths must be relative: $relativePath"
        }
        return "models/${config.id}/$relativePath"
    }

    private fun assetExists(context: Context, path: String): Boolean {
        return try {
            context.assets.open(path).use { true }
        } catch (_: Throwable) {
            false
        }
    }

    private fun copyAsset(context: Context, assetPath: String, destination: File) {
        destination.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
