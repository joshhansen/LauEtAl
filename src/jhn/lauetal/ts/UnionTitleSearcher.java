package jhn.lauetal.ts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jhn.util.Util;

public class UnionTitleSearcher implements TitleSearcher, AutoCloseable {
	private final int n;
	private final OrderedTitleSearcher[] searchers;
	public UnionTitleSearcher(int topN, OrderedTitleSearcher... searchers) {
		this.n = topN;
		this.searchers = searchers;
	}
	
	@Override
	public Set<String> titles(String... terms) throws Exception {
		Set<String> titles = new HashSet<>();
		
		for(OrderedTitleSearcher ts : searchers) {
			List<String> subTitles = ts.titles(terms);
			if(subTitles.size() <= n) {
				titles.addAll(subTitles);
			} else {
				titles.addAll(subTitles.subList(0, n));
			}
		}
		
		return titles;
	}

	@Override
	public void close() throws Exception {
		for(OrderedTitleSearcher searcher : searchers) {
			Util.closeIfPossible(searcher);
		}
	}

}
