package com.b.android.openvpn60.helper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.activity.LoginActivity;
import com.b.android.openvpn60.util.JSEEUtil;

import java.io.File;
import java.security.Security;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Created by b on 9/19/17.
 */

public class EmailHelper extends AsyncTask<Void, Void, Integer> {
    private LoginActivity loginActivity;
    public static boolean processFlag = false;
    private LogHelper logHelper;
    private ProgressBar progressBar;
    private String mailhost = "smtp.gmail.com";
    private Session session;
    private javax.mail.Authenticator authenticator;


    public EmailHelper(Context context, final String userName, final String password) {
        // Best Practice!
        loginActivity = (LoginActivity) context;
        Security.addProvider(new JSEEUtil());
        authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        };
        progressBar = (ProgressBar) ((LoginActivity) context).findViewById(R.id.progressBar);
        logHelper = LogHelper.getLogHelper(context);
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", mailhost);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");// 587
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.quitwait", "false");
        session = Session.getDefaultInstance(props, authenticator);
    }

    @Override
    protected void onPreExecute() {
        logHelper.logInfo("Preparing to send email...");
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (sendEmail())
            return 11;
        return 99;
    }

    @Override
    protected void onPostExecute(Integer errorCode) {
        if (errorCode == 11) {
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(loginActivity, "Success", Toast.LENGTH_SHORT).show();
            logHelper.logInfo("Email successfully sended...");
        } else if (errorCode == 99) {
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(loginActivity, "An error occured while sending email", Toast.LENGTH_SHORT).show();
            logHelper.logWarning("An error occured while sending email");
        }
    }

    private boolean sendEmail() {
        try {
            sendMail("Reset Password", "This email will contain reset password link",
                    "bilalccaliskan@gmail.com", "bilalccaliskan@gmail.com", "");
            return true;
        } catch (Exception e) {
            logHelper.logException(e);
            logHelper.logWarning("sendMail() with no parameter");
        }
        return false;
    }

    private synchronized void sendMail(String subject, String body,
                                       String senderEmail, String recipients, String logFilePath) throws Exception {
        File file= new File(logFilePath);
        boolean fileExists =file.exists();
        if (fileExists) {
            String from = senderEmail;
            String to = recipients;
            // Define message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            // create the message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            // fill message
            messageBodyPart.setText(body);
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            // part three for logs
            messageBodyPart = new MimeBodyPart();
            DataSource sourceb = new FileDataSource(logFilePath);
            messageBodyPart.setDataHandler(new DataHandler(sourceb));
            messageBodyPart.setFileName(file.getName());
            multipart.addBodyPart(messageBodyPart);
            // Put parts in message
            message.setContent(multipart);
            // Send the message
            Transport.send(message);
        } else {
            sendMail(subject, body, senderEmail, recipients);
        }
    }

    private synchronized void sendMail(String subject, String body, String sender, String recipients) throws Exception {
        try {
            MimeMessage message = new MimeMessage(session);
            DataHandler handler = new DataHandler(new javax.mail.util.ByteArrayDataSource(body.getBytes(),
                    "text/plain"));
            message.setSender(new InternetAddress(sender));
            message.setSubject(subject);
            message.setDataHandler(handler);
            if (recipients.indexOf(',') > 0)
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipients));
            else
                message.setRecipient(Message.RecipientType.TO,
                        new InternetAddress(recipients));
            Transport.send(message);
        } catch (Exception e) {
            logHelper.logException(e);
            logHelper.logWarning("sendMail() with 4 parameters");
        }
    }
}
