package jhn.lauetal;

import java.io.File;
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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import jhn.assoc.AssociationMeasure;
import jhn.assoc.PhraseWordProportionalPMI;
import jhn.lauetal.ts.LuceneTitleSearcher;
import jhn.lauetal.ts.MediawikiTitleSearcher;
import jhn.lauetal.ts.OrderedTitleSearcher;
import jhn.lauetal.ts.TitleSearcher;
import jhn.lauetal.ts.UnionTitleSearcher;
import jhn.wp.Fields;



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
		private final AssociationMeasure<String,String> labelWordAssocMeas;
		private final TitleSearcher titleSearcher;
	
		public Labeler(IndexReader topicWordIdx, String linksIdxDir, String articleCategoriesIdxDir, String chunkerPath,
				String posTaggerPath, AssociationMeasure<String,String> labelWordAssocMeas, TitleSearcher titleSearcher) throws IOException {
			this.topicWordIdx = new TopicWordIndex(topicWordIdx);
			this.raco = new RacoCalculator(linksIdxDir, articleCategoriesIdxDir);
			this.openNlp = new OpenNLPHelper(posTaggerPath, chunkerPath);
			this.labelWordAssocMeas = labelWordAssocMeas;
			this.titleSearcher = titleSearcher;
		}
		
		public List<ScoredLabel> labelTopic(String... topWords) throws Exception {
			Set<String> candidates = candidateLabels(topWords);
			System.out.println("Candidate Labels: ");
			for(String candidate : candidates) {
				System.out.append('\t').append(candidate).append('\n');
			}
			List<ScoredLabel> rankedCandidates = rank(candidates, topWords);
			return rankedCandidates;
		}
		
		private List<ScoredLabel> rank(Set<String> labels, String... topWords) throws Exception {
			System.out.println("Ranking label candidates...");
			List<ScoredLabel> scored = new ArrayList<ScoredLabel>();
			
			for(String label : labels) {
				double assoc = labelWordAssocMeas.association(label, topWords);
				System.out.println("\t" + label + ": " + assoc);
				scored.add(new ScoredLabel(label, assoc));
			}
			
			Collections.sort(scored, new Comparator<ScoredLabel>(){
				public int compare(ScoredLabel o1, ScoredLabel o2) {
					return Double.compare(o2.score, o1.score);
				}
			});
			
			return scored;
		}
		
		private Set<String> candidateLabels(String... topWords) throws Exception {
			Set<String> primaryCandidates = primaryCandidates(topWords);
			System.out.println("Primary Candidates:");
			for(String primary : primaryCandidates) {
				System.out.println("\t"+primary);
			}
			Set<String> rawSecondaryCandidates = rawSecondaryCandidates(primaryCandidates);
			System.out.println("Raw Secondary Candidates:");
			for(String rawSecondary : rawSecondaryCandidates) {
				System.out.println("\t"+rawSecondary);
			}
			Set<String> secondaryCandidates = raco.minAvgRacoFilter(primaryCandidates, rawSecondaryCandidates, MIN_AVG_RACO);
			System.out.println("Secondary Candidates:");
			for(String secondary : secondaryCandidates) {
				System.out.println("\t"+secondary);
			}
			Set<String> fallbackCandidates = fallbackCandidates(topWords);
			System.out.println("Fallback Candidates:");
			for(String fallback : fallbackCandidates) {
				System.out.println("\t"+fallback);
			}
			
			Set<String> candidateLabels = new HashSet<String>();
			candidateLabels.addAll(primaryCandidates);
			candidateLabels.addAll(secondaryCandidates);
			candidateLabels.addAll(fallbackCandidates);
			return candidateLabels;
		}
		
		private Set<String> primaryCandidates(String... topWords) throws Exception {
			return new HashSet<String>(titleSearcher.titles(topWords));
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
			
			for(int i = 0; i < Math.min(topWords.length, FALLBACK_SIZE); i++) {
				fallbacks.add(topWords[i]);
			}
			
			return fallbacks;
		}
		
	}
	
	
	public static void main(String[] args) throws Exception {
		final String idxBase = System.getenv("HOME") + "/Projects/eda_output/indices";
		final String topicWordDir = idxBase + "/topic_words/wp_lucene4";
		final String linksDir = idxBase + "/page_links";
		final String artCatsDir = idxBase + "/article_categories";
		
		final String modelsBase = System.getenv("HOME") + "/Libraries/apache-opennlp-1.5.2-incubating/models";
		final String chunkerPath = modelsBase + "/en-chunker.bin";
		final String posTaggerPath = modelsBase + "/en-pos-maxent.bin";
		
		IndexReader topicWordIdx = IndexReader.open(NIOFSDirectory.open(new File(topicWordDir)));
		
		PhraseWordProportionalPMI assocMeasure = new PhraseWordProportionalPMI(topicWordIdx, Fields.text, 1000);
		OrderedTitleSearcher ts1 = new MediawikiTitleSearcher();
		OrderedTitleSearcher ts2 = new LuceneTitleSearcher(topicWordIdx, 10);
		TitleSearcher ts = new UnionTitleSearcher(10, ts1, ts2);
		Labeler l = new Labeler(topicWordIdx, linksDir, artCatsDir, chunkerPath, posTaggerPath, (AssociationMeasure<String, String>) assocMeasure, ts);

//		final String topic = "government republican states";
//		final String topic = "mazda maruts man ahura";
//		final String topic = "california oregon pacific wealth believed";
//		final String topic = "act territory convention American foreign";
		final String topic = "Lord God people man earth";
		final String[] topicWords = topic.split(" ");
		List<ScoredLabel> labels = l.labelTopic(topicWords);
		System.out.println("Labels for topic '" + topic + "':");
		for(ScoredLabel sl : labels) {
			System.out.print('\t');
			System.out.print(sl.label);
			System.out.print(": ");
			System.out.println(sl.score);
		}
		
		topicWordIdx.close();
	}
}
