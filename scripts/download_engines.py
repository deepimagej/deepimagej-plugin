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
Jython script that downloads the basic engines needed to run most of the Deep Learning models
"""

from io.bioimage.modelrunner.engine.installation import EngineInstall

import os
import argparse
import sys

print(f"All arguments (including script name): {sys.argv}")

# Create the argument parser
parser = argparse.ArgumentParser()

# Add the arguments
parser.add_argument('-engines_path', type=str, default="engines", required=False,
                    help='Path where the engines are going to be installed')


# Parse the arguments
args = parser.parse_args()

engines_path = args.engines_path


if not os.path.exists(engines_path) or not os.path.isdir(engines_path):
    os.makedirs(engines_path)

installer = EngineInstall.createInstaller(engines_path)
installer.basicEngineInstallation()