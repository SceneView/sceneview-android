# Marketing assets

Content for positioning SceneView as the **#1 cross-platform 3D & AR SDK** for Android, iOS, macOS, and visionOS.

---

## Showcase & positioning

| File | What it is | Audience |
|---|---|---|
| [showcase.md](showcase.md) | Full capability showcase — why SceneView is #1 | Developers evaluating 3D/AR options |
| [comparison.md](comparison.md) | Honest comparison vs. Sceneform, Unity, raw ARCore, RealityKit, etc. | Developers who haven't switched yet |
| [PLATFORM_STRATEGY.md](PLATFORM_STRATEGY.md) | Cross-platform architecture — Android + Apple, native renderers, KMP core | Technical decision-makers |
| [v4-preview.md](v4-preview.md) | v4.0 vision — multi-scene, portals, XR, deeper KMP | Existing users + forward-looking devs |

## Social & content

| File | What it is | Where to publish |
|---|---|---|
| [x-thread.md](x-thread.md) | 14-tweet thread template — cross-platform angle | Twitter/X, Bluesky, Mastodon |
| [linkedin-post.md](linkedin-post.md) | Cross-platform announcement + hashtags | LinkedIn |
| [linkedin-video-storyboard.md](linkedin-video-storyboard.md) | 60s video — shot-by-shot production guide | LinkedIn (film → upload) |
| [devto-article.md](devto-article.md) | Updated article for cross-platform v3.3.0 | Dev.to, Hashnode |
| [medium-article.md](medium-article.md) | Cross-platform launch article | Medium |
| [youtube-script.md](youtube-script.md) | 10-min video script + code + b-roll notes | YouTube |

## Assets & production

| File | What it is |
|---|---|
| [assets-catalog.md](assets-catalog.md) | Curated catalog of free glTF/USDZ models, HDR environments, and recommended combos for marketing screenshots |
| [github-profile.md](github-profile.md) | GitHub repo description, topics, org profile README, social preview image specs, release template, badges |

## Tutorials

| File | Where to publish |
|---|---|
| [codelabs/codelab-3d-compose.md](codelabs/codelab-3d-compose.md) | Docs site, Google CodeLabs |
| [codelabs/codelab-ar-compose.md](codelabs/codelab-ar-compose.md) | Docs site, Google CodeLabs |
| [codelab-ar-tap-to-place.md](codelab-ar-tap-to-place.md) | Docs site — includes iOS (SwiftUI) version |

---

## Publishing checklist

### Dev.to article
- [ ] Create Dev.to account or log in
- [ ] Paste content from `devto-article.md` (front matter included)
- [ ] Add cover image: 1000x420 screenshot of SceneView render (use assets from `assets-catalog.md`)
- [ ] Publish and cross-post to Hashnode

### X/Twitter thread
- [ ] Post tweet 1 from `x-thread.md`
- [ ] Reply-chain tweets 2–14
- [ ] Pin tweet 1 to profile for the week
- [ ] Cross-post to Bluesky and Mastodon

### LinkedIn video
- [ ] Download assets: ChronographWatch, MaterialsVariantsShoe, Kenney Furniture Kit
- [ ] Record 3D renders (right side), then AI code gen (left side)
- [ ] Show both Android (Compose) and iOS (SwiftUI) side by side
- [ ] Edit: split screen, hard cuts, white flash transitions
- [ ] Export 9:16, H.264, ~50 MB max
- [ ] Post with caption from `linkedin-post.md`

### GitHub
- [ ] Update repo description (see `github-profile.md`)
- [ ] Add topic tags (include `ios`, `swift`, `swiftui`, `realitykit`, `arkit`, `visionos`)
- [ ] Upload social preview image (1280x640)
- [ ] Update org profile README if exists

### Medium article
- [ ] Paste content from `medium-article.md`
- [ ] Add 3 images: render, code side-by-side, product viewer
- [ ] Tags: `android`, `ios`, `jetpack-compose`, `swiftui`, `3d`
- [ ] Submit to `Better Programming` or `Android Weekly` publication

### iOS distribution
- [ ] Verify Swift Package Manager installation works (`from: "3.3.0"`)
- [ ] Test SceneViewSwift on iOS 17+, macOS 14+, visionOS 1+
- [ ] Submit iOS sample app to TestFlight (when ready)
- [ ] Cross-post iOS-specific content to iOS Dev Weekly, Swift Weekly Brief

### Website
- [ ] Merge docs branch to trigger deployment
- [ ] Verify sceneview.github.io shows updated MkDocs site with iOS docs
- [ ] Check all nav links work
- [ ] Verify iOS quickstart page is accessible
