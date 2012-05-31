package jhn.lauetal.ts;

import java.util.List;

public interface OrderedTitleSearcher extends TitleSearcher {
	List<String> titles(String... terms) throws Exception;
}
