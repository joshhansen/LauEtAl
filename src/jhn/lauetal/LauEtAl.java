package jhn.lauetal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import jhn.assoc.AssociationMeasure;
import jhn.lauetal.tc.TitleChecker;
import jhn.lauetal.ts.TitleSearcher;
import jhn.util.Config;
import jhn.util.Log;
import jhn.util.RandUtil;
import jhn.util.Util;



public class LauEtAl implements AutoCloseable {
	private static final Version luceneVersion = Version.LUCENE_36;
	
	public static class ScoredLabel {
		final String label;
		final double score;
		public ScoredLabel(String label, double score) {
			this.label = label;
			this.score = score;
		}
	}
	
	private final Config conf;
	private final Log log;
	private final OpenNLPHelper openNlp;
	private final RacoCalculator raco;
	private final TitleChecker titleChecker;
	private final AssociationMeasure<String,String> labelWordAssocMeas;
	private final TitleSearcher titleSearcher;

	public LauEtAl(Config conf, Log log, String linksIdxDir, String articleCategoriesIdxDir, String chunkerPath,
			String posTaggerPath, AssociationMeasure<String,String> labelWordAssocMeas, TitleSearcher titleSearcher, TitleChecker titleChecker) throws IOException {
		this.conf = conf;
		this.log = log;
		this.raco = new RacoCalculator(linksIdxDir, articleCategoriesIdxDir);
		this.openNlp = new OpenNLPHelper(posTaggerPath, chunkerPath);
		this.labelWordAssocMeas = labelWordAssocMeas;
		this.titleSearcher = titleSearcher;
		this.titleChecker = titleChecker;
	}
	
	public ScoredLabel[] labelTopic(String... topWords) throws Exception {
		Set<String> candidates = candidateLabels(topWords);
		log.println("Candidate Labels: ");
		for(String candidate : candidates) {
			log.println("\t" + candidate);
		}
		ScoredLabel[] rankedCandidates = rank(candidates, topWords);
		return rankedCandidates;
	}
	
	
	private static final Comparator<ScoredLabel> labelCmp = new Comparator<ScoredLabel>(){
		@Override
		public int compare(ScoredLabel o1, ScoredLabel o2) {
			return Double.compare(o2.score, o1.score);
		}
	};
	private ScoredLabel[] rank(Set<String> labels, String... topWords) throws Exception {
		ScoredLabel[] scored = new ScoredLabel[labels.size()];
		
		int idx = 0;
		double assoc;
		for(String label : labels) {
			assoc = labelWordAssocMeas.association(label, topWords);
			scored[idx++] = new ScoredLabel(label, assoc);
		}
		
		Arrays.sort(scored, labelCmp);
		
		log.println("Ranked label candidates...");
		for(ScoredLabel label : scored) {
			log.println("\t" + label.label + ": " + label.score);
		}
		
		return scored;
	}
	
	private Set<String> candidateLabels(String... topWords) throws Exception {
		Set<String> primaryCandidates = primaryCandidates(topWords);
		log.println("Primary Candidates:");
		for(String primary : primaryCandidates) {
			log.println("\t"+primary);
		}
		Set<String> rawSecondaryCandidates = rawSecondaryCandidates(primaryCandidates);
		log.println("Raw Secondary Candidates:");
		for(String rawSecondary : rawSecondaryCandidates) {
			log.println("\t"+rawSecondary);
		}
		Set<String> secondaryCandidates = raco.minAvgRacoFilter(primaryCandidates, rawSecondaryCandidates, conf.getDouble(Options.MIN_AVG_RACO));
		log.println("Secondary Candidates:");
		for(String secondary : secondaryCandidates) {
			log.println("\t"+secondary);
		}
		Set<String> fallbackCandidates = fallbackCandidates(topWords);
		log.println("Fallback Candidates:");
		for(String fallback : fallbackCandidates) {
			log.println("\t"+fallback);
		}
		
		Set<String> candidateLabels = new HashSet<>();
		candidateLabels.addAll(primaryCandidates);
		candidateLabels.addAll(secondaryCandidates);
		candidateLabels.addAll(fallbackCandidates);
		return candidateLabels;
	}
	
	private Set<String> primaryCandidates(String... topWords) throws Exception {
		Set<String> candidates = new HashSet<>();
		for(String title : titleSearcher.titles(topWords)) {
			if(titleChecker.isWikipediaArticleTitle(title)) {
				candidates.add(title);
			}
		}
		return candidates;
	}
	
	private Set<String> rawSecondaryCandidates(Set<String> primaryCandidates) throws Exception {
		Set<String> secondaryCandidates = new HashSet<>();
		for(String primaryCandidate : primaryCandidates) {
			for(String chunk : openNlp.npChunks(primaryCandidate)) {
				for(String ngram : componentNgrams(chunk)) {
					if(titleChecker.isWikipediaArticleTitle(ngram)) {
						secondaryCandidates.add(ngram);
					}
				}
			}
		}
		return secondaryCandidates;
	}
	
	private static Set<String> componentNgrams(String s) throws IOException {
		Tokenizer tok = new StandardTokenizer(luceneVersion, new StringReader(s));
		
		ShingleFilter sf = new ShingleFilter(tok);
		
		Set<String> ngrams = new HashSet<>();
		CharTermAttribute token;
		while(sf.incrementToken()) {
			token = sf.getAttribute(CharTermAttribute.class);
			ngrams.add(token.toString());
		}
		return ngrams;
	}
	
	private Set<String> fallbackCandidates(String... topWords) throws Exception {
		Set<String> fallbacks = new HashSet<>();
		
		int numAdded = 0;
		for(String word : topWords) {
			if(numAdded >= conf.getInt(Options.NUM_FALLBACK_CANDIDATES)) {
				break;
			}
			
			//Uppercase first letter:
			word = word.substring(0, 1).toUpperCase() + word.substring(1);
			
			if(titleChecker.isWikipediaArticleTitle(word)) {
				fallbacks.add(word);
				numAdded++;
			} else {
				System.err.println("Did not add '" + word + "'");
			}
		}
		
		return fallbacks;
	}
	
	public void labelAllTopics(String topicKeysFilename, String outputFilename) throws Exception {
		try(	BufferedReader r = new BufferedReader(new FileReader(topicKeysFilename));
				PrintWriter w = new PrintWriter(new FileWriter(outputFilename), true)) {
		
			String[] parts;
			String[] topicWords;
			ScoredLabel[] labels;
			String tmp = null;
			int topicNum;
			while( (tmp=r.readLine()) != null) {
				log.println(tmp);
				parts = tmp.split("\\s+");
				
				topicNum = Integer.parseInt(parts[0]);
				
				topicWords = new String[parts.length - 2];
				for(int i = 2; i < parts.length; i++) {
					topicWords[i-2] = parts[i];
				}
				labels = labelTopic(topicWords);
				w.print(topicNum);
				for(String word : topicWords) {
					w.print(',');
					w.print(word);
				}
				w.print(",\"");
				w.print(labels[0].label);
				w.println("\"");
				
				final int base = 60 + RandUtil.rand.nextInt(60);
				if(conf.isTrue(Options.SPOOF_DELAY)) {
					int seconds = base + RandUtil.rand.nextInt(60);
					Thread.sleep(seconds*1000);
				}
			}
		}
	}

	@Override
	public void close() throws Exception {
		log.close();
		Util.closeIfPossible(openNlp);
		Util.closeIfPossible(raco);
		Util.closeIfPossible(titleSearcher);
		Util.closeIfPossible(titleChecker);
		Util.closeIfPossible(labelWordAssocMeas);
	}
}
