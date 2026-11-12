# Site des applications FASTEK

Site statique (HTML/CSS) hébergé sur Netlify : catalogue des applications, pages de téléchargement de **FastNotea** (élèves) et **NoteaTrick** (professeurs), pages légales de chacune.

- Déploiement : Netlify, dépôt `TGLD1/Notea`, répertoire de base `site-apps`, répertoire de publication `.`. La commande de build (voir `netlify.toml`) remplace `__SITE_URL__` par l'adresse principale du site dans les adresses canoniques, les images de partage, `sitemap.xml` et `robots.txt`.
- Ancienne adresse : `/notea/…` redirige définitivement vers `/fastnotea/…` (fichier `_redirects`).
- Téléchargement de FastNotea : la page lit la dernière release GitHub et pointe vers `FastNotea.apk`.
- Téléchargement de NoteaTrick : la page cherche la dernière release dont l'étiquette commence par `trick-v` (fichier `NoteaTrick.apk`) ; sans release, elle affiche « Bientôt disponible ». Les releases NoteaTrick ne sont jamais marquées « dernière release », pour ne pas perturber le lien de FastNotea.
- Référencement : titres et descriptions uniques, adresses canoniques, balises de partage (Open Graph, Twitter), données structurées schema.org (organisation, application, questions fréquentes), `sitemap.xml`, `robots.txt`, icônes et manifeste.
