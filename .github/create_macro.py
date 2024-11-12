# Copyright (C) 2024 deepImageJ developers
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============================================================================

"""
Jython script that downloads the wanted model(s) from the Bioimage.io repo and 
creates a macro to run the model(s) downloaded on the sample input with deepimageJ
"""

from io.bioimage.modelrunner.bioimageio import BioimageioRepo
from io.bioimage.modelrunner.bioimageio.description import ModelDescriptorFactory
from io.bioimage.modelrunner.utils import Constants

import os
import argparse

MACRO_STR = "run(\"DeepImageJ Run\", \"modelPath={modelPath} inputPath={inputPath} outputFolder={outputFolder} displayOutput=null\")"

# Create the argument parser
parser = argparse.ArgumentParser()

# Add the arguments
parser.add_argument('-model_nicknames', type=str, required=True, 
                    help='Nickname of the models to be downloaded and used by the macros')
parser.add_argument('-models_dir', type=str, required=True, 
                    help='Path to the directory where models are installed')
parser.add_argument('-macro_path', type=str, required=True, 
                    help='Path to the macro file that is going to be created')

# Parse the arguments
args = parser.parse_args()

# Helper function to parse the model path(s)
def parse_model_paths(models: str):
    if ',' in models:
        return models.split(',')
    else:
        return models

# Access the argument values
model_nicknames = parse_model_paths(args.model_nicknames)
models_dir = args.models_dir
macro_path = args.macro_path


if not os.path.exists(models_dir) or not os.path.isdir(models_dir):
    os.makedirs(models_dir)

    
print("Connecting to the Bioimage.io repository")
br = BioimageioRepo.connect()
models_full_path = []
for model in model_nicknames:
    print("Downloading the Bioimage.io model: " + model.strip())
    model_dir = br.downloadByName(model.strip(), models_dir)
    models_full_path.append(model_dir)

## Create macros
with open(macro_path, "a") as file:

    for mfp in models_full_path:
        descriptor = ModelDescriptorFactory.readFromLocalFile(os.path.join(mfp, Constants.RDF_NAME))
        sample_name = descriptor.getInputTensors().get(0).getSampleTensorName()
        macro = MACRO_STR.format(model_path=mfp, input_path=os.path.join(mfp, Constants.sample_name), outputFolder=mfp)
        file.write(macro + os.linesep)

