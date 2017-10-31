package fr.slaynash.lum.api;

public interface LumConnectionListener {

	void connectionStarted();
	void waitingForConnection();
	void connecting();
	void connected();
	void connectionFailed(Throwable error);

}
