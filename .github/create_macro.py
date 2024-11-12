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
import sys
import os
import argparse

# Create the argument parser
parser = argparse.ArgumentParser()

# Add the arguments
parser.add_argument('-model_nicknames', type=str, required=True, 
                    help='Nickname of the models to be downloaded and used by the macros')
parser.add_argument('-models_dir', type=str, required=True, 
                    help='Path to the directory where models are installed')
parser.add_argument('-macros_path', type=str, required=True, 
                    help='Directory where the macros are going to be created into')

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
macros_dir = args.macros_path


full_path = os.path.join(os. getcwd(), "models")
bmzModelName = "B. Sutilist bacteria segmentation - Widefield microscopy - 2D UNet"

if not os.path.exists(models_dir) or not os.path.isdir(models_dir):
    os.makedirs(models_dir)
if not os.path.exists(macros_dir) or not os.path.isdir(macros_dir):
    os.makedirs(macros_dir)
    
print("Connecting to the Bioimage.io repository")
br = BioimageioRepo.connect()
model_dirs = []
for model in model_nicknames:
    print("Downloading the Bioimage.io model: " + model)
    model_dir = br.downloadByName(model, models_dir)
    model_dirs.append(model_dir)

