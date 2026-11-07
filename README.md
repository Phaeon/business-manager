# Business Manager

> Application SaaS de gestion pour TPE, indépendants et micro-entreprises : clients, devis, factures, projets, tâches, agenda et documents dans une seule interface.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring](https://img.shields.io/badge/Spring_Boot-3.3-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3-42b883)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED)

## Démarrage rapide (Docker)

```bash
cp .env.example .env
docker compose up --build
```

| Service    | URL                                   |
|------------|---------------------------------------|
| Application | http://localhost:5173                |
| API         | http://localhost:8080                |
| Swagger UI  | http://localhost:8080/swagger-ui.html |

Avec `APP_SEED_DEMO=true` (défaut dans `.env.example`), des données de démonstration sont créées au premier lancement :

| Compte              | Mot de passe | Rôle           |
|---------------------|--------------|----------------|
| `admin@demo.com`    | `Admin1234!` | Administrateur |
| `lea@demo.com`      | `Demo1234!`  | Employé        |
| `marc@demo.com`     | `Demo1234!`  | Comptabilité   |

> **Production** : mettez `APP_SEED_DEMO=false`, changez `ADMIN_PASSWORD` et `JWT_SECRET` (32 caractères minimum).

## Développement local (sans Docker)

Prérequis : Java 21, Maven, Node 20+, PostgreSQL 16 (base `business_manager`).

```bash
# Backend — les migrations Flyway créent le schéma au démarrage
cd backend && APP_SEED_DEMO=true mvn spring-boot:run

# Frontend — proxy /api -> localhost:8080
cd frontend && npm install && npm run dev
```

Tests : `cd backend && mvn verify` (rapport JaCoCo dans `target/site/jacoco`) et `cd frontend && npm test`.

## Fonctionnalités

| Domaine | Contenu |
|---|---|
| **Tableau de bord** | CA encaissé, reste à encaisser, devis et taux de transformation, factures en attente/payées, nouveaux clients, graphique mensuel (12 mois), répartition des projets, activité récente. Les employés voient à la place leurs tâches en cours. |
| **Clients** | CRUD, recherche, archivage / désarchivage, historique (devis, factures, projets), export CSV, pagination. |
| **Devis** | Lignes multiples (quantité, PU, TVA, remise), calcul HT / TVA / TTC, numérotation `DEV-2026-0001`, envoi, validation, refus, duplication, conversion en facture. |
| **Factures** | Génération depuis un devis ou à la main, paiements partiels et historique, annulation, passage automatique **En retard** (tâche quotidienne), **PDF**, export CSV. |
| **Projets & tâches** | Budget, dates, commentaires ; tâches (priorité, statut, estimation, temps passé, responsable) ; **progression calculée** à partir des tâches terminées. |
| **Kanban** | Glisser-déposer natif (HTML5) avec mise à jour optimiste. |
| **Calendrier** | Vue mensuelle : rendez-vous (CRUD), débuts/échéances de projets, échéances de factures. |
| **Recherche globale** | Clients, devis, factures, projets — filtrée selon le rôle. |
| **Documents** | Téléversement (10 Mo), catégories, rattachement à un client, téléchargement authentifié. |
| **Notifications** | Nouveau client / projet, devis accepté, paiement reçu, facture impayée, échéance de projet proche (J-3). |
| **Divers** | Mode sombre, gestion des utilisateurs (admin), changement de mot de passe, interface responsive. |

## Sécurité et rôles

- JWT **access** (15 min) + **refresh** (7 j). Un refresh token ne peut pas servir à appeler l'API ; l'access token expiré renvoie `401` et le frontend rafraîchit automatiquement.
- **Pas d'inscription publique** : un admin initial est créé au premier démarrage, les autres comptes sont créés par un administrateur.
- BCrypt, validation Bean Validation, gestion centralisée des erreurs, CORS configurable (`CORS_ORIGINS`).
- Export CSV protégé contre l'injection de formules.

| Ressource | Admin | Comptabilité | Employé |
|---|:-:|:-:|:-:|
| Clients (lecture) | ✅ | ✅ | ✅ |
| Clients (écriture / export) | ✅ | ✅ | ❌ |
| Devis, factures, statistiques | ✅ | ✅ | ❌ |
| Projets, tâches, Kanban | ✅ | ❌ | ✅ |
| Calendrier, documents, notifications | ✅ | ✅ | ✅ |
| Utilisateurs | ✅ | ❌ | ❌ |

## Architecture

```
backend/src/main/java/com/businessmanager/
├── controller/   Endpoints REST (Swagger)
├── service/      Logique métier, transactions
├── repository/   Spring Data JPA
├── entity/       Entités JPA (@SuperBuilder, audit createdAt/updatedAt)
├── dto/          Records de requête / réponse
├── mapper/       MapStruct (entité -> DTO)
├── security/     JWT, filtre, utilitaires de rôles
├── config/       Sécurité, CORS, OpenAPI, JPA, scheduling
├── exception/    Handler global -> réponses JSON cohérentes
├── bootstrap/    Admin initial + données de démonstration
└── utils/        PricingCalculator, CSV, fabrique de lignes
frontend/src/     views · components · stores (Pinia) · services (Axios) · router · utils
```

Migrations versionnées dans `backend/src/main/resources/db/migration`.

## Points à connaître

- **Notifications partagées** : une notification est visible (et « lue ») pour toute l'équipe, pas par utilisateur.
- **Numérotation** : séquence par année calculée à partir du plus grand numéro existant ; la contrainte `UNIQUE` protège d'un doublon en cas d'accès concurrent, mais deux créations simultanées peuvent en faire échouer une.
- **Tokens côté navigateur** : stockés dans `localStorage` (simple, mais exposé en cas de XSS). Pour un déploiement réel, privilégier des cookies `HttpOnly`.
- **Pas de révocation** des refresh tokens avant leur expiration (désactiver un utilisateur bloque cependant son accès).
- **Documents** : stockés sur disque (volume Docker) et servis en pièce jointe ; pas d'analyse antivirus.

## Évolutions possibles

WebSocket (notifications temps réel), signature électronique, Stripe, génération de devis par IA, synchronisation Google Calendar / Outlook, multi-entreprises, internationalisation FR/EN.

## Licence

Projet développé à des fins pédagogiques et de démonstration technique.
