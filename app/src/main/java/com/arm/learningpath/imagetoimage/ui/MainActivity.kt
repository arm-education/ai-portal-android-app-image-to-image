package com.arm.learningpath.imagetoimage.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import com.arm.learningpath.imagetoimage.catalog.ModelCatalog
import com.arm.learningpath.imagetoimage.catalog.ModelConfig
import com.arm.learningpath.imagetoimage.image.DecodedImage
import com.arm.learningpath.imagetoimage.image.ImageLoader
import com.arm.learningpath.imagetoimage.inference.RuntimeRunner
import com.arm.learningpath.imagetoimage.inference.RuntimeRunnerFactory
import com.arm.learningpath.imagetoimage.storage.BundledModelInstaller
import java.io.File

class MainActivity : Activity() {
    private val colorAccent = Color.rgb(0, 140, 145)
    private val colorTextPrimary = Color.rgb(24, 47, 55)
    private val colorTextSecondary = Color.rgb(85, 106, 114)
    private val colorSurface = Color.WHITE
    private val colorBackground = Color.rgb(246, 251, 250)
    private val colorBorder = Color.rgb(190, 217, 219)

    private lateinit var catalog: List<ModelConfig>
    private lateinit var modelDropdownView: TextView
    private lateinit var imagePreview: SegmentationPreviewView
    private lateinit var promptInfoView: TextView
    private lateinit var statusView: TextView
    private lateinit var outputView: TextView
    private var runner: RuntimeRunner? = null
    private var modelDropdownPopup: PopupWindow? = null
    private var loadedConfig: ModelConfig? = null
    private var selectedImageUri: Uri? = null
    private var selectedImage: DecodedImage? = null
    private var selectedModelIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        catalog = ModelCatalog.load(this)
        setContentView(createLayout())
        updateSelectedModelStatus()
    }

    override fun onDestroy() {
        modelDropdownPopup?.dismiss()
        runner?.close()
        super.onDestroy()
    }

    @Deprecated("Use Activity Result APIs in production apps.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_IMAGE || resultCode != RESULT_OK) {
            return
        }

        val uri = data?.data ?: return
        selectedImageUri = uri
        val selected = selectedConfig()
        setBusy("Decoding selected image")
        Thread {
            try {
                val decoded = ImageLoader.decode(this, uri, selected.inputImageSize)
                selectedImage = decoded
                runOnUiThread {
                    imagePreview.setBitmap(decoded.bitmap)
                    imagePreview.setPromptBox(defaultBoxPrompt(selected), selected.inputImageSize)
                    promptInfoView.text = "Prompt box: center 80% of the image"
                    statusView.text = "Selected image decoded"
                    outputView.text = "Selected image: $uri"
                }
            } catch (error: Throwable) {
                showError("Image decode failed", error)
            }
        }.start()
    }

    private fun createLayout(): View {
        val basePadding = dp(16)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(basePadding, basePadding, basePadding, basePadding)
            setBackgroundColor(colorBackground)
        }
        applySystemInsets(root, basePadding)

        root.addView(TextView(this).apply {
            text = "Photo Insight"
            textSize = 22f
            setTextColor(colorTextPrimary)
            setTypeface(typeface, Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text = "Run MobileSAM locally with a reusable Android app shell and a validated adapter."
            textSize = 14f
            setTextColor(colorTextSecondary)
            setPadding(0, dp(6), 0, dp(16))
        })

        modelDropdownView = TextView(this).apply {
            text = selectedConfig().displayName
            textSize = 16f
            setTextColor(colorTextPrimary)
            gravity = Gravity.CENTER_VERTICAL
            setSingleLine(false)
            includeFontPadding = false
        }
        val modelFrame = FrameLayout(this).apply {
            background = roundedBackground(fillColor = colorSurface, strokeColor = colorBorder)
            contentDescription = "Model: ${selectedConfig().displayName}"
            isClickable = true
            isFocusable = true
            setPadding(dp(14), 0, dp(14), 0)
            addView(modelDropdownView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply {
                rightMargin = dp(32)
            })
            addView(TextView(this@MainActivity).apply {
                text = "▼"
                textSize = 18f
                setTextColor(colorTextSecondary)
                gravity = Gravity.CENTER
                includeFontPadding = false
            }, FrameLayout.LayoutParams(
                dp(28),
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.END,
            ))
            setOnClickListener { showModelDropdown(this) }
        }
        root.addView(modelFrame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply {
            bottomMargin = dp(12)
        })

        root.addView(createActionButton("Load model", filled = false).apply {
            setOnClickListener { loadSelectedModel() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply {
            bottomMargin = dp(10)
        })

        root.addView(createActionButton("Choose image", filled = false).apply {
            setOnClickListener { chooseImage() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply {
            bottomMargin = dp(12)
        })

        imagePreview = SegmentationPreviewView(this).apply {
            background = roundedBackground(fillColor = colorSurface, strokeColor = colorBorder)
            setPromptBox(defaultBoxPrompt(selectedConfig()), selectedConfig().inputImageSize)
            isClickable = true
            isFocusable = true
            contentDescription = "Image preview. Tap to choose an image."
            setOnClickListener { chooseImage() }
        }
        root.addView(imagePreview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(150)).apply {
            bottomMargin = dp(8)
        })

        promptInfoView = TextView(this).apply {
            text = "Prompt box: center 80% of the image"
            textSize = 13f
            setTextColor(colorTextSecondary)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(promptInfoView)

        root.addView(createActionButton("Run segmentation", filled = true).apply {
            setOnClickListener { runSelectedModel() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply {
            bottomMargin = dp(12)
        })

        root.addView(View(this).apply {
            setBackgroundColor(colorAccent)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)).apply {
            bottomMargin = dp(14)
        })

        statusView = TextView(this).apply {
            textSize = 13f
            setTextColor(colorTextSecondary)
            setPadding(0, 0, 0, dp(10))
        }
        root.addView(statusView)

        outputView = TextView(this).apply {
            textSize = 14f
            setTextColor(colorTextPrimary)
            setTextIsSelectable(true)
        }

        root.addView(ScrollView(this).apply {
            addView(outputView)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(142)))

        return root
    }

    private fun showModelDropdown(anchor: View) {
        modelDropdownPopup?.dismiss()
        val selected = selectedConfig()
        val row = TextView(this).apply {
            text = selected.displayName
            textSize = 15f
            setTextColor(colorTextPrimary)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = roundedBackground(fillColor = colorSurface, strokeColor = colorBorder)
            minHeight = dp(52)
            setOnClickListener { modelDropdownPopup?.dismiss() }
        }
        val popup = PopupWindow(
            row,
            anchor.width,
            dp(56),
            true,
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(6).toFloat()
            }
        }
        modelDropdownPopup = popup
        popup.showAsDropDown(anchor, 0, dp(4))
    }

    private fun applySystemInsets(root: View, basePadding: Int) {
        root.setOnApplyWindowInsetsListener { view, insets ->
            val topInset: Int
            val bottomInset: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val systemInsets = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
                )
                topInset = systemInsets.top
                bottomInset = systemInsets.bottom
            } else {
                @Suppress("DEPRECATION")
                topInset = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottomInset = insets.systemWindowInsetBottom
            }

            view.setPadding(basePadding, basePadding + topInset, basePadding, basePadding + bottomInset)
            insets
        }
        root.requestApplyInsets()
        root.post { root.requestApplyInsets() }
    }

    private fun createActionButton(label: String, filled: Boolean): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (filled) Color.WHITE else colorAccent)
            background = roundedBackground(
                fillColor = if (filled) colorAccent else colorSurface,
                strokeColor = colorAccent,
            )
            setAllCaps(false)
            minHeight = 0
            minWidth = 0
            includeFontPadding = false
        }
    }

    private fun roundedBackground(fillColor: Int, strokeColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(fillColor)
            setStroke(dp(1), strokeColor)
        }
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_IMAGE)
    }

    private fun loadSelectedModel() {
        val selected = selectedConfig()
        val modelDir = modelDir(selected)

        setBusy("Loading ${selected.displayName} from ${modelDir.absolutePath}")
        Thread {
            try {
                val installed = BundledModelInstaller.installIfAvailable(this, selected, modelDir)
                runner?.close()
                val nextRunner = RuntimeRunnerFactory.create(this, selected)
                val loadTimeMs = nextRunner.load(modelDir, selected)
                runner = nextRunner
                loadedConfig = selected
                showResult(
                    status = "Loaded ${selected.id} in ${loadTimeMs} ms",
                    output = buildString {
                        if (installed) {
                            appendLine("Bundled model copied into app-private storage.")
                        }
                        appendLine("Ready. Runtime: ${selected.runtime}")
                        append("Model path: ${modelDir.absolutePath}")
                    },
                )
            } catch (error: Throwable) {
                showError("Load failed", error)
            }
        }.start()
    }

    private fun runSelectedModel() {
        val selected = selectedConfig()
        val activeRunner = runner
        val image = selectedImage
        if (activeRunner == null || loadedConfig?.id != selected.id) {
            outputView.text = "Load the selected model before running it."
            return
        }
        if (image == null) {
            outputView.text = "Choose a photo before running the model."
            return
        }

        setBusy("Running ${selected.displayName}")
        Thread {
            try {
                val result = activeRunner.runImage(image, defaultBoxPrompt(selected))
                showResult(
                    status = "Load: ${result.loadTimeMs} ms | Run: ${result.runTimeMs} ms",
                    output = result.summary,
                    resultBitmap = result.resultBitmap,
                )
            } catch (error: Throwable) {
                showError("Run failed", error)
            }
        }.start()
    }

    private fun defaultBoxPrompt(config: ModelConfig): FloatArray {
        if (config.defaultBoxPrompt.size == 4) {
            return config.defaultBoxPrompt.toFloatArray()
        }
        val size = if (config.inputImageSize > 0) config.inputImageSize.toFloat() else 1024f
        val margin = size * 0.10f
        return floatArrayOf(margin, margin, size - margin, size - margin)
    }

    private fun selectedConfig(): ModelConfig {
        return catalog[selectedModelIndex.coerceIn(catalog.indices)]
    }

    private fun modelDir(config: ModelConfig): File {
        return File(filesDir, "models/${config.id}")
    }

    private fun updateSelectedModelStatus() {
        if (!::statusView.isInitialized) {
            return
        }

        val selected = selectedConfig()
        val dir = modelDir(selected)
        val modelFile = File(dir, selected.modelFile)
        val state = BundledModelInstaller.state(this, selected, dir).label
        statusView.text = """
            Runtime: ${selected.runtime}
            Workload: ${selected.workload}
            Adapter: ${selected.adapterId}
            App-local model directory: ${dir.absolutePath}
            Required model file: ${modelFile.name} ($state)
        """.trimIndent()
    }

    private fun setBusy(message: String) {
        runOnUiThread {
            statusView.text = message
            outputView.text = "Working..."
        }
    }

    private fun showResult(status: String, output: String, resultBitmap: android.graphics.Bitmap? = null) {
        runOnUiThread {
            resultBitmap?.let {
                imagePreview.setResultBitmap(it)
                promptInfoView.text = "Segmentation mask shown in cyan"
            }
            statusView.text = status
            outputView.text = output
        }
    }

    private fun showError(title: String, error: Throwable) {
        runOnUiThread {
            statusView.text = title
            outputView.text = error.message ?: error.toString()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val REQUEST_IMAGE = 2001
    }
}
