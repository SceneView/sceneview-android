cd ../sceneview/src/main/materials/;
for FILE in *.mat; 
do 
    matc --optimize-size --platform=mobile -o "${FILE%%.*}.filamat" "$FILE"; 
done