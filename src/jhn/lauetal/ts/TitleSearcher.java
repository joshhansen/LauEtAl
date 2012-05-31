package jhn.lauetal.ts;

import java.util.Collection;

public interface TitleSearcher {
	Collection<String> titles(String... terms) throws Exception;
}