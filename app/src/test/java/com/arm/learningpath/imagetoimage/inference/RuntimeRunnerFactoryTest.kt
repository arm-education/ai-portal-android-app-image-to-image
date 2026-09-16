package com.arm.learningpath.imagetoimage.inference

import android.content.ContextWrapper
import com.arm.learningpath.imagetoimage.catalog.toModelConfig
import com.arm.learningpath.imagetoimage.inference.models.depthanything.DepthAnythingExecuTorchAdapter
import com.arm.learningpath.imagetoimage.inference.models.mobilesam.MobileSamExecuTorchAdapter
import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class RuntimeRunnerFactoryTest {
    private val catalog = run {
        val path = Paths.get("").toAbsolutePath().resolve("src/main/assets/model_catalog.json")
        val array = JSONArray(Files.newBufferedReader(path).use { it.readText() })
        List(array.length()) { index -> array.getJSONObject(index).toModelConfig() }
    }
    private val unusedContext = ContextWrapper(null)

    @Test
    fun routesMobileSamToItsExistingAdapter() {
        val runner = RuntimeRunnerFactory.create(
            unusedContext,
            catalog.single { it.isSegmentation },
        )

        assertTrue(runner is MobileSamExecuTorchAdapter)
    }

    @Test
    fun routesDepthAnythingToItsAdapter() {
        val runner = RuntimeRunnerFactory.create(
            unusedContext,
            catalog.single { it.isDepthEstimation },
        )

        assertTrue(runner is DepthAnythingExecuTorchAdapter)
    }
}
