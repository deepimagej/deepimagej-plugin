The Cellpose workflow uses `cellpose-test.png`, the image supplied for channel debugging:
red is empty, green contains cytoplasm, and blue contains nuclei.

Run **Cellpose channels across operating systems** manually in GitHub Actions.
It builds JDLL's `legacy` branch, then calls DeepImageJ's own installer and inference
methods with `cyto3` and diameter 30. No unit tests are added or run.

| Input | Cytoplasm channel | Nuclei channel |
| --- | --- | --- |
| Original RGB | green | none / blue |
| Two channels: green, blue | 1 | none / 2 |
| Two channels: blue, green | Channel 2 | none / Channel 1 |
| Grayscale: extracted green | gray | gray (no separate nuclei channel) |
| Four channels: blank, blue, blank, green | 4 | none / 2 |

Each OS must produce exactly the native Python Cellpose mask for the corresponding
cytoplasm-only or cytoplasm-plus-nuclei input, ignoring instance label numbering.
The Python reference uses the same denoising model, weights, diameter and XY axis
convention as JDLL. Equivalent layouts must therefore agree exactly within an OS.

The final job compares every pair of platforms. It requires the same cell count,
one-to-one instance matches with IoU at least 0.98 for every cell, and foreground
IoU at least 0.995, allowing small numerical differences between CPU architectures.
It also checks that the fixture, model hashes, Cellpose version and JDLL revision
match across platforms. Artifacts include input TIFFs, integer label PNGs, overlays,
reference masks, environment versions, logs and comparison tables.

Grayscale matches cytoplasm-only **when it is the extracted green channel**; an
RGB-to-gray conversion can mix channels and change the result. A single-channel
image cannot retain a distinct nuclei channel. These checks verify consistency
with Cellpose, rather than segmentation accuracy against annotated ground truth.
