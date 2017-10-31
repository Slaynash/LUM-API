package fr.slaynash.lum.api.commands;

import fr.slaynash.lum.common.LumClientType;

public class LCAuth extends LumCommand {

	private String login = "";
	private String password = "";
	private LCAuthListener listener = null;
	private String loginType = LumClientType.BASIC;
	
	public LCAuth() {}
	
	public void auth(String loginType, String login, String password, LCAuthListener listener) {
		this.login = login;
		this.password = password;
		this.listener = listener;
		this.loginType = loginType;
		println(loginType);
	}

	@Override
	public void handle(String in) {
		if(in.equals("LOGIN")) {
			println(login);
		}
		else if(in.equals("PASSWORD")) {
			printlnSecure(password);
		}
		else if(in.startsWith("OK ")) {
			String permissionLevel = in.substring(3);
			getClient().setType(loginType);
			getClient().setPermissionLevel(Integer.parseInt(permissionLevel));
			destroy();
			listener.onSuccess();
		}
	}
	
	@Override
	public void remoteError(String error) {
		if(error.equals("NOT_HANDLED")) {
			if(getClient().isLog()) System.out.println("[LUM] Requested level of permission not handled by server.");
			listener.onError(error);
			destroy();
		}
		else if(error.equals("BAD_AUTH")) {
			if(getClient().isLog()) System.out.println("[LUM] Requested level of permission not found on server.");
			listener.onError(error);
			destroy();
		}
		else if(error.equals("INVALID_CREDENTIALS")) {
			if(getClient().isLog()) System.out.println("[LUM] Wrong credentials for permission request.");
			listener.onError(error);
			destroy();
		}
		else {
			if(getClient().isLog()) System.out.println("[LUM] Unknown error while requesting permission level: "+error+".");
			listener.onError(error);
			destroy();
		}
		destroy();
	}
	
	public static interface LCAuthListener{
		public void onError(String error);
		public void onSuccess();
	}

}
