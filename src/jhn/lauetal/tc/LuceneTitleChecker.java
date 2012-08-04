package jhn.lauetal.tc;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import jhn.wp.Fields;

public class LuceneTitleChecker implements TitleChecker, AutoCloseable {
	private final IndexReader reader;
	private final IndexSearcher searcher;
	
	public LuceneTitleChecker(IndexReader topicWordIdx) {
		this.reader = topicWordIdx;
		searcher = new IndexSearcher(topicWordIdx);
	}
	
	public LuceneTitleChecker(final String luceneDir) throws IOException {
		this(IndexReader.open(FSDirectory.open(new File(luceneDir))));
	}
	
	@Override
	public boolean isWikipediaArticleTitle(String s) {
		TermQuery q = new TermQuery(new Term(Fields.label, s));
		
		try {
			TopDocs result = searcher.search(q, 1);
			return result.totalHits > 0;
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		throw new IllegalArgumentException();
	}

	@Override
	public void close() throws Exception {
		reader.close();
		searcher.close();
	}
}