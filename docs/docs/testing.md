# Testing SceneView Apps

Strategies for testing 3D and AR features in your Android app.

---

## Unit testing node logic

Business logic that drives your scene — model selection, animation state, anchor management — can be tested with standard JUnit tests. Keep scene logic in ViewModels or plain Kotlin classes.

```kotlin
// ViewModel
class SceneViewModel : ViewModel() {
    var selectedModel by mutableStateOf("helmet")
        private set

    var isAnimating by mutableStateOf(true)
        private set

    fun selectModel(name: String) { selectedModel = name }
    fun toggleAnimation() { isAnimating = !isAnimating }
}

// Test
@Test
fun `selecting a model updates state`() {
    val vm = SceneViewModel()
    vm.selectModel("sword")
    assertEquals("sword", vm.selectedModel)
}
```

!!! tip "Keep Filament out of unit tests"
    Filament requires native libraries and a GPU context. Don't instantiate `Engine`, `ModelLoader`, or any Filament objects in unit tests. Test the state logic, not the rendering.

---

## Compose UI testing

Use `composeTestRule` to test the Compose UI around your scene — buttons, sliders, model pickers. You can't render the actual 3D scene in instrumented tests (no GPU in CI), but you can verify that state changes propagate.

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun modelPickerUpdatesSelection() {
    val vm = SceneViewModel()
    composeTestRule.setContent {
        ModelPickerUI(viewModel = vm)
    }

    composeTestRule.onNodeWithText("Sword").performClick()
    assertEquals("sword", vm.selectedModel)
}
```

---

## Screenshot testing

For visual regression testing, use [Paparazzi](https://github.com/cashapp/paparazzi) or [Roborazzi](https://github.com/takahirom/roborazzi). These render Compose UI without a device but **cannot render Filament 3D content** (they use layoutlib, not a real GPU).

**What you can screenshot-test:**

- Compose UI overlays (buttons, HUD, model pickers)
- Loading states (skeleton/placeholder before model loads)
- Error states

**What you cannot screenshot-test:**

- The 3D scene itself
- AR camera feed
- Filament rendering output

For 3D visual testing, use on-device screenshot tests with [Shot](https://github.com/pedrovgs/Shot) or manual QA.

---

## Instrumented testing

For on-device tests that exercise the full rendering pipeline:

```kotlin
@get:Rule
val composeTestRule = createAndroidComposeRule<MainActivity>()

@Test
fun sceneRendersWithoutCrash() {
    // Just verify the Scene composable doesn't throw
    composeTestRule.waitForIdle()
    // If we get here, the scene initialized successfully
}

@Test
fun tappingPlacesModel() {
    // Wait for scene to initialize
    composeTestRule.waitForIdle()

    // Simulate a tap
    composeTestRule.onRoot().performClick()

    // Verify state changed (model placed)
    // Check via ViewModel or test tag
}
```

!!! warning "AR tests require a physical device"
    AR features need a real camera and ARCore. Run AR instrumented tests on physical devices only — not emulators.

---

## CI pipeline

### What to run in CI

| Check | CI-safe? | Tool |
|---|---|---|
| Unit tests (state logic) | Yes | JUnit |
| Compose UI tests | Yes | `composeTestRule` |
| Lint / ktlint | Yes | `./gradlew lint` |
| Build verification | Yes | `./gradlew assembleDebug` |
| Screenshot tests (UI only) | Yes | Paparazzi/Roborazzi |
| 3D rendering tests | No (needs GPU) | On-device only |
| AR tests | No (needs camera) | Physical device only |

### Sample CI config

```yaml
# .github/workflows/test.yml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: ./gradlew test              # Unit tests
      - run: ./gradlew lintDebug         # Lint
      - run: ./gradlew assembleDebug     # Build verification
```

---

## Testing patterns

### Mock the model loader

For tests that need a `ModelInstance`, create a fake:

```kotlin
// In test
val fakeInstance = mockk<ModelInstance>(relaxed = true)

// Pass to composable under test
ModelNode(modelInstance = fakeInstance, scaleToUnits = 1.0f)
```

### Test animation state transitions

```kotlin
@Test
fun `animation toggles between walk and idle`() {
    val vm = SceneViewModel()

    assertEquals("Idle", vm.currentAnimation)

    vm.startWalking()
    assertEquals("Walk", vm.currentAnimation)

    vm.stopWalking()
    assertEquals("Idle", vm.currentAnimation)
}
```

### Test AR anchor lifecycle

```kotlin
@Test
fun `clearing anchor removes placed model`() {
    val vm = ARViewModel()

    vm.placeAnchor(mockAnchor)
    assertTrue(vm.hasPlacedModel)

    vm.clearAnchor()
    assertFalse(vm.hasPlacedModel)
    assertNull(vm.currentAnchor)
}
```
