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
Jython script that tests Cellpose on 3 channel images
"""

from io.bioimage.modelrunner.model.special.stardist import Stardist2D
import Cellpose_DeepImageJ

from ij import IJ
from net.imglib2.img.display.imagej import ImageJFunctions

import os


MODEL_NAME = "StarDist H&E Nuclei Segmentation"


model_path = Stardist2D.donwloadPretrained(MODEL_NAME, os.getcwd()) 
im_path = os.path.join(model_path, "sample_input_0.tif")
imp = IJ.openImage(im_path)
rai = ImageJFunctions.wrap(imp)


output_rai = Cellpose_DeepImageJ.runStarDist("cyto3", rai, "red", "blue")

max = 0
cursor = output_rai.cursor()
while cursor.hasNext():
	cursor.next()
	val = cursor.get().getRealDouble()
	if val > max:
		max = val

print("Number of instances counted by Cellpose: " + str(max))