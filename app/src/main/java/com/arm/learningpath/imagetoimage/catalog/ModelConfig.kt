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
    val inputImageSize: Int?,
    val outputImageSize: Int?,
    val inputImageWidth: Int?,
    val inputImageHeight: Int?,
    val outputImageWidth: Int?,
    val outputImageHeight: Int?,
    val colorFormat: String,
    val normalization: String,
    val promptType: String,
    val defaultBoxPrompt: List<Float>?,
    val inputTensorNames: List<String>,
    val inputTensorShapes: List<String>,
    val outputTensorNames: List<String>,
    val outputTensorShapes: List<String>,
) {
    val isSegmentation: Boolean
        get() = workload == WORKLOAD_SEGMENTATION

    val isImageToImage: Boolean
        get() = workload == WORKLOAD_IMAGE_TO_IMAGE

    val isDepthEstimation: Boolean
        get() = workload == WORKLOAD_DEPTH_ESTIMATION

    companion object {
        const val WORKLOAD_SEGMENTATION = "image-segmentation"
        const val WORKLOAD_IMAGE_TO_IMAGE = "image-to-image"
        const val WORKLOAD_DEPTH_ESTIMATION = "depth-estimation"
    }
}

fun JSONObject.toModelConfig(): ModelConfig {
    val inputImageSize = positiveIntOrNull("inputImageSize")
    val outputImageSize = positiveIntOrNull("outputImageSize")
    val inputDimensions = optionalImageDimensions("input")
    val outputDimensions = optionalImageDimensions("output")
    requireDimensionDefinition("input", inputImageSize, inputDimensions.first)
    requireDimensionDefinition("output", outputImageSize, outputDimensions.first)
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
        inputImageSize = inputImageSize,
        outputImageSize = outputImageSize,
        inputImageWidth = inputDimensions.first,
        inputImageHeight = inputDimensions.second,
        outputImageWidth = outputDimensions.first,
        outputImageHeight = outputDimensions.second,
        colorFormat = optString("colorFormat"),
        normalization = optString("normalization"),
        promptType = optString("promptType"),
        defaultBoxPrompt = optJSONArray("defaultBoxPrompt")?.toFloatList(),
        inputTensorNames = optJSONArray("inputTensorNames").toStringList(),
        inputTensorShapes = optJSONArray("inputTensorShapes").toStringList(),
        outputTensorNames = optJSONArray("outputTensorNames").toStringList(),
        outputTensorShapes = optJSONArray("outputTensorShapes").toStringList(),
    )
}

private fun requireDimensionDefinition(prefix: String, square: Int?, rectangularWidth: Int?) {
    require((square == null) != (rectangularWidth == null)) {
        "Use either ${prefix}ImageSize or ${prefix}ImageWidth and ${prefix}ImageHeight."
    }
}

private fun JSONObject.optionalImageDimensions(prefix: String): Pair<Int?, Int?> {
    val width = positiveIntOrNull("${prefix}ImageWidth")
    val height = positiveIntOrNull("${prefix}ImageHeight")
    require((width == null) == (height == null)) {
        "${prefix}ImageWidth and ${prefix}ImageHeight must be provided together."
    }
    return width to height
}

private fun JSONObject.positiveIntOrNull(name: String): Int? {
    if (!has(name) || isNull(name)) {
        return null
    }
    return getInt(name).also { value ->
        require(value > 0) { "$name must be positive." }
    }
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
