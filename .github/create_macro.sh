#!/bin/bash

# Check if models are provided
if [ -z "$1" ]; then
  echo "No models specified. Exiting."
  exit 1
fi

IFS=',' read -ra MODEL_LIST <<< "$1"
mkdir -p macros

for MODEL_NAME in "${MODEL_LIST[@]}"; do
  MODEL_PATH="./models/$MODEL_NAME"
  INPUT_PATH="$MODEL_PATH/sample_input.tif"
  OUTPUT_PATH="$MODEL_PATH/output/"
  
  # Format the macro string using printf
  MACRO=$(printf 'run("DeepImageJ Run", "modelPath=%s inputPath=%s outputFolder=%s displayOutput=false");' "$MODEL_PATH" "$INPUT_PATH" "$OUTPUT_PATH")
  
  # Write the macro to a file
  echo "$MACRO" > macros/${MODEL_NAME}_macro.ijm
  echo "Created macro for model: $MODEL_NAME"
done
