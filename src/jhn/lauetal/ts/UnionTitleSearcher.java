package jhn.lauetal.ts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnionTitleSearcher implements TitleSearcher {
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

}
