package com.fastek.notea.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastek.notea.R
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private val BLEU_NUIT = Color(0xFF0B2A6B)
private val ORANGE = Color(0xFFFF7A00)
private val FOND = Color(0xFFFFFFFF)
private const val DUREE_TOTALE_MS = 3000L

/**
 * Écran de lancement FASTEK, d'une durée de 3 secondes : le logo apparaît en grossissant
 * légèrement, un trait orange se déploie, puis le nom du produit et la signature TGLD s'affichent.
 * Un toucher permet de passer l'animation.
 */
@Composable
fun SplashFastek(produit: String = "Notea", onTermine: () -> Unit) {
    val logo = remember { Animatable(0f) }
    val trait = remember { Animatable(0f) }
    val nomProduit = remember { Animatable(0f) }
    val signature = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            // 0 – 900 ms : le logo apparaît
            animer(logo, 900, 0L)
            // 1200 – 1800 ms : le trait orange se déploie
            animer(trait, 600, 1200L)
            // 1700 – 2300 ms : le nom du produit apparaît
            animer(nomProduit, 600, 1700L)
            // 2300 – 2800 ms : la signature TGLD apparaît
            animer(signature, 500, 2300L)
        }
        delay(DUREE_TOTALE_MS - 2800L)
        onTermine()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FOND)
            .clickable { onTermine() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo_fastek),
                contentDescription = "FASTEK",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .scale(0.92f + 0.08f * logo.value)
                    .alpha(logo.value)
            )
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .width((140f * trait.value).dp)
                    .height(3.dp)
                    .background(ORANGE)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = produit,
                color = BLEU_NUIT,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(nomProduit.value)
            )
        }
        Text(
            text = "TGLD",
            color = BLEU_NUIT.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 8.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
                .alpha(signature.value)
        )
    }
}

private fun kotlinx.coroutines.CoroutineScope.animer(anim: Animatable<Float, *>, duree: Int, attente: Long) {
    async {
        delay(attente)
        anim.animateTo(1f, tween(duree, easing = if (duree > 500) FastOutSlowInEasing else LinearEasing))
    }
}
