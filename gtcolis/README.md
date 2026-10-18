# Grand Theft Colis VI — mod Minecraft 1.20.1 (Forge 47.0.10+)

Adaptation 3D du jeu navigateur « Grand Theft Colis VI ».

## Obtenir le .jar

Le projet est un projet Forge standard (ForgeGradle 6, mappings officiels). Il faut le compiler une fois.

### Option A — sans rien installer (GitHub)
1. Cree un depot GitHub (gratuit) et envoie-y **tout le contenu de ce dossier** (y compris `.github/`).
2. Onglet **Actions** > workflow « Build mod jar » (il se lance seul a chaque envoi, ou bouton *Run workflow*).
3. Quand c'est vert, ouvre l'execution > **Artifacts > gtcolis-jar** : le .jar est dedans (`gtcolis-1.0.0.jar`).

### Option B — en local
Prerequis : JDK 17 et Gradle 8.1.1 (ou copie `gradlew`, `gradlew.bat` et le dossier `gradle/` depuis le MDK Forge 1.20.1).
```
gradle build
```
Le .jar est dans `build/libs/`. Mets-le dans `mods/` (client **et** serveur) avec Forge 1.20.1-47.0.10 ou plus recent.
Pour CurseForge : upload du .jar tel quel (Minecraft 1.20.1, loader Forge).

Si la compilation affiche une erreur, copie-la moi : le code n'a pas pu etre compile dans mon environnement (voir plus bas).

## Jouer
1. A la premiere connexion tu recois le **Telephone** (3 vies).
2. Un OP (ou toi en solo) tape **`/gtc ville`** : Ohio City (10 x 7 immeubles, ~1-2 min) est construite dans la dimension `gtcolis:ohio_city` (nuit permanente).
3. **`/gtc ohio`** pour y aller.
4. Ramasse les **colis** qui brillent (+50 CASH chacun), livre-les a un **Point Relais** (bloc vert : clic droit ou reste a cote) : gain = colis² × 100.
5. Plus tu portes de colis : ralentissement + etoiles de **Douane** (colis/2). Les **Douaniers** te poursuivent, un contact = arrestation (perte des colis et d'une vie). Cache-toi (dans un batiment ou loin de eux) pour perdre des etoiles.
6. **Telephone** (touche Fleche haut, ou clic droit) : Missions, Carte, Taxi, Armes, Journal, Stats, Contacts, Reglages. Appels entrants aleatoires (repondre / refuser).

### Contenu repris du jeu
- Missions des PNJ « ! » : ramener des colis, aller chez un commerce, danser 6 s sur la piste du Club Ohio.
- Missions telephone : Course-poursuite, Filature, Braquage, Contrebande, Course de rue (en voiture), Escorte.
- Commerces (PNJ marchands) : Armurerie Sigma (batte, pistolet, fusil a pompe, mitraillette, munitions), Auto Skibidi (voiture), Garage Brainrot (peinture, carrosserie), Friperie Ohio, Casino Le Gyatt (machine a sous, des), Indic, Flic corrompu.
- Voiture conduisible (Z/S avancer-reculer, Q/D tourner, clic droit pour monter, fer pour reparer), qui renverse les passants. La cle appelle/fait apparaitre ta voiture.
- Evenements aleatoires : portefeuille perdu, colis tombe d'un camion, controle de la Douane (clic dans le chat), SMS de l'indic.
- 70 commerces nommes comme dans le jeu, 9 Points Relais aux memes intersections.

### Commandes
`/gtc ville`, `/gtc ohio`, `/gtc start`, `/gtc payer`, `/gtc fuir`, `/gtc reset`, et (OP) `/gtc cash <n>`, `/gtc stars <0-6>`, `/gtc mission <chase|tail|heist|smuggle|race|escort>`.
Config : `config/gtcolis-common.toml` (colis partout, douane, appels, evenements).

## Etat / limites honnetes
- Le code n'a **pas** pu etre compile ni teste en jeu ici (pas d'acces au Maven de Forge). Il parse correctement, mais des erreurs d'API de 1.20.1 sont possibles : envoie-moi le log de build et je corrige.
- Les textures sont generees par code (pixel-art simple), a remplacer si tu veux mieux.
- Les missions sont gardees en memoire serveur (perdues au redemarrage). Pas de loot tables sur les blocs.
