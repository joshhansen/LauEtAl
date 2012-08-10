package jhn.lauetal;

import java.io.File;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import jhn.assoc.AssociationMeasure;
import jhn.assoc.AverageWordWordPMI;
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
		
		String wordIdxFilename = jhn.Paths.outputDir("JhnCommon") + "/word_sets/chunks/19.set";
		String countsDbFilename = jhn.Paths.outputDir("JhnCommon") + "/counts/counts.sqlite3";
		String cocountsDbFilename = jhn.Paths.outputDir("JhnCommon") + "/cocounts/cocounts.sqlite3";
		AssociationMeasure<String,String> assocMeasure = new AverageWordWordPMI(wordIdxFilename, countsDbFilename, cocountsDbFilename);
		
		OrderedTitleSearcher ts1 = new MediawikiTitleSearcher(conf.getInt(Options.TITLE_SEARCHER_TOP_N));
		OrderedTitleSearcher ts2 = new LuceneTitleSearcher(topicWordIdx, conf.getInt(Options.TITLE_SEARCHER_TOP_N));
		OrderedTitleSearcher ts3 = new GoogleTitleSearcher(conf.getInt(Options.TITLE_SEARCHER_TOP_N));
		
		TitleSearcher ts = new UnionTitleSearcher(conf.getInt(Options.TITLE_UNION_TOP_N), ts1, ts2, ts3);
		TitleChecker tc = new OrderedTitleChecker(new LuceneTitleChecker(titleIdx), new HTTPTitleChecker());
		try(LauEtAl l = new LauEtAl(conf, log, linksDir, artCatsDir, Paths.chunkerFilename(), Paths.posTaggerFilename(), assocMeasure, ts, tc)) {
			String keysFilename = jhn.Paths.ldaKeysFilename("reuters21578", 0);
			String topicLabelsFilename = Paths.topicLabelsFilename(run) + "_2";
			l.labelAllTopics(keysFilename, topicLabelsFilename);
		}
	}
}
