package jhn.lauetal.tc;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class HTTPTitleChecker implements TitleChecker {

	private static URL urlify(String s) throws URISyntaxException, MalformedURLException {
		URI uri = new URI(
				"http",
				"en.wikipedia.org",
				"/wiki/" + s,
				null);
		return uri.toURL();
	}
	
	@Override
	public boolean isWikipediaArticleTitle(String s) throws MalformedURLException, IOException, URISyntaxException {
		HttpURLConnection connection = (HttpURLConnection) urlify(s).openConnection();
		connection.setRequestMethod("HEAD");
		int responseCode = connection.getResponseCode();
		return responseCode != 404;
	}

}
