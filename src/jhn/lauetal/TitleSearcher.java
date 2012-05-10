package jhn.lauetal;

import java.util.List;

public interface TitleSearcher {
	List<String> topTitles(String... terms) throws Exception;
}