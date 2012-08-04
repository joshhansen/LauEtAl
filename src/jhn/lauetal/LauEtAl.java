package jhn.lauetal;

import java.io.BufferedReader;
import java.io.File;
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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import jhn.assoc.AssociationMeasure;
import jhn.assoc.PhraseWordProportionalPMI;
import jhn.lauetal.tc.HTTPTitleChecker;
import jhn.lauetal.tc.OrderedTitleChecker;
import jhn.lauetal.tc.TitleChecker;
import jhn.lauetal.tc.LuceneTitleChecker;
import jhn.lauetal.ts.GoogleTitleSearcher;
import jhn.lauetal.ts.LuceneTitleSearcher;
import jhn.lauetal.ts.MediawikiTitleSearcher;
import jhn.lauetal.ts.OrderedTitleSearcher;
import jhn.lauetal.ts.TitleSearcher;
import jhn.lauetal.ts.UnionTitleSearcher;
import jhn.util.Config;
import jhn.util.Log;
import jhn.util.RandUtil;
import jhn.util.Util;
import jhn.wp.Fields;



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
			log.println(token);
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
		BufferedReader r = new BufferedReader(new FileReader(topicKeysFilename));
		PrintWriter w = new PrintWriter(new FileWriter(outputFilename), true);
		
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
		w.close();
		r.close();
	}
	
	public static void main(String[] args) throws Exception {
		final String topicWordDir = jhn.eda.Paths.topicWordIndexDir("wp_lucene4");
		final String linksDir = jhn.eda.Paths.indexDir("page_links");
		final String artCatsDir = jhn.eda.Paths.indexDir("article_categories");
		
		Config conf = new Config();
		conf.putInt(Options.PROP_PMI_MAX_HITS, 1000);
		conf.putInt(Options.TITLE_SEARCHER_TOP_N, 10);
		conf.putDouble(Options.MIN_AVG_RACO, 0.1);
		conf.putInt(Options.NUM_FALLBACK_CANDIDATES, 5);
		conf.putInt(Options.TITLE_UNION_TOP_N, 10);
		conf.putBool(Options.SPOOF_DELAY, true);
		
		final int run = jhn.Paths.nextRun(Paths.runsDir());
		new File(Paths.runDir(run)).mkdirs();
		Log log = new Log(System.out, Paths.logFilename(run));
		log.println("Lau, et al. configuration:");
		log.println(conf);
		
		
		IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(topicWordDir)));
		IndexReader titleIdx = IndexReader.open(FSDirectory.open(new File(jhn.Paths.titleIndexDir())));
		
		PhraseWordProportionalPMI assocMeasure = new PhraseWordProportionalPMI(topicWordIdx, Fields.text, conf.getInt(Options.PROP_PMI_MAX_HITS));
		OrderedTitleSearcher ts1 = new MediawikiTitleSearcher(conf.getInt(Options.TITLE_SEARCHER_TOP_N));
		OrderedTitleSearcher ts2 = new LuceneTitleSearcher(topicWordIdx, conf.getInt(Options.TITLE_SEARCHER_TOP_N));
		OrderedTitleSearcher ts3 = new GoogleTitleSearcher(conf.getInt(Options.TITLE_SEARCHER_TOP_N));
		
		TitleSearcher ts = new UnionTitleSearcher(conf.getInt(Options.TITLE_UNION_TOP_N), ts1, ts2, ts3);
		TitleChecker tc = new OrderedTitleChecker(new LuceneTitleChecker(titleIdx), new HTTPTitleChecker());
		LauEtAl l = new LauEtAl(conf, log, linksDir, artCatsDir, Paths.chunkerFilename(), Paths.posTaggerFilename(), assocMeasure, ts, tc);

//		String keysFilename = Paths.projectDir() + "/datasets/reuters-keys.txt";
		String keysFilename = jhn.Paths.ldaKeysFilename("reuters21578", 0);
//		String topicLabelsFilename = Paths.outputDir() + "/reuters-labels.txt";
		String topicLabelsFilename = Paths.topicLabelsFilename(run) + "_2";
		l.labelAllTopics(keysFilename, topicLabelsFilename);
		
////		final String topic = "government republican states";
////		final String topic = "mazda maruts man ahura";
////		final String topic = "california oregon pacific wealth believed";
////		final String topic = "act territory convention American foreign";
//		final String topic = "Lord God people man earth";
//		final String[] topicWords = topic.split(" ");
//		ScoredLabel[] labels = l.labelTopic(topicWords);
//		log.println("Labels for topic '" + topic + "':");
//		for(ScoredLabel sl : labels) {
//			log.print('\t');
//			log.print(sl.label);
//			log.print(": ");
//			log.println(sl.score);
//		}
		
		topicWordIdx.close();
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
