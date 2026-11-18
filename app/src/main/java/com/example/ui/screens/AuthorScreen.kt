package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserAiPreferences
import com.example.ui.DeviceDiagnostics
import com.example.ui.components.Button3DTone
import com.example.ui.components.Interactive3DCard
import com.example.ui.components.Tactile3DButton
import com.example.ui.components.formatBytes

@Composable
fun AuthorScreen(
    preferences: UserAiPreferences,
    diagnostics: DeviceDiagnostics,
    onToggleDarkTheme: (Boolean) -> Unit,
    onTogglePortuguese: (Boolean) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val authorName = stringResource(R.string.author_name)
    val authorEmail = stringResource(R.string.author_email)
    val authorPhoneDisplay = stringResource(R.string.author_phone_display)
    val authorPhoneRaw = stringResource(R.string.author_phone_raw)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 760.dp)) {
                Text(
                    text = "Menu do Autor",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Créditos, contactos diretos e configuração do sistema local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hero Author Card with interactive 3D perspective tilt & overlapping emblem
        item {
            Interactive3DCard(
                glowColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .testTag("author_profile_card")
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_ai_studio_banner_1794936078179),
                            contentDescription = "Fundo tecnológico do autor",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                        )
                                    )
                                )
                        )
                        Image(
                            painter = painterResource(id = R.drawable.img_author_emblem_1794936079230),
                            contentDescription = "Emblema Duarte Cunha",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp)
                                .offset(y = 32.dp)
                                .size(84.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }

                    Spacer(Modifier.height(40.dp))

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = authorName,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Autor & Criador • Núcleo IA Offline & Rede Local",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Email Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = "Email",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text("Email", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(authorEmail, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(authorEmail)) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_author_email")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar email")
                            }
                        }

                        // Phone Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = "Telefone",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column {
                                    Text("Contacto Telefónico", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(authorPhoneDisplay, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(authorPhoneRaw)) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_author_phone")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar telefone")
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Tactile3DButton(
                                text = "Ligar $authorPhoneRaw",
                                icon = Icons.Filled.Call,
                                tone = Button3DTone.EMERALD,
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$authorPhoneRaw"))
                                    context.startActivity(dialIntent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("call_author_button")
                            )
                            Tactile3DButton(
                                text = "Enviar Email",
                                icon = Icons.Filled.Email,
                                tone = Button3DTone.CYAN,
                                onClick = {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:$authorEmail")
                                        putExtra(Intent.EXTRA_SUBJECT, "Contacto Núcleo IA")
                                    }
                                    context.startActivity(emailIntent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("email_author_button")
                            )
                        }
                    }
                }
            }
        }

        // Privacy & Question History Reset Card
        item {
            Interactive3DCard(
                containerColor = MaterialTheme.colorScheme.surface,
                glowColor = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Privacidade & Histórico", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Apague todas as perguntas e respostas guardadas localmente no Assistente e na Câmara IA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tactile3DButton(
                        text = "Apagar Histórico de Perguntas",
                        icon = Icons.Filled.DeleteSweep,
                        tone = Button3DTone.CORAL,
                        onClick = onClearAllHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("author_clear_all_history_button")
                    )
                }
            }
        }

        // App Preferences Card
        item {
            Interactive3DCard(
                containerColor = MaterialTheme.colorScheme.surface,
                glowColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Preferências da Aplicação", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Guardadas localmente via Android DataStore (tema, idioma e definições da câmara).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Tema Escuro Obsidian", style = MaterialTheme.typography.bodyLarge)
                                Text("Visual de elevado contraste para IA local", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = preferences.darkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            modifier = Modifier.testTag("theme_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Column {
                                Text("Operar em Português (PT-PT)", style = MaterialTheme.typography.bodyLarge)
                                Text("Instrui os modelos offline e LAN a responder sempre em Português", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = preferences.forcePortuguese,
                            onCheckedChange = onTogglePortuguese,
                            modifier = Modifier.testTag("portuguese_switch")
                        )
                    }
                }
            }
        }

        // System Info Card
        item {
            Interactive3DCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                glowColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Informação Local & Rede", style = MaterialTheme.typography.titleMedium)
                    DiagnosticRow(Icons.Filled.PhoneAndroid, "Modelo", "${diagnostics.deviceName} • ${diagnostics.androidVersion}")
                    DiagnosticRow(Icons.Filled.Language, "IP na Rede Local", diagnostics.localIpv4)
                    DiagnosticRow(Icons.Filled.Storage, "Espaço em Modelos Offline", formatBytes(diagnostics.modelsStorageBytes))
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(text = "$label:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}
