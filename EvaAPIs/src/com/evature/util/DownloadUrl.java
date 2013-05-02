package com.evature.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadUrl {

	// Given a URL, establishes an HttpUrlConnection and retrieves
	// the web page content as a InputStream, which it returns as a string.
	public static String get(String myurl) throws IOException {
		URL url = new URL(myurl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(20000 /* milliseconds */);
		conn.setConnectTimeout(25000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		// int response = conn.getResponseCode();
		// Read from web: http://stackoverflow.com/a/1381784/78234
		Reader r = new InputStreamReader(conn.getInputStream(), "UTF-8");
		StringBuilder buf = new StringBuilder();
		while (true) {
			int ch = r.read();
			if (ch < 0)
				break;
			buf.append((char) ch);
		}
		String str = buf.toString();
		return str;
	}

}