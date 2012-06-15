package jhn.lauetal;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
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
import jhn.util.Config;
import jhn.util.Log;
import jhn.wp.Fields;



public class LauEtAl {
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
	private final TopicWordIndex topicWordIdx;
	private final AssociationMeasure<String,String> labelWordAssocMeas;
	private final TitleSearcher titleSearcher;

	public LauEtAl(Config conf, Log log, IndexReader topicWordIdx, String linksIdxDir, String articleCategoriesIdxDir, String chunkerPath,
			String posTaggerPath, AssociationMeasure<String,String> labelWordAssocMeas, TitleSearcher titleSearcher) throws IOException {
		this.conf = conf;
		this.log = log;
		this.topicWordIdx = new TopicWordIndex(topicWordIdx);
		this.raco = new RacoCalculator(linksIdxDir, articleCategoriesIdxDir);
		this.openNlp = new OpenNLPHelper(posTaggerPath, chunkerPath);
		this.labelWordAssocMeas = labelWordAssocMeas;
		this.titleSearcher = titleSearcher;
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
		public int compare(ScoredLabel o1, ScoredLabel o2) {
			return Double.compare(o2.score, o1.score);
		}
	};
	private ScoredLabel[] rank(Set<String> labels, String... topWords) throws Exception {
		log.println("Ranking label candidates...");
//		List<ScoredLabel> scored = new ArrayList<ScoredLabel>();
		ScoredLabel[] scored = new ScoredLabel[labels.size()];
		
		int idx = 0;
		for(String label : labels) {
			double assoc = labelWordAssocMeas.association(label, topWords);
			log.println("\t" + label + ": " + assoc);
			scored[idx++] = new ScoredLabel(label, assoc);
		}
		
		Arrays.sort(scored, labelCmp);
		
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
			for(String chunk : openNlp.npChunks(primaryCandidate)) {
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
		CharTermAttribute token;
		while(sf.incrementToken()) {
			token = sf.getAttribute(CharTermAttribute.class);
			log.println(token);
			ngrams.add(token.toString());
		}
		return ngrams;
	}
	
	private Set<String> fallbackCandidates(String... topWords) {
		Set<String> fallbacks = new HashSet<String>();
		
		for(int i = 0; i < Math.min(topWords.length, conf.getInt(Options.NUM_FALLBACK_CANDIDATES)); i++) {
			fallbacks.add(topWords[i]);
		}
		
		return fallbacks;
	}
	
	
	public static void main(String[] args) throws Exception {
		final String topicWordDir = jhn.eda.Paths.topicWordIndexDir("wp_lucene4");
		final String linksDir = jhn.eda.Paths.indexDir("page_links");
		final String artCatsDir = jhn.eda.Paths.indexDir("article_categories");
		
		Config conf = new Config();
		conf.putInt(Options.PROP_PMI_MAX_HITS, 1000);
		conf.putInt(Options.LUCENE_TITLE_SEARCHER_TOP_N, 10);
		conf.putDouble(Options.MIN_AVG_RACO, 0.1);
		conf.putInt(Options.NUM_FALLBACK_CANDIDATES, 5);
		conf.putInt(Options.TITLE_UNION_TOP_N, 10);
		
		Log log = new Log(System.out, Paths.logFilename());
		log.println("Lau, et al. configuration:");
		log.println(conf);
		
		
		IndexReader topicWordIdx = IndexReader.open(NIOFSDirectory.open(new File(topicWordDir)));
		
		PhraseWordProportionalPMI assocMeasure = new PhraseWordProportionalPMI(topicWordIdx, Fields.text, conf.getInt(Options.PROP_PMI_MAX_HITS));
		OrderedTitleSearcher ts1 = new MediawikiTitleSearcher();
		OrderedTitleSearcher ts2 = new LuceneTitleSearcher(topicWordIdx, conf.getInt(Options.LUCENE_TITLE_SEARCHER_TOP_N));
		TitleSearcher ts = new UnionTitleSearcher(conf.getInt(Options.TITLE_UNION_TOP_N), ts1, ts2);
		LauEtAl l = new LauEtAl(conf, log, topicWordIdx, linksDir, artCatsDir, Paths.chunkerFilename(), Paths.posTaggerFilename(), (AssociationMeasure<String, String>) assocMeasure, ts);

//		final String topic = "government republican states";
//		final String topic = "mazda maruts man ahura";
//		final String topic = "california oregon pacific wealth believed";
//		final String topic = "act territory convention American foreign";
		final String topic = "Lord God people man earth";
		final String[] topicWords = topic.split(" ");
		ScoredLabel[] labels = l.labelTopic(topicWords);
		log.println("Labels for topic '" + topic + "':");
		for(ScoredLabel sl : labels) {
			log.print('\t');
			log.print(sl.label);
			log.print(": ");
			log.println(sl.score);
		}
		
		topicWordIdx.close();
	}
}
