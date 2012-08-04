package jhn.lauetal.tc;

import jhn.util.Util;

public class OrderedTitleChecker implements TitleChecker, AutoCloseable {
	private TitleChecker[] checkers;
	public OrderedTitleChecker(TitleChecker... checkers) {
		this.checkers = checkers;
	}
	
	@Override
	public boolean isWikipediaArticleTitle(String s) throws Exception {
		for(TitleChecker checker : checkers) {
			if(checker.isWikipediaArticleTitle(s)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void close() throws Exception {
		for(TitleChecker tc : checkers) {
			Util.closeIfPossible(tc);
		}
	}

}
