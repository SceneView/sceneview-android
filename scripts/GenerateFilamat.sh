#!/usr/bin/env bash
# Generate .filamat files from .mat material sources
# Usage: cd scripts && bash GenerateFilamat.sh

cd ../assets/materials;
for FILE in *.mat; 
do 
    matc --optimize-size --platform=mobile -o "${FILE%%.*}.filamat" "$FILE"; 
done