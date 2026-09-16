package com.capyreader.desktop.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.capyreader.desktop.model.DesktopAccountState
import com.jocmp.capy.accounts.Source

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopLoginDialog(
    state: DesktopAccountState,
    onDismissRequest: () -> Unit
) {
    var selectedSource by remember { mutableStateOf(Source.FRESHRSS) }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val sources = listOf(
        Source.FRESHRSS to "FreshRSS",
        Source.READER to "Google Reader API (Inoreader, etc.)",
        Source.MINIFLUX to "Miniflux",
        Source.FEEDBIN to "Feedbin",
        Source.LOCAL to "Local (On-Device RSS)"
    )

    Dialog(onDismissRequest = { if (!isLoading) onDismissRequest() }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Service Dropdown
                ExposedDropdownMenuBox(
                    expanded = sourceDropdownExpanded,
                    onExpandedChange = { if (!isLoading) sourceDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = sources.firstOrNull { it.first == selectedSource }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false }
                    ) {
                        sources.forEach { (src, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedSource = src
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Server URL
                if (selectedSource.hasCustomURL) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Server URL") },
                        placeholder = {
                            Text(
                                if (selectedSource == Source.FRESHRSS) "https://freshrss.example.com/api/greader.php"
                                else "https://example.com"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    )
                }

                // Username
                if (selectedSource.requiresUsername && selectedSource != Source.LOCAL) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    )
                }

                // Password / Token
                if (selectedSource != Source.LOCAL) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                if (selectedSource == Source.FRESHRSS) "API Password"
                                else if (selectedSource == Source.MINIFLUX_TOKEN) "API Token"
                                else "Password"
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    )
                }

                // Error message
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            state.login(
                                source = selectedSource,
                                username = username.trim(),
                                password = password.trim(),
                                url = url.trim(),
                                onSuccess = {
                                    isLoading = false
                                    onDismissRequest()
                                },
                                onError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                }
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (selectedSource == Source.LOCAL) "Create Local Account" else "Sign In")
                    }
                }
            }
        }
    }
}
