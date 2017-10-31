package fr.slaynash.lum.api.commands;

import fr.slaynash.lum.api.LumClient;
import fr.slaynash.lum.api.LumCommandManager;

public abstract class LumCommand {
	
	private LumClient client = null;
	private String outId = "";
	
	public abstract void handle(String parts);
	
	
	public void println(String string) {
		client.println(outId+" "+string);
	}
	
	public void printlnSecure(String string) {
		client.printlnSecure(outId+" "+string);
	}
	
	public void setClient(LumClient client) {
		this.client = client;
	}
	
	protected LumClient getClient() {
		return client;
	}
	
	protected void destroy() {
		LumCommandManager.remove(this);
	}

	public void setOutId(String outId) {
		this.outId = outId;
	}
	
	public String getOutId() {
		return outId;
	}
	
	public void remoteError(String string) {
		destroy();
	}
	
}
