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
	
	public static String runsDir() {
		return outputDir() + "/runs";
	}
	
	public static String runDir(int run) {
		return runsDir() + "/" + run;
	}
	
	public static String logFilename(int run) {
		return runDir(run) + "/main.log";
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
	
	public static String nextRunDir() {
		return runDir(jhn.Paths.nextRun(runsDir()));
	}
	
	public static String documentLabelHitDataFilename(int run) {
		return runDir(run) + "/document_label_hit_data.csv";
	}
	
	public static String topicLabelsFilename(int run) {
		return runDir(run) + "/topic_labels.csv";
	}

}
