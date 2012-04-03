package jhn.lauetal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;



public class LauEtAl {
	private static final double MIN_AVG_RACO = 0.1;
	private static final int FALLBACK_SIZE = 5;
	
	private static class Labeler {
		private final OpenNLPHelper openNlp;
		private final RacoCalculator raco;
		private final LuceneHelper lucene;
	
	
		public Labeler(String luceneDir, OpenNLPHelper openNlp, RacoCalculator raco) {
			this.lucene = new LuceneHelper(luceneDir);
			this.openNlp = openNlp;
			this.raco = raco;
		}
		
		public List<String> labelTopic(String topWords) throws IOException {
			Set<String> candidates = candidateLabels(topWords);
			
		}
		
		private static class ScoredLabel {
			final String label;
			final double score;
			public ScoredLabel(String label, double score) {
				this.label = label;
				this.score = score;
			}
		}
		
		private List<ScoredLabel> rank(Set<String> labels) {
			List<ScoredLabel> scored = new ArrayList<ScoredLabel>();
			
			//TODO
			
			Collections.sort(scored, new Comparator<ScoredLabel>(){
				public int compare(ScoredLabel o1, ScoredLabel o2) {
					return Double.compare(o2.score, o1.score);
				}
			});
			
			return scored;
		}
		
		private Set<String> candidateLabels(String topWords) throws IOException {
			Set<String> primaryCandidates = primaryCandidates(topWords);
			Set<String> rawSecondaryCandidates = rawSecondaryCandidates(primaryCandidates);
			Set<String> secondaryCandidates = raco.minAvgRacoFilter(primaryCandidates, rawSecondaryCandidates, MIN_AVG_RACO);
			Set<String> fallbackCandidates = fallbackCandidates(topWords);
			
			Set<String> candidateLabels = new HashSet<String>();
			candidateLabels.addAll(primaryCandidates);
			candidateLabels.addAll(secondaryCandidates);
			candidateLabels.addAll(fallbackCandidates);
			return candidateLabels;
		}
		
		private Set<String> primaryCandidates(String topWords) throws IOException {
			return new HashSet<String>(MediawikiAPI.topTitles(topWords));
		}
		
		private Set<String> rawSecondaryCandidates(Set<String> primaryCandidates) {
			Set<String> secondaryCandidates = new HashSet<String>();
			for(String primaryCandidate : primaryCandidates) {
				Set<String> chunks = openNlp.chunks(primaryCandidate);
				for(String chunk : chunks) {
					for(String ngram : componentNgrams(chunk)) {
						if(lucene.isWikipediaArticleTitle(ngram)) {
							secondaryCandidates.add(ngram);
						}
					}
				}
			}
			return secondaryCandidates;
		}
		
		
		
		private Set<String> componentNgrams(String s) {
			
		}
		

		
		private static Set<String> fallbackCandidates(String topWords) {
			Set<String> fallbacks = new HashSet<String>();
			
			String[] words = topWords.split(" ");
			for(int i = 0; i < Math.max(words.length, FALLBACK_SIZE); i++) {
				fallbacks.add(words[i]);
			}
			
			return fallbacks;
		}
		
	}
	
	
	public static void main(String[] args) throws IOException {
		
	}
}
