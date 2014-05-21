package com.sukohi.lib;

/*  Dependency: httpcomponents-client for Upload (http://hc.apache.org/downloads.cgi)  */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

/**
 * ver 4.3 (21 May, 2014)
 * @author Sukohi Kuhoh
 */
public class WebCode extends AsyncTask<String, Void, String> {

	public static final int STATUS_START = 1;
	public static final int STATUS_PROGRESS = 2;
	public static final int STATUS_END = 3;
	private final int DEFAULT_CONNECTION_TIMEOUT = 15000;
	private final int DEFAULT_SOCKET_TIMEOUT = 15000;
	private final int BUFFER_SIZE = 8192;
	private String code, savePath;
	private String encoding = HTTP.UTF_8;
	private long downloadedLength, downloadContentLength;
	private int downloadedPercentage;
	private int httpStatusCode = HttpStatus.SC_REQUEST_TIMEOUT;
	private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
	private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
	private boolean responseResult = false;
	private byte[] byteArrayCode;
	private List<NameValuePair> postParams;
	private WebCodeFileData fileData;
	private HttpEntity entityCode;
	private WebCodeCallback callback;
	private WebCodeProgressCallback progressCallback;
	private enum Http_Method { GET, POST }
	private Http_Method httpMethod;
	private enum Connection_Mode { DEFAULT, DOWNLOAD, UPLOAD }
	private Connection_Mode connectionMode;
	
	public WebCode() {}
	
	public void setEncoding(String encoding) {
	
		this.encoding = encoding;
	
	}
	
	public void setConnectionTimeout(int milliseconds) {
		
		connectionTimeout = milliseconds;
		
	}
	
	public void setSocketTimeout(int milliseconds) {
		
		socketTimeout = milliseconds;
		
	}
	
	public void get(String url) {
		
		httpMethod = Http_Method.GET;
		connectionMode = Connection_Mode.DEFAULT;
		execute(url);
		
	}
	
	public void post(String url, Map<String, String> paramsMap) {
		
		httpMethod = Http_Method.POST;
		connectionMode = Connection_Mode.DEFAULT;
		setPostParams(paramsMap);
		execute(url);
		
	}
	
	public void getDownload(String url, String savePath) {

		httpMethod = Http_Method.GET;
		connectionMode = Connection_Mode.DOWNLOAD;
		this.savePath = savePath;
		execute(url);
		
	}
	
	public void postDownload(String url, String savePath, Map<String, String> paramsMap) {

		httpMethod = Http_Method.POST;
		connectionMode = Connection_Mode.DOWNLOAD;
		this.savePath = savePath;
		setPostParams(paramsMap);
		execute(url);
		
	}
	
	public void upload(String url, WebCodeFileData fileData, Map<String, String> paramsMap) {
		
		httpMethod = Http_Method.POST;
		connectionMode = Connection_Mode.UPLOAD;
		this.fileData = fileData;
		setPostParams(paramsMap);
		execute(url);
		
	}
	
	public long getDownloadedLength() {
		
		return downloadedLength;
		
	}
	
	public int getDownloadedPercentage() {
		
		return downloadedPercentage;
		
	}
	
	public long getDownloadContentLength() {
		
		return downloadContentLength;
		
	}

	public void setCallback(WebCodeCallback callback) {
		
		this.callback = callback;
		
	}
	
	public void setProgressCallback(WebCodeProgressCallback callback) {
		
		progressCallback = callback;
		
	}
	
	private void doProgressCallback(int statusCode) {
		
		if(progressCallback != null) {
			
			progressCallback.getStatusCode(statusCode);
			
		}
		
	}

	private void setPostParams(Map<String, String> paramsMap) {
		
		postParams = new ArrayList<NameValuePair>();
		
		for (String key : paramsMap.keySet()) {

			postParams.add(new BasicNameValuePair(key, paramsMap.get(key)));
			
		}
		
	}

	@Override
	protected String doInBackground(String... params) {

		doProgressCallback(STATUS_START);
		
		HttpClient client = new DefaultHttpClient();
		HttpParams clientParams = client.getParams();
		HttpConnectionParams.setConnectionTimeout(clientParams, connectionTimeout);
		HttpConnectionParams.setSoTimeout(clientParams, socketTimeout);
		
		code = "";
		byteArrayCode = null;
		responseResult = false;

		try {
			
			doProgressCallback(STATUS_PROGRESS);
			String url = params[0];
			HttpResponse response;
			
			if(httpMethod == Http_Method.GET) {
				
				HttpGet httpGet = new HttpGet(url);
				response = client.execute(httpGet);
				
			} else {
				
				HttpPost httpPost = new HttpPost(url);
				
				if(connectionMode == Connection_Mode.DEFAULT) {
					
					httpPost.setEntity(new UrlEncodedFormEntity(postParams, encoding));
					
				} else if(connectionMode == Connection_Mode.UPLOAD) {
					
					int fileDataCount = fileData.dataCount;
					MultipartEntityBuilder builder = MultipartEntityBuilder.create();    
				    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);    
					
				    for (int i = 0; i < postParams.size(); i++) {
						
				    	builder.addTextBody(postParams.get(i).getName(), postParams.get(i).getValue());
				    	
					}
				    
					for (int j = 0; j < fileDataCount; j++) {
						
					    String filePath = fileData.filePathes.get(j);
					    String name = fileData.names.get(j);
					    File file = new File(filePath);
					    
					    if(file.exists()) {

						    FileBody fileBody = new FileBody(file);
						    builder.addPart(name, fileBody);
					    	
					    }

					}
					
					httpPost.setEntity(builder.build());
					connectionMode = Connection_Mode.DEFAULT;
					
				}
				
				response = client.execute(httpPost);
				
			}
			
			StatusLine statusLine = response.getStatusLine();
			httpStatusCode = statusLine.getStatusCode();
			
			if(httpStatusCode == HttpStatus.SC_OK) {
				
				if(connectionMode == Connection_Mode.DEFAULT) {
					
					entityCode = response.getEntity();
					byteArrayCode = EntityUtils.toByteArray(entityCode);
					code = new String(byteArrayCode, encoding);
					responseResult = true;
					
				} else if(connectionMode == Connection_Mode.DOWNLOAD) {
					
					entityCode = response.getEntity();
					downloadContentLength = entityCode.getContentLength();
					
					File file = new File(savePath);
		            BufferedInputStream inputStream = new BufferedInputStream(entityCode.getContent(), BUFFER_SIZE);
		            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file, false), BUFFER_SIZE);
		            byte buffer[] = new byte[BUFFER_SIZE];
		            int length = -1;
		            
		            while((length = inputStream.read(buffer)) != -1) {
		            	
		            	downloadedLength += length;
		            	
		                if(downloadContentLength > 0) {
		                	
		                	downloadedPercentage = (int) ((downloadedLength * 100) / downloadContentLength);
		                    
		                } else {
		                	
		                	downloadedPercentage = -1;
		                	
		                }
		            	
		                doProgressCallback(STATUS_PROGRESS);
		            	outputStream.write(buffer, 0, length);
		                
		            }
		            
		            outputStream.flush();
		            outputStream.close();
		            inputStream.close();
					
				}
				
			}
			
		} catch(Exception e){
		} finally {
			
			client.getConnectionManager().shutdown(); 
			
		}
		
		return null;
		
	}
	
	@Override
	protected void onPostExecute(String result) {
		
		doProgressCallback(STATUS_END);
		
    	if(connectionMode == Connection_Mode.DEFAULT) {

    		WebCodeResult webCodeResult = new WebCodeResult();
    		webCodeResult.setResult(responseResult);
    		webCodeResult.setCode(code);
    		webCodeResult.setStatusCode(httpStatusCode);
    		webCodeResult.setByteArrayCode(byteArrayCode);
    		
    		if(callback != null) {
    			
    			callback.getResult(webCodeResult);
    			
    		}
    		
    	}

	}

    protected void onProgressUpdate(Integer... values) {
    	
        doProgressCallback(STATUS_PROGRESS);
        
    }
	
	public static class WebCodeCallback {
		
		public void getResult(WebCodeResult result) {}
		
	}

	public static class WebCodeResult {
		
		private Boolean responseResult;
		private String code;
		private int statusCode;
		private byte[] byteArrayCode;
		
		public void setResult(Boolean result) {
			
			responseResult = result;
			
		}
		
		public Boolean getResult() {
			
			return responseResult;
			
		}
		
		public void setCode(String resultCode) {
			
			code = resultCode; 
			
		}
		
		public String getCode() {
			
			return code;
			
		}
		
		public void setStatusCode(int resultStatusCode) {
			
			statusCode = resultStatusCode;
			
		}
		
		public int getStatusCode() {
			
			return statusCode;
			
		}
		
		public void setByteArrayCode(byte[] resultByteArray) {
			
			byteArrayCode = resultByteArray;
			
		}
		
		public byte[] getByteArrayCode() {
			
			return byteArrayCode;
			
		}
		
		public Bitmap getBitmapCode(BitmapFactory.Options options) {
			
			Bitmap bitmap = null;
			
			if(byteArrayCode != null) {
    			
				bitmap = BitmapFactory.decodeByteArray(byteArrayCode, 0, byteArrayCode.length, options); 
			
			}
			
			return bitmap;
			
		}
		
	}
	
	public static class WebCodeProgressCallback {
	
		public void getStatusCode(int statusCodes) {}
		
	}
		
	public static class WebCodeFileData {

		private int dataCount = 0;
		private List<String> names = new ArrayList<String>();
		private List<String> filePathes = new ArrayList<String>();
		
		public void put(String name, String filePath) {
			
			dataCount++;
			names.add(name);
			filePathes.add(filePath);
			
		}
		
	}
	
}
/*** Example

	WebCode webCode = new WebCode();
	webCode.setEncoding(HTTP.UTF_8);		// Skippable (Default: HTTP.UTF_8)
	webCode.setBitmapOptions(options);		// Skippable
	webCode.setConnectionTimeout(15000);	// Skippable (Default: 15 seconds)
	webCode.setSocketTimeout(15000);		// Skippable (Default: 15 seconds)
	webCode.setCallback(new WebCodeCallback(){
		
		@Override
		public void getResult(WebCodeResult webCodeResult) {
			
			int statusCode = webCodeResult.getStatusCode();
			
			if(webCodeResult.getResult()) {
				
				String code = webCodeResult.getCode();
				byte[] byteArrayCode = webCodeResult.getByteArrayCode();
				
				Bitmap bitmap = webCodeResult.getBitmapCode(BitmapFactoryOptions_or_Null);
				
			} else {
					
				switch (statusCode) {

				case HttpStatus.SC_NOT_FOUND: 
					
					// Not Found 404

					break;
					
				case HttpStatus.SC_REQUEST_TIMEOUT:
				
					// Timeout 408

					break;
					
				}
					
			}
			
		}
		
	});
	webCode.setProgressCallback(new WebCodeProgressCallback(){
		
		@Override
		public void getStatusCode(int statusCodes) {
			
			switch (statusCodes) {

			case WebCode.STATUS_START:
				
				System.out.println("Start");
				break;
				
			case WebCode.STATUS_PROGRESS:
				
				System.out.println("Progress");
				
				// For download

				long downloadedLength = webCode.getDownloadedLength();
				int downloadedPercentage = webCode.getDownloadedPercentage();
				long downloadContentLength = webCode.getDownloadContentLength();
				
				break;

			case WebCode.STATUS_END:
				
				System.out.println("End");
				break;
				
			}
			
		}
		
	});
	
	webCode.get("http://example.com/");								// Get
	webCode.post("http://example.com/", paramsMap);							// Post
	webCode.getDownload("http://example.com/sample.zip", savePath);					// Get download
	webCode.postDownload("http://example.com/sample.zip", savePath, paramsMap_or_Null)		// Post download
	
	// Upload
	
	WebCodeFileData fileData = new WebCodeFileData();
	fileData.put("file_1", "/file/path/");
	fileData.put("file_2", "/file/path2/");
	webCode.upload("http://www.digioss.com/upload_test.php", fileData, paramsMap_or_Null);
	
***/
