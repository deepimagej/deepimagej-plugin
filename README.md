[![](https://travis-ci.com/deepimagej/deepimagej-plugin.svg?branch=master)](https://travis-ci.com/deepimagej/deepimagej-plugin)
![GitHub All Releases](https://img.shields.io/github/downloads/deepimagej/deepimagej-plugin/total?color=red)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/deepimagej/deepimagej-plugin)](https://github.com/deepimagej/deepimagej-plugin/releases)
[![GitHub](https://img.shields.io/github/license/deepimagej/deepimagej-plugin)](https://raw.githubusercontent.com/deepimagej/deepimagej-plugin/master/LICENSE)

# DeepImageJ

### The ImageJ plugin to run deep-learning models

DeepImageJ is a user-friendly plugin that enables the use of a variety of pre-trained deep learning models in ImageJ. The plugin bridges the gap between deep learning and standard life-science applications. DeepImageJ runs image-to-image operations on a standard CPU-based computer and does not require any deep learning expertise.

## One-click Installation
Go to releases (https://github.com/deepimagej/deepimagej-plugin/releases) and download the last version of the plugin: 

- The ZIP file (DeepImageJ.zip) is a plugin for ImageJ or Fiji. It contains all the necessary libraries (JAR files) and DeepImageJ_X.X.X.jar to load and run TensorFlow models on any OS: Windows, Mac OSX, Linux.

Unzip the ZIP file and store the 5 JAR files into the plugins folder of ImageJ or Fiji.

Create the folder models inside ImageJ/Fiji directory (".../ImageJ/models/").

Download a bundled model (https://deepimagej.github.io/deepimagej/models.html), unzip it into the directory 'models' and run it over your image!

## System requirements
Operating systems (same requirements as for ImageJ/Fiji software).
* Windows
* Mac OSX
* Linux.

The java libraries insided ImageJ/Fiji needed to run this plugin in CPU mode are:
* libtensorflow-1.15.0
* libtensorflow_jni-1.15.0
* proto-1.2.0
* protobuf-java-3.2.0
* junit-4.12
* hamcrest-core-1.3

To run the plugin in GPU mode, the dependency *libtensorflow_jni-1.15.0* has to be substituted by *libtensorflow_jni_gpu-1.15.0*. **There should only be one of those two dependencies installed in the ImageJ/Fiji directory.**

The last release of the plugin including GPU connection and TensorFlow JAVA library 1.15 can be downloaded [here](https://github.com/deepimagej/deepimagej-plugin/releases/tag/1.1.0).

### Required cuda version with TensorFlow JavaAPI:
* TensorFlow 1.12: CUDA 9.0 and corresponding CUDnn and drivers. 
* TensorFlow 1.15: CUDA 10.0 and corresponding CUDnn and drivers.

Further information:
- https://github.com/CSBDeep/CSBDeep_website/wiki/CSBDeep-in-Fiji-%E2%80%93-Installation
- https://github.com/tensorflow/tensorflow/issues/16660


## Conditions of use
The DeepImageJ project is an open source software (OSS) under the BSD 2-Clause License. All the resources provided here are freely available. 

As a matter of academic integrity, we strongly encourage users to include adequate references whenever they present or publish results that are based on the resources provided here. 

## References
Cite the appropriate TensorFlow network which is bundled into DeepImageJ.

E. Gómez-de-Mariscal, C. García-López-de-Haro, L. Donati, M. Unser, A. Muñoz-Barrutia, D. Sage, "DeepImageJ: A user-friendly plugin to run deep learning models in ImageJ" BioRxiv, 2019.
https://www.biorxiv.org/content/10.1101/799270v2

### Further information: https://deepimagej.github.io/deepimagej
