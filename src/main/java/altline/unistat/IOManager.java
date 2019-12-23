package altline.unistat;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import altline.utils.Alerts;

final class IOManager {
	private static final IOManager INSTANCE = new IOManager();
	private static final Logger LOGGER = LogManager.getLogger();

	public static final IOManager getInstance() {
		return INSTANCE;
	}

	// pathIDs of the application's files and directories
	public static final String F_APPLOCK = "F_APPLOCK";
	public static final String F_COOKIE_STORE = "F_COOKIE_STORE";
	public static final String DIR_USERDATA = "DIR_USERDATA";
	// ----

	private final Map<String, Path> pathMap = new HashMap<String, Path>();

	private final Path workDir;

	private IOManager() {
		String appdata = System.getenv("APPDATA");
		if (appdata != null && !appdata.isEmpty()) workDir = Paths.get(appdata, App.AUTHOR, App.APPNAME);
		else workDir = Paths.get(System.getProperty("user.home"), "Local Settings", "ApplicationData", App.AUTHOR, App.APPNAME);

		try {
			Files.createDirectories(workDir);
		} catch (IOException e) {
			Alerts.catching("Could not create work directory", e, LOGGER);
		}

		putFile(F_APPLOCK, "app.lock");
		putFile(F_COOKIE_STORE, "cookieStore.dat");

		putDir(DIR_USERDATA, "userdata");
	}

	private void putFile(String pathID, String localPath) {
		putPath(pathID, workDir.resolve(localPath));
	}

	private void putDir(String pathID, String localPath) {
		Path path = workDir.resolve(localPath);
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			Alerts.catching("IOException while creating directory: " + path, e, LOGGER);
		}
		putPath(pathID, path);
	}

	private void putPath(String pathID, Path path) {
		pathMap.put(pathID, path);
	}
	

	public Path getWorkDir() {
		return workDir;
	}

	/**
	 * Gets the {@link Path} representing an application file or directory with the specified pathID
	 * @param pathID A string representing a valid application file
	 * @return A Path representing the requested file or directory
	 */
	public Path getPath(String pathID) {
		return pathMap.get(pathID);
	}

	/**
	 * Gets the data file for the {@link User} with the specified userID
	 * @param userID The userID of the User whose data file is to be retrieved
	 * @return The data file of the specified User
	 */
	public Path getUserFile(String userID) {
		return getPath(DIR_USERDATA).resolve(UUID.nameUUIDFromBytes(userID.getBytes(StandardCharsets.UTF_8)).toString() + ".dat");
	}
	
	void start() {
	}
	
	void stop() {
	}


	public Object readObject(Path path) {
		if (!Files.isReadable(path)) {
			LOGGER.debug("Tried to read a non-readable or non-existing file:\n" + path);
			return null;
		}

		LOGGER.debug("Reading object from: {}", path.toAbsolutePath());
		Object obj = null;

		try (
			InputStream fileIn = Files.newInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(fileIn);
		) {

			obj = ois.readObject();

		} catch (SecurityException e) {
			Alerts.catching("File read access denied.\nPath: " + path.toAbsolutePath(), e, LOGGER);
			return null;
		} catch (ClassNotFoundException e) {
			Alerts.catching("Problem while reading object.\nPath: " + path.toAbsolutePath(), e, LOGGER);
		} catch (IOException e) {
			Alerts.catching("Could not read object from file: " + path.toAbsolutePath(), e, LOGGER);
		}
		return obj;
	}

	public void writeObject(Object obj, Path path) {
		LOGGER.debug("Writing object to: {}", path.toAbsolutePath());

		try {
			Files.createDirectories(path.getParent());
		} catch (SecurityException e) {
			Alerts.catching("Read/Write access denied.\nPath: " + path.toAbsolutePath(), e, LOGGER);
		} catch (IOException e) {
			Alerts.catching("Could not create file: " + path.toAbsolutePath(), e, LOGGER);
		}

		try (
			OutputStream fileOut = Files.newOutputStream(path);
			ObjectOutputStream oos = new ObjectOutputStream(fileOut);
		) {

			oos.writeObject(obj);

		} catch (SecurityException e) {
			Alerts.catching("Read/Write access denied.\nPath: " + path.toAbsolutePath(), e, LOGGER);
		} catch (IOException e) {
			Alerts.catching("Could not write to file: " + path.toAbsolutePath(), e, LOGGER);
		}
	}

	public void writeString(String str, Path path, OpenOption... options) {
		LOGGER.debug("Writing string to: {}", path.toAbsolutePath());

		try {
			Files.createDirectories(path.getParent());
			Files.write(path, str.getBytes(), options);
		} catch (SecurityException e) {
			Alerts.catching("Read/Write access denied.\nPath: " + path.toAbsolutePath(), e, LOGGER);
		} catch (IOException e) {
			Alerts.catching("Could not write string to file: " + path.toAbsolutePath(), e, LOGGER);
		}
	}

}
