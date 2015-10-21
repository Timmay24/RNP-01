package de.haw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.LinkedBlockingQueue;

public class Listener extends Thread {
	
	private LinkedBlockingQueue<String> serverMessages;
	private InputStream stream;

	public Listener(LinkedBlockingQueue<String> serverMessages, InputStream stream) {
		this.serverMessages = serverMessages;
		this.stream = stream;
	}
	
	@Override
	public void run() {
		listen();
	}

	private void listen() {
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		while(!isInterrupted()) {
			try {
				this.serverMessages.add(in.readLine());
			} catch (IOException e) {
				// deal with it.
			}
		}
	}

}
