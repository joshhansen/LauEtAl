package jhn.lauetal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import jhn.wp.Fields;

public class RacoCalculator {
	private IndexSearcher links;
	private IndexSearcher categoryCategories;
	
	public RacoCalculator(String linksIdxDir, String categoryCategoriesIdxDir) throws IOException {
		links = new IndexSearcher(IndexReader.open(FSDirectory.open(new File(linksIdxDir))));
		categoryCategories = new IndexSearcher(IndexReader.open(FSDirectory.open(new File(categoryCategoriesIdxDir))));
	}
	
	public Set<String> minAvgRacoFilter(Set<String> primaryCandidates, Set<String> secondaryCandidates, final double minAvgRaco) throws IOException {
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
	
	/** Computes a Dice coefficient normalized RACO score */
	public double raco(String label1, String label2) throws IOException {
		final Set<String> relatedArticleCategories1 = relatedArticleCategories(label1);
		final Set<String> relatedArticleCategories2 = relatedArticleCategories(label2);
		final double size1 = relatedArticleCategories1.size();
		final double size2 = relatedArticleCategories2.size();
		
		//Make relatedArticleCategories1 represent the intersection
		relatedArticleCategories1.retainAll(relatedArticleCategories2);
		
		final double intersectSize = relatedArticleCategories1.size();
		
		return 2.0 * intersectSize / (size1 + size2);
	}
	
	private Set<String> relatedArticleCategories(String relatedToArticle) throws IOException {
		final Set<String> racs = new HashSet<String>();
		for(String relatedArticle : articlesLinkedTo(relatedToArticle)) {
			racs.addAll(categoriesContaining(relatedArticle));
		}
		return racs;
	}
	
	private int docNum(String label) throws IOException {
		TopDocs td = links.search(new TermQuery(new Term(Fields.label, label)), 1);
		return td.scoreDocs[0].doc;
	}
	
	private Set<String> articlesLinkedTo(String fromArticle) throws IOException {
		final int docNum = docNum(fromArticle);
		TermFreqVector[] tfvs = links.getIndexReader().getTermFreqVectors(docNum);
		TermFreqVector outlinks = links.getIndexReader().getTermFreqVector(docNum, Fields.linkedPage);// Fields.text);//FIXME Change to Fields.links
		return new HashSet<String>(Arrays.asList(outlinks.getTerms()));
	}
	
	private Set<String> categoriesContaining(String childArticle) throws IOException {
		final int docNum = docNum(childArticle);
		TermFreqVector categories = categoryCategories.getIndexReader().getTermFreqVector(docNum, Fields.categoryCategory);
		return new HashSet<String>(Arrays.asList(categories.getTerms()));
	}
}