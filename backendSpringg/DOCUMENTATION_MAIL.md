# 📧 Documentation du Système d'Envoi de Mail - Projet Stuffing

## 📋 Table des matières
1. [Configuration SMTP](#1-configuration-smtp)
2. [Dépendances Maven](#2-dépendances-maven)
3. [Service d'envoi d'emails](#3-service-denvoi-demails)
4. [Logique d'envoi automatique lors de l'approbation](#4-logique-denvoi-automatique-lors-de-lapprobation)
5. [Résumé du flux complet](#5-résumé-du-flux-complet)

---

## 1. Configuration SMTP

### 📁 Fichier : `src/main/resources/application.properties`

```properties
# === Mail ===
app.mail.enabled=true
app.mail.from=no-reply@sopraHr.com

# SMTP Gmail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=mohamednegzaoui8@gmail.com
spring.mail.password=tbrm qzxz gdmd uggp

spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Frontend URL (for email login link)
app.frontend-url=http://localhost:4200/login
```

### 🔧 Explication de la configuration :

| Propriété | Valeur | Description |
|-----------|--------|-------------|
| `app.mail.enabled` | `true` | Active/désactive l'envoi d'emails (mode dev/prod) |
| `app.mail.from` | `no-reply@sopraHr.com` | Adresse email de l'expéditeur |
| `spring.mail.host` | `smtp.gmail.com` | Serveur SMTP utilisé (Gmail) |
| `spring.mail.port` | `587` | Port SMTP avec STARTTLS |
| `spring.mail.username` | `mohamednegzaoui8@gmail.com` | Compte Gmail utilisé |
| `spring.mail.password` | `tbrm qzxz gdmd uggp` | **Mot de passe d'application Gmail** (pas le mot de passe normal) |
| `mail.smtp.auth` | `true` | Active l'authentification SMTP |
| `mail.smtp.starttls.enable` | `true` | Active le chiffrement TLS |
| `app.frontend-url` | `http://localhost:4200/login` | URL du frontend pour le lien de connexion |

> ⚠️ **Important** : Le mot de passe utilisé ici est un **mot de passe d'application Gmail**, pas votre mot de passe Gmail habituel.

---

## 2. Dépendances Maven

### 📁 Fichier : `pom.xml`

```xml
<!-- Mail (envoi d'emails: reset password, etc.) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

Cette dépendance Spring Boot fournit :
- `JavaMailSender` : Interface pour envoyer des emails
- `SimpleMailMessage` : Classe pour créer des emails simples
- Toute la configuration automatique SMTP

---

## 3. Service d'envoi d'emails

### 📁 Fichier : `src/main/java/com/negzaoui/stuffing/service/EmailService.java`

```java
package com.negzaoui.stuffing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envoi d'emails.
 * Toutes les methodes sont @Async pour ne pas bloquer la requete HTTP.
 * Mode PFE/dev: si app.mail.enabled=false, on log le contenu au lieu d'envoyer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@sopraHr.com}")
    private String from;

    private JavaMailSender getMailSenderOrNull() {
        return mailSenderProvider.getIfAvailable();
    }

    @Async
    public void sendPasswordResetEmail(String to, String resetLinkOrToken) {
        String subject = "Réinitialisation de mot de passe";
        String body = "Bonjour,\n\n" +
                "Voici votre lien (ou token) pour réinitialiser votre mot de passe :\n" +
                resetLinkOrToken + "\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.";

        if (!mailEnabled) {
            // Dev/PFE: pas de SMTP configuré, on affiche dans les logs
            log.warn("[MAIL DISABLED] To={}, Subject={}, Body=\n{}", to, subject, body);
            return;
        }

        JavaMailSender mailSender = getMailSenderOrNull();
        if (mailSender == null) {
            log.warn("[MAIL NOT CONFIGURED] Impossible d'envoyer un email (JavaMailSender absent). To={}, Subject={}", to, subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    @Async
    public void sendAccountRequestReceived(String requesterEmail) {
        String subject = "Demande de création de compte reçue";
        String body = "Bonjour,\n\n" +
                "Votre demande de création de compte a été reçue et sera traitée par l'administrateur.\n" +
                "Vous serez contacté(e) dès que votre compte sera créé.\n\n" +
                "Email demandé: " + requesterEmail + "\n";

        if (!mailEnabled) {
            log.warn("[MAIL DISABLED] To={}, Subject={}, Body=\n{}", requesterEmail, subject, body);
            return;
        }

        JavaMailSender mailSender = getMailSenderOrNull();
        if (mailSender == null) {
            log.warn("[MAIL NOT CONFIGURED] Impossible d'envoyer un email (JavaMailSender absent). To={}, Subject={}", requesterEmail, subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(requesterEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    /**
     * ⭐ EMAIL PRINCIPAL D'APPROBATION ⭐
     * Envoie le mail de bienvenue sur l'email PERSONNEL du demandeur,
     * en lui communiquant son email PROFESSIONNEL (@soprahr.com) et son mot de passe temporaire.
     *
     * @param personalEmail     email personnel du demandeur (destinataire du mail)
     * @param professionalEmail email professionnel généré (prenom.nom@soprahr.com)
     * @param temporaryPassword mot de passe temporaire
     * @param loginUrl          URL de connexion (frontend)
     */
    @Async
    public void sendAccountApprovedEmail(String personalEmail, String professionalEmail,
                                         String temporaryPassword, String loginUrl) {
        String subject = "🎉 Votre compte Sopra HR Stuffing a été créé";
        String body = "Bonjour,\n\n" +
                "Votre demande d'accès à la plateforme Stuffing a été approuvée !\n\n" +
                "Voici vos identifiants professionnels :\n\n" +
                "🔗 URL de connexion : " + (loginUrl != null ? loginUrl : "http://localhost:4200/login") + "\n" +
                "📧 Email professionnel : " + professionalEmail + "\n" +
                "🔐 Mot de passe temporaire : " + temporaryPassword + "\n\n" +
                "⚠️  À votre première connexion, vous devrez obligatoirement changer votre mot de passe.\n\n" +
                "📌 Conservez bien votre email professionnel, c'est votre identifiant de connexion.\n\n" +
                "Bienvenue dans l'équipe Sopra HR ! 🚀\n\n" +
                "—\n" +
                "L'équipe Stuffing";
        String to = personalEmail;

        if (!mailEnabled) {
            log.warn("[MAIL DISABLED] To={}, Subject={}, Body=\n{}", to, subject, body);
            return;
        }

        JavaMailSender mailSender = getMailSenderOrNull();
        if (mailSender == null) {
            log.warn("[MAIL NOT CONFIGURED] Impossible d'envoyer un email (JavaMailSender absent). To={}, Subject={}", to, subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    /**
     * Surcharge pour rétrocompatibilité (sans loginUrl).
     */
    @Async
    public void sendAccountApprovedEmail(String personalEmail, String professionalEmail, String temporaryPassword) {
        sendAccountApprovedEmail(personalEmail, professionalEmail, temporaryPassword, null);
    }

    @Async
    public void sendAccountRejectedEmail(String to, String reason) {
        String subject = "Demande de création de compte";
        String body = "Bonjour,\n\n" +
                "Votre demande de création de compte a été rejetée." +
                (reason == null || reason.isBlank() ? "" : ("\nRaison: " + reason)) +
                "\n\nSi vous pensez qu'il s'agit d'une erreur, veuillez contacter l'administrateur.";

        if (!mailEnabled) {
            log.warn("[MAIL DISABLED] To={}, Subject={}, Body=\n{}", to, subject, body);
            return;
        }

        JavaMailSender mailSender = getMailSenderOrNull();
        if (mailSender == null) {
            log.warn("[MAIL NOT CONFIGURED] Impossible d'envoyer un email (JavaMailSender absent). To={}, Subject={}", to, subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
```

### 🔑 Points clés du EmailService :

1. **@Async** : Toutes les méthodes sont asynchrones pour ne pas bloquer la requête HTTP
2. **Mode dev/prod** : Si `app.mail.enabled=false`, les emails sont juste loggés (pas envoyés)
3. **Gestion d'erreurs** : Si JavaMailSender n'est pas disponible, on log un warning
4. **ObjectProvider** : Permet de gérer l'absence optionnelle de JavaMailSender

---

## 4. Logique d'envoi automatique lors de l'approbation

### 📁 Fichier : `src/main/java/com/negzaoui/stuffing/service/AccountCreationRequestService.java`

#### Extrait de la méthode `approve()` (ligne 88-203)

```java
@Transactional
public User approve(Long requestId, Role role, String temporaryPassword, Authentication processedBy) {
    var req = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

    if (req.getStatus() != AccountRequestStatus.PENDING) {
        throw new IllegalStateException("Demande déjà traitée");
    }

    // ═══════════════════════════════════════════════════════
    // 0. Générer l'email PROFESSIONNEL (@soprahr.com)
    // ═══════════════════════════════════════════════════════
    String professionalEmail = generateProfessionalEmail(req.getFirstName(), req.getLastName());
    String personalEmail = req.getEmail(); // email personnel fourni dans la demande

    // Si le compte existe déjà localement (avec cet email pro), on valide sans recréer.
    if (userRepository.existsByEmail(professionalEmail)) {
        req.setStatus(AccountRequestStatus.APPROVED);
        req.setProcessedAt(Instant.now());
        req.setProcessedBy(processedBy != null ? processedBy.getName() : null);
        requestRepository.save(req);
        log.warn("⚠️  Le user {} existait déjà en BD locale. Demande marquée APPROVED sans recréation.", professionalEmail);
        return userRepository.findByEmail(professionalEmail).orElseThrow();
    }

    // Générer un mot de passe temporaire si non fourni
    String rawPassword = (temporaryPassword == null || temporaryPassword.isBlank())
            ? generateTempPassword()
            : temporaryPassword;

    // ═══════════════════════════════════════════════════════
    // 1. Créer (ou réutiliser) le user dans Keycloak avec l'email PRO
    // ═══════════════════════════════════════════════════════
    String keycloakId;
    try {
        String existingId = keycloakAdminService.findUserIdByEmail(professionalEmail);
        if (existingId != null) {
            log.warn("⚠️  Le user {} existe déjà dans Keycloak (id={}). Réinitialisation du mdp.", professionalEmail, existingId);
            keycloakAdminService.setUserPassword(existingId, rawPassword, true);
            keycloakAdminService.assignRealmRole(existingId, role.name());
            keycloakId = existingId;
        } else {
            keycloakId = keycloakAdminService.createUser(
                    professionalEmail, // ← email pro comme username/email Keycloak
                    req.getFirstName(),
                    req.getLastName(),
                    rawPassword,
                    role
            );
        }
    } catch (Exception e) {
        log.error("❌ Échec création user Keycloak pour {} : {}", professionalEmail, e.getMessage(), e);
        throw new RuntimeException("Impossible de créer le compte dans Keycloak : " + e.getMessage());
    }

    // ═══════════════════════════════════════════════════════
    // 2. Créer le user en BD locale avec l'email PRO
    // ═══════════════════════════════════════════════════════
    var user = User.builder()
            .firstName(req.getFirstName())
            .lastName(req.getLastName())
            .email(professionalEmail)        // ← email pro = identifiant
            .personalEmail(personalEmail)     // ← email personnel conservé
            .password(passwordEncoder.encode(rawPassword))
            .keycloakId(keycloakId)
            .role(role)
            .active(true)
            .build();

    userRepository.save(user);
    log.info("✅ User créé en BD locale : {} (email pro={}, email perso={}, keycloakId={})",
            user.getEmail(), professionalEmail, personalEmail, keycloakId);

    // ═══════════════════════════════════════════════════════
    // 2b. Créer un EmployeeProfile vide pour le nouveau user
    // ═══════════════════════════════════════════════════════
    EmployeeProfile profile = EmployeeProfile.builder()
            .user(user)
            .phone(req.getPhone())
            .department(null)
            .build();
    employeeProfileRepository.save(profile);
    log.info("✅ EmployeeProfile créé pour {}", professionalEmail);

    // ═══════════════════════════════════════════════════════
    // 3. Mettre à jour la demande
    // ═══════════════════════════════════════════════════════
    req.setStatus(AccountRequestStatus.APPROVED);
    req.setProcessedAt(Instant.now());
    req.setProcessedBy(processedBy != null ? processedBy.getName() : null);
    requestRepository.save(req);

    // ═══════════════════════════════════════════════════════
    // 4. ⭐ ENVOI EMAIL AUTOMATIQUE ⭐
    // ═══════════════════════════════════════════════════════
    try {
        emailService.sendAccountApprovedEmail(personalEmail, professionalEmail, rawPassword, frontendUrl);
    } catch (Exception e) {
        log.warn("Echec envoi email pour {} : {} (le compte a ete cree quand meme)", personalEmail, e.getMessage());
    }

    // ═══════════════════════════════════════════════════════
    // 5. Notification in-app
    // ═══════════════════════════════════════════════════════
    try {
        notificationService.createNotification(
                "Bienvenue ! Votre compte a été créé. Votre identifiant : " + professionalEmail,
                "ACCOUNT_APPROVED",
                user
        );
    } catch (Exception e) {
        log.warn("Echec notification in-app pour {} : {}", user.getEmail(), e.getMessage());
    }

    return user;
}
```

### 🎯 Points importants de la logique :

1. **Email personnel vs professionnel** :
   - `personalEmail` = Email fourni dans la demande (ex: `mohamed.negzaoui@gmail.com`)
   - `professionalEmail` = Email généré automatiquement (ex: `mohamed.negzaoui@soprahr.com`)

2. **L'email est envoyé sur l'adresse PERSONNELLE** avec les identifiants PROFESSIONNELS

3. **Contenu de l'email** :
   - URL de connexion
   - Email professionnel (identifiant)
   - Mot de passe temporaire

4. **Gestion d'erreurs** : L'échec de l'envoi d'email ne bloque pas la création du compte

---

## 5. Résumé du flux complet

### 📊 Diagramme du processus

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Utilisateur soumet une demande d'accès (email personnel)    │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. Admin approuve la demande via le dashboard                  │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. AccountCreationRequestService.approve() est appelé          │
│     ├─ Génère l'email professionnel (@soprahr.com)              │
│     ├─ Génère un mot de passe temporaire aléatoire              │
│     ├─ Crée le compte dans Keycloak                             │
│     ├─ Crée le User en base de données                          │
│     ├─ Crée l'EmployeeProfile                                   │
│     └─ Marque la demande comme APPROVED                         │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. ⭐ EmailService.sendAccountApprovedEmail() ⭐                │
│     ├─ Destinataire : Email PERSONNEL                           │
│     ├─ Contenu : Email PRO + Mot de passe temporaire            │
│     ├─ Mode async : n'attend pas la fin de l'envoi              │
│     └─ Utilise JavaMailSender + config SMTP Gmail               │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. 📧 Email envoyé via SMTP Gmail                              │
│     ├─ From: no-reply@sopraHr.com                               │
│     ├─ To: email.personnel@example.com                          │
│     ├─ Subject: "🎉 Votre compte Sopra HR Stuffing a été créé"  │
│     └─ Body: Identifiants + Lien de connexion                   │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. Utilisateur reçoit l'email et peut se connecter             │
└─────────────────────────────────────────────────────────────────┘
```

### 🔄 Cas de rejet

Si l'admin rejette la demande, la méthode `reject()` envoie aussi un email :

```java
@Transactional
public void reject(Long requestId, String reason, Authentication processedBy) {
    var req = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

    if (req.getStatus() != AccountRequestStatus.PENDING) {
        throw new IllegalStateException("Demande déjà traitée");
    }

    req.setStatus(AccountRequestStatus.REJECTED);
    req.setProcessedAt(Instant.now());
    req.setProcessedBy(processedBy != null ? processedBy.getName() : null);
    requestRepository.save(req);

    try {
        emailService.sendAccountRejectedEmail(req.getEmail(), reason);
    } catch (Exception e) {
        log.warn("Echec envoi email de rejet pour {} : {}", req.getEmail(), e.getMessage());
    }
}
```

---

## 📝 Notes techniques importantes

### 1. Mot de passe d'application Gmail

Pour que Gmail accepte l'authentification SMTP, vous devez :
1. Activer la validation en deux étapes sur votre compte Gmail
2. Générer un "mot de passe d'application" dans les paramètres Google
3. Utiliser ce mot de passe (pas votre vrai mot de passe Gmail)

### 2. Mode asynchrone (@Async)

- Toutes les méthodes d'envoi d'email sont **@Async**
- Cela évite de bloquer la requête HTTP en attendant l'envoi
- L'utilisateur a une réponse immédiate, l'email est envoyé en arrière-plan

### 3. Mode dev/prod

- Si `app.mail.enabled=false`, les emails sont juste loggés (utile en dev)
- Si `app.mail.enabled=true`, les emails sont réellement envoyés

### 4. Sécurité

⚠️ **ATTENTION** : Le mot de passe Gmail est en clair dans `application.properties`
- En production, utilisez des variables d'environnement
- Ou Spring Cloud Config / Azure Key Vault / AWS Secrets Manager

```properties
# Exemple sécurisé :
spring.mail.username=${GMAIL_USERNAME}
spring.mail.password=${GMAIL_APP_PASSWORD}
```

---

## 🔗 Fichiers liés

| Fichier | Rôle |
|---------|------|
| `EmailService.java` | Service d'envoi d'emails |
| `AccountCreationRequestService.java` | Logique d'approbation/rejet |
| `application.properties` | Configuration SMTP |
| `pom.xml` | Dépendance spring-boot-starter-mail |

---

**📅 Document généré automatiquement le 2026-05-18**

---

## 🔔 Notifications & Calendrier — Gestion des affectations

### Étape 1 — Clôture manuelle par le collaborateur (commit d2fd121)
On a permis au collaborateur de marquer lui-même son affectation comme terminée. Quand il le fait, son manager reçoit automatiquement une notification de type ASSIGNMENT_COMPLETED.

Fichiers modifiés :
- controller/CollaboratorController.java : ajout de l'endpoint POST /api/collaborator/assignments/{id}/complete
- service/CollaboratorService.java : ajout de la méthode completeAssignment() qui passe le statut en COMPLETED et notifie le manager

### Étape 2 — Notifications automatiques (scheduler) (commit a6073d3)
On a ajouté une tâche planifiée qui tourne tous les jours à 06h00 et qui notifie les managers : quand une tâche a dépassé sa date de fin mais est encore active (ASSIGNMENT_OVERDUE), et quand un collaborateur sera bientôt disponible, fin dans 7 jours (COLLABORATOR_SOON_AVAILABLE). On a aussi ajouté un champ assignmentId sur les notifications pour éviter d'envoyer plusieurs fois la même notif.

Fichiers modifiés :
- StuffingApplication.java : ajout de @EnableScheduling
- entity/Notification.java : ajout du champ assignmentId
- repository/AssignmentRepository.java : ajout des requêtes findByStatusAndEndDateBefore et findByStatusAndEndDateBetween
- repository/NotificationRepository.java : ajout de existsByAssignmentIdAndType
- service/NotificationService.java : ajout d'une surcharge createNotification avec assignmentId
- service/AssignmentScheduler.java : nouveau fichier contenant la logique du scheduler

### Étape 3a — Rappel au collaborateur (pas encore commité)
On a ajouté une troisième vérification dans le scheduler qui prévient le collaborateur quand sa tâche se termine dans 3 jours (ASSIGNMENT_ENDING_SOON).

Fichier modifié :
- service/AssignmentScheduler.java : ajout de la méthode notifyCollaboratorsEndingSoon()

### Note sur le calendrier
La partie calendrier (CalendarEventDto et getCalendarEvents) n'a pas été modifiée, elle existait déjà et affiche automatiquement les tâches terminées en gris grâce au statut COMPLETED.

