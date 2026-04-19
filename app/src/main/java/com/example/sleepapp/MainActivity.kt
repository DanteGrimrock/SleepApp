package com.example.sleepapp

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
    var selectedScene by remember { mutableStateOf<Scene?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (selectedScene == null) {
            HomeScreen(
                scenes = demoScenes,
                modifier = Modifier.padding(innerPadding),
                onSceneClick = { scene ->
                    selectedScene = scene
                }
            )
        } else {
            PlayerScreen(
                scene = selectedScene!!,
                modifier = Modifier.padding(innerPadding),
                onBackClick = {
                    selectedScene = null
                }
            )
        }
    }
}

@Composable
fun HomeScreen(
    scenes: List<Scene>,
    onSceneClick: (Scene) -> Unit,
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

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(scenes) { scene ->
                SceneCard(
                    scene = scene,
                    onClick = {
                        onSceneClick(scene)
                    }
                )
            }
        }
    }
}

@Composable
fun SceneCard(
    scene: Scene,
    onClick: () -> Unit
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
        }
    }
}

@Composable
fun PlayerScreen(
    scene: Scene,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioError by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onBackClick()
    }

    DisposableEffect(scene.id) {
        val player = try {
            MediaPlayer.create(context, scene.audioRes)
        } catch (_: Exception) {
            null
        }

        mediaPlayer = player
        audioError = if (player == null) {
            "Этот аудиофайл не удалось открыть через MediaPlayer"
        } else {
            null
        }

        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
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

        Spacer(modifier = Modifier.height(24.dp))

        if (audioError != null) {
            Text(
                text = audioError!!,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    mediaPlayer?.start()
                },
                enabled = mediaPlayer != null
            ) {
                Text("Play")
            }

            Button(
                onClick = {
                    mediaPlayer?.pause()
                },
                enabled = mediaPlayer != null
            ) {
                Text("Pause")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SleepAppTheme {
        HomeScreen(
            scenes = demoScenes,
            onSceneClick = {}
        )
    }
}