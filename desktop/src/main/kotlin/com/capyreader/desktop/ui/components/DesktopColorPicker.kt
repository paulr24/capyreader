package com.capyreader.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capyreader.desktop.ui.theme.parseHexColor

data class PresetColor(val hex: String, val name: String, val color: Color)

val PRESET_ACCENT_COLORS = listOf(
    PresetColor("#E28434", "Capy Orange", Color(0xFFE28434)),
    PresetColor("#2196F3", "Electric Blue", Color(0xFF2196F3)),
    PresetColor("#4CAF50", "Emerald Green", Color(0xFF4CAF50)),
    PresetColor("#E53935", "Crimson Red", Color(0xFFE53935)),
    PresetColor("#9C27B0", "Deep Purple", Color(0xFF9C27B0)),
    PresetColor("#FFB300", "Amber Gold", Color(0xFFFFB300)),
    PresetColor("#009688", "Teal", Color(0xFF009688)),
    PresetColor("#E91E63", "Coral Rose", Color(0xFFE91E63)),
    PresetColor("#3F51B5", "Indigo", Color(0xFF3F51B5)),
    PresetColor("#795548", "Mocha", Color(0xFF795548)),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopColorPicker(
    currentHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialColor = remember(currentHex) {
        parseHexColor(currentHex) ?: Color(0xFFE28434)
    }

    var hexInput by remember(currentHex) {
        mutableStateOf(if (currentHex.isNotBlank()) currentHex else "#E28434")
    }

    var redVal by remember(currentHex) {
        mutableFloatStateOf(initialColor.red * 255f)
    }
    var greenVal by remember(currentHex) {
        mutableFloatStateOf(initialColor.green * 255f)
    }
    var blueVal by remember(currentHex) {
        mutableFloatStateOf(initialColor.blue * 255f)
    }

    val previewColor = remember(redVal, greenVal, blueVal) {
        Color(
            redVal.toInt().coerceIn(0, 255),
            greenVal.toInt().coerceIn(0, 255),
            blueVal.toInt().coerceIn(0, 255)
        )
    }

    fun updateFromRgb(r: Float, g: Float, b: Float) {
        redVal = r
        greenVal = g
        blueVal = b
        val rInt = r.toInt().coerceIn(0, 255)
        val gInt = g.toInt().coerceIn(0, 255)
        val bInt = b.toInt().coerceIn(0, 255)
        val hex = String.format("#%02X%02X%02X", rInt, gInt, bInt)
        hexInput = hex
        onColorSelected(hex)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Preset Palette Swatches
        Text(
            text = "Preset Accents",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PRESET_ACCENT_COLORS.forEach { preset ->
                val isSelected = currentHex.equals(preset.hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(preset.color)
                        .clickable {
                            val r = preset.color.red * 255f
                            val g = preset.color.green * 255f
                            val b = preset.color.blue * 255f
                            redVal = r
                            greenVal = g
                            blueVal = b
                            hexInput = preset.hex
                            onColorSelected(preset.hex)
                        }
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Black.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Hex Input with Live Color Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = hexInput,
                onValueChange = { input ->
                    val clean = if (input.startsWith("#")) input else "#$input"
                    hexInput = clean
                    val color = parseHexColor(clean)
                    if (color != null) {
                        redVal = color.red * 255f
                        greenVal = color.green * 255f
                        blueVal = color.blue * 255f
                        onColorSelected(clean)
                    }
                },
                label = { Text("Hex Code") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            TextButton(
                onClick = {
                    hexInput = "#E28434"
                    redVal = 0xE2.toFloat()
                    greenVal = 0x84.toFloat()
                    blueVal = 0x34.toFloat()
                    onColorSelected("")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Default")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // RGB Sliders for visual tuning
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Red Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "R",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE53935),
                        modifier = Modifier.width(20.dp)
                    )
                    Slider(
                        value = redVal,
                        onValueChange = { updateFromRgb(it, greenVal, blueVal) },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE53935),
                            activeTrackColor = Color(0xFFE53935)
                        )
                    )
                    Text(
                        text = "${redVal.toInt().coerceIn(0, 255)}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(32.dp)
                    )
                }

                // Green Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.width(20.dp)
                    )
                    Slider(
                        value = greenVal,
                        onValueChange = { updateFromRgb(redVal, it, blueVal) },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4CAF50),
                            activeTrackColor = Color(0xFF4CAF50)
                        )
                    )
                    Text(
                        text = "${greenVal.toInt().coerceIn(0, 255)}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(32.dp)
                    )
                }

                // Blue Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "B",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2196F3),
                        modifier = Modifier.width(20.dp)
                    )
                    Slider(
                        value = blueVal,
                        onValueChange = { updateFromRgb(redVal, greenVal, it) },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2196F3),
                            activeTrackColor = Color(0xFF2196F3)
                        )
                    )
                    Text(
                        text = "${blueVal.toInt().coerceIn(0, 255)}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
    }
}

