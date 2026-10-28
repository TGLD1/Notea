package com.fastek.notea.enseignant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palette FASTEK de Notea Enseignant : violet nuit, cyan, et trois couleurs de résultat. */
object Couleurs {
    val VioletNuit = Color(0xFF1B1040)
    val Violet = Color(0xFF4C1D95)
    val VioletVif = Color(0xFF6D28D9)
    val Cyan = Color(0xFF0E7490)
    val CyanClair = Color(0xFF22D3EE)

    val Succes = Color(0xFF16A34A)
    val Alerte = Color(0xFFD97706)
    val Danger = Color(0xFFDC2626)
    val Neutre = Color(0xFF8B87A3)

    private val Avatars = listOf(
        Color(0xFF6D28D9), Color(0xFF0E7490), Color(0xFFDB2777), Color(0xFFD97706),
        Color(0xFF16A34A), Color(0xFF2563EB), Color(0xFF9333EA), Color(0xFF0D9488)
    )

    /** Une couleur stable pour un texte (classe, élève). */
    fun pourTexte(texte: String): Color = Avatars[(texte.hashCode() and Int.MAX_VALUE) % Avatars.size]

    /** Rouge sous 10, ambre de 10 à 12 (objectif par défaut de Notea), vert à partir de 12. */
    fun pourMoyenne(moyenne: Double?): Color = when {
        moyenne == null -> Neutre
        moyenne < 10.0 -> Danger
        moyenne < 12.0 -> Alerte
        else -> Succes
    }

    val Degrade: Brush
        get() = Brush.linearGradient(listOf(VioletNuit, Violet, VioletVif))
}

private val SchemaClair = lightColorScheme(
    primary = Couleurs.Violet, onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE), onPrimaryContainer = Couleurs.VioletNuit,
    secondary = Couleurs.Cyan, onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE), onSecondaryContainer = Color(0xFF083344),
    tertiary = Couleurs.Cyan,
    background = Color(0xFFF4F2FC), onBackground = Couleurs.VioletNuit,
    surface = Color.White, onSurface = Couleurs.VioletNuit,
    surfaceVariant = Color(0xFFEAE6F7), onSurfaceVariant = Color(0xFF4A4660),
    outline = Color(0xFF8B87A3), outlineVariant = Color(0xFFDDD8F0),
    error = Couleurs.Danger
)

private val SchemaSombre = darkColorScheme(
    primary = Color(0xFFB9AEDD), onPrimary = Couleurs.VioletNuit,
    primaryContainer = Color(0xFF3B2A7A), onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Couleurs.CyanClair, onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75), onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Couleurs.CyanClair,
    background = Color(0xFF120B2E), onBackground = Color(0xFFF3F6FC),
    surface = Color(0xFF1F1550), onSurface = Color(0xFFF3F6FC),
    surfaceVariant = Color(0xFF2B1F5C), onSurfaceVariant = Color(0xFFB9AEDD),
    outline = Color(0xFF8B87A3), outlineVariant = Color(0xFF3A2E73),
    error = Color(0xFFF87171)
)

private val Typo = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

private val Formes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EnseignantTheme(sombre: Boolean = isSystemInDarkTheme(), contenu: @Composable () -> Unit) {
    val schema: ColorScheme = if (sombre) SchemaSombre else SchemaClair
    MaterialTheme(colorScheme = schema, typography = Typo, shapes = Formes, content = contenu)
}

// --- Conteneurs ----------------------------------------------------------------------------------

/** Carte blanche aux coins arrondis, ombre légère et fin liseré. */
@Composable
fun CarteDouce(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contenu: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) { contenu() }
    }
}

/** Bandeau dégradé violet nuit, sous la barre d'état, aux coins inférieurs arrondis. */
@Composable
fun EnTete(
    titre: String,
    sousTitre: String? = null,
    gauche: (@Composable () -> Unit)? = null,
    droite: (@Composable () -> Unit)? = null,
    contenu: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Couleurs.Degrade)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (gauche != null) {
                gauche()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    titre, color = Color.White, style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (sousTitre != null) {
                    Text(
                        sousTitre, color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (droite != null) {
                Spacer(Modifier.width(12.dp))
                droite()
            }
        }
        if (contenu != null) {
            Spacer(Modifier.height(16.dp))
            contenu()
        }
    }
}

/** Écran secondaire : en-tête avec « Retour », puis contenu sur fond clair. */
@Composable
fun EcranAvecEntete(
    titre: String,
    onRetour: () -> Unit,
    sousTitre: String? = null,
    contenu: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        EnTete(titre = titre, sousTitre = sousTitre, gauche = { BoutonEntete("‹ Retour", onRetour) })
        Box(Modifier.weight(1f).fillMaxWidth()) { contenu() }
    }
}

@Composable
fun FondEcran(contenu: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), content = contenu)
}

// --- Petits éléments -----------------------------------------------------------------------------

@Composable
fun BoutonEntete(texte: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(texte, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PuceInfo(texte: String) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(texte, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Avatar(texte: String, couleur: Color, taille: Dp = 44.dp) {
    Box(
        Modifier.size(taille).clip(CircleShape).background(couleur.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(texte, color = couleur, fontWeight = FontWeight.Bold, fontSize = (taille.value * 0.36f).sp)
    }
}

@Composable
fun ProgressionFine(
    fraction: Float,
    couleur: Color,
    modifier: Modifier = Modifier,
    fond: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(fond)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(CircleShape).background(couleur)
        )
    }
}

@Composable
fun EtatVide(emoji: String, titre: String, texte: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 44.sp) }
        Spacer(Modifier.height(16.dp))
        Text(titre, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            texte, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TitreSection(texte: String, modifier: Modifier = Modifier) {
    Text(
        texte, modifier = modifier, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Sélecteur à pastilles : l'élément choisi est plein, les autres sont discrets. */
@Composable
fun OngletsPilules(titres: List<String>, selection: Int, onChoisir: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        titres.forEachIndexed { i, titre ->
            val choisi = i == selection
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (choisi) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onChoisir(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    titre, style = MaterialTheme.typography.labelLarge,
                    color = if (choisi) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PuceChoix(texte: String, choisie: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (choisie) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .then(
                if (choisie) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            texte, style = MaterialTheme.typography.labelLarge,
            color = if (choisie) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Anneau de progression sur 20 avec la moyenne au centre. */
@Composable
fun AnneauMoyenne(moyenne: Double?, taille: Dp = 112.dp) {
    val couleur = Couleurs.pourMoyenne(moyenne)
    val fond = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.size(taille), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(taille)) {
            val epaisseur = 12.dp.toPx()
            val diametre = size.minDimension - epaisseur
            val coin = Offset(epaisseur / 2f, epaisseur / 2f)
            val dimension = Size(diametre, diametre)
            drawArc(
                color = fond, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = coin, size = dimension, style = Stroke(width = epaisseur, cap = StrokeCap.Round)
            )
            if (moyenne != null) {
                drawArc(
                    color = couleur, startAngle = -90f,
                    sweepAngle = 360f * (moyenne / 20.0).coerceIn(0.0, 1.0).toFloat(),
                    useCenter = false, topLeft = coin, size = dimension,
                    style = Stroke(width = epaisseur, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formaterMoyenne(moyenne), color = couleur,
                fontWeight = FontWeight.Bold, fontSize = (taille.value * 0.22f).sp
            )
            Text("sur 20", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// --- Boutons, champs, dialogues --------------------------------------------------------------------

@Composable
fun BoutonPlein(texte: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors()
    ) { Text(texte, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun BoutonContour(texte: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.height(52.dp)
    ) { Text(texte, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun Champ(
    valeur: String,
    onChange: (String) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = valeur, onValueChange = onChange, modifier = modifier,
        label = { Text(libelle) },
        singleLine = singleLine, minLines = minLines, maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions
    )
}

/** Boîte de dialogue aux coins arrondis, sur fond de carte. */
@Composable
fun Dialogue(
    titre: String,
    onFermer: () -> Unit,
    confirmer: @Composable () -> Unit,
    annuler: (@Composable () -> Unit)? = null,
    contenu: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onFermer,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text(titre, style = MaterialTheme.typography.titleLarge) },
        text = contenu,
        confirmButton = confirmer,
        dismissButton = annuler
    )
}
