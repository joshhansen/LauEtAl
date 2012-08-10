package jhn.lauetal;

import java.io.File;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import jhn.assoc.PhraseWordProportionalPMI;
import jhn.lauetal.tc.HTTPTitleChecker;
import jhn.lauetal.tc.LuceneTitleChecker;
import jhn.lauetal.tc.OrderedTitleChecker;
import jhn.lauetal.tc.TitleChecker;
import jhn.lauetal.ts.GoogleTitleSearcher;
import jhn.lauetal.ts.LuceneTitleSearcher;
import jhn.lauetal.ts.MediawikiTitleSearcher;
import jhn.lauetal.ts.OrderedTitleSearcher;
import jhn.lauetal.ts.TitleSearcher;
import jhn.lauetal.ts.UnionTitleSearcher;
import jhn.util.Config;
import jhn.util.Log;
import jhn.wp.Fields;

public class RunLau {
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
}
