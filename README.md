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
* proto-1.12.0
* protobuf-java-3.2.0
* junit-4.12
* hamcrest-core-1.3

In order to run the plugin in GPU mode, the dependency *libtensorflow_jni-1.15.0* has to be substituted by *libtensorflow_jni_gpu-1.15.0*. There should only be one of those two dependencies installed in the ImageJ/Fiji directory.

In order to download the needed dependencies click here.

## Conditions of use
The DeepImageJ project is an open source software (OSS) under the BSD 2-Clause License. All the resources provided here are freely available. 

As a matter of academic integrity, we strongly encourage users to include adequate references whenever they present or publish results that are based on the resources provided here. 

## References
Cite the appropriate TensorFlow network which is bundled into DeepImageJ.

E. Gómez-de-Mariscal, C. García-López-de-Haro, L. Donati, M. Unser, A. Muñoz-Barrutia, D. Sage, "DeepImageJ: A user-friendly plugin to run deep learning models in ImageJ" BioRxiv, 2019.
https://www.biorxiv.org/content/10.1101/799270v2

### Further information: https://deepimagej.github.io/deepimagej
