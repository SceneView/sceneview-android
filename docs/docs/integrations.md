# Integrations

How to use SceneView with the rest of your Android app stack.

---

## Jetpack Compose Navigation

Use SceneView inside navigation destinations. The scene is created when you navigate to it and destroyed when you leave — no manual cleanup.

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onViewProduct = { id ->
                navController.navigate("product/$id")
            })
        }
        composable("product/{id}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("id") ?: return@composable
            ProductViewerScreen(productId)
        }
        composable("ar-preview") {
            ARPreviewScreen()
        }
    }
}

@Composable
fun ProductViewerScreen(productId: String) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, "models/$productId.glb")

    Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator()
    ) {
        model?.let { ModelNode(modelInstance = it, scaleToUnits = 1.0f) }
    }
}
```

!!! tip "Engine lifecycle"
    Each `rememberEngine()` call creates a new Filament engine. If you navigate between multiple 3D screens frequently, consider sharing the engine via a ViewModel or CompositionLocal to avoid repeated initialization.

### Shared engine across destinations

```kotlin
// In your Application or top-level composable
val LocalEngine = staticCompositionLocalOf<Engine> { error("No engine") }

@Composable
fun App() {
    val engine = rememberEngine()

    CompositionLocalProvider(LocalEngine provides engine) {
        AppNavigation()
    }
}

// In any destination
@Composable
fun ProductViewer() {
    val engine = LocalEngine.current
    val modelLoader = rememberModelLoader(engine)
    // ...
}
```

---

## Material 3 / Material Design

SceneView renders inside a standard Compose layout. Wrap it with Material 3 components freely.

### 3D viewer in a Material 3 card

```kotlin
@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // 3D viewer as the card hero
            Scene(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                cameraManipulator = rememberCameraManipulator()
            ) {
                rememberModelInstance(modelLoader, product.modelPath)?.let {
                    ModelNode(modelInstance = it, scaleToUnits = 1.0f)
                }
            }

            // Standard Material 3 content below
            Column(modifier = Modifier.padding(16.dp)) {
                Text(product.name, style = MaterialTheme.typography.headlineSmall)
                Text(product.price, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { /* add to cart */ }) {
                    Text("Add to Cart")
                }
            }
        }
    }
}
```

### Bottom sheet with AR

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARWithBottomSheet() {
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            planeRenderer = true
        ) {
            // AR content
        }

        // Floating action button
        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, "Settings")
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            // Model picker, settings, etc.
            ModelPickerContent()
        }
    }
}
```

---

## ViewModel integration

Keep scene state in a ViewModel so it survives configuration changes.

```kotlin
class SceneViewModel : ViewModel() {
    var selectedModel by mutableStateOf("helmet")
        private set

    var isAnimating by mutableStateOf(true)
        private set

    var lightIntensity by mutableFloatStateOf(100_000f)
        private set

    fun selectModel(name: String) { selectedModel = name }
    fun toggleAnimation() { isAnimating = !isAnimating }
    fun setLight(intensity: Float) { lightIntensity = intensity }
}

@Composable
fun SceneScreen(viewModel: SceneViewModel = viewModel()) {
    val model = rememberModelInstance(modelLoader, "models/${viewModel.selectedModel}.glb")

    Scene(modifier = Modifier.fillMaxSize()) {
        model?.let {
            ModelNode(
                modelInstance = it,
                scaleToUnits = 1.0f,
                autoAnimate = viewModel.isAnimating
            )
        }
        LightNode(
            type = LightManager.Type.SUN,
            apply = { intensity(viewModel.lightIntensity) }
        )
    }
}
```

---

## Hilt / dependency injection

Inject model paths, environment configurations, or feature flags.

```kotlin
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {
    val product = productRepository.getProduct(productId)
    val modelUrl get() = product.value?.modelUrl
}

@Composable
fun ProductScreen(viewModel: ProductViewModel = hiltViewModel()) {
    val product by viewModel.product.collectAsStateWithLifecycle()

    product?.modelUrl?.let { url ->
        Scene(modifier = Modifier.fillMaxSize()) {
            rememberModelInstance(modelLoader, url)?.let {
                ModelNode(modelInstance = it, scaleToUnits = 1.0f)
            }
        }
    }
}
```

---

## Room / local database

Store anchor data for persistent AR experiences.

```kotlin
@Entity
data class SavedAnchor(
    @PrimaryKey val id: String,
    val cloudAnchorId: String,
    val label: String,
    val timestamp: Long
)

@Dao
interface AnchorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(anchor: SavedAnchor)

    @Query("SELECT * FROM SavedAnchor ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SavedAnchor>>
}

// In your AR composable
ARScene(...) {
    CloudAnchorNode(
        anchor = localAnchor,
        onHosted = { cloudId, state ->
            if (state == CloudAnchorState.SUCCESS && cloudId != null) {
                scope.launch {
                    anchorDao.save(SavedAnchor(
                        id = UUID.randomUUID().toString(),
                        cloudAnchorId = cloudId,
                        label = "My anchor",
                        timestamp = System.currentTimeMillis()
                    ))
                }
            }
        }
    ) {
        ModelNode(modelInstance = model!!)
    }
}
```
