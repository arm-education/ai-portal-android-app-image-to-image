package com.arm.learningpath.imagetoimage.catalog

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ModelCatalogContractTest {
    private val catalog = readCatalog()

    @Test
    fun catalogContainsOnlyValidatedMobileSamEntry() {
        assertEquals(1, catalog.size)

        val mobileSam = catalog.single()
        assertEquals("mobile-sam-int8-xnnpack-executorch", mobileSam.id)
        assertEquals("MobileSAM INT8 Segmentation", mobileSam.displayName)
        assertEquals(ModelConfig.WORKLOAD_SEGMENTATION, mobileSam.workload)
        assertEquals("executorch", mobileSam.runtime)
        assertEquals("mobile-sam-executorch", mobileSam.adapterId)
        assertTrue(mobileSam.isSegmentation)
        assertFalse(mobileSam.isImageToImage)
    }

    @Test
    fun mobileSamUsesCurrentHuggingFaceModelFilename() {
        val mobileSam = catalog.single()

        assertEquals("mobile_sam_raspberry_executorch_optimized.pte", mobileSam.modelFile)
        assertEquals("mobile-sam-int8-xnnpack-executorch", mobileSam.artifactPath)
    }

    @Test
    fun mobileSamTensorAndPromptContractMatchesAdapter() {
        val mobileSam = catalog.single()

        assertEquals(1024, mobileSam.inputImageSize)
        assertEquals(1024, mobileSam.outputImageSize)
        assertEquals("RGB", mobileSam.colorFormat)
        assertEquals("0_to_1_graph_normalizes", mobileSam.normalization)
        assertEquals("box-corners", mobileSam.promptType)
        assertEquals(listOf(102.4f, 102.4f, 921.6f, 921.6f), mobileSam.defaultBoxPrompt)
        assertEquals(listOf("image", "point_coords"), mobileSam.inputTensorNames)
        assertEquals(listOf("[1,3,1024,1024]", "[1,1,2,2]"), mobileSam.inputTensorShapes)
        assertEquals(listOf("low_res_masks", "iou_predictions"), mobileSam.outputTensorNames)
        assertEquals(listOf("[1,3,256,256]", "[1,3]"), mobileSam.outputTensorShapes)
    }

    @Test
    fun placeholderAssetNameMatchesExpectedModelFilename() {
        val mobileSam = catalog.single()
        val placeholderPath = projectPath(
            "src/main/assets/models/${mobileSam.id}/MOCK_REPLACE_ME_${mobileSam.modelFile}"
        )

        assertTrue("Expected placeholder asset at $placeholderPath", Files.isRegularFile(placeholderPath))
    }

    @Test
    fun oldMobileSamFilenameIsNotReferencedByCatalogOrPlaceholder() {
        val oldModelFile = "mobile-sam-int8-executorch.pte"
        val oldPlaceholderPath = projectPath(
            "src/main/assets/models/mobile-sam-int8-xnnpack-executorch/MOCK_REPLACE_ME_$oldModelFile"
        )

        assertFalse(catalog.any { it.modelFile == oldModelFile })
        assertFalse("Old placeholder should not exist at $oldPlaceholderPath", Files.exists(oldPlaceholderPath))
    }

    private fun readCatalog(): List<ModelConfig> {
        val catalogPath = projectPath("src/main/assets/model_catalog.json")
        val catalogJson = Files.newBufferedReader(catalogPath).use { it.readText() }
        val array = JSONArray(catalogJson)
        return List(array.length()) { index -> array.getJSONObject(index).toModelConfig() }
    }

    private fun projectPath(relativePath: String): Path {
        return Paths.get("").toAbsolutePath().resolve(relativePath)
    }
}
