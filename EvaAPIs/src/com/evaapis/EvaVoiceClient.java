/*
 	Wraps a voice streamer in a http post request to Eva server
 */
package com.evaapis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.evature.util.EvatureSSLSocketFactory;

public class EvaVoiceClient {

	private static final String TAG = "EvaVoiceClient";
	
	private String mSiteCode = "UNKNOWN";
	private String mAppKey = "UNKNOWN";
    private String mDeviceId = "0000";
	private String LANGUAGE = "ENUS";
	private String CODEC = "audio/x-speex;rate=16000";	//MP3
	private String RESULTS_FORMAT = "text/plain";


	private static short PORT = (short) 443;
	private static String HOSTNAME = "vproxy.evaws.com";

	private String mEvaJson;

	private SpeechAudioStreamer mSpeechAudioStreamer;


	private boolean mInTransaction = false;

	public EvaVoiceClient(String siteCode, String appKey, String deviceId, SpeechAudioStreamer speechAudioStreamer) {
		mSiteCode = siteCode;
		mAppKey = appKey;
		mDeviceId = deviceId;
		mSpeechAudioStreamer = speechAudioStreamer;	
	}

	@SuppressWarnings("deprecation")
	private HttpClient getHttpClient() throws NoSuchAlgorithmException, KeyManagementException
	{
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setUseExpectContinue(params, false);

		// Initialize the HTTP client
		HttpClient httpclient = new DefaultHttpClient(params);

		SSLSocketFactory sf=null;
		try {
			sf = new EvatureSSLSocketFactory(null);
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Scheme sch = new Scheme("https", sf, PORT);
		httpclient.getConnectionManager().getSchemeRegistry().register(sch);

		return httpclient;
	}


	private URI getURI() throws Exception
	{
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();

		qparams.add(new BasicNameValuePair("site_code", mSiteCode));
		qparams.add(new BasicNameValuePair("api_key", mAppKey));
		qparams.add(new BasicNameValuePair("id",  mDeviceId));

		try {
			double longitude = EvatureLocationUpdater.getLongitude();
			double latitude = EvatureLocationUpdater.getLatitude();
			if (latitude != -1 && longitude != -1) {
				qparams.add(new BasicNameValuePair("longitude",""+longitude));
				qparams.add(new BasicNameValuePair("latitude",""+latitude));
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		URI uri = URIUtils.createURI("https", HOSTNAME, PORT, "", URLEncodedUtils.format(qparams, "UTF-8"), null);

		return uri;
	}

	/*
	 * This is a simpler helper function to setup the Header parameters
	 */
	private HttpPost getHeader(URI uri, long contentLength) throws UnsupportedEncodingException
	{
		HttpPost httppost = new HttpPost(uri);


		//	httppost.setHeader("Transfer-Encoding", "chunked");	
		//	httppost.addHeader("Transfer-Encoding", "chunked");

		httppost.addHeader("Content-Type",  CODEC);
		httppost.addHeader("Content-Language", LANGUAGE);
		httppost.addHeader("Accept-Language", LANGUAGE);
		httppost.addHeader("Accept", RESULTS_FORMAT);
//		String LM = "Dictation";	// or WebSearch
//		httppost.addHeader("Accept-Topic", LM);	

		return httppost;
	}

	/***
	 * Start the recording and return it as 
	 * @param speechAudioStreamer
	 * @return
	 * @throws NumberFormatException
	 * @throws Exception
	 */
	private InputStreamEntity setAudioContent(SpeechAudioStreamer speechAudioStreamer) throws NumberFormatException, Exception
	{		
		InputStreamEntity reqEntity  = new InputStreamEntity(speechAudioStreamer.getInputStream(), -1);
		speechAudioStreamer.start();

		reqEntity.setContentType(CODEC);

		reqEntity.setChunked(true);

		return reqEntity;
	}


	private StringBuilder inputStreamToString(InputStream is) {
		String line = "";
		StringBuilder total = new StringBuilder();

		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));

		// Read response until the end
		try {
			while ((line = rd.readLine()) != null) { 
				total.append(line); 
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Return full string
		return total;
	}

	private void processResponse(HttpResponse response) throws IllegalStateException, IOException
	{
		HttpEntity resEntity = response.getEntity();


		System.out.println(response.getStatusLine());
		Header [] h = response.getAllHeaders();

		for(int i=0;i<h.length;i++)
		{
			Log.i("EVA","Header:"+h[i].getName()+"="+h[i].getValue());
		}

		InputStream is = resEntity.getContent();

		mEvaJson = inputStreamToString(is).toString();

		Log.i(TAG, "Response: \n"+mEvaJson);
		
		resEntity.consumeContent();
	}
	
	public String getEvaJson()
	{
		return mEvaJson;
	}

	/**
	 * @param speechAudioStreamer 
	 * @param args
	 * @throws Exception 
	 */
	public void startDictation() throws Exception{

		mInTransaction = true;
		HttpClient httpclient = null;
		try {
			httpclient = getHttpClient();

			Log.i("EVA","Sending post request");

			InputStreamEntity reqEntity = setAudioContent(mSpeechAudioStreamer);
			URI uri = getURI();

			httppost = getHeader(uri, 0);	//fileSize);
			httppost.setEntity(reqEntity);		
			
			HttpResponse response = httpclient.execute(httppost);

			Log.i("EVA","After Sending post request");
			processResponse(response);

			if( httpclient != null ) {
				httpclient.getConnectionManager().shutdown();
				httpclient = null;
			}
		}
		catch(Exception e)	{
			e.printStackTrace();
			Log.e(TAG, "Exception sending voice request", e);
		}
		finally {
			// When HttpClient instance is no longer needed, 
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			if( httpclient != null ) 
				httpclient.getConnectionManager().shutdown();
			mInTransaction = false;
		}

	}
	
	static HttpPost httppost;
	
	public void stopTransfer()
	{
		if(getInTransaction())
		{
			httppost.abort();
			mInTransaction=false;
		}
	}

	public boolean getInTransaction()
	{
		return mInTransaction;
	}

}