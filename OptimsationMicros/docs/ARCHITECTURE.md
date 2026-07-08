# Documentation Technique - Moteur d'Optimisation (OR-Tools)

## Plateforme Stuffing - Architecture Globale

---

## 1. Présentation Générale

**Stuffing** est une plateforme de gestion de staffing RH composée de :

| Composant | Technologie | Rôle |
|-----------|-------------|------|
| Frontend | Angular | Interface utilisateur (Dashboard Manager, Admin RH, Collaborateur) |
| Backend | Spring Boot | API REST, gestion métier, authentification |
| Base de données | PostgreSQL | Stockage des données applicatives |
| Chatbot RAG | Python (LangChain/LLM) | Interrogation des données RH en langage naturel |
| Moteur d'optimisation | Python (OR-Tools + FastAPI) | Résolution des problèmes d'affectation optimale |

---

## 2. Architecture Globale

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ANGULAR FRONTEND                              │
│                    (Dashboard Manager)                               │
│                                                                     │
│  ┌──────────────┐   ┌──────────────────┐   ┌───────────────────┐   │
│  │ Gestion RH   │   │ Chat Interface   │   │ Résultats Optim.  │   │
│  └──────┬───────┘   └────────┬─────────┘   └────────┬──────────┘   │
└─────────┼────────────────────┼───────────────────────┼──────────────┘
          │                    │                       │
          ▼                    ▼                       ▼
┌──────────────────┐  ┌────────────────┐    ┌──────────────────────┐
│  SPRING BOOT     │  │  CHATBOT RAG   │    │  MOTEUR OPTIMISATION │
│  (Backend API)   │  │  (Python)      │    │  (OR-Tools + FastAPI)│
│                  │  │                │    │                      │
│  - Collaborateurs│  │  - Intent      │    │  - Solveur CP-SAT    │
│  - Projets       │  │    Detection   │◄───│  - Contraintes       │
│  - Congés        │  │  - RAG Pipeline│    │  - Score global      │
│  - Affectations  │  │  - Réponse NL  │    │  - Plan optimal      │
└────────┬─────────┘  └───────┬────────┘    └──────────┬───────────┘
         │                    │                        │
         ▼                    ▼                        ▼
┌──────────────────┐  ┌─────────────────────────────────────────────┐
│   POSTGRESQL     │  │         DATASET HR ENRICHI                   │
│   (Données app)  │  │   (CSV / Vectorstore - Données fictives)     │
│                  │  │                                               │
│  - Users         │  │   Colonnes :                                  │
│  - Projects      │  │   - Department, JobRole, MainDomain          │
│  - Leaves        │  │   - Skills, Seniority, Certifications        │
│  - Assignments   │  │   - ProjectHistory, AttritionReason          │
│                  │  │   - AbsenceDaysLast12Months, MainAbsenceType │
└──────────────────┘  └─────────────────────────────────────────────┘
```

---

## 3. Relation entre les Microservices

### 3.1 Sources de données

| Microservice | Source de données | Explication |
|---|---|---|
| Spring Boot | PostgreSQL | Données applicatives (comptes, projets, congés) |
| Chatbot RAG | Dataset HR enrichi (fictif) | Données RH nettoyées et enrichies pour le RAG |
| Moteur Optimisation | **Dual** : Dataset HR + PostgreSQL | Selon le mode d'appel (chatbot ou direct) |

### 3.2 Deux sources de données selon le mode d'appel

| Mode | Source de données | Cas d'usage |
|---|---|---|
| Via Chatbot | Dataset HR enrichi (CSV) | Le chatbot détecte l'intent optimisation et appelle le moteur avec les données enrichies |
| Via Angular/Spring Boot | PostgreSQL (données réelles) | Le manager utilise le dashboard Angular qui appelle Spring Boot, qui appelle le moteur avec les données réelles |

**Justification du dual-source :**
- Le dataset HR enrichi offre des colonnes riches (skills détaillés, certifications, historique projets) pour le chatbot
- PostgreSQL contient les données réelles et à jour de l'application (collaborateurs, projets actifs, congés validés)
- Le moteur abstrait la source via un **DataProvider** commun

### 3.3 Communication entre services

```
┌──────────────────────────────────────────────────────────────┐
│                    FLUX D'APPEL                                │
│                                                              │
│  Manager: "Affecte les meilleurs profils au projet Alpha"    │
│           (Java, Senior, disponible)                         │
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────┐                     │
│  │  CHATBOT RAG                         │                     │
│  │  1. Détecte intent = optimization    │                     │
│  │  2. Extrait paramètres (skills,      │                     │
│  │     seniority, contraintes)          │                     │
│  └──────────────┬──────────────────────┘                     │
│                 │                                            │
│                 ▼  POST /api/v1/optimize                     │
│  ┌─────────────────────────────────────┐                     │
│  │  MOTEUR OR-TOOLS                     │                     │
│  │  1. Lit le dataset HR                │                     │
│  │  2. Formule le problème (variables,  │                     │
│  │     contraintes, objectifs)          │                     │
│  │  3. Résout avec CP-SAT solver        │                     │
│  │  4. Retourne plan optimal + score    │                     │
│  └──────────────┬──────────────────────┘                     │
│                 │                                            │
│                 ▼  JSON Response                             │
│  ┌─────────────────────────────────────┐                     │
│  │  CHATBOT RAG                         │                     │
│  │  Formule la réponse en langage       │                     │
│  │  naturel avec les résultats          │                     │
│  └──────────────┬──────────────────────┘                     │
│                 │                                            │
│                 ▼                                            │
│  Manager reçoit: "Je recommande Ali (score 92%),            │
│  Sara (87%)... car compétences Java Senior et disponibles"  │
└──────────────────────────────────────────────────────────────┘
```

---

## 4. Moteur d'Optimisation - Spécifications

### 4.1 Nature du moteur

OR-Tools est un **solveur mathématique** (programmation par contraintes), **PAS** du Machine Learning :
- ❌ Pas d'entraînement
- ❌ Pas de données historiques nécessaires
- ✅ Résolution exacte en temps réel
- ✅ Données provenant du dataset partagé

### 4.2 Entrées

| Paramètre | Description | Source |
|---|---|---|
| Compétences requises | Skills demandées par le projet | Dataset HR / Requête manager |
| Disponibilités | Jours disponibles des collaborateurs | Dataset HR (AbsenceDays) |
| Congés | Absences planifiées | Dataset HR (MainAbsenceType) |
| Coûts | Coût journalier par collaborateur | Dataset HR (Seniority → coût) |
| Priorités projets | Pondération des projets | Paramètre d'entrée |

### 4.3 Contraintes

| Contrainte | Description |
|---|---|
| FTE max | Un collaborateur ne peut pas dépasser 1.0 FTE |
| Compétences | Le collaborateur doit posséder les skills requises |
| Disponibilité | Le collaborateur doit être disponible sur la période |
| Objectifs pondérés | Maximiser TNF, minimiser coûts, équilibrer la charge |

### 4.4 Sorties

```json
{
  "plan": [
    {
      "collaborateur": "Ali Ben Ahmed",
      "projet": "Projet Alpha",
      "allocation_percentage": 80,
      "skills_match_score": 0.92,
      "period": "2026-06-01 / 2026-08-31"
    }
  ],
  "global_score": 87.5,
  "metrics": {
    "tnf_score": 90,
    "cost_score": 82,
    "balance_score": 88
  }
}
```

---

## 5. Endpoints API du Moteur

| Méthode | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/optimize` | Lancer une optimisation d'affectation |
| POST | `/api/v1/optimize/batch` | Optimisation multi-projets |
| GET | `/api/v1/health` | Health check |
| GET | `/api/v1/metrics` | Métriques du dernier run |

---

## 6. Stack Technique du Moteur

| Composant | Technologie |
|---|---|
| Framework API | FastAPI (Python) |
| Solveur | Google OR-Tools (CP-SAT) |
| Validation données | Pydantic |
| Dataset | Pandas (lecture CSV/Parquet) |
| Containerisation | Docker |
| Communication | REST API (JSON) |

---

## 7. Modes d'accès au Moteur

Le moteur peut être appelé de **deux manières** :

### 7.1 Via le Chatbot (indirect) — Source : Dataset HR
```
Manager → Chatbot → [détecte intent optimisation] → Moteur OR-Tools (dataset HR) → Chatbot → Manager
```
Le chatbot détecte l'intent d'optimisation, extrait les paramètres, et appelle le moteur avec `source: "dataset"`.

### 7.2 Via Angular/Spring Boot (direct) — Source : PostgreSQL
```
Manager → Angular Dashboard → Spring Boot API → Moteur OR-Tools (PostgreSQL) → Spring Boot → Angular
```
Spring Boot récupère les données réelles (collaborateurs, projets, congés) depuis PostgreSQL et les envoie au moteur avec `source: "postgres"`. Affichage visuel des résultats (tableaux, graphiques d'allocation).

---

## 8. Rôles utilisateurs concernés

| Rôle | Interaction avec le moteur |
|---|---|
| Delivery Manager | Demande d'optimisation via chatbot ou dashboard |
| Admin RH | Configuration des contraintes et pondérations |
| Collaborateur | Aucune interaction directe |

---

## 9. Décisions d'architecture

| Décision | Justification |
|---|---|
| Dual-source (Dataset + Postgres) | Dataset enrichi pour le chatbot, Postgres pour les données réelles du dashboard |
| DataProvider abstrait | Permet de switcher entre CSV et Postgres sans changer la logique d'optimisation |
| FastAPI (pas Spring Boot) | OR-Tools natif Python, performance optimale |
| Communication REST | Simplicité, découplage, indépendance des services |
| Chatbot comme orchestrateur | UX naturelle pour le manager, réponses argumentées |
| Spring Boot comme passerelle | Angular n'appelle pas le moteur directement, passe par Spring Boot qui enrichit la requête avec les données Postgres |

---

*Document créé le 04/06/2026 - Projet Stuffing - Moteur d'Optimisation*
