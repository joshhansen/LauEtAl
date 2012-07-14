package jhn.lauetal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import jhn.wp.Fields;

public class RacoCalculator {
	private IndexSearcher links;
	private IndexSearcher articleCategories;
	
	public RacoCalculator(String linksIdxDir, String articleCategoriesIdxDir) throws IOException {
		links = new IndexSearcher(IndexReader.open(FSDirectory.open(new File(linksIdxDir))));
		articleCategories = new IndexSearcher(IndexReader.open(FSDirectory.open(new File(articleCategoriesIdxDir))));
	}
	
	public Set<String> minAvgRacoFilter(Set<String> primaryCandidates, Set<String> secondaryCandidates, final double minAvgRaco) throws IOException {
		Set<String> passing = new HashSet<>();
		for(String secondaryCandidate : secondaryCandidates) {
			double sum = 0.0;
			for(String primaryCandidate : primaryCandidates) {
				sum += raco(primaryCandidate, secondaryCandidate);
			}
			double avgRaco = sum / primaryCandidates.size();
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
		final Set<String> racs = new HashSet<>();
		for(String relatedArticle : articlesLinkedTo(relatedToArticle)) {
			racs.addAll(categoriesContaining(relatedArticle));
		}
		return racs;
	}
	
	private static int docNum(String label, IndexSearcher s) throws IOException {
		TopDocs td = s.search(new TermQuery(new Term(Fields.label, label.replace(' ', '_'))), 1);
		return td.scoreDocs[0].doc;
	}
	
	private Set<String> articlesLinkedTo(String fromArticle) throws IOException {
		try {
			final int docNum = docNum(fromArticle, links);
			Document d = links.doc(docNum);
			String[] linkedPages = d.getValues(Fields.linkedPage);
			
			return new HashSet<>(Arrays.asList(linkedPages));
		} catch(ArrayIndexOutOfBoundsException e) {
//			System.err.println(fromArticle);
			return Collections.emptySet();
		}
	}
	
	private Set<String> categoriesContaining(String childArticle) throws IOException {
		try {
			final int docNum = docNum(childArticle, articleCategories);
			Document d = articleCategories.doc(docNum);
			String[] categoriesContaining = d.getValues(Fields.articleCategory);
			return new HashSet<>(Arrays.asList(categoriesContaining));
		} catch(ArrayIndexOutOfBoundsException e) {
//			System.err.println(childArticle);
			return Collections.emptySet();
		}
	}
}
