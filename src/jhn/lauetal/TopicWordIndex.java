package jhn.lauetal;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import jhn.wp.Fields;

public class TopicWordIndex {
	private final IndexSearcher searcher;
	public TopicWordIndex(final String luceneDir) throws IOException {
		searcher = new IndexSearcher(IndexReader.open(FSDirectory.open(new File(luceneDir))));
	}
	
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
}