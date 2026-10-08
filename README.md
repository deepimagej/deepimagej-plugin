![GitHub All Releases](https://img.shields.io/github/downloads/deepimagej/deepimagej-plugin/total?color=red)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/deepimagej/deepimagej-plugin)](https://github.com/deepimagej/deepimagej-plugin/releases)
[![GitHub](https://img.shields.io/github/license/deepimagej/deepimagej-plugin)](https://raw.githubusercontent.com/deepimagej/deepimagej-plugin/master/LICENSE)

<img src="https://raw.githubusercontent.com/deepimagej/deepimagej.github.io/master/images/icon.png" align="right" width="250"/>

# DeepImageJ

### The ImageJ plugin to run deep-learning models

DeepImageJ is a user-friendly plugin that enables the use of a variety of pre-trained deep learning models in ImageJ. The plugin bridges the gap between deep learning and standard life-science applications. DeepImageJ runs image-to-image operations on a standard CPU-based computer or on a GPU, and does not require any deep learning expertise.

Start using it with the guidelines in the [Wiki](https://github.com/deepimagej/deepimagej-plugin/wiki).

Find further information about DeepImageJ environment at https://deepimagej.github.io

## Macros

Run these examples in Fiji's macro editor (**Plugins > New > Macro**), or save them as `.ijm` files and use **Plugins > Macros > Run**. Replace the example paths with your own. Use forward slashes in paths on all operating systems, including Windows (`C:/data/cells.tif`), and surround option values containing spaces with square brackets.

To record a command, open **Plugins > Macros > Record**, configure the DeepImageJ dialog, and click **Run**. Use the recorded command that includes the model and other options; the initial command without options does not run a model.

### DeepImageJ Run

For a BioImage.IO model, `model_path` accepts a model directory or a model nickname. The command uses the active image when `input_path` is omitted. To process a file or a folder of images and save the results:

```ijm
run("DeepImageJ Run", "model_path=[/path/to/model] input_path=[/path/to/images] output_folder=[/path/to/results] display_output=all");
```

`model_path` is required. `input_path`, `output_folder`, and `display_output` are optional; `display_output=all` displays the results. Stack and channel support depend on the selected model.

### Cellpose

`DeepImageJ Cellpose` processes the active image and displays a label image named `<input>_labels.tif`. Background is 0; each detected object has a positive integer label. Required dependencies and missing pretrained weights are installed on the first run, which needs internet access.

| Option | Meaning |
| --- | --- |
| `model` (required) | `cyto`, `cyto2`, `cyto3`, `nuclei`, or the path to a custom weights file. |
| `cyto_color` (required) | Channel used for segmentation: `gray`, `red`, `blue`, or `green`. |
| `nuclei_color` (required) | Supporting nuclei channel, using the same choices. For a grayscale image, set both channel options to `gray`. |
| `diameter` | Object diameter in pixels. Set it explicitly, for example `30`; omitting it currently passes `0` from the macro parser. |
| `display_all` | `true` displays flows and the restored image as well as labels; the default is `false`. |

Grayscale image:

```ijm
open("/path/to/cells.tif");
run("DeepImageJ Cellpose", "model=cyto3 cyto_color=gray nuclei_color=gray diameter=30 display_all=false");
```

Three-channel image, with the segmentation signal in channel 1 and nuclei in channel 2:

```ijm
open("/path/to/three-channel-cells.tif");
run("DeepImageJ Cellpose", "model=cyto3 cyto_color=red nuclei_color=blue diameter=30 display_all=false");
```

The current channel map is **`red` = channel 1, `blue` = channel 2, `green` = channel 3**. Check your channel order against this mapping. The wrapper expects either one grayscale channel or three channels; extract a single channel or prepare a three-channel image before running it on a two-channel image.

For custom weights, replace `model=cyto3` with, for example, `model=[C:/models/my-cellpose-weights]`.

### StarDist

`DeepImageJ StarDist` processes the active image and displays an instance mask named `<input>_mask.tif`. The pretrained fluorescence model expects one channel; the H&E model expects three channels. Dependencies and missing pretrained models are downloaded on the first run.

| Option | Meaning |
| --- | --- |
| `model` (required) | `StarDist Fluorescence Nuclei Segmentation`, `StarDist H&E Nuclei Segmentation`, or the path to a custom StarDist model directory. |
| `prob_thresh` | Detection probability threshold; the macro default is `0.5`. |
| `min_percentile` | Lower intensity normalization percentile; use an explicit value such as `1`. |
| `max_percentile` | Upper intensity normalization percentile; use an explicit value such as `99.8`. |

Set both percentile options explicitly, with `min_percentile < max_percentile`: the current macro parser defaults both to `0.5`, unlike the Java API's `1` and `99.8` defaults.

Fluorescence image:

```ijm
open("/path/to/nuclei.tif");
run("DeepImageJ StarDist", "model=[StarDist Fluorescence Nuclei Segmentation] prob_thresh=0.5 min_percentile=1 max_percentile=99.8");
```

RGB H&E image:

```ijm
open("/path/to/he-image.tif");
run("DeepImageJ StarDist", "model=[StarDist H&E Nuclei Segmentation] prob_thresh=0.5 min_percentile=1 max_percentile=99.8");
```

For a custom model, use a directory containing its configuration and weights, for example `model=[C:/models/my-stardist-model]`.

### Stacks and time series

With the 2D models above, a Z stack with one time point is segmented slice by slice, and a time series with one Z plane is segmented frame by frame. The results form an output stack, which may use ImageJ's **T** dimension even when the input uses **Z**. Object labels are assigned independently in each plane; they do not identify or track an object across planes.

Cellpose on a grayscale Z stack:

```ijm
open("/path/to/cells-z-stack.tif");
run("DeepImageJ Cellpose", "model=cyto3 cyto_color=gray nuclei_color=gray diameter=30 display_all=false");
```

StarDist on a fluorescence time series (one Z plane per frame):

```ijm
open("/path/to/nuclei-time-series.tif");
run("DeepImageJ StarDist", "model=[StarDist Fluorescence Nuclei Segmentation] prob_thresh=0.5 min_percentile=1 max_percentile=99.8");
```

For a hyperstack containing **both Z slices and time frames**, the current 2D wrappers process only the first Z plane when several frames are present. Use the following loop to process every Z/time pair and save one mask per plane. It preserves all channels when duplicating a plane and waits for the result window before saving it.

```ijm
open("/path/to/nuclei-zt-hyperstack.tif");
input = getImageID();
getDimensions(width, height, channels, slices, frames);
outputDir = getDirectory("Choose output folder");
prefix = "plane-" + getTime();
command = "DeepImageJ StarDist";
options = "model=[StarDist Fluorescence Nuclei Segmentation] prob_thresh=0.5 min_percentile=1 max_percentile=99.8";
suffix = "_mask.tif";

for (t = 1; t <= frames; t++) {
    for (z = 1; z <= slices; z++) {
        selectImage(input);
        planeTitle = prefix + "-z" + z + "-t" + t;
        run("Duplicate...", "title=[" + planeTitle + "] duplicate channels=1-" + channels + " slices=" + z + " frames=" + t);
        plane = getImageID();
        run(command, options);
        resultTitle = planeTitle + suffix;
        while (!isOpen(resultTitle)) wait(50);
        selectWindow(resultTitle);
        saveAs("Tiff", outputDir + resultTitle);
        close();
        selectImage(plane);
        close();
    }
}
selectImage(input);
```

For Cellpose on a grayscale Z/time hyperstack, replace the three configuration lines with:

```ijm
command = "DeepImageJ Cellpose";
options = "model=cyto3 cyto_color=gray nuclei_color=gray diameter=30 display_all=false";
suffix = "_labels.tif";
```

These examples use the current macro parsers and stack handling in [Cellpose_DeepImageJ.java](src/main/java/Cellpose_DeepImageJ.java) and [Stardist_DeepImageJ.java](src/main/java/Stardist_DeepImageJ.java). See the [ImageJ macro functions reference](https://imagej.net/ij/developer/macro/functions2.html) for image selection, dimensions, and saving.

## Conditions of use
The DeepImageJ project is an open source software (OSS) under the BSD 2-Clause License. All the resources provided here are freely available. 

As a matter of academic integrity, we strongly encourage users to include adequate references whenever they present or publish results that are based on the resources provided here. 

## References
* If you used one of the material provided within DeepImageJ such as trained models or Python notebooks, cite their authors' work. 
* [E. Gómez-de-Mariscal, C. García-López-de-Haro, W. Ouyang, L. Donati, E. Lundberg, M. Unser, A. Muñoz-Barrutia, D. Sage,
*DeepImageJ: A user-friendly environment to run deep learning models in ImageJ*, Nat Methods 18, 1192–1195 (2021).](https://doi.org/10.1038/s41592-021-01262-9)

```bibtex
@article{gomez2021deepimagej,
  title={DeepImageJ: A user-friendly environment to run deep learning models in ImageJ},
  author={G{\'o}mez-de-Mariscal, Estibaliz and Garc{\'i}a-L{\'o}pez-de-Haro, Carlos and Ouyang, Wei and Donati, Laur{\`e}ne and Lundberg, Emma and Unser, Michael and Mu{\~{n}}oz-Barrutia, Arrate and Sage, Daniel},
  journal={Nature Methods},
  year={2021},
  volume={18},
  number={10},
  pages={1192-1195},
  URL = {https://doi.org/10.1038/s41592-021-01262-9},
  doi = {10.1038/s41592-021-01262-9}
}
```
