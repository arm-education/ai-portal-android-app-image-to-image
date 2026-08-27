#!/usr/bin/env python3
"""Create local model context for the Android image-to-image app."""

from __future__ import annotations

import argparse
import json
import mimetypes
import shutil
from datetime import datetime, timezone
from pathlib import Path


RUNTIME_EXTENSIONS = {
    "executorch": [".pte"],
    "litert": [".tflite"],
    "tflite": [".tflite"],
    "onnx": [".onnx"],
    "onnxruntime": [".onnx"],
}

CONFIG_SUFFIXES = {".json", ".yaml", ".yml", ".txt", ".md", ".py"}
MAX_METADATA_BYTES = 512 * 1024


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Inspect a local Android image model folder and generate context for a runtime adapter."
    )
    parser.add_argument("--model-id", required=True, help="Stable ID used in model_catalog.json.")
    parser.add_argument("--model-source", required=True, help="Model repository URL, model card URL, or artifact source.")
    parser.add_argument("--runtime", required=True, help="Runtime value, such as executorch, litert, or onnxruntime.")
    parser.add_argument("--workload", required=True, choices=["image-segmentation", "image-to-image"])
    parser.add_argument("--local-model-dir", required=True, type=Path, help="Directory containing downloaded model files.")
    parser.add_argument("--output-dir", default=Path("."), type=Path, help="Android app project root.")
    return parser.parse_args()


def relative_files(model_dir: Path) -> list[dict[str, object]]:
    files = []
    for path in sorted(model_dir.rglob("*")):
        if not path.is_file() or any(part.startswith(".") for part in path.relative_to(model_dir).parts):
            continue
        relative = path.relative_to(model_dir).as_posix()
        files.append(
            {
                "path": relative,
                "size_bytes": path.stat().st_size,
                "extension": path.suffix.lower(),
                "mime_type": mimetypes.guess_type(path.name)[0] or "",
            }
        )
    return files


def select_candidates(files: list[dict[str, object]], runtime: str) -> list[str]:
    extensions = RUNTIME_EXTENSIONS.get(runtime, [])
    return [str(item["path"]) for item in files if str(item["extension"]) in extensions]


def copy_metadata(model_dir: Path, context_dir: Path, files: list[dict[str, object]]) -> list[str]:
    metadata_dir = context_dir / "metadata"
    metadata_dir.mkdir(parents=True, exist_ok=True)
    copied = []
    for item in files:
        path = str(item["path"])
        if int(item["size_bytes"]) > MAX_METADATA_BYTES or str(item["extension"]) not in CONFIG_SUFFIXES:
            continue
        destination = metadata_dir / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(model_dir / path, destination)
        copied.append(path)
    return copied


def main() -> None:
    args = parse_args()
    model_dir = args.local_model_dir.expanduser().resolve()
    output_dir = args.output_dir.expanduser().resolve()
    context_dir = output_dir / "model-context"

    if not model_dir.is_dir():
        raise SystemExit(f"Local model directory not found: {model_dir}")

    files = relative_files(model_dir)
    candidates = select_candidates(files, args.runtime)
    metadata_files = copy_metadata(model_dir, context_dir, files)
    primary_artifact = candidates[0] if len(candidates) == 1 else ""

    summary = {
        "created_at": datetime.now(timezone.utc).isoformat(),
        "model_id": args.model_id,
        "model_source": args.model_source,
        "runtime": args.runtime,
        "workload": args.workload,
        "local_model_dir": str(model_dir),
        "files": files,
        "primary_artifact_candidates": candidates,
        "metadata_files_copied": metadata_files,
        "suggested_catalog_entry": {
            "id": args.model_id,
            "displayName": args.model_id.replace("-", " ").title(),
            "workload": args.workload,
            "runtime": args.runtime,
            "artifactType": "file" if primary_artifact else "directory",
            "artifactPath": args.model_id,
            "modelFile": primary_artifact,
            "externalDataFiles": [],
            "adapterId": "generated-runtime-adapter",
            "inputImageSize": 0,
            "outputImageSize": 0,
            "colorFormat": "RGB",
            "normalization": "model-specific",
            "promptType": "model-specific",
            "defaultBoxPrompt": [],
            "inputTensorNames": [],
            "inputTensorShapes": [],
            "outputTensorNames": [],
            "outputTensorShapes": [],
        },
    }

    context_dir.mkdir(parents=True, exist_ok=True)
    (context_dir / "model-summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    (output_dir / "android_model_config.json").write_text(
        json.dumps(
            {
                "model_id": args.model_id,
                "model_source": args.model_source,
                "runtime": args.runtime,
                "workload": args.workload,
                "local_model_dir": str(model_dir),
                "primary_artifact": primary_artifact,
                "catalog_entry": summary["suggested_catalog_entry"],
            },
            indent=2,
        )
        + "\n"
    )

    print(f"Wrote {context_dir / 'model-summary.json'}")
    print(f"Wrote {output_dir / 'android_model_config.json'}")


if __name__ == "__main__":
    main()
