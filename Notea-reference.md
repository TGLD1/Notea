# Notea — Document de référence du projet

FASTEK — application de suivi scolaire pour lycéens béninois

---

## 1. Vue d'ensemble

- **Nom** : Notea (anciennement "TGLD School V1", dépôt GitHub `TGLD1/Schoolnote`)
- **Objectif** : véritable application professionnelle FASTEK
- **Cible** : lycéens (15-18 ans), smartphones d'entrée de gamme
- **Priorité actuelle** : établissement **public** (2 semestres) d'abord ; le **privé** (3 trimestres) suivra le même principe, à développer plus tard

### Contraintes techniques
- 100% hors ligne, aucune connexion internet requise
- Légère : < 50 Mo
- Compatible Android 7+ (API 24)
- Pas de backend : tout en local via SQLite (Room)
- Notifications locales via WorkManager
- Material Design 3
- Thème aux couleurs vives, adapté aux ados

### Stack technique
- Kotlin + Jetpack Compose, Navigation Compose
- Room (SQLite) — pas de framework d'injection (pas de Hilt), une factory manuelle à la place, pour rester léger
- Build via GitHub Actions à chaque push : Gradle 8.9, AGP 8.5.2, Kotlin 2.0.21, JDK 17
- Repo GitHub : `TGLD1/Notea`

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

- **Seuil de réussite officiel** : 10/20
- **Objectif par défaut proposé par l'app** : 12/20

### Format du bulletin officiel réel
- Colonnes : Disciplines, Coef, Moy. interro., Devoir 1, Devoir 2, Moy/20, Moy Coef, Rang, Appréciation
- En-tête : Matricule, Nom, Prénoms, Date et lieu de naissance, Année scolaire, Classe, Effectif, Semestre, Aptitude EPS, Redoublant (Oui/Non), Abandon (Oui/Non), photo
- Rang par matière, Rang de classe, Moy. Classe, Plus forte/faible moyenne : demandent les notes de **toute la classe**, indisponibles dans Notea (app mono-élève). Décision : champs laissés **vides**, modifiables manuellement par l'élève sur le PDF généré.
- Décision du conseil (Passe/Redouble) + appréciation générale : uniquement sur le bulletin de **fin d'année** (2e semestre), pas au 1er semestre.

---

## 3. Écrans de l'application

| Écran | Contenu |
|---|---|
| **Onboarding** | Nom, prénom, classe (niveau + série en menus déroulants), type d'établissement, liste de matières prédéfinie à cocher + coefficient libre. Conduite ajoutée automatiquement. |
| **Accueil (Dashboard)** | Moyenne générale (cercle de progression avec effet "glow" — pas de cartes de couleur pleine), sélecteur Semestre 1/2, liste des matières colorée selon le statut (vert/jaune/rouge/gris) |
| **Mes matières** | Liste des matières, moyenne en direct, tap → écran de saisie des notes |
| **Saisie des notes** | Valeur (0-20), type Devoir/Interrogation (max 2 devoirs/semestre), coefficient éditable, moyenne recalculée en temps réel |
| **Planning/Événements** | Liste triée par date, ajout (titre, date JJ/MM/AAAA, matière optionnelle), notification à 18h pour les événements à 48h (WorkManager, auto-replanifié chaque jour) |
| **Objectifs** | Objectif annuel + objectif par semestre |
| **Statistiques** | Graphique d'évolution de la moyenne générale sur 30 jours (reconstruit à partir des dates des notes, pas de table d'historique séparée ; dessiné en Canvas natif, sans librairie externe) + simulateur "si j'ai X au prochain devoir/interro, ma moyenne devient Y" |
| **Paramètres** | Profil éditable (nom, prénom, matricule), objectif/classe en lecture seule, Don à FASTEK (Moov Money), Signaler un problème (email pré-rempli), À propos de l'app, Comment sont calculées les moyennes, À propos du développeur, Politique de confidentialité, CGU |

Navigation : 3 onglets en bas (Accueil, Matières, Planning) + bouton **⋮** en haut menant vers Objectifs et Paramètres.

---

## 4. État d'avancement (au 22/09)

✅ Fait et confirmé fonctionnel sur téléphone :
- Pipeline CI GitHub Actions (build APK à chaque push)
- Onboarding, Accueil/Dashboard, Mes matières, Saisie des notes
- Objectifs, Planning/Événements, Notifications WorkManager
- Statistiques (graphique + simulateur)
- Paramètres (profil éditable + matricule, Don, Signaler un problème, pages légales)
- Refonte Conduite → matière normale, coefficients éditables après coup
- Page d'explication du calcul des moyennes

🔜 Reste à faire :
- Bulletin PDF (format officiel)
- Sauvegarde Google Drive (OAuth2)
- Branche établissement privé (3 trimestres)
- Vrai logo/icône (actuellement un placeholder bleu)

⚠️ Point technique : base de données encore en `fallbackToDestructiveMigration()` (efface les données à chaque changement de schéma) — à remplacer par de vraies migrations avant la sortie publique.

---

## 5. Contacts affichés dans l'app

- Email : tgldivin@gmail.com
- WhatsApp : +229 01 64 40 94 45
- Don (Moov Money) : +229 01 94 81 83 24

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
