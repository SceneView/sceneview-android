package io.github.sceneview.demo.demos

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.demo.DemoSettings
import io.github.sceneview.demo.DockItem
import io.github.sceneview.demo.R
import io.github.sceneview.demo.ai.scene.AgentState
import io.github.sceneview.demo.ai.scene.AiContent
import io.github.sceneview.demo.ai.scene.AiEvent
import io.github.sceneview.demo.ai.scene.AiMessage
import io.github.sceneview.demo.ai.scene.AiProvider
import io.github.sceneview.demo.ai.scene.AiTurn
import io.github.sceneview.demo.ai.scene.ApiKeyStore
import io.github.sceneview.demo.ai.scene.ChatLine
import io.github.sceneview.demo.ai.scene.ClaudeAiProvider
import io.github.sceneview.demo.ai.scene.RecordedAiProvider
import io.github.sceneview.demo.ai.scene.RoomModel
import io.github.sceneview.demo.ai.scene.SceneAgent
import io.github.sceneview.demo.ai.scene.SceneToolExecutor
import io.github.sceneview.demo.ai.scene.SpatialSession
import io.github.sceneview.demo.ai.scene.Takeoff
import io.github.sceneview.demo.common.putVoiceSilenceExtras
import io.github.sceneview.demo.common.qaStateOverridesAllowed
import io.github.sceneview.demo.demos.internal.ArDebugFraming
import io.github.sceneview.demo.demos.internal.ArDebugOrbitCamera
import io.github.sceneview.demo.demos.internal.ArDebugSession
import io.github.sceneview.demo.demos.internal.CameraRig
import io.github.sceneview.demo.demos.internal.DebugGroup
import io.github.sceneview.demo.theme.LocalStageChrome
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.demo.theme.SceneViewTokens.ArOverlay
import io.github.sceneview.demo.theme.SceneViewTokens.Space
import io.github.sceneview.demo.theme.StageChrome
import io.github.sceneview.demo.ui.GlassSurface
import io.github.sceneview.demo.ui.overMediaEdge
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.compose.runtime.DisposableEffect

/**
 * AI Scene (prototype, debug builds only): an AI rebuilds a room from a real AR recording, then
 * furnishes it.
 *
 * The stage is the bundled Rerun session — ARCore's planes, the feature-point cloud, the camera's
 * path and keyframes. The model reads it only through tools ([SceneToolExecutor]): it casts rays
 * through keyframes, proposes walls, openings and furniture that the points must confirm, then
 * places catalogue items in floor areas the code found free. It chooses; the code computes every
 * length, area and clearance. Each request is one undo step.
 *
 * Two providers: the recorded demo (default), which replays a real conversation whose tool calls
 * run live against the same data, and Claude with the user's own key, kept on the phone by the
 * Android Keystore ([ApiKeyStore]).
 */
@Composable
fun AiSceneDemo(onBack: () -> Unit) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    LaunchedEffect(engine, materialLoader) { warmUpReplay(engine, materialLoader) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val qaState = remember { DemoSettings.qaDemoState?.takeIf { qaStateOverridesAllowed() } }

    val media by produceState<RerunReplayMedia?>(null) {
        value = runCatching { loadRerunReplay(context) }.getOrNull()
    }
    val spatial by produceState<SpatialSession?>(null, media) {
        val m = media ?: return@produceState
        value = withContext(Dispatchers.Default) { SpatialSession.of(m.trace, m.manifest.lens, m.manifest.floorY) }
    }
    val agent = remember(spatial) {
        val s = spatial ?: return@remember null
        val m = media ?: return@remember null
        SceneAgent(
            executor = SceneToolExecutor(s),
            keyframes = { withContext(Dispatchers.Default) { keyframesFor(m, s) } },
            toolPaceMillis = if (qaState == null) TOOL_PACE_MS else QA_TOOL_PACE_MS,
        )
    }
    val stateFlow = remember(agent) { agent?.state ?: MutableStateFlow(AgentState()) }
    val state by stateFlow.collectAsState()

    // Providers: the recorded demo, or Claude with the key the user saved on this phone.
    val keyStore = remember { ApiKeyStore(context.applicationContext) }
    var keyHint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(keyStore) { keyHint = withContext(Dispatchers.Default) { keyStore.hint } }
    var useClaude by rememberSaveable { mutableStateOf(false) }
    val recorded = remember { RecordedAiProvider() }
    val claude = remember(keyStore) { ClaudeAiProvider(key = { keyStore.load() }) }
    val provider: AiProvider = if (useClaude) claude else recorded

    // The stage: the whole recorded session, without the anchors (the rebuilt room says more).
    val session = remember {
        ArDebugSession().apply { toggle(DebugGroup.Anchors) }
    }
    val orbit = remember(media) {
        ArDebugOrbitCamera(
            initialPose = ArDebugFraming.DEFAULT_POSE.copy(azimuthDegrees = STAGE_AZIMUTH),
            drift = qaState == null,
            lift = STAGE_LIFT,
        )
    }
    LaunchedEffect(media) {
        val m = media ?: return@LaunchedEffect
        session.trace = m.trace
        session.scrubTo(m.trace.duration)
    }
    var revealed by remember(media) { mutableStateOf(false) }
    val readyState = rememberUpdatedState(media != null && revealed)
    val hudIn = rememberReveal(revealed, delayMillis = REVEAL_STAGGER_MS)
    val chatIn = rememberReveal(revealed, delayMillis = REVEAL_STAGGER_MS * 2)
    var cloudShown by remember { mutableStateOf(true) }
    var modelShown by remember { mutableStateOf(true) }

    // The looks (rays, probes, tried walls) stay while the model works, then fade out.
    var looksShown by remember { mutableStateOf(false) }
    LaunchedEffect(state.busy) {
        if (state.busy) {
            looksShown = true
        } else {
            delay(LOOKS_LINGER_MS)
            looksShown = false
        }
    }
    val lookFade by animateFloatAsState(
        targetValue = if (looksShown) 1f else 0f,
        animationSpec = tween(SceneViewTokens.Duration.longMillis),
        label = "ai-scene-looks",
    )

    // QA (`--ez qa_mode true --es qa_state <state>`): the recorded flow, stopped at a step.
    LaunchedEffect(agent, revealed) {
        val a = agent ?: return@LaunchedEffect
        if (!revealed) return@LaunchedEffect
        when (qaState) {
            QA_LOOKING -> a.ask(HeldProvider(recorded, holdAt = 1), RecordedAiProvider.REBUILD_PROMPT)
            QA_BUILDING -> a.ask(HeldProvider(recorded, holdAt = 2), RecordedAiProvider.REBUILD_PROMPT)
            QA_REBUILT -> a.ask(recorded, RecordedAiProvider.REBUILD_PROMPT)
            QA_CORNER -> {
                a.ask(recorded, RecordedAiProvider.REBUILD_PROMPT)
                a.ask(recorded, RecordedAiProvider.READING_CORNER_PROMPT)
            }
        }
    }

    fun ask(prompt: String) {
        val a = agent ?: return
        scope.launch { a.ask(provider, prompt) }
    }

    val nextPrompt = if (state.model.walls.isEmpty()) RecordedAiProvider.REBUILD_PROMPT
        else RecordedAiProvider.READING_CORNER_PROMPT
    DemoScaffold(
        title = stringResource(R.string.demo_ai_scene_title),
        onBack = onBack,
        controls = {
            AiSceneSheet(
                useClaude = useClaude,
                onUseClaude = { useClaude = it },
                keyHint = keyHint,
                onSaveKey = { key ->
                    scope.launch {
                        keyHint = withContext(Dispatchers.Default) {
                            keyStore.save(key)
                            keyStore.hint
                        }
                    }
                },
                onClearKey = {
                    scope.launch {
                        withContext(Dispatchers.Default) { keyStore.clear() }
                        keyHint = null
                    }
                },
            )
        },
        firstFrameRendered = readyState,
        loadingLabel = RERUN_REPLAY_LOADING,
        themedStage = true,
        topOverlay = {
            if (media != null) {
                StatusCard(
                    state = state,
                    useClaude = useClaude,
                    spatial = spatial,
                    modifier = Modifier
                        .padding(horizontal = Space.md)
                        .widthIn(max = ArOverlay.maxWidth)
                        .fillMaxWidth()
                        .reveal(hudIn, rise = -Space.md),
                )
            }
        },
        bottomOverlay = {
            if (media != null) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = Space.md)
                        .widthIn(max = ArOverlay.maxWidth)
                        .fillMaxWidth()
                        .reveal(chatIn, rise = Space.lg),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    ChatCard(state = state)
                    PromptField(enabled = agent != null && !state.busy, onSend = ::ask)
                }
            }
        },
        dock = listOf(
            DockItem(
                icon = Icons.Rounded.Grain,
                label = "Show the scan",
                caption = "Scan",
                onClick = {
                    cloudShown = !cloudShown
                    SCAN_LAYERS.filter { session.isVisible(it) != cloudShown }.forEach(session::toggle)
                },
                selected = cloudShown,
            ),
            DockItem(
                icon = Icons.Rounded.HomeWork,
                label = "Show the rebuilt room",
                caption = "Room",
                onClick = { modelShown = !modelShown },
                selected = modelShown,
            ),
            DockItem(
                icon = Icons.AutoMirrored.Rounded.Undo,
                label = "Undo the last request",
                caption = "Undo",
                onClick = { agent?.undo() },
                enabled = state.canUndo && !state.busy,
            ),
            DockItem(
                icon = Icons.Rounded.RestartAlt,
                label = "Start over",
                caption = "Reset",
                onClick = { agent?.reset() },
                enabled = !state.busy && (state.lines.isNotEmpty() || !state.model.isEmpty),
            ),
        ),
        dockAccent = DockItem(
            icon = if (nextPrompt == RecordedAiProvider.REBUILD_PROMPT) Icons.Rounded.AutoAwesome
                else Icons.Rounded.Chair,
            label = nextPrompt,
            caption = if (nextPrompt == RecordedAiProvider.REBUILD_PROMPT) "Rebuild" else "Furnish",
            onClick = { ask(nextPrompt) },
            enabled = agent != null && !state.busy,
        ),
    ) {
        val m = media
        if (m != null) {
            val chrome = LocalStageChrome.current
            // Created before the SceneView so they are released after it (Compose forgets in reverse).
            val inks = remember(materialLoader, chrome) { RoomInks.of(
                materialLoader,
                chrome.debug,
                ArOverlay.accentRecord,
            ) }
            DisposableEffect(inks) {
                onDispose { inks.all.forEach { materialLoader.destroyMaterialInstance(it) } }
            }
            val label = remember(chrome) { LabelInk(chrome.onCard.toArgb(), chrome.card.toArgb()) }
            val floorY = spatial?.floorY ?: 0f
            ArDebugSceneView(
                session = session,
                orbit = orbit,
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                modifier = Modifier.fillMaxSize(),
                replay = m,
                onShown = { revealed = true },
                roomMeasure = false,
            ) {
                AiRoomLayer(
                    model = state.model,
                    looks = state.looks,
                    lookFade = lookFade,
                    showModel = modelShown,
                    floorY = floorY,
                    inks = inks,
                    label = label,
                    eye = { CameraRig.eye(orbit.pose) },
                )
            }
        }
    }
}

/** The card under the header: which AI, what scan it reads, what it has built so far. */
@Composable
private fun StatusCard(state: AgentState, useClaude: Boolean, spatial: SpatialSession?, modifier: Modifier) {
    val chrome = LocalStageChrome.current
    val shape = RoundedCornerShape(SceneViewTokens.Radius.lg)
    val muted = SceneViewTokens.Type.caption.copy(color = chrome.onCardMuted, fontFeatureSettings = "tnum")
    Column(
        modifier = modifier
            .shadow(SceneViewTokens.Elevation.lg, shape, clip = false)
            .clip(shape)
            .background(chrome.card, shape)
            .overMediaEdge(shape, chrome.edgeRing, chrome.edgeHalo)
            .padding(horizontal = Space.md, vertical = Space.sm + Space.xs)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(busy = state.busy, chrome = chrome)
            Spacer(Modifier.width(Space.sm))
            Text(
                text = if (useClaude) "Claude · your key" else "Recorded demo",
                style = SceneViewTokens.Type.caption.copy(color = chrome.onCard, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .background(chrome.track, RoundedCornerShape(SceneViewTokens.Radius.full))
                    .padding(horizontal = Space.sm, vertical = Space.xs / 2),
            )
            Spacer(Modifier.weight(1f))
            if (spatial != null) {
                Text(
                    text = "${String.format(
                        Locale.US,
                        "%,d",
                        spatial.pointCount,
                    )} points · ${spatial.keyframes.size} keyframes",
                    style = muted,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = modelSummary(state.model),
            style = SceneViewTokens.Type.caption.copy(color = chrome.onCard),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (useClaude && state.inputTokens > 0) {
            Text(text = "${state.inputTokens} tokens in · ${state.outputTokens} out", style = muted)
        }
        if (state.takeoffShown && !state.model.isEmpty) {
            TakeoffSection(Takeoff.of(state.model), chrome)
        }
    }
}

@Composable
private fun StatusDot(busy: Boolean, chrome: StageChrome) {
    val pulse = rememberInfiniteTransition(label = "ai-scene-dot")
    val alpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (busy) DOT_PULSE_LOW else 1f,
        animationSpec = infiniteRepeatable(tween(SceneViewTokens.Duration.longMillis), RepeatMode.Reverse),
        label = "ai-scene-dot-alpha",
    )
    val color = if (busy) chrome.accent else ArOverlay.accentSuccess
    Box(Modifier.size(Space.sm).background(color.copy(alpha = color.alpha * alpha), CircleShape))
}

@Composable
private fun TakeoffSection(takeoff: Takeoff, chrome: StageChrome) {
    Spacer(Modifier.height(Space.xs))
    Text(
        text = "Takeoff",
        style = SceneViewTokens.Type.caption.copy(color = chrome.onCardMuted, fontWeight = FontWeight.SemiBold),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        Figure("Walls", "${SceneToolExecutor.fmt(takeoff.wallArea)} m²", chrome, Modifier.weight(1f))
        Figure("Openings", "${SceneToolExecutor.fmt(takeoff.openingArea)} m²", chrome, Modifier.weight(1f))
        Figure("Net wall", "${SceneToolExecutor.fmt(takeoff.netWallArea)} m²", chrome, Modifier.weight(1f))
    }
    takeoff.items.forEach { line ->
        Text(
            text = "${line.label} · ${line.detail}",
            style = SceneViewTokens.Type.caption.copy(color = chrome.onCard, fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Figure(label: String, value: String, chrome: StageChrome, modifier: Modifier) {
    Column(modifier) {
        Text(text = label, style = SceneViewTokens.Type.caption.copy(color = chrome.onCardMuted), maxLines = 1)
        Text(
            text = value,
            style = SceneViewTokens.Type.card.copy(color = chrome.onCard, fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}

/** The last lines of the conversation: what was asked, what the model said, each tool's verdict. */
@Composable
private fun ChatCard(state: AgentState) {
    val chrome = LocalStageChrome.current
    val shape = RoundedCornerShape(SceneViewTokens.Radius.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(SceneViewTokens.Elevation.lg, shape, clip = false)
            .clip(shape)
            .background(chrome.card, shape)
            .overMediaEdge(shape, chrome.edgeRing, chrome.edgeHalo)
            .padding(horizontal = Space.md, vertical = Space.sm + Space.xs)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        val lines = state.lines.takeLast(CHAT_LINES)
        if (lines.isEmpty()) {
            Text(
                text = "Ask the AI to rebuild this scanned room. It reads the real scan — planes, points, " +
                    "the camera's path — and the code computes every number.",
                style = SceneViewTokens.Type.caption.copy(color = chrome.onCardMuted),
            )
        }
        lines.forEach { line -> ChatRow(line, chrome) }
        if (state.busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(busy = true, chrome = chrome)
                Spacer(Modifier.width(Space.sm))
                Text(text = "Working…", style = SceneViewTokens.Type.caption.copy(color = chrome.onCardMuted))
            }
        }
    }
}

@Composable
private fun ChatRow(line: ChatLine, chrome: StageChrome) {
    val base = SceneViewTokens.Type.caption
    val (marker, style) = when (line.kind) {
        ChatLine.Kind.User -> null to base.copy(color = chrome.onCard, fontWeight = FontWeight.SemiBold)
        ChatLine.Kind.Assistant -> null to base.copy(color = chrome.onCard)
        ChatLine.Kind.ToolOk -> ArOverlay.accentSuccess to base.copy(
            color = chrome.onCardMuted,
            fontFeatureSettings = "tnum",
        )
        ChatLine.Kind.ToolRefused -> ArOverlay.accentRecord to base.copy(
            color = chrome.onCardMuted,
            fontFeatureSettings = "tnum",
        )
        ChatLine.Kind.Note -> null to base.copy(color = chrome.onCardMuted)
        ChatLine.Kind.Error -> ArOverlay.accentRecord to base.copy(color = chrome.onCard)
    }
    Row(verticalAlignment = Alignment.Top) {
        if (marker != null) {
            Box(Modifier.padding(top = Space.xs + Space.xs / 2).size(Space.xs + Space.xs / 2).background(
                marker,
                CircleShape,
            ))
            Spacer(Modifier.width(Space.sm))
        }
        Text(
            text = if (line.kind == ChatLine.Kind.User) "You · ${line.text}" else line.text,
            style = style,
            maxLines = if (line.kind == ChatLine.Kind.Assistant) ASSISTANT_MAX_LINES else 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A glass prompt field: type or say what to do; the dock's accent sends the next suggested step. */
@Composable
private fun PromptField(enabled: Boolean, onSend: (String) -> Unit) {
    val chrome = LocalStageChrome.current
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var text by remember { mutableStateOf("") }
    val speechAvailable = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(context.packageManager) != null
    }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text = it }
        }
    }
    fun send() {
        if (!enabled || text.isBlank()) return
        onSend(text.trim())
        text = ""
        focus.clearFocus()
    }
    GlassSurface(
        modifier = Modifier.fillMaxWidth().imePadding(),
        shape = RoundedCornerShape(SceneViewTokens.Radius.full),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = SceneViewTokens.Type.body.copy(color = chrome.onGlass),
                cursorBrush = SolidColor(chrome.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    if (text.isEmpty()) {
                        Text(
                            text = "Ask about this room…",
                            style = SceneViewTokens.Type.body,
                            color = chrome.onGlassMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    field()
                },
            )
            if (speechAvailable) {
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "What should the AI do?")
                            putVoiceSilenceExtras()
                        }
                        runCatching { speech.launch(intent) }
                    },
                    enabled = enabled,
                ) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Say it", tint = chrome.onGlass)
                }
            }
            IconButton(onClick = ::send, enabled = enabled && text.isNotBlank()) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send",
                    tint = if (enabled && text.isNotBlank()) chrome.onGlass else chrome.onGlassMuted,
                )
            }
        }
    }
}

/** The settings sheet: what the demo does, which AI runs it, and the user's own Claude key. */
@Composable
private fun ColumnScope.AiSceneSheet(
    useClaude: Boolean,
    onUseClaude: (Boolean) -> Unit,
    keyHint: String?,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
) {
    val body = MaterialTheme.typography.bodyMedium
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = "A prototype. The AI reads a real AR recording only through tools — rays through the " +
            "keyframes, the points inside a box, the planes — and rebuilds the room: walls, windows, " +
            "furniture. A wall the points do not support is refused. Then it places real-size items " +
            "where the floor is free. It chooses; the code computes every dimension and quantity.",
        style = body,
        color = muted,
    )
    Spacer(Modifier.height(Space.md))
    Text(text = "AI", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        ProviderChoice("Recorded demo", selected = !useClaude) { onUseClaude(false) }
        ProviderChoice("Claude, your key", selected = useClaude) { onUseClaude(true) }
    }
    Text(
        text = if (useClaude) {
            "Live: each request goes to the Claude API with your key. It receives the scan's summary, " +
                "four keyframe photos with their camera positions, and the tool results."
        } else {
            "Replays a real conversation. Its tool calls run live against the recorded scan."
        },
        style = body,
        color = muted,
    )
    Spacer(Modifier.height(Space.md))
    Text(text = "Claude API key", style = MaterialTheme.typography.titleSmall)
    if (keyHint != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Saved key …$keyHint", style = body, modifier = Modifier.weight(1f))
            TextButton(onClick = onClearKey) { Text("Remove") }
        }
    } else {
        // Never saveable state: the key must not land in the saved instance state.
        var draft by remember { mutableStateOf("") }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            singleLine = true,
            label = { Text("Paste your key") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = {
                onSaveKey(draft)
                draft = ""
            },
            enabled = draft.isNotBlank(),
        ) { Text("Save key") }
    }
    Text(
        text = "Stays on this phone, encrypted with the Android Keystore. Never sent anywhere but the Claude API.",
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
}

@Composable
private fun ProviderChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    io.github.sceneview.demo.ui.GlassChip(label = label, selected = selected, toggle = false, onClick = onClick)
}

private fun modelSummary(model: RoomModel): String {
    if (model.isEmpty && model.rejections.isEmpty()) return "Raw scan · nothing rebuilt yet"
    val parts = buildList {
        add(count(model.walls.size, "wall"))
        val windows = model.openings.count { it.kind.wire == "window" }
        val doors = model.openings.size - windows
        if (windows > 0) add(count(windows, "window"))
        if (doors > 0) add(count(doors, "door"))
        add(count(model.objects.size, "object"))
        if (model.items.isNotEmpty()) add(count(model.items.size, "item") + " placed")
        if (model.rejections.isNotEmpty()) add("${model.rejections.size} refused")
    }
    return parts.joinToString(" · ")
}

private fun count(n: Int, noun: String) = if (n == 1) "1 $noun" else "$n ${noun}s"

/**
 * Four keyframes spread over the session, each a line saying where the camera stood and the photo
 * it took — what Claude is shown of the room besides the tools.
 */
private fun keyframesFor(media: RerunReplayMedia, spatial: SpatialSession): List<AiContent> {
    val frames = spatial.keyframes
    if (frames.isEmpty()) return emptyList()
    val picks = (0 until KEYFRAMES_SENT).map { frames[it * (frames.size - 1) / (KEYFRAMES_SENT - 1)] }.distinct()
    return picks.flatMap { k ->
        val bytes = media.bytesOf(k.path) ?: return@flatMap emptyList()
        val type = if (k.path.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"
        listOf(
            AiContent.Text(
                String.format(
                    Locale.US,
                    "Keyframe %d at %.1f s, camera at [%.2f, %.2f, %.2f] (x, height, z):",
                    k.index, k.time, k.pose.x, k.pose.y - spatial.floorY, k.pose.z,
                ),
            ),
            AiContent.Image(type, Base64.encodeToString(bytes, Base64.NO_WRAP)),
        )
    }
}

/**
 * QA only: [inner], held for good once [holdAt] assistant turns of the current request have run,
 * so a capture shows the model mid-work — its rays and probes still on screen.
 */
private class HeldProvider(private val inner: AiProvider, private val holdAt: Int) : AiProvider by inner {
    override fun run(turn: AiTurn): Flow<AiEvent> = flow {
        val asked = turn.messages.indexOfLast { m ->
            m.role == AiMessage.Role.User &&
                m.content.any { it is AiContent.Text && it.text.startsWith(RecordedAiProvider.PROMPT_MARK) }
        }
        val done = turn.messages.drop(asked + 1).count { it.role == AiMessage.Role.Assistant }
        if (done >= holdAt) awaitCancellation()
        emitAll(inner.run(turn))
    }
}

private val SCAN_LAYERS = listOf(DebugGroup.Points, DebugGroup.Planes, DebugGroup.Trail)

private const val QA_LOOKING = "looking"
private const val QA_BUILDING = "building"
private const val QA_REBUILT = "rebuilt"
private const val QA_CORNER = "corner"

private const val TOOL_PACE_MS = 600L
private const val QA_TOOL_PACE_MS = 250L
private const val LOOKS_LINGER_MS = 1500L
private const val KEYFRAMES_SENT = 4
private const val CHAT_LINES = 4
private const val ASSISTANT_MAX_LINES = 3
private const val DOT_PULSE_LOW = 0.35f
// The stage's picture sits a little high, clear of the chat card (as the Rerun replay's).
private const val STAGE_LIFT = 0.07f

// Faces the recording's one solid wall from inside the room (the Rerun replay's 35° sees it edge-on).
private const val STAGE_AZIMUTH = -55f
