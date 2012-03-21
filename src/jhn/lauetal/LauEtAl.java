package jhn.lauetal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

public class LauEtAl {
	private static final double MIN_AVG_RACO = 0.1;
	private static final int FALLBACK_SIZE = 5;
	
	private static class OpenNLPHelper {
		private final ChunkerME chunker;
		private final POSTaggerME tagger;
		
		public OpenNLPHelper(String taggerModelPath, String chunkerModelPath) {
			this.tagger = loadTagger(taggerModelPath);
			this.chunker = loadChunker(chunkerModelPath);
		}
		
		private static POSTaggerME loadTagger(String path) {
			InputStream modelIn = null;
			POSModel model = null;
			try {
				modelIn = new FileInputStream(path);
				model = new POSModel(modelIn);
			}
			catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (modelIn != null) {
					try {
						modelIn.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return new POSTaggerME(model);
		}
		
		private static ChunkerME loadChunker(String path) {
			InputStream modelIn = null;
			ChunkerModel model = null;
			try {
				modelIn = new FileInputStream(path);
				model = new ChunkerModel(modelIn);
			} catch (IOException e) {
				// Model loading failed, handle the error
				e.printStackTrace();
			} finally {
				if (modelIn != null) {
					try {
						modelIn.close();
					} catch (IOException e) {
					}
				}
			}
			return new ChunkerME(model);
		}

		public String[] posTag(String[] sentence) {
			return tagger.tag(sentence);
		}
		
		public String[] chunk(String[] sentence, String[] posTags) {
			return chunker.chunk(sentence, posTags);
		}
		
		public Set<String> chunks(String sentence) {
			String[] sArr = sentence.split(" ");
			String[] pos = posTag(sArr);
			String[] chunkTags = chunk(sArr, pos);
			
		}
	}
	

	
	private static class RacoCalculator {
		public Set<String> minAvgRacoFilter(Set<String> primaryCandidates, Set<String> secondaryCandidates, final double minAvgRaco) {
			Set<String> passing = new HashSet<String>();
			for(String secondaryCandidate : secondaryCandidates) {
				double sum = 0.0;
				for(String primaryCandidate : primaryCandidates) {
					sum += raco(primaryCandidate, secondaryCandidate);
				}
				double avgRaco = sum / (double) primaryCandidates.size();
				if(avgRaco >= minAvgRaco) {
					passing.add(secondaryCandidate);
				}
			}
			return passing;
		}
		
		public double raco(String label1, String label2) {
			
		}
	}
	
	private static class Labeler {
		private final OpenNLPHelper openNlp;
		private final RacoCalculator raco;
	
	
		public Labeler(OpenNLPHelper openNlp, RacoCalculator raco) {
			this.openNlp = openNlp;
			this.raco = raco;
		}

		public List<String> labelTopic(String topWords) throws IOException {
			
			
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
						if(isWikipediaArticleTitle(ngram)) {
							secondaryCandidates.add(ngram);
						}
					}
				}
			}
			return secondaryCandidates;
		}
		
		
		
		private Set<String> componentNgrams(String s) {
			
		}
		
		private static boolean isWikipediaArticleTitle(String s) {
			
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
