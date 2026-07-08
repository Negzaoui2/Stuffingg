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
