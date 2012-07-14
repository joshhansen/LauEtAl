package jhn.lauetal.hit;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class GenerateDocumentLabelHITData {
	
	private static Int2ObjectMap<String> loadLabels(String topicLabelsFilename) throws Exception {
		Int2ObjectMap<String> labels = new Int2ObjectOpenHashMap<String>();
		
		BufferedReader r = new BufferedReader(new FileReader(topicLabelsFilename));
		
		String tmp = null;
		while( (tmp=r.readLine()) != null) {
			if(!tmp.startsWith("#")) {
				String[] parts = tmp.split(",");
				int topicNum = Integer.parseInt(parts[0]);
				String label = parts[parts.length-1];
				label = label.replaceAll("[\"]", "");
				labels.put(topicNum, label);
			}
		}
		r.close();
		
		return labels;
	}
	
	private static void generate(String docTopicsFilename, String topicLabelsFilename, String outputFilename, int numTopics) throws Exception {
		Int2ObjectMap<String> labels = loadLabels(topicLabelsFilename);
		
		PrintStream w = new PrintStream(new FileOutputStream(outputFilename));
		w.print("#docnum,source");
		for(int i = 0; i < numTopics; i++) {
			w.print(",label");
			w.print(i+1);
		}
		w.println();
		
		BufferedReader r = new BufferedReader(new FileReader(docTopicsFilename));
		String tmp = null;
		while( (tmp=r.readLine()) != null) {
			if(!tmp.startsWith("#")) {
				String[] parts = tmp.split("\t");
				
				w.append(parts[0]).append(',').append(parts[1]);
				
				for(int i = 0; i < numTopics; i++) {
					int topicNum = Integer.parseInt(parts[2*i + 2]);
					w.append(",\"").append(labels.get(topicNum)).append("\"");
				}
				w.println();
			}
		}
		w.close();
	}
	
	public static void main(String[] args) throws Exception {
		final String datasetName = "reuters21578";// toy_dataset2 debates2012 sacred_texts state_of_the_union reuters21578
		final int run = 0;
		final int topNlabels = 10;
		generate(jhn.Paths.ldaDocTopicsFilename(datasetName, run),
				 jhn.lauetal.Paths.topicLabelsFilename(0),
				 jhn.lauetal.Paths.documentLabelHitDataFilename(run),
				 topNlabels);
	}
}
