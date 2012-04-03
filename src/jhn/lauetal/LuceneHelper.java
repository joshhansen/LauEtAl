package jhn.lauetal;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

class LuceneHelper {
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