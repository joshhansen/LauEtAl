package jhn.lauetal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class MediawikiAPI {
	private static final String API_SCHEME = "http";
	private static final String API_HOST = "en.wikipedia.org";
	
	private static String readRequest(String path) throws Exception {
		URI uri = new URI(API_SCHEME, API_HOST, path, null);
		return readURL(uri.toURL());
	}
	
	private static final String API_BASE = "http://en.wikipedia.org/w/api.php";
	private static String readURL(String urlS) throws IOException {
		return readURL(new URL(urlS));
	}
	
	private static String readURL(URL url) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
		
		StringBuilder text = new StringBuilder();
		String tmp = null;
		while( (tmp=r.readLine()) != null) {
			text.append(tmp);
			text.append('\n');
		}
		return text.toString();
	}
	
//	public static List<String> topTitles(String query) throws IOException {
//		return topTitles(query.split(" "));
//	}
	
	public static List<String> topTitles(String[] terms) throws IOException {
		final StringBuilder url = new StringBuilder(API_BASE);
		url.append("?action=query");
		url.append("&list=search");
		url.append("&format=json");
		url.append("&srsearch=");
		for(int i = 0; i < terms.length; i++) {
			if(i > 0) {
				url.append("+");
			}
			url.append(terms[i]);
		}
//		url.append("major+histocompatibility+complex+class+II+antigens");
		
		final String jsonS = readURL(url.toString());
		
		
		List<String> titles = new ArrayList<String>();
		JSONObject json = (JSONObject) JSONValue.parse(jsonS);
		JSONObject query = (JSONObject) json.get("query");
		JSONArray search = (JSONArray) query.get("search");
		for(Object entryO : search) {
			JSONObject entry = (JSONObject) entryO;
			titles.add(entry.get("title").toString());
		}
		return titles;
	}
}
