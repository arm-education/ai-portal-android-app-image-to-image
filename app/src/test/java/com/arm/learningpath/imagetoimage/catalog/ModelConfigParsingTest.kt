package com.arm.learningpath.imagetoimage.catalog

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelConfigParsingTest {
    @Test
    fun rectangularDimensionsAndPromptFreeModelAreParsed() {
        val config = JSONObject(baseJson()).toModelConfig()

        assertEquals(686, config.inputImageWidth)
        assertEquals(518, config.inputImageHeight)
        assertEquals(686, config.outputImageWidth)
        assertEquals(518, config.outputImageHeight)
        assertNull(config.inputImageSize)
        assertNull(config.outputImageSize)
        assertNull(config.defaultBoxPrompt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompleteRectangularDimensionsAreRejected() {
        JSONObject(baseJson().replace(", \"inputImageHeight\": 518", "")).toModelConfig()
    }

    @Test(expected = IllegalArgumentException::class)
    fun mixedSquareAndRectangularDimensionsAreRejected() {
        JSONObject(baseJson().replace("\"inputImageWidth\": 686", "\"inputImageSize\": 686, \"inputImageWidth\": 686"))
            .toModelConfig()
    }

    private fun baseJson(): String = """
        {
          "id": "depth",
          "displayName": "Depth",
          "workload": "depth-estimation",
          "runtime": "executorch",
          "artifactType": "file",
          "artifactPath": "depth",
          "modelFile": "depth.pte",
          "adapterId": "depth-anything-v2-executorch",
          "inputImageWidth": 686, "inputImageHeight": 518,
          "outputImageWidth": 686, "outputImageHeight": 518,
          "colorFormat": "RGB",
          "normalization": "imagenet_mean_std",
          "promptType": "none"
        }
    """.trimIndent()
}
