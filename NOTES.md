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

## Avant de pouvoir compiler (`./gradlew build`)
Il manque uniquement le **wrapper Gradle binaire** (`gradle/wrapper/gradle-wrapper.jar`,
`gradlew`, `gradlew.bat`) — je ne peux pas générer ce `.jar`, c'est un fichier
téléchargé, pas du texte. Copie ces 3 éléments depuis un de tes projets FASTEK
existants (ils sont identiques d'un projet Kotlin à l'autre), ou lance
`gradle wrapper` si tu as Gradle installé en local.

Sans ça, **le premier commit git est possible** (rien n'empêche de committer du
code qui ne compile pas encore), mais le premier `./gradlew build` ne marchera
qu'une fois le wrapper ajouté.

## Ce qui n'est PAS encore fait
- Écrans Compose (onboarding, dashboard, etc.) — les ViewModels sont prêts à être branchés
- WorkManager (rappels planning)
- Branche PRIVÉ (3 trimestres) — le moteur est structuré pour l'accueillir
  facilement (mêmes fonctions matière/période, seule `moyenneAnnuellePublic`
  est spécifique au public), mais pas encore écrite
- Icône de l'app (`ic_launcher` référencée dans le manifeste, pas encore créée)
- `gradle-wrapper.jar` + `gradlew`/`gradlew.bat` — à copier depuis un projet FASTEK existant

## Prochaine étape suggérée
Les écrans Compose Onboarding puis Dashboard, en s'appuyant sur
`OnboardingViewModel` et `DashboardViewModel` (déjà prêts, via `NoteaViewModelFactory`).
