# Photo Insight Android application

This Android application accompanies the Arm Learning Path for running image-to-image models from the Arm AI Portal. It is intended for learning how image-to-image model adapters run on devices and is not a reference production application. It is provided under the [Arm Education End User License Agreement](LICENSE.md).

The visible application flow is MobileSAM-only for the Learning Path. The project is structured as a reusable Android app shell with one validated MobileSAM adapter. Additional models require model-specific adapters for their own input tensors, output tensors, preprocessing, result decoding, and, when the task differs from MobileSAM segmentation, UI controls and output rendering.

The application does not store real model binaries in this repository. Model files are copied into app-private storage on the device. The repository includes a `MOCK_REPLACE_ME_mobile-sam-int8-executorch.pte` placeholder under `app/src/main/assets/models/mobile-sam-int8-xnnpack-executorch/` to show the optional bundled-model location for local experiments.

## Application flow

The application opens with:

- a MobileSAM model dropdown showing **MobileSAM INT8 Segmentation**
- a **Load model** button
- a **Choose image** button
- an image preview/result area that shows **No image selected** before an image is chosen
- a **Run segmentation** button
- a status/result panel
- a cyan mask overlay and output statistics after inference

The default MobileSAM box prompt covers the center 80 percent of the resized `1024 x 1024` image. The prompt is fixed to make the validated Learning Path run repeatable.

## Requirements

- Android Studio with Android SDK 35
- Java 17, supplied by Android Studio or available on your `PATH`
- An Arm64 Android device running Android 9, API 28, or later
- The MobileSAM ExecuTorch model file downloaded from Hugging Face

## Supported launch model

| Model | Runtime | Copy this file |
| --- | --- | --- |
| MobileSAM INT8 Segmentation | ExecuTorch with XNNPACK | `mobile-sam-int8-executorch.pte` |

The model catalog entry is stored in `app/src/main/assets/model_catalog.json`. The entry uses:

```json
"adapterId": "mobile-sam-executorch"
```

`RuntimeRunnerFactory.kt` maps this adapter ID to `MobileSamExecuTorchAdapter.kt`.

## Project structure

The app separates reusable infrastructure from model-specific code:

```text
app/src/main/java/com/arm/learningpath/imagetoimage/
├── ui/
├── catalog/
├── storage/
├── image/
└── inference/
    ├── segmentation/
    └── models/
        └── mobilesam/
```

The reusable shell lives in:

- `ui/` for the Android activity and preview surface.
- `catalog/` for catalog parsing and model metadata.
- `storage/` for app-private and optional bundled model handling.
- `image/` for orientation-aware image decoding and target-size sampling.
- `inference/` for the runtime adapter interface and factory.

The MobileSAM-specific implementation lives in `inference/models/mobilesam/`:

- `MobileSamExecuTorchAdapter.kt` loads the ExecuTorch module and invokes `forward`.
- `MobileSamPreprocessor.kt` converts the selected image and fixed box prompt into MobileSAM tensors.
- `MobileSamPostprocessor.kt` validates MobileSAM outputs, creates the cyan mask overlay, and formats output statistics.

## Download MobileSAM

Create a Python virtual environment and install the Hugging Face Hub package.

On macOS or Linux:

```bash
python3 -m venv .hf-venv
.hf-venv/bin/python -m pip install --upgrade pip huggingface_hub
```

On Windows PowerShell:

```powershell
py -m venv .hf-venv
.\.hf-venv\Scripts\python.exe -m pip install --upgrade pip huggingface_hub
```

If the model repository requires authentication, sign in with an account that has access:

```bash
.hf-venv/bin/hf auth login
```

On Windows PowerShell:

```powershell
.\.hf-venv\Scripts\hf.exe auth login
```

Download the model file into a local model directory.

On macOS or Linux:

```bash
mkdir -p model/mobile-sam-int8-xnnpack-executorch
.hf-venv/bin/python - <<'PY'
from huggingface_hub import hf_hub_download

path = hf_hub_download(
    repo_id="Arm/mobile-sam-int8-xnnpack-executorch",
    filename="mobile-sam-int8-executorch.pte",
    local_dir="model/mobile-sam-int8-xnnpack-executorch",
)
print(path)
PY
```

On Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path model\mobile-sam-int8-xnnpack-executorch | Out-Null
.\.hf-venv\Scripts\python.exe -c "from huggingface_hub import hf_hub_download; print(hf_hub_download(repo_id='Arm/mobile-sam-int8-xnnpack-executorch', filename='mobile-sam-int8-executorch.pte', local_dir='model/mobile-sam-int8-xnnpack-executorch'))"
```

## Open and run the application

Clone or download this repository, then open the repository root in Android Studio.

Connect an Arm64 Android phone or start an Arm64 emulator. Wait for Gradle sync to finish, select the `app` configuration, and run it once so Android creates the app-private directory.

Copy the downloaded model into app-private storage.

On macOS or Linux:

```bash
MODEL_ID="mobile-sam-int8-xnnpack-executorch"
MODEL_FILE="mobile-sam-int8-executorch.pte"
MODEL_PATH="model/$MODEL_ID/$MODEL_FILE"

adb shell run-as com.arm.learningpath.imagetoimage mkdir -p "files/models/$MODEL_ID"
adb push "$MODEL_PATH" "/data/local/tmp/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage \
  cp "/data/local/tmp/$MODEL_FILE" "files/models/$MODEL_ID/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage \
  ls -l "files/models/$MODEL_ID/$MODEL_FILE"
```

On Windows PowerShell:

```powershell
$MODEL_ID = "mobile-sam-int8-xnnpack-executorch"
$MODEL_FILE = "mobile-sam-int8-executorch.pte"
$MODEL_PATH = "model\$MODEL_ID\$MODEL_FILE"

adb shell run-as com.arm.learningpath.imagetoimage mkdir -p "files/models/$MODEL_ID"
adb push $MODEL_PATH "/data/local/tmp/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage `
  cp "/data/local/tmp/$MODEL_FILE" "files/models/$MODEL_ID/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage `
  ls -l "files/models/$MODEL_ID/$MODEL_FILE"
```

In the app:

1. Select **Load model**.
2. Select **Choose image** or tap the preview area.
3. Choose a JPEG or PNG image.
4. Confirm that the dashed prompt box is visible over the image.
5. Select **Run segmentation**.

The app displays the selected mask as a translucent cyan overlay. The result panel displays output statistics including the selected mask, predicted IoU, mask coverage, mask logit range, and timing.

For local experiments, you can alternatively replace the placeholder under `app/src/main/assets/models/mobile-sam-int8-xnnpack-executorch/` with the real `mobile-sam-int8-executorch.pte` file before building the APK. When **Load model** runs, the app copies a real bundled asset into app-private storage. Do not commit real model binaries to this repository.

## Extend the application

The reusable parts of the app are the Android shell, model catalog loading, app-local or optional bundled model storage, image decoding, preview surface, and adapter interface. The MobileSAM adapter is model-specific.

For another image-to-image model, inspect the model package and add a matching adapter. Updating only `model_catalog.json` is not enough unless a registered adapter already supports that exact model contract. Depending on the model, you may also need to adjust:

- the catalog entry
- model files and storage layout
- input controls, such as point prompts, text prompts, sliders, or no prompt
- output rendering, such as a generated image, depth map, edge map, or segmentation mask

The helper script can generate local model context for an adapter prompt:

```bash
python3 scripts/inspect_android_model.py \
  --model-id <model-id> \
  --model-source <model-source-or-card-url> \
  --runtime <executorch|litert|onnxruntime> \
  --workload <image-segmentation|image-to-image> \
  --local-model-dir <local-model-folder>
```

The script writes:

- `android_model_config.json`
- `model-context/model-summary.json`
- small copied metadata files under `model-context/metadata`

Generated adapter code, Gradle dependencies, layouts, resources, and runtime dependencies must be compiled into a new APK and tested on an Arm64 Android device.

Do not add Hugging Face tokens, Arm AI Portal credentials, long-lived artifact credentials, or model binaries to this repository.

## License

This project is provided under the [Arm Education End User License Agreement](LICENSE.md).
