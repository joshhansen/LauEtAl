package jhn.lauetal;

/**
 * Don't-Repeat-Yourself paths for Lau, et al.
 *
 */
public final class Paths {
	private Paths() {}
	
	public static String projectDir() {
		return System.getenv("HOME") + "/Projects/LauEtAl";
	}
	
	public static String outputDir() {
		return jhn.Paths.outputDir("LauEtAl");
	}
	
	public static String defaultRunsDir() {
		return outputDir() + "/runs";
	}
	
	public static String runDir(String runsDir, int run) {
		return runsDir + "/" + run;
	}

	public static String logFilename(String runDir) {
		return runDir + "/main.log";
	}
	
	public static String modelsBase() {
		return projectDir() + "/models";
	}
	
	public static String chunkerFilename() {
		return modelsBase() + "/en-chunker.bin";
	}
	
	public static String posTaggerFilename() {
		return modelsBase() + "/en-pos-maxent.bin";
	}

	public static String documentLabelHitDataFilename(String runDir) {
		return runDir + "/document_label_hit_data.csv";
	}

	public static String topicLabelsFilename(String runDir) {
		return runDir + "/topic_labels.csv";
	}

}
