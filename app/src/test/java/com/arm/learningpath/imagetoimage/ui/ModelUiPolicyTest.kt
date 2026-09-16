package com.arm.learningpath.imagetoimage.ui

import com.arm.learningpath.imagetoimage.catalog.toModelConfig
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class ModelUiPolicyTest {
    private val catalog = run {
        val path = Paths.get("").toAbsolutePath().resolve("src/main/assets/model_catalog.json")
        val array = JSONArray(Files.newBufferedReader(path).use { it.readText() })
        List(array.length()) { index -> array.getJSONObject(index).toModelConfig() }
    }

    @Test
    fun mobileSamRetainsItsPromptAndActionLabel() {
        val mobileSam = catalog.single { it.isSegmentation }

        assertTrue(ModelUiPolicy.showsBoxPrompt(mobileSam))
        assertEquals("Run segmentation", ModelUiPolicy.runActionLabel(mobileSam))
        assertEquals("Segmentation mask shown in cyan", ModelUiPolicy.resultDescription(mobileSam))
    }

    @Test
    fun depthEstimationHidesPromptAndUsesDepthLabels() {
        val depthAnything = catalog.single { it.isDepthEstimation }

        assertFalse(ModelUiPolicy.showsBoxPrompt(depthAnything))
        assertEquals("Run depth estimation", ModelUiPolicy.runActionLabel(depthAnything))
        assertEquals(
            "Depth map shown in grayscale: white is nearer",
            ModelUiPolicy.resultDescription(depthAnything),
        )
    }
}
