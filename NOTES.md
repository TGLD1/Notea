# Notea — Base de données + moteur de calcul

## Contenu de ce livrable
```
com.fastek.notea/
├── data/
│   ├── local/
│   │   ├── entity/        Eleve, Matiere, Note, Periode, TypeNote, TypeEtablissement
│   │   ├── dao/            EleveDao, MatiereDao, NoteDao, PeriodeDao
│   │   ├── Converters.kt
│   │   └── NoteaDatabase.kt
│   └── repository/
│       └── NoteaRepository.kt   ← flux réactifs (moyennes recalculées en direct)
├── domain/
│   ├── calcul/
│   │   └── MoyenneCalculator.kt   ← moteur de calcul (matière → période → annuel)
│   └── reference/
│       └── MatieresReference.kt   ← liste des matières proposées à l'onboarding
├── ui/
│   ├── NoteaViewModelFactory.kt
│   ├── onboarding/OnboardingViewModel.kt
│   └── dashboard/DashboardViewModel.kt
└── NoteaApplication.kt   ← expose `database` et `repository`
```

## Ossature Gradle incluse dans ce livrable
`settings.gradle.kts`, `build.gradle.kts` (racine + module `app`),
`AndroidManifest.xml`, `NoteaApplication.kt`, `.gitignore`, `gradle.properties`.

**Point d'attention** : `LocalDate` (utilisé dans `Note`) nécessite le desugaring
déjà activé dans `app/build.gradle.kts` pour fonctionner sur Android 7/7.1
(API 24-25), sinon crash au runtime sur ces versions précises. Poids ajouté :
négligeable (~100 Ko), donc pas de souci pour le budget < 50 Mo.

## Compilation via GitHub Actions (comme tes autres apps FASTEK)
Pas de build Gradle local, pas de wrapper à committer. Le workflow
`.github/workflows/build.yml` se déclenche à chaque push et :
1. installe JDK 17
2. installe Gradle 8.9 directement (action `gradle/actions/setup-gradle`)
3. lance `gradle assembleDebug`
4. publie l'APK en artifact téléchargeable depuis l'onglet **Actions** du repo GitHub

Versions alignées sur Faxcek/TGLD Assistant : **Gradle 8.9, AGP 8.5.2,
Kotlin 2.0.21, JDK 17**. Avec Kotlin 2.0+, le compilateur Compose passe par le
plugin `org.jetbrains.kotlin.plugin.compose` (plus besoin de
`composeOptions.kotlinCompilerExtensionVersion`).

Après le push, va dans l'onglet **Actions** du repo → le run le plus récent →
section **Artifacts** en bas de page → télécharge `notea-debug-apk` (zip
contenant l'APK).

*Build release signé (comme Faxcek) : à mettre en place plus tard, avec les
mêmes secrets GitHub (`KEYSTORE_BASE64`, `KEY_ALIAS`, `KEYSTORE_PASSWORD`,
`KEY_PASSWORD`) — pas nécessaire pour un premier test.*

## État du premier build testable
- ✅ Icône placeholder ajoutée (`res/drawable/ic_launcher.xml`) — sans ça,
  le build échouait à la liaison des ressources (référence à une icône
  inexistante). À remplacer par le vrai logo Notea plus tard.
- ✅ `MainActivity` minimale + Compose activé (BOM, Material3, activity-compose)
  — juste un écran "Notea — build OK ✓", pour valider que tout le pipeline
  (Gradle + Kotlin + Compose) fonctionne avant d'investir dans les vrais écrans.
- ✅ `<activity>` déclarée dans le manifeste avec l'intent-filter LAUNCHER —
  sans ça l'app se serait installée mais sans aucune icône dans le tiroir
  d'applications, rien à lancer.

## Ce qui n'est PAS encore fait
- Vrais écrans Compose (Onboarding, Dashboard, etc.) — `MainActivity` n'affiche
  qu'un texte de test pour l'instant, les ViewModels sont prêts à être branchés
- WorkManager (rappels planning)
- Branche PRIVÉ (3 trimestres) — le moteur est structuré pour l'accueillir
  facilement (mêmes fonctions matière/période, seule `moyenneAnnuellePublic`
  est spécifique au public), mais pas encore écrite
- Vrai logo (l'icône actuelle est un simple placeholder bleu/blanc)

## Prochaine étape suggérée
Une fois l'APK de test installé et vérifié sur ton téléphone : les écrans
Compose Onboarding puis Dashboard, en s'appuyant sur `OnboardingViewModel` et
`DashboardViewModel` (déjà prêts, via `NoteaViewModelFactory`).
