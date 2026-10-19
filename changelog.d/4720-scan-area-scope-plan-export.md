<!-- category: Fixed -->
- **3D Scan (demo)**: the export sheet only offers the floor-plan PDF when there is a plan. A room scanned without depth shows the row greyed out, with one line saying why. The plan writes the scan's date once, and every screen uses the same date format (#4714, #4720).
- **3D Scan (demo)**: the Dollhouse chip says "Scale 1:40" instead of a bare "1:40", which read as a duration. A room placed at real size now rests on the detected floor instead of the table it was placed on. A new "An outdoor area" scope covers gardens, yards and terraces, with a ground map and no floor plan (#4720).
- **ARSceneView**: `AutoPlacementState.floorHeight` gives the lowest tracked upward-facing plane, so content can stand on the floor (#4720).
