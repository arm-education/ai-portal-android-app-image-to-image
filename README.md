# Photo Insight Android application

This Android application accompanies the Arm Learning Path for running image-to-image models from the Arm AI Portal. It is intended for learning how image-to-image model adapters run on devices and is not a reference production application. It is provided under the [Arm Education End User License Agreement](LICENSE.md).

The visible application flow is MobileSAM-only for the Learning Path. The project is structured as a reusable Android app shell with one validated MobileSAM adapter. Additional models require model-specific adapters for their own input tensors, output tensors, preprocessing, result decoding, and UI controls.

The application does not store model binaries in the Android application package (APK) or in this repository. Model files are copied into app-private storage on the device.

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

`RuntimeRunnerFactory.kt` maps this adapter ID to `ExecuTorchSegmentationAdapter.kt`.

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

## Extend the application

The reusable parts of the app are the Android shell, model catalog loading, app-local model storage, preview surface, and adapter interface. The MobileSAM adapter is model-specific.

For another image-to-image model, inspect the model package and add a matching adapter. The helper script can generate local model context for an adapter prompt:

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
