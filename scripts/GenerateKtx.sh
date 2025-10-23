cd ../sceneview/src/main/environments/;
for FILE in *.hdr;
do
    cmgen --deploy ./output --format=ktx --size=256 --extract-blur=0.1 "$FILE"
done
