package jhn.lauetal.ts;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.gson.Gson;

public class GoogleTitleSearcher implements OrderedTitleSearcher {
	private static final int NUM_RESULTS = 8;
	private static final String API_BASE = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&rsz=" + NUM_RESULTS + "&q=";
    private static final String CHARSET = "UTF-8";
    
    private final int maxTerms;
	public GoogleTitleSearcher(int maxTerms) {
		this.maxTerms = maxTerms;
	}

	@Override
	public List<String> titles(String... terms) throws Exception {
		StringBuilder search = new StringBuilder("site:en.wikipedia.org/wiki ");
		search.append(terms[0]);
		
		if(terms.length > 1) {
			for(int i = 1; i < Math.min(terms.length, maxTerms); i++) {
				search.append(' ');
				search.append(terms[i]);
			}
		}
		
	    URL url = new URL(API_BASE + URLEncoder.encode(search.toString(), CHARSET));
	    Reader reader = new InputStreamReader(url.openStream(), CHARSET);
	    GoogleResults results = new Gson().fromJson(reader, GoogleResults.class);
	    
	    List<String> titles = new ArrayList<>();
	    
	    for(Result result : results.getResponseData().getResults()) {
	    	String title = cleanTitle(result.getTitleNoFormatting());
	    	if(labelOK(title)) {
	    		titles.add(title);
	    	}
	    }
	    
	    return titles;
	}
	
	private static final Pattern wpTitleRgx = Pattern.compile("(.+?)( - Wiki.+)?( [.][.][.])?");
	private static String cleanTitle(String title) {
		return wpTitleRgx.matcher(title).replaceAll("$1");
	}
	
	private static final Pattern badLabels = Pattern.compile("^(?:(?:Portal|Wikipedia|Category|File|Template|Book|MediaWiki|Help|P|Talk|User):)|(?:(?:List|Glossary|Index) of )");
	private boolean labelOK(String label) {
		if(badLabels.matcher(label).find()) {
			return false;
		}
		
		if(label.contains("(disambiguation)")) {
			return false;
		}
		return true;
	}
	
	private static class GoogleResults {
		private ResponseData responseData;

		public ResponseData getResponseData() {
			return responseData;
		}

		@SuppressWarnings("unused")
		public void setResponseData(ResponseData responseData) {
			this.responseData = responseData;
		}

		@Override
		public String toString() {
			return "ResponseData[" + responseData + "]";
		}
	}
	
	private static class ResponseData {
		private List<Result> results;

		public List<Result> getResults() {
			return results;
		}

		@SuppressWarnings("unused")
		public void setResults(List<Result> results) {
			this.results = results;
		}

		@Override
		public String toString() {
			return "Results[" + results + "]";
		}
	}
	
	private static class Result {
		private String titleNoFormatting;
		private String url;
		private String title;

		public String getUrl() {
			return url;
		}

		public String getTitle() {
			return title;
		}

		@SuppressWarnings("unused")
		public void setUrl(String url) {
			this.url = url;
		}

		@SuppressWarnings("unused")
		public void setTitle(String title) {
			this.title = title;
		}
		
		@SuppressWarnings("unused")
		public void setTitleNoFormatting(String titleNoFormatting) {
			this.titleNoFormatting = titleNoFormatting;
		}

		@Override
		public String toString() {
			return "Result[url:" + url + ",title:" + title + "]";
		}

		public String getTitleNoFormatting() {
			return titleNoFormatting;
		}
	}
}
