package jhn.lauetal;

import java.io.File;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;

import jhn.assoc.PhraseWordProportionalPMI;

public class PropPMIAnalyzer {
	public static void main(String[] args) throws Exception {
		final String idxBase = System.getenv("HOME") + "/Projects/eda_output/indices";
		final String topicWordIdxDir = idxBase + "/topic_words/wp_lucene4";
		
		IndexReader topicWordIdx = IndexReader.open(NIOFSDirectory.open(new File(topicWordIdxDir)));
		
		PhraseWordProportionalPMI assocMeasure = new PhraseWordProportionalPMI(topicWordIdx);
		
		
		analyze(assocMeasure, "United States", new String[]{"government", "federal", "budget"});
		analyze(assocMeasure, "United States", new String[]{"puppy", "tennis", "clown"});
		analyze(assocMeasure, "Dog", new String[]{"cat"});
		analyze(assocMeasure, "Dog", new String[]{"mars"});
		analyze(assocMeasure, "Dog", new String[]{"phonation"});
		
		topicWordIdx.close();
	}

	private static void analyze(PhraseWordProportionalPMI assocMeasure, String label, String[] words) throws Exception {
		System.out.println("Label: " + label);
		for(String word : words) {
			System.out.println("\t" + word);
		}
		
		for(int maxHits : new int[]{1, 2, 3, 4, 5}) {
			assocMeasure.setMaxHits(maxHits);
			double pmi = assocMeasure.association(label, words);
			System.out.println(maxHits + ": " + pmi);
		}
		
		for(int maxHits = 10; maxHits < 100; maxHits += 10) {
			assocMeasure.setMaxHits(maxHits);
			double pmi = assocMeasure.association(label, words);
			System.out.println(maxHits + ": " + pmi);
		}
		
		for(int maxHits = 100; maxHits < 1000; maxHits += 100) {
			assocMeasure.setMaxHits(maxHits);
			double pmi = assocMeasure.association(label, words);
			System.out.println(maxHits + ": " + pmi);
		}
	}
	
}
