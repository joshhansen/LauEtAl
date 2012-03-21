package jhn.lauetal;

import jhn.wp.ArticlesCounter;
import jhn.wp.CorpusCounter;


public class CountCooccurrences {
	public static void main(String[] args) {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
		final String mongoDir = outputDir + "/wp_mongo_cocounts";
		
		final String srcDir = System.getenv("HOME") + "/Data/wikipedia.org";
		final String articlesFilename = srcDir + "/enwiki-20121122-pages-articles.xml.bz2";
		
		
		CorpusCounter ac = new ArticlesCounter(articlesFilename);
//		ac.addVisitor(new PrintingVisitor());
//		ac.addVisitor(new MapReduceVisitor(MongoConf.server, MongoConf.port, "wp"));
		ac.addVisitor(new LuceneVisitor(luceneDir));
		ac.count();
	}
}
