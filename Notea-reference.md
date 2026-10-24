# Notea — Document de référence du projet

FASTEK — application de suivi scolaire pour lycéens béninois

---

## 1. Vue d'ensemble

- **Nom** : Notea, application professionnelle FASTEK (Ouidah, Bénin)
- **Cible** : lycéens (15-18 ans), smartphones d'entrée de gamme
- **Établissements pris en charge** : **public** (2 semestres) et **privé** (3 trimestres)

### Contraintes techniques
- 100% hors ligne, sans compte ; internet uniquement pour le don en ligne (KkiaPay)
- Légère : < 50 Mo
- Compatible Android 7+ (API 24)
- Pas de backend : tout en local via SQLite (Room)
- Notifications locales via WorkManager
- Material Design 3, thèmes clair et sombre
- Identité FASTEK : violet nuit #1B1040 → #4C1D95, cyan #22D3EE, blanc cassé #F3F6FC ; icône en N ; écran de lancement animé de 3 secondes

### Stack technique
- Kotlin + Jetpack Compose, Navigation Compose
- Room (SQLite), sans framework d'injection : une factory manuelle pour rester léger
- Don en ligne : SDK KkiaPay (`com.github.kkiapay:android-sdk` via JitPack) ; l'activité principale est une `AppCompatActivity` (exigé par le SDK) avec un thème AppCompat
- Gradle 8.9, AGP 8.5.2, Kotlin 2.0.21, JDK 17
- Dépôt GitHub : `TGLD1/Notea` (branche `master`)

### Compilation et publication
- **Build de test** (`build.yml`) : à chaque push, APK debug et schéma Room (artifacts `notea-debug-apk` et `room-schemas`)
- **Build de production** (`release.yml`) : à chaque push (hors `docs/` et fichiers `.md`) ou lancement manuel ; produit l'APK signé, l'AAB pour Google Play et le fichier de correspondance R8, et refuse un APK de plus de 50 Mo. Le numéro de version est le numéro d'exécution du workflow ; le nom de version est `1.0.0` sauf indication lors d'un lancement manuel
- **Signature** : clé FASTEK au format `.p12`, commune à toutes les applications FASTEK ; secrets GitHub `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Le fichier `.p12` n'est jamais dans le dépôt ; en conserver plusieurs copies privées
- **Clé publique KkiaPay** : secret (ou variable) GitHub `KKIAPAY_PUBLIC_KEY`, ou propriété Gradle `kkiapayPublicKey` ; variable `KKIAPAY_SANDBOX` (`true` par défaut, `false` en production). Jamais la clé privée ni le secret dans le code
- **Pages légales publiques** : dossier `docs/` (accueil, politique de confidentialité, conditions d'utilisation), publiables avec GitHub Pages (Settings → Pages → `master` / `docs`)

---

## 2. Système scolaire béninois — règles confirmées

- **Public** : année en 2 semestres. **Privé** : jusqu'à 3 trimestres.
- Le bulletin doit permettre de saisir séparément les notes de **devoir** et d'**interrogation**.
- **Limite : 2 devoirs maximum par matière et par semestre** (comme le bulletin officiel).

### Formules de calcul (validées à l'identique sur un vrai bulletin — CEG Pahou, 2nde C)

**Par matière et par période :**
```
M_intero = moyenne simple de toutes les notes d'interrogation
Moyenne matière = (M_intero + somme des notes de devoir) ÷ (1 + nombre de devoirs)
```
M_intero compte comme **une seule valeur**, à égalité avec chaque devoir pris individuellement.

**Conduite** : n'est plus un champ spécial — c'est une **matière normale** (coefficient 1, créée automatiquement à l'onboarding). Sa note unique donnée par le conseil des professeurs en fin de période sert **directement** de moyenne pour cette "matière".

**Moyenne générale d'une période :**
```
Moyenne générale = Σ(moyenne matière × coefficient) ÷ Σ coefficients
```
(Conduite comprise dans la liste, comme les autres matières.)

**Moyenne annuelle (établissement public, 2 semestres) :**
```
Moyenne annuelle = (2 × moyenne S2 + moyenne S1) ÷ 3
```
Le 2e semestre pèse double (poids 2 + poids 1 = diviseur 3).

**Moyenne annuelle (établissement privé, 3 trimestres) :**
```
Moyenne annuelle = (T1 + T2 + T3) ÷ 3
```

- **Seuil de réussite officiel** : 10/20
- **Objectif par défaut proposé par l'app** : 12/20

### Format du bulletin officiel réel
- Colonnes : Disciplines, Coef, Moy. interro., Devoir 1, Devoir 2, Moy/20, Moy Coef, Rang, Appréciation (Conduite : colonnes interro/devoirs fusionnées)
- En-tête : Matricule, Nom, Prénoms, Date et lieu de naissance, Année scolaire, Classe, Effectif, Semestre, Aptitude EPS, Redoublant (Oui/Non), Abandon (Oui/Non), photo
- Rang par matière, Rang de classe, Moy. Classe, Plus forte/faible moyenne : demandent les notes de **toute la classe**, indisponibles dans Notea (app mono-élève). Décision : champs laissés **vides**, modifiables manuellement par l'élève sur le PDF généré.
- Décision du conseil (Passe/Redouble) + appréciation générale : uniquement sur le bulletin de **fin d'année** (dernier semestre ou trimestre) ; le 1er semestre porte le bloc BILAN + SANCTION.
- Mise en page du PDF alignée sur le bulletin réel du CEG Pahou : en-tête ministère/établissement, cadre identité, tableau des disciplines, bilan, cadre appréciation avec cachet et signature.

---

## 3. Écrans de l'application

| Écran | Contenu |
|---|---|
| **Lancement** | Écran FASTEK animé de 3 secondes (symbole, lettres FASTEK, trait cyan, « Notea ») ; un toucher le passe |
| **Présentation** | 5 diapositives au premier démarrage ; accessible ensuite depuis Paramètres → « Présentation de Notea » |
| **Onboarding** | Nom, prénom, classe (niveau + série), type d'établissement, matières prédéfinies à cocher + coefficient libre ; Conduite ajoutée automatiquement ; possibilité de restaurer une sauvegarde |
| **Accueil** | Moyenne générale (cercle de progression), choix de la période, interrupteur « période terminée », encadré « Matières à surveiller » (moyenne sous 10/20 et effet de chaque matière en points), liste des matières colorée selon le statut, bouton « Partager mon résumé » (texte pour WhatsApp ou SMS) |
| **Mes matières** | Liste des matières, moyenne en direct, accès à la saisie des notes |
| **Saisie des notes** | Valeur (0-20), type Devoir/Interrogation (2 devoirs maximum par période), modification et suppression d'une note, coefficient éditable, moyenne recalculée en direct ; Conduite : une seule note par période |
| **Planning** | Événements à venir triés par date ; ajout et modification (intitulé, date, matière facultative) ; bandeau d'alerte si les notifications sont désactivées |
| **Objectifs** | Objectif annuel et objectif par période |
| **Statistiques** | Choix de la période ; évolution de la moyenne générale note après note (repères 10/20 et objectif) ; moyennes par matière en barres ; impact de chaque matière sur la moyenne générale ; simulateur de note hypothétique ; « Quelle note me faut-il ? » (note minimale pour une moyenne générale visée) ; calculateur de passage (moyenne requise au dernier semestre ou trimestre pour 10, 12 ou l'objectif annuel) |
| **Bulletin PDF** | (Paramètres) Une seule page A4 sur le modèle officiel, Conduite en dernière ligne, photo, appréciation par matière (menu rapide, saisie libre ou proposition selon la moyenne) ; rang, moyenne de classe et décision du conseil laissés vides ; partage par le menu Android |
| **Profil** | Nom, prénom, matricule, date de naissance (calendrier), lieu de naissance, effectif, aptitude EPS, redoublant ; classe, établissement, année et objectif en lecture seule |
| **Photo de l'élève** | (Paramètres) Photo choisie dans la galerie, réduite à 400 px, conservée dans la mémoire privée, imprimée dans le cadre « Photo » du bulletin |
| **Années scolaires** | (Paramètres) « Démarrer une nouvelle année » : archive l'année (JSON), efface notes et événements, remet les périodes à non terminées, passe à l'année suivante ; liste des archives avec moyennes, partage et suppression |
| **Sauvegarde / restauration** | (Paramètres) Sauvegarde automatique par Android dans le compte Google de l'utilisateur (règles dans `res/xml/data_extraction_rules.xml` et `backup_rules.xml`, file technique WorkManager exclue), explication et accès aux réglages ; sauvegarde manuelle en fichier JSON et restauration |
| **Export CSV** | (Paramètres) Toutes les notes en CSV (séparateur « ; », virgule décimale) |
| **Rappels et notifications** | (Paramètres) Autorisation guidée, heure d'envoi (6 h à 22 h, 18 h par défaut), notification de test, conseils pour les téléphones qui limitent la batterie. Rappels : deux jours avant (une fois), la veille et le jour même |
| **Don** | (Paramètres) Montants 500 / 1000 / 2000 / 5000 FCFA ou libre (minimum 100 FCFA), Mobile Money ou carte via KkiaPay, sans compte ; message « momentanément indisponible » si la clé est absente de la compilation |
| **Journal de diagnostic** | (Paramètres) État de chaque fonctionnalité et dernières lignes du journal local ; aucune donnée personnelle |
| **Textes** | À propos, Calcul des moyennes, Développeur, Politique de confidentialité, Conditions d'utilisation, Signaler un problème (e-mail pré-rempli) |

Navigation : 3 onglets en bas (Accueil, Matières, Planning) et un bouton **⋮** en haut menant vers Objectifs, Statistiques et Paramètres.

Période affichée : Semestre 1 (ou Trimestre 1) tant qu'il n'est pas marqué « terminé » ; le choix manuel d'une période est partagé entre l'Accueil, Mes matières et Statistiques.

---

## 4. État d'avancement (au 24/10/2026)

### Livré
- Calculs conformes au bulletin officiel (public et privé), tests unitaires
- Tous les écrans du chapitre 3, y compris notes modifiables, simulateurs, statistiques, bulletin d'une page avec photo et appréciations, archives par année, export CSV, sauvegarde JSON
- Notifications avec autorisation guidée et notification de test
- Don en ligne KkiaPay, validé en mode test
- Identité FASTEK complète : icône N, lancement animé, thèmes
- Build de test et build de production signé (`.p12`) à chaque push, pages légales dans `docs/`

### À valider sur appareil
- Notifications : notification de test, puis rappel réel (veille et jour même)
- Statistiques : graphique et barres avec des données réelles, simulateurs
- Interrupteur « période terminée » et passage au Semestre 2
- Bulletin PDF sur téléphone et sur tablette IDINO (Android 11, 32 bits, ~2 Go de RAM), avec beaucoup de matières
- Premier build de production signé (réduction du code R8 et SDK KkiaPay)

### Point technique
Base de données en version 6 (colonnes `photo` et `terminee`) ; migrations 3→4, 4→5 et 5→6 ; bases v1 et v2 réinitialisées (`fallbackToDestructiveMigrationFrom(1, 2)`). Chaque futur changement de schéma exige sa migration. Le schéma exporté (`app/schemas/.../6.json`) est à récupérer dans l'artifact `room-schemas` et à commiter. La photo n'est pas incluse dans les sauvegardes JSON.

---

## 4 bis. Prochaines étapes

### Sortie publique
1. Valider le build de production signé et tester l'APK sur un téléphone Android 7 ou 8 et sur un petit modèle
2. Publier les pages légales (GitHub Pages sur `docs/`) et obtenir l'adresse de la politique de confidentialité
3. Préparer la fiche de l'application : captures d'écran, description, icône
4. Test terrain avec de vrais lycéens, puis correction des retours
5. Publier : Google Play (compte développeur, phases de test imposées par Google ; vérifier les conditions actuelles) ou distribution directe de l'APK

### Don en production
- Activer le compte KkiaPay (documents administratifs : RCCM et IFU pour une entreprise, IFU, justificatif de domicile et pièce d'identité pour un consultant individuel)
- Passer `KKIAPAY_SANDBOX` à `false` et vérifier la clé publique de production
- Remplacer `master-SNAPSHOT` par une version fixe du SDK KkiaPay
- Vérifier les règles de Google Play sur les dons
- Tant que le compte n'est pas activé : publier avec le don indisponible

### Fiabilité des données
- Tests de migration Room
- Sauvegarde automatique Android : tester la restauration sur un second appareil via un test interne Google Play
- Option future : synchronisation directe avec Google Drive (OAuth2, dossier privé de l'application, empreinte SHA-1 de la clé de signature) pour les installations hors Play Store
- Journal partagé avec FastekLog (authority `com.fastek.log.provider`, signature commune des deux applications)

### Pistes futures
- Restauration d'une archive en un geste depuis l'application
- Comparaison entre années scolaires
- Version École (V2) : voir le chapitre 6

---

## 5. Contacts affichés dans l'app

- Email : tgldivin@gmail.com
- WhatsApp : +229 01 64 40 94 45
- Don : en ligne via KkiaPay (Mobile Money et carte), sans numéro affiché

---

## 6. Piste V2 — Version École (projet séparé, après Notea solo)

**Constat** : la version élève actuelle est mono-utilisateur et 100% locale, donc incapable de calculer rang par matière, rang de classe ou moyenne de classe (données de toute la classe indisponibles). Une version école centraliserait les notes de tous les élèves et résoudrait ça.

**Option A — avec API/cloud**
- Comptes par rôle : Admin, Enseignant, Élève, Parent
- Admin : crée les classes avec coefficients officiels, importe la liste des élèves, assigne les enseignants, valide les bulletins
- Enseignant : saisit les notes de toute sa classe (vue tableau)
- Élève : consultation, bulletin PDF complet et automatique
- Avantages : temps réel, multi-appareil
- Inconvénients : nécessite internet fiable, coûts d'hébergement, sécurité des données de centaines d'élèves à la charge de FASTEK

**Option B — sans API**
- Réseau local à l'école (un appareil "serveur" au secrétariat) ou export/import de fichiers (CSV/JSON) entre enseignants et administration
- Avantages : zéro coût d'hébergement, fonctionne hors ligne, cohérent avec la philosophie actuelle de Notea
- Inconvénients : plus de friction manuelle, moins temps réel

**Décision actuelle** :
- La **consultation parentale est retirée** du périmètre initial de la version école
- Elle sera réintégrée **seulement si** la version avec API se concrétise un jour
- Recommandation : démarrer par l'option B (hors ligne) comme MVP, l'API venant plus tard si des écoles avec budget/connexion le demandent

Ce projet reste une V2 séparée, à envisager une fois Notea solo terminé et testé en conditions réelles.
