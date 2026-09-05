package com.circadiandisplay.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circadiandisplay.app.ui.preview.PreviewContent
import com.circadiandisplay.app.util.currentTimeMinutes
import com.circadiandisplay.app.util.minutesToTimeString
import com.circadiandisplay.core.curve.CurveEngine
import com.circadiandisplay.core.curve.CurveProfile
import kotlin.math.roundToInt

private val WarmLineColor = Color(0xFFFF9800)
private val DimLineColor = Color(0xFF757575)
private val GridColor = Color(0x22000000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurveEditorScreen(
    viewModel: CurveEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var pointToDelete by remember { mutableStateOf<CurvePointDraft?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Error -> snackbarHostState.showSnackbar(event.message)
                EditorEvent.Saved -> snackbarHostState.showSnackbar("Saved")
            }
        }
    }

    fun close() {
        if (state.hasUnsavedChanges) showDiscardDialog = true else onNavigateBack()
    }

    BackHandler(onBack = { close() })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Curve") },
                navigationIcon = {
                    IconButton(onClick = { close() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showPreview = true }) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Preview",
                        )
                    }
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled =
                            state.hasUnsavedChanges &&
                                !state.isSaving &&
                                state.profileName.isNotBlank(),
                    ) {
                        Text(if (state.isSaving) "Saving…" else "Save")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading…")
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = state.profileName,
                    onValueChange = { viewModel.setProfileName(it) },
                    label = { Text("Profile name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                CurveGraph(
                    points = state.points,
                    selectedId = state.selectedPointId,
                    onAddPoint = { viewModel.addPoint(it) },
                    onSelectPoint = { viewModel.selectPoint(it) },
                    onMovePointTime = { id, time -> viewModel.movePointTime(id, time) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendItem("Warmth", WarmLineColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem("Dimming", DimLineColor)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Tap to add • Drag to move",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val selected = state.points.firstOrNull { it.id == state.selectedPointId }
                when {
                    selected != null ->
                        PointEditorCard(
                            point = selected,
                            onUpdate = { time, warmth, dimming ->
                                viewModel.updatePoint(selected.id, time, warmth, dimming)
                            },
                            onDelete = {
                                if (state.points.size <= 1) {
                                    pointToDelete = selected
                                } else {
                                    viewModel.deletePoint(selected.id)
                                }
                            },
                        )

                    state.points.isEmpty() ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "No points yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the graph above to add your first point.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes to this curve.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            },
        )
    }

    pointToDelete?.let { point ->
        AlertDialog(
            onDismissRequest = { pointToDelete = null },
            title = { Text("Delete point?") },
            text = { Text("This is the only point. The profile will have an empty curve.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePoint(point.id)
                        pointToDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pointToDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (showPreview) {
        EditorPreviewDialog(
            profileId = state.profileId,
            profileName = state.profileName,
            points = state.points,
            onDismiss = { showPreview = false },
        )
    }
}

@Composable
private fun CurveGraph(
    points: List<CurvePointDraft>,
    selectedId: Long?,
    onAddPoint: (Int) -> Unit,
    onSelectPoint: (Long?) -> Unit,
    onMovePointTime: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val insetPx = with(density) { 16.dp.toPx() }
    val touchPx = with(density) { 22.dp.toPx() }

    Canvas(
        modifier =
            modifier.pointerInput(points) {
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val sorted = points.sortedBy { it.timeMinutes }
                    val hit =
                        sorted.firstOrNull { p ->
                            val c =
                                Offset(
                                    timeToX(p.timeMinutes, w, insetPx),
                                    valueToY(p.warmth, h, insetPx),
                                )
                            (down.position - c).getDistance() <= touchPx
                        }
                    if (hit != null) {
                        onSelectPoint(hit.id)
                        drag(down.id) { change ->
                            change.consume()
                            onMovePointTime(hit.id, xToTime(change.position.x, w, insetPx))
                        }
                    } else {
                        val dragged = drag(down.id) { change -> change.consume() }
                        if (!dragged) {
                            onAddPoint(xToTime(down.position.x, w, insetPx))
                        }
                    }
                }
            },
    ) {
        val w = size.width
        val h = size.height

        for (i in 0..4) {
            val y = valueToY(i / 4f, h, insetPx)
            drawLine(
                color = GridColor,
                start = Offset(insetPx, y),
                end = Offset(w - insetPx, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val sorted = points.sortedBy { it.timeMinutes }
        if (sorted.isNotEmpty()) {
            val warmPath = Path()
            val dimPath = Path()
            sorted.forEachIndexed { index, p ->
                val x = timeToX(p.timeMinutes, w, insetPx)
                val wy = valueToY(p.warmth, h, insetPx)
                val dy = valueToY(p.dimming, h, insetPx)
                if (index == 0) {
                    warmPath.moveTo(x, wy)
                    dimPath.moveTo(x, dy)
                } else {
                    warmPath.lineTo(x, wy)
                    dimPath.lineTo(x, dy)
                }
            }
            drawPath(dimPath, color = DimLineColor, style = Stroke(width = 2.dp.toPx()))
            drawPath(warmPath, color = WarmLineColor, style = Stroke(width = 3.dp.toPx()))
        }

        sorted.forEach { p ->
            val center =
                Offset(
                    timeToX(p.timeMinutes, w, insetPx),
                    valueToY(p.warmth, h, insetPx),
                )
            val radius = if (p.id == selectedId) 10.dp.toPx() else 7.dp.toPx()
            drawCircle(color = WarmLineColor, radius = radius, center = center)
            drawCircle(color = Color.White, radius = radius * 0.5f, center = center)
            if (p.id == selectedId) {
                drawCircle(
                    color = WarmLineColor,
                    radius = radius + 3.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun PointEditorCard(
    point: CurvePointDraft,
    onUpdate: (timeMinutes: Int, warmth: Float, dimming: Float) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Selected point",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = minutesToTimeString(point.timeMinutes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            LabeledSlider(
                label = "Time",
                value = point.timeMinutes.toFloat(),
                valueRange = 0f..1439f,
                valueLabel = minutesToTimeString(point.timeMinutes),
                onValueChange = { onUpdate(it.roundToInt(), point.warmth, point.dimming) },
            )
            LabeledSlider(
                label = "Warmth",
                value = point.warmth,
                valueRange = 0f..1f,
                valueLabel = "${(point.warmth * 100).toInt()}%",
                onValueChange = { onUpdate(point.timeMinutes, it, point.dimming) },
            )
            LabeledSlider(
                label = "Dimming",
                value = point.dimming,
                valueRange = 0f..1f,
                valueLabel = "${(point.dimming * 100).toInt()}%",
                onValueChange = { onUpdate(point.timeMinutes, point.warmth, it) },
            )

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Delete point")
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EditorPreviewDialog(
    profileId: Long,
    profileName: String,
    points: List<CurvePointDraft>,
    onDismiss: () -> Unit,
) {
    val engine = remember { CurveEngine() }
    val domainPoints = remember(points) { points.map { it.toDomain(profileId) } }
    val profile =
        remember(profileId, profileName) {
            CurveProfile(id = profileId, name = profileName.ifBlank { "Preview" })
        }
    var time by remember { mutableIntStateOf(currentTimeMinutes()) }
    val displayState = engine.calculateDisplayState(profile, domainPoints, time)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Preview — ${profile.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                PreviewContent(
                    displayState = displayState,
                    timeMinutes = time,
                    currentTimeMinutes = currentTimeMinutes(),
                    onTimeChange = { time = it },
                )
            }
        }
    }
}

private fun timeToX(
    timeMinutes: Int,
    width: Float,
    inset: Float,
): Float = inset + (width - 2 * inset) * (timeMinutes / 1439f)

private fun valueToY(
    value: Float,
    height: Float,
    inset: Float,
): Float = inset + (height - 2 * inset) * (1f - value)

private fun xToTime(
    x: Float,
    width: Float,
    inset: Float,
): Int = ((x - inset) / (width - 2 * inset) * 1439f).roundToInt().coerceIn(0, 1439)
