# Notea — Base de données + moteur de calcul

## Contenu de ce livrable
```
com.fastek.notea/
├── data/local/
│   ├── entity/        Eleve, Matiere, Note, Periode, TypeNote, TypeEtablissement
│   ├── dao/            EleveDao, MatiereDao, NoteDao, PeriodeDao
│   ├── Converters.kt
│   └── NoteaDatabase.kt
├── domain/calcul/
│   └── MoyenneCalculator.kt   ← moteur de calcul (matière → période → annuel)
└── (tests) MoyenneCalculatorTest.kt  ← reproduit ton exemple chiffré exact
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
- Repository (couche au-dessus des DAO pour les ViewModels)
- Entité/DAO pour les objectifs par période — actuellement `objectifCible` vit
  directement sur `Periode`, et `objectifAnnuel` sur `Eleve` ; à revoir si tu
  veux un historique des objectifs modifiés en cours d'année
- WorkManager (rappels planning)
- Écrans Compose (onboarding, dashboard, etc.)
- Branche PRIVÉ (3 trimestres) — le moteur est structuré pour l'accueillir
  facilement (mêmes fonctions matière/période, seule `moyenneAnnuellePublic`
  est spécifique au public), mais pas encore écrite

## Prochaine étape suggérée
Le Repository + les premiers ViewModels (Onboarding, Dashboard), pour brancher
la base de données aux écrans.
