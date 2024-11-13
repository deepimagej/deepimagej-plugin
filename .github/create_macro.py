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
from io.bioimage.modelrunner.numpy import DecodeNumpy
from io.bioimage.modelrunner.versionmanagement import AvailableEngines

from deepimagej.tools import ImPlusRaiManager

from ij import IJ

import os
import argparse
import json

MACRO_STR = "run(\"DeepImageJ Run\", \"modelPath={model_path} inputPath={input_path} outputFolder={output_folder} displayOutput=null\")"
CREATED_SAMPLE_NAME = "sample_input_0.tif"


def is_model_supported_on_os(descriptor):
    modelWeights = descriptor.getWeights().gettAllSupportedWeightObjects()
    for ww in modelWeights:
        isAvailable = AvailableEngines.isEngineSupportedInOS(ww.getFramework(), ww.getTrainingVersion(), None, None)
        if isAvailable:
            return True
    return False

def convert_npy_to_tif(folder_path, test_name, axesOrder):
    rai = DecodeNumpy.loadNpy(os.path.join(folder_path, test_name))
    imp = ImPlusRaiManager.convert(rai, axesOrder)
    out_path = os.path.join(folder_path, CREATED_SAMPLE_NAME)
    IJ.saveAsTiff(imp, out_path)


# Create the argument parser
parser = argparse.ArgumentParser()

# Add the arguments
parser.add_argument('-model_nicknames', type=str, required=True, 
                    help='Nickname of the models to be downloaded and used by the macros')
parser.add_argument('-models_dir', type=str, required=True, 
                    help='Path to the directory where models are installed')
parser.add_argument('-macro_path', type=str, required=True, 
                    help='Path to the macro file that is going to be created')
parser.add_argument('-json_fpath', type=str, required=True,
                    help='Path to the json file that contains the files to check')

# Parse the arguments
args = parser.parse_args()

# Helper function to parse the model path(s)
def parse_model_paths(models):
    if ',' in models:
        return models.split(',')
    else:
        return [models]

# Access the argument values
model_nicknames = parse_model_paths(args.model_nicknames)
models_dir = args.models_dir
macro_path = args.macro_path
json_fpath = args.json_fpath


if not os.path.exists(models_dir) or not os.path.isdir(models_dir):
    os.makedirs(models_dir)

    
br = BioimageioRepo.connect()
models_full_path = []
for model in model_nicknames:
    print("Downloading the Bioimage.io model: " + model.strip())
    model_dir = br.downloadModelByID(model.strip(), models_dir)
    models_full_path.append(os.path.abspath(model_dir))

## Create macros

expected_files = []
with open(macro_path, "a") as file:

    for mfp in models_full_path:
        descriptor = ModelDescriptorFactory.readFromLocalFile(os.path.join(mfp, Constants.RDF_FNAME))
        if not is_model_supported_on_os(descriptor):
            print("Model skipped: " + mfp)
            continue
        sample_name = descriptor.getInputTensors().get(0).getSampleTensorName()
        if sample_name is None:
            test_name = descriptor.getInputTensors().get(0).getTestTensorName()
            if test_name is None:
                continue
            convert_npy_to_tif(mfp, test_name, descriptor.getInputTensors().get(0).getAxesOrder())
            sample_name = CREATED_SAMPLE_NAME
        if " " in mfp:
            macro = MACRO_STR.format(model_path="[" + mfp + "]", input_path="[" + os.path.join(mfp, sample_name) + "]", output_folder="[" + mfp + "]")
        else:
            macro = MACRO_STR.format(model_path=mfp, input_path=os.path.join(mfp, sample_name), output_folder=mfp)
        file.write(macro + os.linesep)

        name_without_extension = sample_name[:sample_name.rfind(".")]
        for out in descriptor.getOutputTensors():
            out_dic = {}
            out_dic['path'] = os.path.join(mfp, name_without_extension + "_" + out.getName() + ".tif")
            out_dic['min_size'] = 1
            expected_files.append(out_dic)

with open(json_fpath, 'w') as f:
    json.dump(expected_files, f)