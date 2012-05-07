package jhn.lauetal;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import jhn.assoc.AssociationMeasure;
import jhn.assoc.Label;
import jhn.assoc.PhraseWordProportionalPMI;
import jhn.assoc.Word;



public class LauEtAl {
	private static final Version luceneVersion = Version.LUCENE_36;
	private static final double MIN_AVG_RACO = 0.1;
	private static final int FALLBACK_SIZE = 5;
	
	public static class ScoredLabel {
		final String label;
		final double score;
		public ScoredLabel(String label, double score) {
			this.label = label;
			this.score = score;
		}
	}
	
	private static class Labeler {
		private final OpenNLPHelper openNlp;
		private final RacoCalculator raco;
		private final TopicWordIndex topicWordIdx;
		private final AssociationMeasure<Label,Word> labelWordAssocMeas;
	
		public Labeler(String topicWordIdxDir, String linksIdxDir, String categoryCategoriesIdxDir, String chunkerPath,
				String posTaggerPath, AssociationMeasure<Label,Word> labelWordAssocMeas) throws IOException {
			this.topicWordIdx = new TopicWordIndex(topicWordIdxDir);
			this.raco = new RacoCalculator(linksIdxDir, categoryCategoriesIdxDir);
			this.openNlp = new OpenNLPHelper(posTaggerPath, chunkerPath);
			this.labelWordAssocMeas = labelWordAssocMeas;
		}
		
		public List<ScoredLabel> labelTopic(String... topWords) throws Exception {
			Set<String> candidates = candidateLabels(topWords);
			List<ScoredLabel> rankedCandidates = rank(candidates, topWords);
			return rankedCandidates;
		}
		
		private double averageAssociation(String label, String... topWords) throws Exception {
			double total = 0.0;
			for(String w : topWords) {
				total += labelWordAssocMeas.association(new Label(label), new Word(w));
			}
			
			return total / (double)topWords.length;
		}
		
		private List<ScoredLabel> rank(Set<String> labels, String... topWords) throws Exception {
			List<ScoredLabel> scored = new ArrayList<ScoredLabel>();
			
			for(String label : labels) {
				scored.add(new ScoredLabel(label, averageAssociation(label, topWords)));
			}
			
			Collections.sort(scored, new Comparator<ScoredLabel>(){
				public int compare(ScoredLabel o1, ScoredLabel o2) {
					return Double.compare(o2.score, o1.score);
				}
			});
			
			return scored;
		}
		
		private Set<String> candidateLabels(String... topWords) throws IOException {
			Set<String> primaryCandidates = primaryCandidates(topWords);
			System.out.println(primaryCandidates);
			Set<String> rawSecondaryCandidates = rawSecondaryCandidates(primaryCandidates);
			Set<String> secondaryCandidates = raco.minAvgRacoFilter(primaryCandidates, rawSecondaryCandidates, MIN_AVG_RACO);
			Set<String> fallbackCandidates = fallbackCandidates(topWords);
			
			Set<String> candidateLabels = new HashSet<String>();
			candidateLabels.addAll(primaryCandidates);
			candidateLabels.addAll(secondaryCandidates);
			candidateLabels.addAll(fallbackCandidates);
			return candidateLabels;
		}
		
		private Set<String> primaryCandidates(String... topWords) throws IOException {
			return new HashSet<String>(MediawikiAPI.topTitles(topWords));
		}
		
		private Set<String> rawSecondaryCandidates(Set<String> primaryCandidates) throws IOException {
			Set<String> secondaryCandidates = new HashSet<String>();
			for(String primaryCandidate : primaryCandidates) {
				Set<String> chunks = openNlp.npChunks(primaryCandidate);
				for(String chunk : chunks) {
					for(String ngram : componentNgrams(chunk)) {
						if(topicWordIdx.isWikipediaArticleTitle(ngram)) {
							secondaryCandidates.add(ngram);
						}
					}
				}
			}
			return secondaryCandidates;
		}
		
		private Set<String> componentNgrams(String s) throws IOException {
			Tokenizer tok = new StandardTokenizer(luceneVersion, new StringReader(s));
			
			ShingleFilter sf = new ShingleFilter(tok);
			
			Set<String> ngrams = new HashSet<String>();
			while(sf.incrementToken()) {
				CharTermAttribute token = sf.getAttribute(CharTermAttribute.class);
				System.out.println(token);
				ngrams.add(token.toString());
			}
			return ngrams;
		}
		
		private static Set<String> fallbackCandidates(String... topWords) {
			Set<String> fallbacks = new HashSet<String>();
			
			for(int i = 0; i < Math.max(topWords.length, FALLBACK_SIZE); i++) {
				fallbacks.add(topWords[i]);
			}
			
			return fallbacks;
		}
		
	}
	
	
	public static void main(String[] args) throws Exception {
		final String idxBase = System.getenv("HOME") + "/Projects/eda_output/indices";
		final String topicWordDir = idxBase + "/topic_words/wp_lucene3";
		final String linksDir = idxBase + "/page_links";
		final String catCatsDir = idxBase + "/category_categories";
		
		final String modelsBase = System.getenv("HOME") + "/Libraries/apache-opennlp-1.5.2-incubating/models";
		final String chunkerPath = modelsBase + "/en-chunker.bin";
		final String posTaggerPath = modelsBase + "/en-pos-maxent.bin";
		
		PhraseWordProportionalPMI assocMeasure = new PhraseWordProportionalPMI(topicWordDir);
		Labeler l = new Labeler(topicWordDir, linksDir, catCatsDir, chunkerPath, posTaggerPath, (AssociationMeasure<Label, Word>) assocMeasure);
//		l.openNlp.npChunks("once upon a time in a magical little kingdom there was a tow truck");
//		final String topic = "government republican states";
		final String[] topicWords = {"government","republican","states"};
		List<ScoredLabel> labels = l.labelTopic(topicWords);
		System.out.println("Labels for topic '" + topicWords + "':");
		for(ScoredLabel sl : labels) {
			System.out.print('\t');
			System.out.print(sl.label);
			System.out.print(": ");
			System.out.println(sl.score);
		}
		
		assocMeasure.close();
	}
}
