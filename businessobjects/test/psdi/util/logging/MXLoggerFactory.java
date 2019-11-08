package psdi.util.logging;


// dummy logger factory for cli testing

public class MXLoggerFactory {

	public static MXLogger getLogger(String string) {

		return new MXLogger();
	}

}
