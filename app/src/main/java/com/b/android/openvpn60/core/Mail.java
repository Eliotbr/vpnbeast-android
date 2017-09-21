package com.b.android.openvpn60.core;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Created by b on 9/19/17.
 */

public class Mail {
    private final String PORT_587 = "587";
    private final String SMTP_AUTH = "true";
    private final String START_TLS = "true";
    private final String HOST_HOTMAIL = "smtp.live.com";
    private final String HOST_GMAIL = "smtp.gmail.com";

    private String fromEmail;
    private String fromPassword;
    private String toEmailList;
    private String emailSubject;
    private String emailBody;

    Properties emailProperties;
    Session mailSession;
    MimeMessage emailMessage;


    public Mail() {

    }

    public Mail(String fromEmail, String fromPassword,
                String toEmailList, String emailSubject, String emailBody) {
        this.fromEmail = fromEmail;
        this.fromPassword = fromPassword;
        this.toEmailList = toEmailList;
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;

        emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port", PORT_587);
        emailProperties.put("mail.smtp.auth", SMTP_AUTH);
        emailProperties.put("mail.smtp.starttls.enable", START_TLS);
    }

    public MimeMessage createEmailMessage() throws AddressException,
            MessagingException, UnsupportedEncodingException {
        mailSession = Session.getDefaultInstance(emailProperties, null);
        emailMessage = new MimeMessage(mailSession);
        emailMessage.setFrom(new InternetAddress(fromEmail, fromEmail));
        emailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmailList));
        emailMessage.setSubject(emailSubject);
        emailMessage.setContent(emailBody, "text/html");// for a html email
        // emailMessage.setText(emailBody);// for a text email
        return emailMessage;
    }

    public void sendEmail() throws AddressException, MessagingException {
        Transport transport = mailSession.getTransport("smtp");
        transport.connect(HOST_GMAIL, fromEmail, fromPassword);
        transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
        transport.close();
    }
}