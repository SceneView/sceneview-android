cd ../assets/materials;
for FILE in *.mat; 
do 
    matc --optimize-size --platform=mobile -o "${FILE%%.*}.filamat" "$FILE"; 
done