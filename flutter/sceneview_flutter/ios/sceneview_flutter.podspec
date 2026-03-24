Pod::Spec.new do |s|
  s.name             = 'sceneview_flutter'
  s.version          = '0.1.0'
  s.summary          = 'Flutter plugin for SceneView 3D and AR.'
  s.description      = <<-DESC
  Flutter plugin bridging to SceneViewSwift (RealityKit) for 3D and AR scenes on iOS.
                       DESC
  s.homepage         = 'https://github.com/SceneView/sceneview-android'
  s.license          = { :type => 'Apache-2.0' }
  s.author           = { 'SceneView' => 'contact@sceneview.github.io' }
  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform         = :ios, '17.0'
  s.swift_version    = '5.9'

  # TODO: Add SceneViewSwift SPM dependency
  # s.dependency 'SceneViewSwift', '~> 3.3.0'
end
