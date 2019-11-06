# DeepImageJ - WORK IN PROGRESS
---
This is unfinished project that is still being developed
---
### The ImageJ plugin to run deep-learning models

DeepImageJ is a user-friendly plugin that enables the use of a variety of pre-trained deep learning models in ImageJ. The plugin bridges the gap between deep learning and standard life-science applications. DeepImageJ runs image-to-image operations on a standard CPU-based computer and does not require any deep learning expertise.

## One-click Installation
The ZIP file (DeepImageJ.zip) is a plugin for ImageJ or Fiji. It contains all the necessary libraries (JAR files) to load and run TensorFlow models on any OS: Windows, Mac OSX, Linux.

Unzip the ZIP file and store the 5 JAR files into the plugins folder of ImageJ or Fiji.

Create the folder models inside ImageJ/Fiji directory (".../ImageJ/models/").

Download a bundled model (https://deepimagej.github.io/deepimagej/models.html), unzip it into the directory 'models' and run it over your image!

## System requirements
Operating systems (same requirements as for ImageJ/Fiji software).
* Windows
* Mac OSX
* Linux.

The java libraries insided ImageJ/Fiji needed to run this plugin are:
* libtensorflow-1.12.0
* libtensorflow_jni-1.12.0
* proto-1.2.0
* protobuf-java-3.2.0

## Conditions of use
The DeepImageJ project is an open source software (OSS) under the BSD 2-Clause License. All the resources provided here are freely available for noncommercial and research purposes. Their use for any other purpose is generally possible, but solely with the explicit permission of the authors. You are expected to include adequate references whenever you present or publish results that are based on the resources provided.

## References
Cite the appropriate TensorFlow network which is bundled into DeepImageJ.

E. Gómez-de-Mariscal, C. García-López-de-Haro, L. Donati, M. Unser, A. Muñoz-Barrutia, D. Sage, "DeepImageJ: A user-friendly plugin to run deep learning models in ImageJ" BioRxiv, 2019.
https://www.biorxiv.org/content/10.1101/799270v2

### Further information: https://deepimagej.github.io/deepimagej
