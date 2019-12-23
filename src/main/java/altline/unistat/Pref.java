package altline.unistat;

import java.util.prefs.Preferences;

public final class Pref {
	
	private static final Preferences pref = Preferences.userNodeForPackage(App.class);

	public static final String AUTO_LOGIN = "auto_login";
	public static final String USER_ID = "user_id";
	public static final String AL_PWD = "al_pwd";
	public static final String EXIT_ON_CLOSE = "exit_on_close";

	private Pref() {
	}
	
	public static Preferences getPreferences() {
		return pref;
	}
	
	public static boolean getAutoLogin() {
		return pref.getBoolean(AUTO_LOGIN, false);
	}
	
	public static String getSavedUserId() {
		return pref.get(USER_ID, "");
	}
	
	public static boolean getExitOnClose() {
		return pref.getBoolean(EXIT_ON_CLOSE, true);
	}

	
	public static void setAutoLogin(boolean autoLogin) {
		pref.putBoolean(AUTO_LOGIN, autoLogin);
	}
	
	public static void setSavedUserId(String userID) {
		pref.put(USER_ID, userID);
	}
	
	public static void setExitOnClose(boolean exitOnClose) {
		pref.putBoolean(EXIT_ON_CLOSE, exitOnClose);
	}

}
