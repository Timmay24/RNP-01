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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MailFile {

    private static final String CLIENT_LOG_PREFIX = "Client: ";
	private static final String SERVER_LOG_PREFIX = "Server: ";
	private static final int DEFAULT_TIMEOUT_IN_MS = 1000;
	private final Properties prop;
    private String recipient;
    private String sender;
    private String attachmentPath;
    private final String BOUNDARY = "fu";

    public MailFile(String recipient, String attachmentPath) {
        prop = loadProperties();
        new EMail(recipient);
        this.recipient = recipient;
        new EMail(prop.getProperty("sender"));
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
        String log = "";

        try {
            clientOutputStream = clientSocket.getOutputStream();
            clientInputStream = clientSocket.getInputStream();
            PrintWriter outputWriter = new PrintWriter(clientOutputStream, false);
            LinkedBlockingQueue<String> serverMessages = new LinkedBlockingQueue<>();
			listener = new Listener(serverMessages, clientInputStream);
			listener.start();

            log = getFirstResponse(log, serverMessages);

            log = sendClientInitiation(log, outputWriter);

            log = evaluateInitiationResponse(log, serverMessages);

            log = authenticate(log, outputWriter, serverMessages);

			log = sendEMail(log, outputWriter, serverMessages);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		} finally {
			FileWriter.writeStringToFile(log, "log.txt");
			listener.interrupt();
			System.out.println(log);
			try {
				clientSocket.close();
			} catch (IOException e) {
				// deal with it.
			}
        }
        return true;
    }

	private String sendEMail(String log, PrintWriter outputWriter, LinkedBlockingQueue<String> serverMessages)
			throws InterruptedException, IOException {
		log = sendMessageWithResponseCodeCheck(log, outputWriter, serverMessages, "MAIL FROM: " + sender, "250");

		log = sendMessageWithResponseCodeCheck(log, outputWriter, serverMessages, "RCPT TO: " + recipient, "250");

		// assemble header and body
		log = sendMessageWithResponseCodeCheck(log, outputWriter, serverMessages, "DATA", "354");

		// set subject
		outputWriter.println("Subject: " + prop.getProperty("subject"));

		// set mime preferences
		outputWriter.println("MIME-Version: 1.0");
		outputWriter.println("Content-Type: multipart/mixed; boundary=" + BOUNDARY);

		// mime settings for text body + content
		outputWriter.println("--" + BOUNDARY);
		outputWriter.println("Content-Transfer-Encoding: quoted-printable");
		outputWriter.println("Content-Type: text/plain");
		outputWriter.println();
		outputWriter.println(prop.getProperty("body").replace("\n.\n", "\n..\n"));

		// mime settings for attachment body + content
		outputWriter.println("--" + BOUNDARY);
		outputWriter.println("Content-Transfer-Encoding: base64");
		outputWriter.println("Content-Type: image/png");
		outputWriter.println("Content-Disposition: attachment; filename=" + new File(attachmentPath).getName());
		outputWriter.println();
		// put in encoded attachment string
		Path path = Paths.get(attachmentPath);
		byte[] data = Files.readAllBytes(path);
		String attachmentEncoded = new String(Base64.encodeBytesToBytes(data));
		outputWriter.println(attachmentEncoded);
		outputWriter.println("--" + BOUNDARY + "--");

		// end data block
		log = sendMessageWithResponseCodeCheck(log, outputWriter, serverMessages, ".", "250");
		return log;
	}

	private String sendMessageWithResponseCodeCheck(String log, PrintWriter outputWriter,
			LinkedBlockingQueue<String> serverMessages, String message, String expectedCode)
					throws InterruptedException {
		log = sendLnToServer(log, message, outputWriter);
		String response = serverMessages.poll(DEFAULT_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
		log += SERVER_LOG_PREFIX + response;
		checkResponseCode(response, expectedCode);
		return log;
	}

	private String authenticate(String log, PrintWriter outputWriter, LinkedBlockingQueue<String> serverMessages) throws InterruptedException {
        String encodedUser = new String(Base64.encodeBytesToBytes(prop.getProperty("user").getBytes()));
        String encodedPassword = new String(Base64.encodeBytesToBytes(prop.getProperty("password").getBytes()));
		log = sendMessageWithResponseCodeCheck(log, outputWriter, serverMessages, "AUTH LOGIN", "334");
		log = sendMessageWithResponseCodeCheck(log, outputWriter, serverMessages, encodedUser, "334");
		log = sendMessageWithResponseCodeCheck(log, outputWriter, serverMessages, encodedPassword, "235");
		return log;
	}

	private String evaluateInitiationResponse(String log, LinkedBlockingQueue<String> serverMessages) throws InterruptedException {
		String awnser = readLinesFrom(serverMessages, 700, 10);
		log += SERVER_LOG_PREFIX +  awnser;
		checkResponseCode(awnser, "250");
		return log;
	}

	private void checkResponseCode(String actualMessage, String expectedCode) {
		if (null == actualMessage || !actualMessage.startsWith(expectedCode)) {
			throw new RuntimeException("Execution failed, answer from server was:\n" + actualMessage);
		}
	}

	private String getFirstResponse(String log, LinkedBlockingQueue<String> serverMessages)
			throws InterruptedException {
		String answer = serverMessages.poll(1, TimeUnit.SECONDS);
		log += SERVER_LOG_PREFIX + answer;
		checkResponseCode(answer, "220");
		return log;
	}
    
    private String readLinesFrom(LinkedBlockingQueue<String> serverMessages) throws InterruptedException{
    	return readLinesFrom(serverMessages, 700, 3);
    }

	private String readLinesFrom(LinkedBlockingQueue<String> serverMessages, int timeout, int amountOfLines) throws InterruptedException {
		String lines = "";
		for (int i = 0; i < amountOfLines; i++) {
			String message = serverMessages.poll(timeout, TimeUnit.MILLISECONDS);
			if (null != message) {
				lines += message + "\n";
			}
		}
		return lines.substring(0, lines.length() - 2);
	}

	private String sendClientInitiation(String log, PrintWriter outputWriter) {
		String ehlo = "EHLO " + prop.getProperty("smtp").substring(prop.getProperty("smtp").indexOf('.') + 1);
		return sendLnToServer(log, ehlo, outputWriter);
	}

	private String sendLnToServer(String log, String message, PrintWriter output) {
		output.println(message);
		output.flush();
		return log + "\n" + CLIENT_LOG_PREFIX + message + "\n";
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
	private Thread listener;

    /**
     * Returns a SSL Factory instance that accepts all server certificates.
     * <pre>SSLSocket sock =
     *     (SSLSocket) getSocketFactory.createSocket ( host, 443 ); </pre>
     * @return  An SSL-specific socket factory. 
     **/
    public static final SSLSocketFactory getSocketFactory() {
		if (sslSocketFactory == null) {
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
