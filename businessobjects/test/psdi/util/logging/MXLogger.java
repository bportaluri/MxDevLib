package psdi.util.logging;

public class MXLogger {

	public void info(String string) {
		System.out.println("[INFO]  " + string);
	}

	public void debug(String string) {
		System.out.println("[DEBUG] " + string);
	}

	public void error(String string) {
		System.out.println("[ERROR] " + string);
	}

	public void error(String string, Throwable e) {
		System.out.println("[ERROR] " + string);
		e.printStackTrace();
	}

	public void warn(String string) {
		System.out.println("[WARN]  " + string);
	}

}
