package jhn.lauetal;


public final class Paths {
	private Paths() {}
	
	public static String outputDir() {
		return System.getenv("HOME") + "/Projects/Output/LauEtAl";
	}
	
	public static String runsDir() {
		return outputDir() + "/runs";
	}
	
	public static String modelsBase() {
		return System.getenv("HOME") + "/Libraries/apache-opennlp-1.5.2-incubating/models";
	}
	
	public static String chunkerFilename() {
		return modelsBase() + "/en-chunker.bin";
	}
	
	public static String posTaggerFilename() {
		return modelsBase() + "/en-pos-maxent.bin";
	}
	
	public static String logFilename() {
		final String logDir = runsDir();
		String filename = logDir + "/" + String.valueOf(jhn.eda.Paths.nextLogNum(logDir));
		System.out.println("Writing to log file: " + filename);
		return filename;
	}

}
