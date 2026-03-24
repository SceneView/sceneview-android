// Objective-C bridge — required by React Native to register view managers.

#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(RNSceneViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(environment, NSString)
RCT_EXPORT_VIEW_PROPERTY(modelNodes, NSArray)
RCT_EXPORT_VIEW_PROPERTY(cameraOrbit, BOOL)
RCT_EXPORT_VIEW_PROPERTY(onTap, RCTDirectEventBlock)

@end

@interface RCT_EXTERN_MODULE(RNARSceneViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(environment, NSString)
RCT_EXPORT_VIEW_PROPERTY(modelNodes, NSArray)
RCT_EXPORT_VIEW_PROPERTY(planeDetection, BOOL)
RCT_EXPORT_VIEW_PROPERTY(depthOcclusion, BOOL)
RCT_EXPORT_VIEW_PROPERTY(instantPlacement, BOOL)
RCT_EXPORT_VIEW_PROPERTY(onTap, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPlaneDetected, RCTDirectEventBlock)

@end
