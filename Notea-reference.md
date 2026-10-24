# Notea — Document de référence du projet

FASTEK — application de suivi scolaire pour lycéens béninois

---

## 1. Vue d'ensemble

- **Nom** : Notea (anciennement "TGLD School V1", dépôt GitHub `TGLD1/Schoolnote`)
- **Objectif** : véritable application professionnelle FASTEK
- **Cible** : lycéens (15-18 ans), smartphones d'entrée de gamme
- **Établissements pris en charge** : **public** (2 semestres) et **privé** (3 trimestres)

### Contraintes techniques
- 100% hors ligne ; internet uniquement au moment d'un don en ligne (KkiaPay)
- Légère : < 50 Mo
- Compatible Android 7+ (API 24)
- Pas de backend : tout en local via SQLite (Room)
- Notifications locales via WorkManager
- Material Design 3
- Identité visuelle officielle FASTEK (violet nuit #1B1040 → #4C1D95, cyan #22D3EE, blanc cassé #F3F6FC), thèmes clair et sombre

### Stack technique
- Kotlin + Jetpack Compose, Navigation Compose
- Room (SQLite) — pas de framework d'injection (pas de Hilt), une factory manuelle à la place, pour rester léger
- Don en ligne : SDK KkiaPay (`com.github.kkiapay:android-sdk` via JitPack), activité principale en `AppCompatActivity` (exigé par le SDK), thème AppCompat
- Build via GitHub Actions à chaque push : Gradle 8.9, AGP 8.5.2, Kotlin 2.0.21, JDK 17
- Repo GitHub : `TGLD1/Notea` (branche `master`)
- Clé publique KkiaPay injectée à la compilation : secret (ou variable) GitHub `KKIAPAY_PUBLIC_KEY` ; variable `KKIAPAY_SANDBOX` (`true` par défaut, `false` pour la production). Jamais la clé privée ni le secret dans le code

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
| **Présentation** | 5 diapositives au premier démarrage sur les fonctionnalités principales ; accessible ensuite depuis Paramètres → « Découvrir Notea » |
| **Onboarding** | Nom, prénom, classe (niveau + série en menus déroulants), type d'établissement, liste de matières prédéfinie à cocher + coefficient libre. Conduite ajoutée automatiquement. |
| **Accueil (Dashboard)** | Moyenne générale (cercle de progression avec effet « glow »), sélecteur de période, encadré « Matières à surveiller » (moyenne sous 10/20, avec l'effet de chaque matière sur la moyenne générale), liste des matières colorée selon le statut (vert/jaune/rouge/gris) |
| **Mes matières** | Liste des matières, moyenne en direct, tap → écran de saisie des notes |
| **Saisie des notes** | Valeur (0-20), type Devoir/Interrogation (max 2 devoirs par période), modification d'une note existante (bouton « Modif. »), coefficient éditable, moyenne recalculée en temps réel. Conduite : une seule note par période, saisie ici comme les autres matières |
| **Planning/Événements** | Événements à venir uniquement, triés par date ; ajout et modification d'un événement (titre, date, matière optionnelle) ; choix de la date par calendrier affiché à la place du formulaire ; rappels notifiés à l'heure choisie : dans 2 jours (une fois), la veille et le jour même (WorkManager, replanifié chaque jour) |
| **Objectifs** | Objectif annuel + objectif par période (la Conduite n'apparaît plus ici : c'est une matière normale) |
| **Statistiques** | Graphique d'évolution de la moyenne générale sur 30 jours (Canvas natif) + simulateur « si j'ai X au prochain devoir/interro, ma moyenne devient Y » + « Quelle note me faut-il ? » (note minimale au prochain devoir/interro pour atteindre une moyenne générale visée) + calculateur de passage (moyenne à obtenir au dernier semestre/trimestre pour 10, 12 ou l'objectif annuel) |
| **Bulletin PDF** | (Paramètres) Choix de la période, PDF A4 sur le modèle du bulletin officiel (API native `PdfDocument`), partage via le menu Android. Appréciation par matière (menu rapide ou saisie libre, proposition automatique selon la moyenne) ; rang, moyenne de classe et décision du conseil laissés vides |
| **Journal de diagnostic** | (Paramètres) État OK/ÉCHEC de chaque fonctionnalité + dernières lignes du journal local, partage et remise à zéro. Aucune donnée personnelle journalisée |
| **Sauvegarde / restauration** | (Paramètres) Export et import d'un fichier JSON local |
| **Photo de l'élève** | (Paramètres) Choix d'une photo dans la galerie, réduite à 400 px et enregistrée dans la mémoire privée ; elle remplit le cadre « Photo » du bulletin PDF |
| **Années scolaires** | (Paramètres) Bouton « Démarrer une nouvelle année » (archive puis efface notes et événements, passe l'année scolaire à la suivante) ; liste des archives avec moyennes par période et annuelle, partage et suppression |
| **Export CSV** | (Paramètres) Toutes les notes en fichier CSV (séparateur « ; », virgule décimale), partagé via le menu Android |
| **Rappels et notifications** | (Paramètres) État de l'autorisation et bouton pour l'accorder, heure d'envoi de 6 h à 22 h (défaut 18 h), notification de test, conseils pour les téléphones qui limitent la batterie |
| **Don** | (Paramètres) Montants 500 / 1000 / 2000 / 5000 FCFA ou montant libre (min. 100 FCFA), paiement Mobile Money ou carte via KkiaPay. Affiche l'état de la clé (début de la clé publique et mode test/réel) ; si la clé est absente de la compilation, un message l'indique |
| **Paramètres** | Profil éditable, objectif/classe en lecture seule, Don, Signaler un problème (email pré-rempli), À propos, Calcul des moyennes, Développeur, Politique de confidentialité, CGU, Découvrir Notea |

Navigation : 3 onglets en bas (Accueil, Matières, Planning) + bouton **⋮** en haut menant vers Objectifs et Paramètres.

---

## 4. État d'avancement (au 19/10/2026)

✅ Fait :
- Pipeline CI GitHub Actions : build APK debug vert à chaque push, artifacts `notea-debug-apk` et `room-schemas`
- Onboarding, Accueil, Mes matières, Saisie des notes, Objectifs, Planning, Statistiques, Paramètres
- Conduite = matière normale, coefficients éditables, page d'explication des calculs
- Établissement privé : moyenne annuelle (T1+T2+T3)÷3, bulletin trimestriel, tests unitaires
- Bulletin PDF refait sur le modèle CEG Pahou
- Journal de diagnostic
- Sauvegarde/restauration JSON locale
- Heure du rappel réglable
- Planning : modification d'un événement, événements à venir uniquement
- Identité visuelle FASTEK officielle (splash, icône adaptative, thèmes) et écran de présentation
- Don en ligne KkiaPay intégré et fonctionnel (mode test) ; clé publique fournie par `gradle.properties`
- Export du schéma Room activé

🟡 À valider sur appareil :
- Don : l'écran doit afficher « Clé KkiaPay : … — mode test » ; si le message indique l'absence de clé, le secret `KKIAPAY_PUBLIC_KEY` n'est pas transmis au build (le workflow affiche un avertissement ou une confirmation à l'étape « Vérifier la clé KkiaPay »). Puis tester un paiement en mode test
- Planning : le choix de date ne répondait pas (ni calendrier intégré, ni calendrier système) ; remplacé par un calendrier affiché à la place du formulaire, à confirmer
- Bulletin PDF sur téléphone et tablette IDINO (Android 11, 32 bits, ~2 Go RAM)

⚠️ Point technique : base de données en version 5 (colonne `photo`) ; migrations 3→4 et 4→5 en place, bases v1 et v2 réinitialisées (`fallbackToDestructiveMigrationFrom(1, 2)`). Chaque futur changement de schéma exige sa migration. Le schéma exporté (`app/schemas/.../6.json`) est à récupérer dans l'artifact `room-schemas` et à commiter.

---

## 4 bis. Fonctionnalités restantes

### Fonctionnalités élève livrées
- Modifier une note existante
- « Quelle note me faut-il ? » : note minimale requise pour une moyenne générale visée
- Alerte « Matières à surveiller » sur l'Accueil
- Calculateur de passage (public : (2×S2 + S1)÷3 ; privé : (T1+T2+T3)÷3)
- Appréciation par matière sur le bulletin PDF
- Bulletin PDF sur une seule page, Conduite en dernière ligne
- Classement des matières par impact sur la moyenne générale (Statistiques)
- Partage d'un résumé en texte (WhatsApp, SMS…) depuis l'Accueil
- Export CSV de toutes les notes (Paramètres)
- Rappels du planning : dans 2 jours (une fois), la veille et le jour même
- Photo de l'élève sur le bulletin PDF (Paramètres → Photo de l'élève, stockée localement)
- Notifications : demande d'autorisation guidée, bandeau d'alerte dans Planning, écran « Rappels et notifications » (état de l'autorisation, heure d'envoi, notification de test, conseils batterie)
- Statistiques : choix de la période, graphique d'évolution note après note avec repères 10/20 et objectif, barres par matière, matière présélectionnée dans les simulateurs
- Période affichée : Semestre 1 (ou Trimestre 1) tant qu'il n'est pas marqué « terminé » (interrupteur sur l'Accueil) ; le choix d'une période est partagé entre l'Accueil, Mes matières et Statistiques ; « Nouvelle année » remet toutes les périodes à non terminées
- Dates : sélecteur de calendrier unique (Planning, date de naissance du Profil) au format JJ/MM/AAAA
- Profil : saisie corrigée (le texte ne s'inversait plus, état local par champ)
- Identité : icône en N (diagonale cyan, montants blancs) ; écran de lancement FASTEK animé de 3 secondes
- Années scolaires : « Nouvelle année » archive notes et événements dans un fichier, conserve profil, matières, coefficients et objectifs ; archives consultables, partageables et supprimables

Aucune fonctionnalité en attente. Pistes futures : restauration d'une archive en un geste depuis l'application, comparaison entre années.

### Fiabilité des données
- Tests de migration Room et commit du schéma exporté
- Sauvegarde sur Google Drive (OAuth2, empreinte SHA-1 de la clé de signature)
- Journal → FastekLog (authority `com.fastek.log.provider`, signature commune des deux apps)

### Don en ligne
- Passer en production : variable `KKIAPAY_SANDBOX=false` et clé publique de production
- Épingler une version stable du SDK KkiaPay à la place de `master-SNAPSHOT`
- Vérifier les règles de Google Play sur les dons (la politique de confidentialité mentionne déjà KkiaPay)

### Sortie publique
- **Build release signé** : secrets GitHub `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, règles R8/ProGuard, `versionCode`/`versionName`, poids < 50 Mo, test sur Android 7 (API 24)
- **Textes légaux** : validation par Divin de la politique de confidentialité et des CGU ; adresse web publique si le Play Store l'exige
- **Test terrain** avec de vrais lycéens, puis correction des retours

### Plus tard
- **Version École (V2)** : voir section 6 (projet séparé, MVP hors ligne envisagé en premier)

---

## 5. Contacts affichés dans l'app

- Email : tgldivin@gmail.com
- WhatsApp : +229 01 64 40 94 45
- Don : en ligne via KkiaPay (Mobile Money et carte), plus de numéro affiché

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
