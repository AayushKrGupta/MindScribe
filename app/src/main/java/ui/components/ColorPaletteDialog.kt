package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.mindscribe.R // Important: Your app's R file

val colorPalette = listOf(
    R.color.note_color_default,
    R.color.note_red,
    R.color.note_pink,
    R.color.note_purple,
    R.color.note_deep_purple,
    R.color.note_indigo,
    R.color.note_blue,
    R.color.note_light_blue,
    R.color.note_cyan,
    R.color.note_teal,
    R.color.note_green,
    R.color.note_light_green,
    R.color.note_lime,
    R.color.note_yellow,
    R.color.note_amber,
    R.color.note_orange,
    R.color.note_deep_orange,
    R.color.note_brown,
    R.color.note_grey,
    R.color.note_blue_grey
)

@Composable
fun ColorPaletteDialog(
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Note Color") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(colorPalette) { colorResId ->
                    ColorDot(colorResId = colorResId) {
                        onColorSelected(it)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ColorDot(colorResId: Int, onClick: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(androidx.compose.ui.res.colorResource(id = colorResId))
            .clickable { onClick(colorResId) }
    )
}