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
Python script that checks that the wanted objects have actually been created
"""

import os

from io.bioimage.modelrunner.numpy import DecodeNumpy
from net.imglib2.img.display.imagej import ImageJFunctions
from ij import IJ

print("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")

for ff in os.listdir("outputs"):
    print(ff)
    rai = ImageJFunctions.wrap(IJ.openImage("outputs/" + ff))
    DecodeNumpy.saveNpy("outputs_npy/" + ff, rai)

