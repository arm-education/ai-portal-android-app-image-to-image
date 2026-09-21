# Image Analysis Android application

This Android application accompanies Arm Learning Paths for running image models from the Arm AI Portal. It is intended for learning how model-specific adapters run on devices and is not a reference production application.

The application supports MobileSAM segmentation and Depth Anything V2 Small depth estimation. Each model has a dedicated adapter for its tensor contract, preprocessing, and result decoding.

The repository does not include runnable model binaries. Real model files are copied into app-private storage on the device. Each model directory contains a `MOCK_REPLACE_ME_...` text placeholder that explains the optional bundled-model location for local experiments. Clear the app's storage or uninstall the app to remove app-private model files.

All inference runs on the device. The app declares no network permissions and does not upload the selected image or result. It reads images through Android's system document picker, whose provider may have its own storage and network behavior. The app holds the decoded image and generated result in memory and does not save it.

## Application flow

The application opens with:

- a model dropdown for MobileSAM and Depth Anything V2 Small
- a **Load model** button
- a **Choose image** button
- an image preview/result area that shows **No image selected** before an image is chosen
- a workload-aware **Run segmentation** or **Run depth estimation** button
- a status/result panel
- a cyan mask overlay or grayscale relative-disparity map after inference

The default MobileSAM box prompt spans the central 80 percent of both the width and height of the resized `1024 x 1024` image. The fixed prompt keeps the box coordinates consistent across runs.

## Requirements

- Android Studio Narwhal 3 Feature Drop (2025.1.3) or a newer compatible release, with Android SDK 35
- Android SDK Platform Tools, with `adb` available on your `PATH`
- Java 17, supplied by Android Studio or available on your `PATH`
- Python 3 with `venv` and `pip`; the commands below use `python3` on macOS and Linux and `py` on Windows
- An Arm64 Android device running Android 9 (API level 28) or later
- Access to the selected ExecuTorch model file on Hugging Face

## Supported models

| Model | Runtime | Copy this file |
| --- | --- | --- |
| MobileSAM INT8 Segmentation | ExecuTorch with XNNPACK | `mobile_sam_raspberry_executorch_optimized.pte` |
| Depth Anything V2 Small INT8 | ExecuTorch with XNNPACK | `depth_anything_v2_small_executorch_optimized.pte` |

The model catalog entries are stored in `app/src/main/assets/model_catalog.json`. The model-specific adapter IDs are `mobile-sam-executorch` and `depth-anything-v2-executorch`.

`RuntimeRunnerFactory.kt` maps each ID to its dedicated adapter.

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
        ├── depthanything/
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

The Depth Anything implementation lives in `inference/models/depthanything/`. It resizes RGB input to `686 x 518`, applies ImageNet normalization, validates the `[1, 518, 686]` relative-disparity output, and renders a grayscale map where white represents nearer regions.

`LiteRtImageToImageAdapter.kt` and `OnnxImageToImageAdapter.kt` are incomplete extension placeholders. They do not include the required runtime dependencies or implement model loading and inference.

## Download a model

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

### Download MobileSAM

Download the MobileSAM model file into a local model directory.

On macOS or Linux:

```bash
mkdir -p model/mobile-sam-int8-xnnpack-executorch
.hf-venv/bin/python - <<'PY'
from huggingface_hub import hf_hub_download

path = hf_hub_download(
    repo_id="Arm/mobile-sam-int8-xnnpack-executorch",
    filename="mobile_sam_raspberry_executorch_optimized.pte",
    local_dir="model/mobile-sam-int8-xnnpack-executorch",
)
print(path)
PY
```

On Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path model\mobile-sam-int8-xnnpack-executorch | Out-Null
.\.hf-venv\Scripts\python.exe -c "from huggingface_hub import hf_hub_download; print(hf_hub_download(repo_id='Arm/mobile-sam-int8-xnnpack-executorch', filename='mobile_sam_raspberry_executorch_optimized.pte', local_dir='model/mobile-sam-int8-xnnpack-executorch'))"
```

### Download Depth Anything V2 Small

Download the exact Arm-optimized Depth Anything V2 Small artifact.

On macOS or Linux:

```bash
MODEL_ID="depth-anything-v2-small-int8-xnnpack-executorch-vivo-x300"
MODEL_FILE="depth_anything_v2_small_executorch_optimized.pte"
mkdir -p "model/$MODEL_ID"
.hf-venv/bin/hf download "Arm/$MODEL_ID" "$MODEL_FILE" \
  --local-dir "model/$MODEL_ID"
```

On Windows PowerShell:

```powershell
$MODEL_ID = "depth-anything-v2-small-int8-xnnpack-executorch-vivo-x300"
$MODEL_FILE = "depth_anything_v2_small_executorch_optimized.pte"
$MODEL_DIR = "model\$MODEL_ID"
New-Item -ItemType Directory -Force -Path $MODEL_DIR | Out-Null
.\.hf-venv\Scripts\hf.exe download "Arm/$MODEL_ID" $MODEL_FILE `
  --local-dir $MODEL_DIR
```

## Open and run the application

Clone or download this repository, then open the repository root in Android Studio.

Connect an Arm64 Android phone or start an Arm64 emulator. Wait for Gradle sync to finish, select the `app` configuration, and run it once so Android creates the app-private directory.

### Copy and run MobileSAM

Copy the downloaded MobileSAM model into app-private storage.

On macOS or Linux:

```bash
MODEL_ID="mobile-sam-int8-xnnpack-executorch"
MODEL_FILE="mobile_sam_raspberry_executorch_optimized.pte"
MODEL_PATH="model/$MODEL_ID/$MODEL_FILE"

adb shell run-as com.arm.learningpath.imagetoimage mkdir -p "files/models/$MODEL_ID"
adb push "$MODEL_PATH" "/data/local/tmp/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage \
  cp "/data/local/tmp/$MODEL_FILE" "files/models/$MODEL_ID/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage \
  ls -l "files/models/$MODEL_ID/$MODEL_FILE"
adb shell rm "/data/local/tmp/$MODEL_FILE"
```

On Windows PowerShell:

```powershell
$MODEL_ID = "mobile-sam-int8-xnnpack-executorch"
$MODEL_FILE = "mobile_sam_raspberry_executorch_optimized.pte"
$MODEL_PATH = "model\$MODEL_ID\$MODEL_FILE"

adb shell run-as com.arm.learningpath.imagetoimage mkdir -p "files/models/$MODEL_ID"
adb push $MODEL_PATH "/data/local/tmp/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage `
  cp "/data/local/tmp/$MODEL_FILE" "files/models/$MODEL_ID/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage `
  ls -l "files/models/$MODEL_ID/$MODEL_FILE"
adb shell rm "/data/local/tmp/$MODEL_FILE"
```

The final `adb shell rm` command deletes only the temporary device copy. The downloaded source file remains under `model/` on your development computer.

In the app:

1. Select **MobileSAM INT8 Segmentation** from the model menu.
2. Select **Load model**.
3. Select **Choose image** or tap the preview area.
4. Choose a JPEG or PNG image.
5. Confirm that the dashed prompt box is visible over the image.
6. Select **Run segmentation**.

The app displays the selected mask as a translucent cyan overlay. The status panel displays model load and run times. The result panel displays the selected mask, predicted IoU, low-resolution mask coverage, and mask logit range.

For local experiments, you can alternatively replace the placeholder under `app/src/main/assets/models/mobile-sam-int8-xnnpack-executorch/` with the real `mobile_sam_raspberry_executorch_optimized.pte` file before building the APK. When **Load model** runs and no app-private model file exists, the app copies a real bundled asset into app-private storage. App-private model files persist across normal app updates, so clear the app's storage or remove the existing file before testing a replacement. Do not commit real model binaries to this repository.

### Copy and run Depth Anything V2 Small

Copy the downloaded Depth Anything model into app-private storage.

On macOS or Linux:

```bash
MODEL_ID="depth-anything-v2-small-int8-xnnpack-executorch-vivo-x300"
MODEL_FILE="depth_anything_v2_small_executorch_optimized.pte"
MODEL_PATH="model/$MODEL_ID/$MODEL_FILE"

adb shell run-as com.arm.learningpath.imagetoimage mkdir -p "files/models/$MODEL_ID"
adb push "$MODEL_PATH" "/data/local/tmp/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage \
  cp "/data/local/tmp/$MODEL_FILE" "files/models/$MODEL_ID/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage \
  ls -l "files/models/$MODEL_ID/$MODEL_FILE"
adb shell rm "/data/local/tmp/$MODEL_FILE"
```

On Windows PowerShell:

```powershell
$MODEL_ID = "depth-anything-v2-small-int8-xnnpack-executorch-vivo-x300"
$MODEL_FILE = "depth_anything_v2_small_executorch_optimized.pte"
$MODEL_PATH = "model\$MODEL_ID\$MODEL_FILE"

adb shell run-as com.arm.learningpath.imagetoimage mkdir -p "files/models/$MODEL_ID"
adb push $MODEL_PATH "/data/local/tmp/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage `
  cp "/data/local/tmp/$MODEL_FILE" "files/models/$MODEL_ID/$MODEL_FILE"
adb shell run-as com.arm.learningpath.imagetoimage `
  ls -l "files/models/$MODEL_ID/$MODEL_FILE"
adb shell rm "/data/local/tmp/$MODEL_FILE"
```

In the app:

1. Select **Depth Anything V2 Small INT8** from the model menu.
2. Select **Load model**.
3. Select **Choose image** and choose a JPEG or PNG scene.
4. Select **Run depth estimation**.

The app displays a grayscale relative-disparity map where white represents nearer regions and black represents farther regions. The result panel reports the disparity range, and the status panel reports model load and run times.

For local experiments, you can alternatively replace the placeholder under `app/src/main/assets/models/depth-anything-v2-small-int8-xnnpack-executorch-vivo-x300/` with the real `depth_anything_v2_small_executorch_optimized.pte` file before building the APK. Do not commit real model binaries to this repository.

## Extend the application

The reusable parts of the app are the Android shell, model catalog loading, app-local or optional bundled model storage, image decoding, preview surface, and adapter interface. The MobileSAM and Depth Anything adapters are model-specific.

For another image-to-image model, inspect the model package and add a matching adapter. Updating only `model_catalog.json` is not enough unless a registered adapter already supports that exact model contract. Depending on the model, you may also need to adjust:

- the catalog entry
- model files and storage layout
- model-selection UI and state when exposing more than one catalog entry
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

Do not add the `.hf-venv/` virtual environment, Hugging Face tokens, Arm AI Portal credentials, long-lived artifact credentials, or model binaries to this repository.

## License

This project is provided under the [Arm Education End User License Agreement](LICENSE.md).
