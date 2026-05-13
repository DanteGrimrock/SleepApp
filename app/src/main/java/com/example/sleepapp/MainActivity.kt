package com.example.sleepapp

import android.os.Bundle
import android.content.ContentResolver
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.KeyboardType
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sleepapp.ui.theme.SleepAppTheme
import androidx.media3.common.Player
import android.content.Context

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SleepAppTheme {
                AppScreen()
            }
        }
    }
}

data class Scene(
    val id: String,
    val title: String,
    val subtitle: String,
    @field:DrawableRes val coverRes: Int,
    @field:RawRes val audioRes: Int
)

val demoScenes = listOf(
    Scene(
        id = "forest_ambience",
        title = "Forest ambience",
        subtitle = "Летний лес, ветер и дальние птицы",
        coverRes = R.drawable.forest_ambience,
        audioRes = R.raw.forest_ambience
    ),
    Scene(
        id = "birds_forest",
        title = "Birds forest",
        subtitle = "Птицы в лесу, спокойное утро",
        coverRes = R.drawable.birds_forest,
        audioRes = R.raw.birds_forest
    ),
    Scene(
        id = "thunder_rain",
        title = "Thunder rain",
        subtitle = "Гроза и дождь в горах",
        coverRes = R.drawable.thunder_rain,
        audioRes = R.raw.thunder_rain
    ),
    Scene(
        id = "summer_field",
        title = "Summer field",
        subtitle = "Тёплое летнее поле",
        coverRes = R.drawable.summer_field,
        audioRes = R.raw.summer_field
    ),
    Scene(
        id = "night_field_cricket",
        title = "Night field cricket",
        subtitle = "Ночное поле и сверчки",
        coverRes = R.drawable.night_field_cricket,
        audioRes = R.raw.night_field_cricket
    ),
    Scene(
        id = "field_grasshoppers",
        title = "Field grasshoppers",
        subtitle = "Летнее поле и кузнечики",
        coverRes = R.drawable.field_grasshoppers,
        audioRes = R.raw.field_grasshoppers
    )
)

@Composable
fun AppScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("sleep_app_prefs", Context.MODE_PRIVATE)
    }

    val savedFavoriteIds = remember {
        prefs.getStringSet("favorite_ids", emptySet())?.toSet() ?: emptySet()
    }

    val savedShowFavoritesOnly = remember {
        prefs.getBoolean("show_favorites_only", false)
    }

    val savedSceneId = remember {
        prefs.getString("last_scene_id", null)
    }

    val restoredScene = remember(savedSceneId) {
        demoScenes.find { it.id == savedSceneId }
    }

    var selectedScene by remember { mutableStateOf(restoredScene) }
    var currentScene by remember { mutableStateOf(restoredScene) }

    var showFavoritesOnly by remember { mutableStateOf(savedShowFavoritesOnly) }
    var favoriteIds by remember { mutableStateOf(savedFavoriteIds) }

    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var audioError by remember { mutableStateOf<String?>(null) }

    var sleepTimerSeconds by remember { mutableStateOf<Int?>(null) }
    var hoursInput by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("") }
    var timerInputError by remember { mutableStateOf<String?>(null) }

    val visibleScenes = if (showFavoritesOnly) {
        demoScenes.filter { it.id in favoriteIds }
    } else {
        demoScenes
    }

    LaunchedEffect(favoriteIds) {
        prefs.edit()
            .putStringSet("favorite_ids", favoriteIds)
            .apply()
    }

    LaunchedEffect(showFavoritesOnly) {
        prefs.edit()
            .putBoolean("show_favorites_only", showFavoritesOnly)
            .apply()
    }

    LaunchedEffect(currentScene?.id, selectedScene?.id) {
        val sceneIdToSave = selectedScene?.id ?: currentScene?.id
        prefs.edit()
            .putString("last_scene_id", sceneIdToSave)
            .apply()
    }

    DisposableEffect(currentScene?.id) {
        if (currentScene == null) {
            onDispose { }
        } else {
            val uri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .path(currentScene!!.audioRes.toString())
                .build()

            val exoPlayer = ExoPlayer.Builder(context).build()
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE

            try {
                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                player = exoPlayer
                audioError = null
            } catch (_: Exception) {
                exoPlayer.release()
                player = null
                audioError = "Этот аудиофайл не удалось открыть через ExoPlayer"
            }

            onDispose {
                if (player === exoPlayer) {
                    player?.release()
                    player = null
                } else {
                    exoPlayer.release()
                }
            }
        }
    }

    LaunchedEffect(sleepTimerSeconds) {
        val current = sleepTimerSeconds
        if (current != null) {
            if (current > 0) {
                delay(1000)
                sleepTimerSeconds = current - 1
            } else {
                player?.pause()
                sleepTimerSeconds = null
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (selectedScene == null) {
            HomeScreen(
                scenes = visibleScenes,
                favoriteIds = favoriteIds,
                showFavoritesOnly = showFavoritesOnly,
                onToggleFavoritesFilter = {
                    showFavoritesOnly = !showFavoritesOnly
                },
                onSceneClick = { scene ->
                    currentScene = scene
                    selectedScene = scene
                },
                onToggleFavorite = { scene ->
                    favoriteIds = if (scene.id in favoriteIds) {
                        favoriteIds - scene.id
                    } else {
                        favoriteIds + scene.id
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            PlayerScreen(
                scene = selectedScene!!,
                isFavorite = selectedScene!!.id in favoriteIds,
                onToggleFavorite = {
                    val scene = selectedScene!!
                    favoriteIds = if (scene.id in favoriteIds) {
                        favoriteIds - scene.id
                    } else {
                        favoriteIds + scene.id
                    }
                },
                onBackClick = {
                    selectedScene = null
                },
                onPlayClick = {
                    player?.play()
                },
                onPauseClick = {
                    player?.pause()
                },
                isPlayerReady = player != null,
                audioError = audioError,
                sleepTimerSeconds = sleepTimerSeconds,
                hoursInput = hoursInput,
                minutesInput = minutesInput,
                timerInputError = timerInputError,
                onHoursInputChange = { value ->
                    hoursInput = value.filter { it.isDigit() }.take(2)
                },
                onMinutesInputChange = { value ->
                    minutesInput = value.filter { it.isDigit() }.take(2)
                },
                onSetTimerClick = {
                    val hours = hoursInput.toIntOrNull() ?: 0
                    val minutes = minutesInput.toIntOrNull() ?: 0
                    val totalSeconds = hours * 3600 + minutes * 60

                    if (minutes >= 60) {
                        timerInputError = "Минуты должны быть меньше 60"
                    } else if (totalSeconds <= 0) {
                        timerInputError = "Введи время больше 0"
                    } else {
                        sleepTimerSeconds = totalSeconds
                        timerInputError = null
                    }
                },
                onNoStopClick = {
                    sleepTimerSeconds = null
                    timerInputError = null
                },
                onResetClick = {
                    hoursInput = ""
                    minutesInput = ""
                    sleepTimerSeconds = null
                    timerInputError = null
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun HomeScreen(
    scenes: List<Scene>,
    favoriteIds: Set<String>,
    showFavoritesOnly: Boolean,
    onToggleFavoritesFilter: () -> Unit,
    onSceneClick: (Scene) -> Unit,
    onToggleFavorite: (Scene) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sleep App",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Выбери атмосферу для сна",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (showFavoritesOnly) onToggleFavoritesFilter()
                }
            ) {
                Text("Все")
            }

            Button(
                onClick = {
                    if (!showFavoritesOnly) onToggleFavoritesFilter()
                }
            ) {
                Text("Избранное")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (showFavoritesOnly) {
                "Показаны только избранные сцены"
            } else {
                "Всего избранных: ${favoriteIds.size}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (scenes.isEmpty()) {
            Text(
                text = "Избранных сцен пока нет",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(scenes) { scene ->
                    SceneCard(
                        scene = scene,
                        isFavorite = scene.id in favoriteIds,
                        onClick = {
                            onSceneClick(scene)
                        },
                        onToggleFavorite = {
                            onToggleFavorite(scene)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SceneCard(
    scene: Scene,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = scene.coverRes),
                contentDescription = scene.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = scene.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = scene.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onToggleFavorite
            ) {
                Text(if (isFavorite) "★" else "☆")
            }
        }
    }
}

@Composable
fun PlayerScreen(
    scene: Scene,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    isPlayerReady: Boolean,
    audioError: String?,
    sleepTimerSeconds: Int?,
    hoursInput: String,
    minutesInput: String,
    timerInputError: String?,
    onHoursInputChange: (String) -> Unit,
    onMinutesInputChange: (String) -> Unit,
    onSetTimerClick: () -> Unit,
    onNoStopClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onBackClick) {
                Text(text = "← Назад")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(id = scene.coverRes),
            contentDescription = scene.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = scene.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = scene.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (audioError != null) {
            Text(
                text = audioError,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onToggleFavorite
        ) {
            Text(if (isFavorite) "Убрать из избранного" else "Добавить в избранное")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayClick,
                enabled = isPlayerReady
            ) {
                Text("Play")
            }

            Button(
                onClick = onPauseClick,
                enabled = isPlayerReady
            ) {
                Text("Pause")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Таймер сна",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = hoursInput,
                onValueChange = onHoursInputChange,
                label = { Text("Часы") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = minutesInput,
                onValueChange = onMinutesInputChange,
                label = { Text("Минуты") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onSetTimerClick) {
                Text("Установить")
            }

            Button(onClick = onNoStopClick) {
                Text("Без остановки")
            }

            Button(onClick = onResetClick) {
                Text("Сброс")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (timerInputError != null) {
            Text(
                text = timerInputError,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (sleepTimerSeconds != null) {
            Text(
                text = "Осталось: ${formatTime(sleepTimerSeconds)}",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                text = "Режим: без остановки",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SleepAppTheme {
        HomeScreen(
            scenes = demoScenes,
            favoriteIds = setOf("forest_ambience"),
            showFavoritesOnly = false,
            onToggleFavoritesFilter = {},
            onSceneClick = {},
            onToggleFavorite = {}
        )
    }
}