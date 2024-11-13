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

import json
import sys
import os
import argparse

def main(expected_files_path):
    with open(expected_files_path, 'r') as f:
        expected_files = json.load(f)

    all_passed = True
    for item in expected_files:
        file_path = item['path']
        min_size = item.get('min_size', 1)
        if not os.path.isfile(file_path):
            print("Error: Expected file '{file_path}' not found.".format(file_path=file_path))
            all_passed = False
            continue
        actual_size = os.path.getsize(file_path)
        if actual_size < min_size:
            print("Error: File '{file_path}' size {actual_size} bytes is less than expected {min_size} bytes.".format(file_path=file_path, 
                                                                                                                      actual_size=actual_size, min_size=min_size))
            all_passed = False
        else:
            print("File '{file_path}' exists with size {actual_size} bytes.".format(file_path=file_path, actual_size=actual_size))

    if not all_passed:
        sys.exit(1)

if __name__ == '__main__':
    # Create the argument parser
    parser = argparse.ArgumentParser()

    # Add the arguments
    parser.add_argument('-json_fpath', type=str, required=True,
                        help='Path to the json file that contains the files to check')


    # Parse the arguments
    args = parser.parse_args()

    json_fpath = args.json_fpath
    main(json_fpath)
