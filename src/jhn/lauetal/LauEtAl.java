package jhn.lauetal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

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
			
			
			return null;
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
			return 0.0;//TODO
		}
	}
	
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
	
	
	private static class LuceneHelper {
		private IndexSearcher searcher;
		public LuceneHelper(final String luceneDir) {
			
			try {
				FSDirectory dir = FSDirectory.open(new File(luceneDir));
				this.searcher = new IndexSearcher(IndexReader.open(dir));
			} catch (CorruptIndexException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public boolean isWikipediaArticleTitle(String s) {
			TermQuery q = new TermQuery(new Term("label", s));
			TopDocs result = null;
			try {
				result = searcher.search(q, 1);
			} catch(IOException e) {
				e.printStackTrace();
			}
			return result.totalHits > 0;
		}
	}
	
	public static void main(String[] args) throws IOException {
		
	}
}
