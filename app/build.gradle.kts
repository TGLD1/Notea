plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.fastek.notea"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fastek.notea.enseignant"
        minSdk = 24        // Android 7.0 — contrainte du cahier des charges
        targetSdk = 34
        // Numéro de version : valeurs par défaut pour le développement ; le workflow « Release »
        // les fournit pour la publication (VERSION_CODE doit augmenter à chaque mise à jour).
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "0.1.0"

        // Clé API PUBLIQUE KkiaPay : injectée à la compilation (secret GitHub KKIAPAY_PUBLIC_KEY
        // ou propriété Gradle kkiapayPublicKey) — jamais écrite dans le code. La clé privée et
        // le secret ne doivent JAMAIS figurer dans l'application ni dans le dépôt.
        val cleKkiapay = (project.findProperty("kkiapayPublicKey") as String?)
            ?: System.getenv("KKIAPAY_PUBLIC_KEY")
            ?: ""
        // Mode test par défaut (aucun vrai débit) ; KKIAPAY_SANDBOX=false pour les paiements réels.
        val sandboxKkiapay = System.getenv("KKIAPAY_SANDBOX")?.takeIf { it.isNotBlank() } ?: "true"
        buildConfigField("String", "KKIAPAY_PUBLIC_KEY", "\"$cleKkiapay\"")
        buildConfigField("boolean", "KKIAPAY_SANDBOX", sandboxKkiapay)

        // Clé API PUBLIQUE FedaPay (pk_live_… ou pk_sandbox_…) : injectée à la compilation (secret GitHub
        // FEDAPAY_PUBLIC_KEY ou propriété Gradle fedapayPublicKey). La clé SECRÈTE (sk_…) ne doit jamais
        // entrer dans l'application : la compilation s'arrête si elle y est mise par erreur.
        val cleFedapay = (project.findProperty("fedapayPublicKey") as String?)
            ?: System.getenv("FEDAPAY_PUBLIC_KEY")
            ?: ""
        require(!cleFedapay.trim().startsWith("sk_")) {
            "FEDAPAY_PUBLIC_KEY contient une clé SECRÈTE (sk_…). Utilise la clé PUBLIQUE (pk_…)."
        }
        buildConfigField("String", "FEDAPAY_PUBLIC_KEY", "\"${cleFedapay.trim()}\"")
    }

    // Signature de production : uniquement si le workflow Release fournit le keystore.
    // Le keystore et ses mots de passe ne sont jamais dans le dépôt (secrets GitHub).
    val keystoreFile = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (!keystoreFile.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystoreFile)
                storeType = if (keystoreFile.endsWith(".p12") || keystoreFile.endsWith(".pfx")) "pkcs12" else "jks"
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (!keystoreFile.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true   // requis pour java.time.LocalDate sur API 24
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Avec Kotlin 2.0+, le compilateur Compose est géré par le plugin
    // org.jetbrains.kotlin.plugin.compose ci-dessus — plus besoin de composeOptions.
}

// Schémas Room exportés (app/schemas) : base des futurs tests de migration.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.github.kkiapay:android-sdk:master-SNAPSHOT")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
