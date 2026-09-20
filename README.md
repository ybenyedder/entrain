# EnTrain

Application Android **native** (Kotlin + Jetpack Compose) d'horaires et de temps réel
des trains SNCF — **sans aucun traceur**.

> Clone fonctionnel de l'expérience SNCF Connect (recherche de trajets, horaires
> temps réel, perturbations, tableaux de gare) sans compte, sans publicité, sans
> analytique, sans SDK tiers. La seule permission demandée est `INTERNET`.

## Fonctionnalités

- **Recherche d'itinéraires** porte-à-porte train (TGV INOUI, OUIGO, INTERCITÉS
  de jour et de nuit, TER, Tram-Train, cars TER, Eurostar, Lyria, ICE…) avec
  correspondances, jusqu'à 4 étapes, sur 6 mois d'horaires, en mode « partir à » ou « arriver à ». Tri par
  départ / arrivée / durée.
- **Temps réel national** : retards, horaires estimés, trains supprimés —
  rafraîchi automatiquement chaque minute, à chaque retour sur l'app et au
  tirez-pour-rafraîchir.
- **Horaires auto-actualisés (v0.4)** : la base de circulation se met à jour
  toute seule en arrière-plan quand la SNCF publie une nouvelle version
  (contrôle quotidien, téléchargement si la base a plus de 4 jours).
- **Temps réel Transilien/RER (v0.2)** : passages en gare d'Île-de-France via la
  plateforme open data PRIM d'IDFM. Optionnel : il faut y coller votre clé API
  gratuite personnelle dans Réglages (rien d'autre n'en dépend).
- **Recherche « arriver à » (v0.3)** : mode Arriver à — les trajets aboutissant
  avant l'heure cible, du dernier départ possible au plus tôt.
- **Voyage en cours (v0.3)** : suivez votre train (bouton « Suivre » sur un
  trajet ou un départ) — prochain arrêt, barre de progression entre gares,
  arrivée estimée à votre gare, retard en direct, timeline du parcours ;
  persiste entre les lancements.
- **Perturbations liées au train (v0.4)** : les alertes touchant votre trajet
  s'affichent sur le détail et sur le voyage en cours.
- **Quai/voie (v0.4)** : affiché pour les trains Île-de-France quand IDFM le
  fournit (les flux nationaux SNCF ne le publient pas).
- **Accessibilité (v0.3)** : descriptions complètes pour TalkBack sur les
  cartes de trajet, tableaux de départs et étapes du voyage en cours.
- **Perturbations** (travaux, grèves, infos trafic) triées par importance.
- **Tableaux de départs/arrivées** par gare, façon panneau d'affichage.
- **Favoris, gares récentes, gares principales** — tout en local sur l'appareil.
- **Mode sombre** complet, animations d'entrée décalées, typographie Inter
  (embarquée), pull-to-refresh partout où c'est utile.

## D'où viennent les données ?

100 % open data, téléchargées **anonymement** (pas de compte, pas d'identifiant,
pas de cookie) :

| Donnée | Source |
| --- | --- |
| Horaires théoriques 151 jours (GTFS, ~4 Mo) | [SNCF open data](https://data.sncf.com) / [transport.data.gouv.fr](https://transport.data.gouv.fr) |
| Temps réel retards + suppressions (GTFS-RT) | [transport.data.gouv.fr](https://transport.data.gouv.fr) |
| Perturbations (GTFS-RT Service Alerts) | [transport.data.gouv.fr](https://transport.data.gouv.fr) |
| Passages Transilien/RER (SIRI-Lite, clé perso) | [PRIM Île-de-France Mobilités](https://prim.iledefrance-mobilites.fr) |
| Correspondance gares SNCF ↔ arrêts IDFM | GTFS Transilien (appariement géographique à l'import) |

Licence Etalab 2.0. EnTrain est une application indépendante, non affiliée à la
SNCF ; le nom et les logos SNCF ne sont pas réutilisés.

## Zéro traceur — vérifié

- Aucune dépendance analytics/ads/push : uniquement `androidx.*`, Kotlin,
  OkHttp, Room, protobuf-lite.
- L'APK release ne contient aucune signature de traceur (Firebase, GMS,
  AppsFlyer, Adjust, Facebook, AT Internet… : 0 occurrence) et demande
  uniquement la permission `INTERNET`.
- Les favoris et l'historique restent dans la base locale de l'appareil.
- La clé PRIM, si vous en configurez une, reste dans le téléphone et ne sert
  qu'à appeler le service d'Île-de-France Mobilités.
- L'achat de billet n'est pas intégré (aucune API publique n'existe) : le bouton
  « Acheter » ouvre le site SNCF Connect dans le navigateur, sans lui transmettre
  la moindre donnée.

## Architecture

```
app/src/main/kotlin/fr/webtvmedia/entrain/
├── data/
│   ├── db/          Room : stops, trips, stop_times, calendar_dates, favoris, pont IDFM
│   ├── gtfs/        Import GTFS (zip + CSV → Room) + pont géo OCE↔IDFM
│   ├── rt/          Flux GTFS-RT protobuf nationaux (trip updates + alerts)
│   ├── prim/        Temps réel IDFM (SIRI-Lite stop-monitoring, clé perso)
│   └── AppContainer.kt  DI manuelle, index en mémoire
├── domain/
│   ├── model/       Journey, Leg, Departure, AlertInfo, TrainCategory
│   └── routing/     TimetableIndex (compact) + RaptorRouter (RAPTOR)
├── ui/              Compose : Accueil, Résultats (+tri), Détail, Gare, Trafic, Réglages
└── util/            Temps (Europe/Paris), normalisation accents, HTML→texte
```

Le moteur de calcul (RAPTOR adapté, correspondances intra-gare à 5 min) tourne
**sur l'appareil** : aucune requête n'est nécessaire pour chercher un itinéraire
une fois les horaires importés.

## Build

```bash
./gradlew assembleDebug          # APK debug
./gradlew assembleRelease        # APK release signé (keystore local)
./gradlew testDebugUnitTest      # tests unitaires (routage, parsing, PRIM, temps)
```

Prérequis : JDK 17, Android SDK 35. La signature release utilise un keystore
local (`entrain-release.keystore`, non versionné) : renseignez
`entrain.storePassword` / `entrain.keyAlias` / `entrain.keyPassword` dans
`local.properties`. Sans keystore, `assembleRelease` produit un APK non signé.

## Limites connues

- Pas d'achat/e-billet (voir ci-dessus), pas de porte-à-porte multimodal
  (métro/bus), pas de quai/voie (donnée non publiée en open data).
- Le temps réel Transilien/RER exige une clé PRIM gratuite (limite de quota
  ~1 000 requêtes/jour pour les comptes récents) ; sans clé, tout le reste
  fonctionne à l'identique.
- Le temps réel national GTFS-RT a la fraîcheur de la donnée publique
  (rafraîchie toutes les ~30 s côté producteur).

