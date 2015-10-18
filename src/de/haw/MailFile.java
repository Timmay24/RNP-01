package de.haw;

import de.haw.util.EMail;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Scanner;

public class MailFile {

    private final Properties prop;

    public MailFile(String recipient, String attachmentPath) {
        prop = loadProperties();
        EMail recipientAddress = new EMail(recipient);
        EMail senderAddress = new EMail(prop.getProperty("sender"));
    }

    private Properties loadProperties() {
        Properties prop = new Properties();
        InputStream in = getClass().getResourceAsStream("mail.properties");
        try {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return prop;
    }

    public boolean sendMail() {
        Socket clientSocket = openSocket();
        OutputStream clientOutputStream = null;
        InputStream clientInputStream = null;
        try {
            clientOutputStream = clientSocket.getOutputStream();
            clientInputStream = clientSocket.getInputStream();
            PrintWriter output = new PrintWriter(clientOutputStream, false);
            output.print("EHLO " + prop.getProperty("smtp"));
            BufferedReader in = new BufferedReader(new InputStreamReader(clientInputStream));
            System.out.println(in.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        authenticate();
//        mailFrom();
//        rcptTo();
//        data();
//        quit();

        /**
            S: 220 hamburger.edu
            C: EHLO crepes.fr
            S: 250 Hello crepes.fr, pleased to meet you
            C: MAIL FROM: <alice@crepes.fr>
            S: 250 alice@crepes.fr... Sender ok
            C: RCPT TO: <bob@hamburger.edu>
            S: 250 bob@hamburger.edu ... Recipient ok
            C: DATA
            S: 354 Enter mail,end with "." on a line by itself
            C: Do you like ketchup?
            C: How about pickles?
                    C: .
            S: 250 Message accepted for delivery
            C: QUIT
            S: 221 hamburger.edu closing connection
        */

//        C: EHLO jgm.example.com
//        S: 250-smtp.example.com
//        S: 250 AUTH CRAM-MD5 DIGEST-MD5
//        C: AUTH FOOBAR
//        S: 504 Unrecognized authentication type.
//                C: AUTH CRAM-MD5
//        S: 334
//        PENCeUxFREJoU0NnbmhNWitOMjNGNndAZWx3b29kLmlubm9zb2Z0LmNvbT4=
//                C: ZnJlZCA5ZTk1YWVlMDljNDBhZjJiODRhMGMyYjNiYmFlNzg2ZQ==
//                S: 235 Authentication successful.

        return false;
    }

    private Socket openSocket() {
        Socket clientSocket = null;

        if (prop.getProperty("ssl").equals("True")) {
            SSLSocketFactory factory = (SSLSocketFactory) getSocketFactory();
            try {
                SSLSocket sslSocket = (SSLSocket)factory.createSocket(prop.getProperty("smtp"), Integer.valueOf(prop.getProperty("port")));
                clientSocket = sslSocket;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                clientSocket = new Socket(prop.getProperty("smtp"), Integer.valueOf(prop.getProperty("port")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return clientSocket;
    }
    
    private static SSLSocketFactory sslSocketFactory;

    /**
     * Returns a SSL Factory instance that accepts all server certificates.
     * <pre>SSLSocket sock =
     *     (SSLSocket) getSocketFactory.createSocket ( host, 443 ); </pre>
     * @return  An SSL-specific socket factory. 
     **/
    public static final SSLSocketFactory getSocketFactory()
    {
      if ( sslSocketFactory == null ) {
        try {
          TrustManager[] tm = new TrustManager[] { new NaiveTrustManager() };
          SSLContext context = SSLContext.getInstance ("SSL");
          context.init( new KeyManager[0], tm, new SecureRandom( ) );

          sslSocketFactory = (SSLSocketFactory) context.getSocketFactory ();

        } catch (KeyManagementException e) {
        	
        } catch (NoSuchAlgorithmException e) {
        	
        }
      }
      return sslSocketFactory;
    }
}
