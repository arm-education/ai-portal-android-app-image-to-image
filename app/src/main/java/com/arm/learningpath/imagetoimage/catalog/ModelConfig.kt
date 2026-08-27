package com.arm.learningpath.imagetoimage.catalog

import org.json.JSONArray
import org.json.JSONObject

data class ModelConfig(
    val id: String,
    val displayName: String,
    val workload: String,
    val runtime: String,
    val artifactType: String,
    val artifactPath: String,
    val modelFile: String,
    val externalDataFiles: List<String>,
    val adapterId: String,
    val inputImageSize: Int,
    val outputImageSize: Int,
    val colorFormat: String,
    val normalization: String,
    val promptType: String,
    val defaultBoxPrompt: List<Float>,
    val inputTensorNames: List<String>,
    val inputTensorShapes: List<String>,
    val outputTensorNames: List<String>,
    val outputTensorShapes: List<String>,
) {
    val isSegmentation: Boolean
        get() = workload == WORKLOAD_SEGMENTATION

    val isImageToImage: Boolean
        get() = workload == WORKLOAD_IMAGE_TO_IMAGE

    companion object {
        const val WORKLOAD_SEGMENTATION = "image-segmentation"
        const val WORKLOAD_IMAGE_TO_IMAGE = "image-to-image"
    }
}

fun JSONObject.toModelConfig(): ModelConfig {
    return ModelConfig(
        id = getString("id"),
        displayName = getString("displayName"),
        workload = getString("workload"),
        runtime = getString("runtime"),
        artifactType = getString("artifactType"),
        artifactPath = getString("artifactPath"),
        modelFile = getString("modelFile"),
        externalDataFiles = optJSONArray("externalDataFiles").toStringList(),
        adapterId = optString("adapterId"),
        inputImageSize = optInt("inputImageSize"),
        outputImageSize = optInt("outputImageSize"),
        colorFormat = optString("colorFormat"),
        normalization = optString("normalization"),
        promptType = optString("promptType"),
        defaultBoxPrompt = optJSONArray("defaultBoxPrompt").toFloatList(),
        inputTensorNames = optJSONArray("inputTensorNames").toStringList(),
        inputTensorShapes = optJSONArray("inputTensorShapes").toStringList(),
        outputTensorNames = optJSONArray("outputTensorNames").toStringList(),
        outputTensorShapes = optJSONArray("outputTensorShapes").toStringList(),
    )
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) {
        return emptyList()
    }

    return List(length()) { index -> getString(index) }
}

private fun JSONArray?.toFloatList(): List<Float> {
    if (this == null) {
        return emptyList()
    }

    return List(length()) { index -> getDouble(index).toFloat() }
}
