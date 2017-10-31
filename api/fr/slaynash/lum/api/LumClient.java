package fr.slaynash.lum.api;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import fr.slaynash.lum.api.commands.LCAuth;
import fr.slaynash.lum.api.commands.LCAuth.LCAuthListener;
import fr.slaynash.lum.common.LumClientType;
import fr.slaynash.lum.common.LumCommonValues;
import fr.slaynash.lum.common.LumPermissionLevels;

public class LumClient {
	
	private SSLSocket socket = null;

	private boolean listen = true;
	
	private BufferedReader inputStream;
	private PrintWriter outputStream;

	private String login;
	private String password;

	private String targetType = LumClientType.BASIC;
	private String clientType = LumClientType.BASIC;
	
	private int permissionLevel = LumPermissionLevels.BASIC;
	
	private Thread thread;

	private boolean log = false;

	private SSLContext sslContext;
	private static final String PASSPHRASE = "clientpw";
	
	private static LumConnectionListener connectionListener = null;

	public LumClient() {
		thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				clientThread();
			}
		});
		thread.setName("LUM Thread");
		thread.setDaemon(true);
	}
	
	public void setAuth(String type, String login, String password) {
		this.targetType = type;
		this.login = login;
		this.password = password;
	}
	
	public void startConnection() {
		thread.start();
	}
	
	private void clientThread() {
		try {
			if(connectionListener != null) connectionListener.connectionStarted();
			if(log) System.out.println("[LUM] Connecting to server...");
			setupSSL();
			SSLSocketFactory sf = sslContext.getSocketFactory();
			socket = (SSLSocket)sf.createSocket(LumCommonValues.LUMSERVER_ADDRESS, LumCommonValues.PORT);
			inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			outputStream = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()));
			String ln;
			if(connectionListener != null) connectionListener.waitingForConnection();
			if(log) System.out.println("[LUM] Waiting for connection...");
			while((ln = readln()) != null && !ln.equals("READY"));
			if(connectionListener != null) connectionListener.connecting();
			if(log) System.out.println("[LUM] Connecting...");
			println("LUMVERSION_"+LumCommonValues.VERSION);
			if((ln = readln()) == null || !ln.equals("OK")) {
				throw new Exception("Connection aborted");
			}
			if(connectionListener != null) connectionListener.connected();
			if(log) System.out.println("[LUM] Connected.");
			if(!targetType.equals(LumClientType.BASIC)) {
				if(log) System.out.println("[LUM] Trying to auth as "+targetType+"...");
				LCAuth auth = (LCAuth)LumCommandManager.createInstance("AUTH", this);
				auth.auth(targetType, login, password, new LCAuthListener() {
					
					@Override
					public void onSuccess() {
						if(log) System.out.println("[LUM] Permission granted successfully.");
					}
					
					@Override
					public void onError(String error) {
						if(log) System.out.println("[LUM] Error with permission request.");
					}
				});
			}
			listen();
		} catch (Exception e) {
			if(log) e.printStackTrace();
			if(connectionListener != null) connectionListener.connectionFailed(e);
		}
		//http://math.univ-lyon1.fr/~pujo/fondmath1.pdf
		//https://stackoverflow.com/questions/2200176/keytool-create-a-trusted-self-signed-certificate/17764629
		
	}
	
	private void setupSSL() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, KeyManagementException, UnrecoverableKeyException {
		SecureRandom secureRandom = new SecureRandom();
	    secureRandom.nextInt();
		
		KeyStore serverKeyStore = KeyStore.getInstance( "JKS" );
		serverKeyStore.load( new FileInputStream( "server.public" ), "public".toCharArray() );
		KeyStore clientKeyStore = KeyStore.getInstance( "JKS" );
		clientKeyStore.load( new FileInputStream( "client.private" ), PASSPHRASE.toCharArray() );
		
		TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
		tmf.init( serverKeyStore );
		
		KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
		kmf.init( clientKeyStore, PASSPHRASE.toCharArray() );
		
		sslContext = SSLContext.getInstance( "TLS" );
		sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), secureRandom );
	}

	public String readln() throws IOException {
		String in = inputStream.readLine();
		if(in != null) System.out.println("<<< "+in);
		return in;
	}

	public String readlnSecure() throws IOException {
		String in = inputStream.readLine();
		if(in != null) System.out.println("<<< *****************");
		return in;
	}

	public void println(String out) {
		System.out.println(">>> "+out);
		outputStream.println(out);
		outputStream.flush();
	}
	
	public void printlnSecure(String out) {
		System.out.println(">>> *****************");
		outputStream.println(out);
		outputStream.flush();
	}

	private void listen() throws IOException {
		String input = "";
		while(listen && (input = readln()) != null) {
			LumCommandManager.runCommand(input, this);
		}
	}

	public void setLog(boolean log) {
		this.log = log;
	}

	public void setConnectionListener(LumConnectionListener lumConnectionEventListener) {
		connectionListener = lumConnectionEventListener;
	}

	public void setType(String type) {
		clientType = type;
	}

	public void setPermissionLevel(int level) {
		permissionLevel = level;
	}
	
	public String getType() {
		return clientType;
	}
	
	public int getPermissionLevel() {
		return permissionLevel;
	}

	public boolean isLog() {
		return log;
	}
}
