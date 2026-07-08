# 📊 Notes de Présentation — Projet Stuffing

---

## Slide : Problématique

1. La planification du staffing chez Sopra HR se fait **manuellement** (fichiers Excel, emails) → processus lent, erreurs fréquentes, manque de visibilité en temps réel.

2. Les Delivery Managers n'ont **aucun outil centralisé** pour visualiser la disponibilité des collaborateurs, leurs compétences, et les affecter aux projets de manière optimale.

3. L'absence d'un système intelligent rend difficile la **prise de décision rapide** (qui est disponible ? quelle compétence manque ? quel est le taux d'occupation ?).

**Question :**
> *Comment digitaliser et optimiser la planification du staffing grâce à une plateforme intelligente ?*

---

## Slide : Analyse et critique de l'existant

| Outil existant | Description | Limites |
|---|---|---|
| Fichiers Excel partagés | Tableaux de suivi des affectations | Pas de temps réel, conflits de versions, pas de notifications |
| Emails / réunions | Coordination entre managers et RH | Lenteur, perte d'information, pas de traçabilité |
| Outils génériques (MS Teams, Jira) | Non dédiés au staffing | Pas de vue consolidée des disponibilités ni des compétences |

### Limites identifiées :
        - ❌ Aucune vue temps réel sur la disponibilité des collaborateurs
- ❌ Pas d'historique ni de KPIs (taux d'occupation, absentéisme)
❌ Pas d'aide à la décision intelligente (matching compétences/projets)
- ❌ Processus de demande de congé et d'accès non digitalisés

---

## Slide : Solution proposée

**"Stuffing — Plateforme intelligente de planification du staffing"**

| Composant | Technologie | Fonctionnalité |
|-----------|-------------|----------------|
| Frontend | Angular 19 | Dashboards par rôle (Admin, Manager, Collaborateur) |
| Backend | Spring Boot 3 + Java 21 | API REST, logique métier, gestion des rôles |
| Authentification | Keycloak (OAuth2/OIDC) | SSO, gestion des accès, rôles sécurisés |
| Base de données | PostgreSQL | Persistance des données |
| Chatbot IA | Python (microservice) | Assistance intelligente au manager (suggestions staffing) |

### Fonctionnalités clés :
- ✅ Gestion des demandes d'accès (workflow Admin → email automatique)
- ✅ Affectation des collaborateurs aux projets (Manager)
- ✅ Gestion des congés (soumission → approbation → notification)
- ✅ Calendrier interactif (FullCalendar)
- ✅ KPIs & rapports (taux d'occupation, absentéisme, distribution compétences)
- ✅ Chatbot IA pour aide à la décision staffing
- ✅ Notifications temps réel in-app
- ✅ Authentification sécurisée via SSO (Keycloak)
- ✅ partie Analytics pour le manager 

---

## Slide : Besoins fonctionnels

| # | Besoin fonctionnel | Acteur |
|---|---|---|
| BF1 | Soumettre une demande d'accès à la plateforme | Nouveau collaborateur |
| BF2 | Approuver/Rejeter les demandes d'accès + envoi email automatique | Admin |
| BF3 | Gérer les utilisateurs (activer, désactiver, supprimer) | Admin |
| BF4 | Consulter les rapports et KPIs (absentéisme, taux d'occupation) | Admin |
| BF5 | Visualiser le tableau de bord avec les indicateurs de l'équipe | Delivery Manager |
| BF6 | Créer des projets et affecter des collaborateurs | Delivery Manager |
| BF7 | Approuver/Rejeter les demandes de congé de l'équipe | Delivery Manager |
| BF8 | Consulter le planning de l'équipe (calendrier) | Delivery Manager |
| BF9 | Interagir avec le chatbot IA pour aide à la décision | Delivery Manager |
| BF10 | Consulter son tableau de bord personnel | Collaborateur |
| BF11 | Soumettre / annuler une demande de congé | Collaborateur |
| BF12 | Consulter ses affectations et son planning | Collaborateur |
| BF13 | Gérer son profil et ses compétences | Collaborateur |
| BF14 | Recevoir des notifications en temps réel | Tous les acteurs |
| BF15 | S'authentifier via SSO (Keycloak) | Tous les acteurs |

---

## Slide : Besoins non fonctionnels

| # | Besoin non fonctionnel | Description |
|---|---|---|
| BNF1 | **Sécurité** | Authentification OAuth2/OIDC via Keycloak, contrôle d'accès par rôle (RBAC), tokens JWT |
| BNF2 | **Performance** | Temps de réponse < 2s, pool de connexions HikariCP, cache JWKS |
| BNF3 | **Ergonomie** | Interface responsive, dashboards intuitifs par rôle, UX moderne |
| BNF4 | **Maintenabilité** | Architecture en couches, code modulaire, séparation frontend/backend |
| BNF5 | **Disponibilité** | Application accessible en continu (serveur local pour le PFE) |
| BNF6 | **Extensibilité** | Architecture permettant l'ajout de nouveaux modules (microservice chatbot séparé) |
| BNF7 | **Confidentialité** | Mots de passe hashés (BCrypt), emails professionnels générés, données personnelles protégées |
| BNF8 | **Compatibilité** | Compatible tous navigateurs modernes (Chrome, Firefox, Edge) |

---

## Slide : Conclusion

1. ✅ **Objectif atteint** — Une plateforme fonctionnelle qui digitalise la planification du staffing avec des dashboards par rôle, gestion des congés, affectations et notifications.

2. 🤖 **Valeur ajoutée IA** — Intégration d'un chatbot intelligent qui assiste le Delivery Manager dans ses décisions de staffing (disponibilité, compétences, suggestions).

3. 🚀 **Perspectives** — Déploiement en production, ajout de rapports avancés (BI), extension mobile, et enrichissement du dataset IA pour des recommandations plus précises.
