<!-- category: Fixed -->
- **Demo app: the 3D Scan floor plan shared as DXF in feet and inches gets its CAD dimensions in feet and inches ([#4799](https://github.com/sceneview/sceneview/pull/4799)).** The drawing in inches now declares `$DIMLUNIT` 4 (architectural), so a CAD tool dimensions and measures it as `13'-1 1/2"`, not `157.48` decimal inches. The metric drawing is unchanged.
