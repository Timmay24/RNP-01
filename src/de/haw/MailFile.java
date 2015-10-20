package de.haw;

import de.haw.util.EMail;
import de.haw.util.base64.Base64;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;

public class MailFile {

    private final Properties prop;
    private String recipient;
    private String sender;
    private String attachmentPath;
    private final String BOUNDARY = "fu";

    public MailFile(String recipient, String attachmentPath) {
        prop = loadProperties();
        EMail recipientAddress = new EMail(recipient);
        this.recipient = recipient;
        EMail senderAddress = new EMail(prop.getProperty("sender"));
        this.sender = prop.getProperty("sender");
        this.attachmentPath = attachmentPath;
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
            BufferedReader in = new BufferedReader(new InputStreamReader(clientInputStream));

            // get first response
            System.out.println(in.readLine());

            // negotiate sec standards
            output.println("EHLO " + prop.getProperty("smtp").substring(prop.getProperty("smtp").indexOf('.') + 1));
            output.flush();

            // TODO beautify dat shit
            System.out.println(in.readLine());
            System.out.println(in.readLine());
            System.out.println(in.readLine());
            System.out.println(in.readLine());
            System.out.println(in.readLine());
            System.out.println(in.readLine());
            System.out.println(in.readLine());
            System.out.println(in.readLine());
            System.out.println(in.readLine());

            // encode login credentials
            String encodedUser = new String(Base64.encodeBytesToBytes(prop.getProperty("user").getBytes()));
            String encodedPassword = new String(Base64.encodeBytesToBytes(prop.getProperty("password").getBytes()));

            // authenticate
            output.println("AUTH LOGIN");
            output.flush();
            System.out.println(in.readLine());

            output.println(encodedUser);
            output.flush();
            System.out.println(in.readLine());

            output.println(encodedPassword);
            output.flush();
            System.out.println(in.readLine());

            // declare sender
            output.println("MAIL FROM: " + sender);

            // declare recipient
            output.println("RCPT TO: " + recipient);
            output.flush();
            System.out.println(in.readLine());
            System.out.println(in.readLine());


            // assemble header and body
            output.println("DATA");
            output.flush();
            System.out.println(in.readLine());

            // set subject
            output.println("Subject: " + prop.getProperty("subject"));

            // set mime preferences
            output.println("MIME-Version: 1.0");
            output.println("Content-Type: multipart/mixed; boundary=" + BOUNDARY);

            // mime settings for text body + content
            output.println("--" + BOUNDARY);
            output.println("Content-Transfer-Encoding: quoted-printable");
            output.println("Content-Type: text/plain");
            output.println();
            output.println(prop.getProperty("body"));

            // mime settings for attachment body + content
            output.println("--" + BOUNDARY);
            output.println("Content-Transfer-Encoding: base64");
            output.println("Content-Type: image/png");
            output.println("Content-Disposition: attachment; filename=" + new File(attachmentPath).getName());
            output.println();
            // put in encoded attachment string
            Path path = Paths.get(attachmentPath);
            byte[] data = Files.readAllBytes(path);
            String attachmentEncoded = new String(Base64.encodeBytesToBytes(data));
            output.println(attachmentEncoded);
            output.println("--" + BOUNDARY + "--");

            // end data block
            output.println(".");
            output.flush();
            System.out.println(in.readLine());


        } catch (IOException e) {
            e.printStackTrace();
        }

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
