"""CI mask comparisons; no training annotations are available for this fixture."""
import hashlib
import importlib.metadata
import itertools
import json
import os
from pathlib import Path
import sys

import numpy as np
from PIL import Image

RUNNERS = ("ubuntu-latest", "windows-latest", "macos-15-intel", "macos-15")


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def canonical(mask):
    """Ignore instance numbering, preserving background and every instance boundary."""
    labels, first, inverse = np.unique(mask, return_index=True, return_inverse=True)
    assert labels[0] == 0 and len(labels) > 1, "Mask must contain background and cells"
    order = np.argsort(first[1:])
    mapping = np.zeros(len(labels), dtype=np.int32)
    mapping[order + 1] = np.arange(1, len(labels))
    return mapping[inverse].reshape(mask.shape)


def load_mask(root, name, shape):
    mask = np.asarray(Image.open(root / f"{name}-mask.png"))
    assert mask.shape == shape, f"{name}: wrong mask shape {mask.shape}"
    return canonical(mask)


def report(path, lines):
    text = "\n".join(lines) + "\n"
    path.write_text(text)
    print(text)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as summary:
            summary.write(text)


def verify(root, fixture):
    from cellpose import denoise
    import torch

    manifest = json.loads((root / "manifest.json").read_text())
    shape = (manifest["height"], manifest["width"])
    rgb = np.asarray(Image.open(fixture).convert("RGB"))
    # Match JDLL legacy's XYC Python input; transpose its XY masks back to PNG's YX.
    # Read the fixture independently of the Java channel selector/packing code.
    image = rgb.transpose(1, 0, 2).astype(np.float32)
    model = denoise.CellposeDenoiseModel(gpu=False, pretrained_model=manifest["weights"])
    references = {}
    for group, channels in (("cyto", [2, 0]), ("cyto-nuclei", [2, 3])):
        masks, *_ = model.eval([image.copy()], channels=[channels], diameter=manifest["diameter"])
        mask = canonical(masks[0].T)
        references[group] = mask
        Image.fromarray(mask.astype(np.uint16)).save(root / f"reference-{group}-mask.png")

    manifest["versions"] = {name: importlib.metadata.version(name)
                            for name in ("cellpose", "torch", "numpy", "numba", "llvmlite")}
    manifest["fixture_sha256"] = digest(fixture)
    manifest["weights_sha256"] = {"segmentation": digest(manifest["weights"]),
                                  "restoration": digest(model.dn.pretrained_model)}
    manifest["device"] = str(model.cp.device)
    (root / "manifest.json").write_text(json.dumps(manifest, indent=2))
    print("ENVIRONMENT:", json.dumps(manifest["versions"]), "device:", model.cp.device,
          "threads:", torch.get_num_threads())
    lines = ["## Cellpose channel comparisons", "", "| Case | Cells | Native Python / equivalent layout |",
             "| --- | ---: | --- |"]
    failures = []
    for case in manifest["cases"]:
        mask = load_mask(root, case["name"], shape)
        equal = np.array_equal(mask, references[case["group"]])
        lines.append(f"| {case['name']} | {mask.max()} | {'PASS (exact)' if equal else 'FAIL'} |")
        if not equal:
            failures.append(case["name"])
        # Downloadable overlay for visual inspection, alongside the original integer labels.
        colors = np.stack(((mask * 67) % 255, (mask * 137) % 255, (mask * 193) % 255), axis=-1)
        overlay = np.where((mask > 0)[..., None], 0.6 * rgb + 0.4 * colors, rgb).astype(np.uint8)
        Image.fromarray(overlay).save(root / f"{case['name']}-overlay.png")
    report(root / "verification.md", lines)
    assert not failures, f"Masks disagree with native Cellpose: {failures}"
    (root / "verified.txt").write_text("All channel combinations match native Python exactly.\n")


def overlap(a, b):
    """Require the same cells and one-to-one instance matches, allowing tiny boundary changes."""
    assert a.shape == b.shape
    na, nb = int(a.max()), int(b.max())
    intersections = np.bincount((a * (nb + 1) + b).ravel(), minlength=(na + 1) * (nb + 1))
    intersections = intersections.reshape(na + 1, nb + 1)
    areas_a = np.bincount(a.ravel(), minlength=na + 1)[1:]
    areas_b = np.bincount(b.ravel(), minlength=nb + 1)[1:]
    cells = intersections[1:, 1:]
    iou = cells / (areas_a[:, None] + areas_b[None, :] - cells)
    foreground_iou = np.count_nonzero((a > 0) & (b > 0)) / np.count_nonzero((a > 0) | (b > 0))
    minimum = float(iou.max(axis=1).min())
    passed = na == nb and len(np.unique(iou.argmax(axis=1))) == nb and minimum >= 0.98 and foreground_iou >= 0.995
    return passed, minimum, foreground_iou


def compare(root):
    manifests = {}
    for runner in RUNNERS:
        directory = root / f"cellpose-{runner}"
        assert (directory / "verified.txt").is_file(), f"Missing completed verification for {runner}"
        manifests[runner] = json.loads((directory / "manifest.json").read_text())
    first = manifests[RUNNERS[0]]
    for runner, manifest in manifests.items():
        for key in ("fixture_sha256", "weights_sha256", "diameter", "cases", "width", "height"):
            assert manifest[key] == first[key], f"{runner}: different {key}"
        assert manifest["versions"]["cellpose"] == first["versions"]["cellpose"], "Cellpose versions differ"
        assert (root / f"cellpose-{runner}" / "jdll-commit.txt").read_text() == (
            root / f"cellpose-{RUNNERS[0]}" / "jdll-commit.txt").read_text(), "JDLL revisions differ"
    lines = ["## Cellpose across operating systems", "",
             "Same cell count; every matched instance IoU ≥ 0.98; foreground IoU ≥ 0.995.", "",
             "| Platforms | Case | Minimum cell IoU | Foreground IoU | Result |",
             "| --- | --- | ---: | ---: | --- |"]
    failures = []
    shape = (first["height"], first["width"])
    for left, right in itertools.combinations(RUNNERS, 2):
        for case in first["cases"]:
            name = case["name"]
            a = load_mask(root / f"cellpose-{left}", name, shape)
            b = load_mask(root / f"cellpose-{right}", name, shape)
            passed, cell_iou, foreground_iou = overlap(a, b)
            lines.append(f"| {left} / {right} | {name} | {cell_iou:.4f} | {foreground_iou:.4f} | {'PASS' if passed else 'FAIL'} |")
            if not passed:
                failures.append(f"{left}/{right}/{name}")
    report(root / "comparison.md", lines)
    assert not failures, f"Cross-platform mask differences: {failures}"


if __name__ == "__main__":
    if sys.argv[1] == "verify":
        verify(Path(sys.argv[2]), Path(sys.argv[3]))
    elif sys.argv[1] == "compare":
        compare(Path(sys.argv[2]))
    else:
        raise ValueError("Expected verify or compare")
