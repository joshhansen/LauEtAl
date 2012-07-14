package jhn.lauetal.ts;

import java.util.List;

public interface OrderedTitleSearcher extends TitleSearcher {
	@Override
	List<String> titles(String... terms) throws Exception;
}
