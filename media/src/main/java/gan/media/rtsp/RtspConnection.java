package gan.media.rtsp;

import android.os.Handler;
import gan.core.system.server.SystemServer;
import gan.core.utils.Base64;
import gan.core.utils.Encrypter;
import gan.core.utils.TextUtils;
import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.network.NetParamsMap;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RtspConnection implements Closeable{

    final static String charsetName = "UTF-8";
    final static String end = "\r\n";

	Socket mSocket;
	String mHost="";
	int mPort;
	String mUrl;
	BufferedReader mBufferedReader;
	OutputStream mOutputStream;
	InputStream	mInputStream;
	private int mCSeq;
    private String mSessionID;
    private String mAuthorization;
    private long mTimestamp;
    private String mUserName = "", mPassword = "";
    private String mAgent = "gan_1.0.0";
	private int mRtpPort;
	private int mTimeOut;//秒
	private String mSdp;
	private RtspSdpParser mRtspSdpParser;
	FileLogger mLogger;

	public RtspConnection(String url){
		this(url,"","");
	}

	public RtspConnection(String url,String userName,String password){
		mUrl = url;
		mUserName = userName;
		mPassword = password;
		if(url.contains("@")){//大华摄像头
			try{
				String userAndPassword = url.split("@")[0]
						.replace("rtsp://","");
				if(userAndPassword.contains(":")){
					mUrl = mUrl.replace(userAndPassword+"@","");
					String[] usePassword = userAndPassword.split(":");
					mUserName = usePassword[0];
					mPassword = usePassword[1];
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		URI uri = URI.create(mUrl);
		mHost = uri.getHost();
		mPort = uri.getPort();
		if(mPort<=0){
			mPort = 554;
		}
		long uptime = System.currentTimeMillis();
		mTimestamp = uptime/1000;
	}

	public void setPort(int port){
		mPort = port;
	}

	public void setAgent(String agent) {
		this.mAgent = agent;
	}

	public void connect() throws Exception {
		if(mPort<=0){
			throw new IllegalArgumentException("please set port");
		}
		initLogger();
		mLogger.log(String.format("connect:%s",mUrl));
		mSocket = new Socket(mHost, mPort);
		mLogger.log(String.format("connect success"));
		mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream=mSocket.getInputStream()));
		mOutputStream = mSocket.getOutputStream();
		mRtpPort = mSocket.getLocalPort();
	}

	public FileLogger initLogger(){
		return mLogger = FileLogger.getInstance("/rtsp/"+URLEncoder.encode(mHost));
	}

	public void close() throws IOException {
		mLogger.log("close");
		try {
			sendRequestTeardown();
		} catch (Exception ignore) {
		}
		stopHeartBeat();
		try {
			if(mSocket!=null) {
				mSocket.close();
			}
		} finally {
			try {
				if(mBufferedReader!=null) {
					mBufferedReader.close();
				}
			} finally {
				if(mOutputStream!=null) {
					mOutputStream.close();
				}
			}
		}
	}

	public void addCsep(){
		mCSeq++;
	}

	private String addHeaders() {
        StringBuffer sb = new StringBuffer();
        sb.append("CSeq: " + (++mCSeq) + "\r\n")
                .append((mSessionID != null ? "Session: " + mSessionID + "\r\n" :""))
                .append("User-Agent: " + mAgent + "\r\n");
        return sb.toString();
    }

    private String authorization2(String public_method){
	    if(TextUtils.isEmpty(mAuthorization)){
	        return "";
        }
		String authorization = mAuthorization;
	    if(authorization.startsWith("Basic")){
			try {
				return "Authorization: Basic "+Base64.encode(new String(mUserName+":"+mPassword).getBytes(charsetName))+end;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
		}else{
			HashMap<String,String> values = mapValues(authorization);
			String nonce = values.get("nonce").replace("\"", "");
			String realm = values.get("digest realm").replace("\"", "");
			String a1 = Encrypter.encryptByMD5(mUserName+":"+realm+":"+mPassword);
			String a2 = Encrypter.encryptByMD5(public_method+":"+mUrl);
			String responseStr= Encrypter.encryptByMD5(a1+ ":"+nonce+":"+a2);
			return "Authorization: Digest "+"username="+'"'+mUserName+'"'
					+ ","+authorization.replace("Digest","")
					+", uri="+'"'+mUrl+'"'+", response="+'"'+responseStr+'"'+end;
		}
    }

    private String authorization(String public_method){
		return authorization2(public_method);
//		if(TextUtils.isEmpty(mAuthorization)){
//			return "";
//		}
//        String authorization = mAuthorization;
//        HashMap<String,String> values = mapValues(authorization);
//		String nonce = values.get("nonce").replace("\"", "");
//		String realm = values.get("digest realm").replace("\"", "");
//        String responseStr = Encrypter.encryptByMD5(Encrypter.encryptByMD5(mPassword)
//				+":"+nonce+":"+ Encrypter.encryptByMD5(public_method+":"+mUrl));
////        return "Authorization: "+authorization+", username="+'"'+mUserName+'"'
////				+", uri="+'"'+mUrl+'"'+", response="+'"'+responseStr+'"'+end;
//		return "Authorization: Digest "+"username="+'"'+mUserName+'"'
//				+ ","+authorization.replace("Digest","")
//				+", uri="+'"'+mUrl+'"'+", response="+'"'+responseStr+'"'+end;
    }

	/** 
     * Forges and sends the OPTIONS request  
     */  
    public boolean sendRequestOption() throws IOException {
        String request = "OPTIONS " + mUrl + " RTSP/1.0\r\n" + addHeaders()+end;
		mLogger.log("request:%s",request);
        mOutputStream.write(request.getBytes(charsetName));
        Response response = Response.parseResponse(mBufferedReader);
		mLogger.log("response:%s",response.status);
        if (response.status == 200){
            return true;
        }else if(response.status == 401){
			mAuthorization = response.headers.get("www-authenticate");
            request = "OPTIONS " + mUrl + " RTSP/1.0\r\n" + addHeaders()+
                    authorization("OPTIONS")+end;
			mLogger.log("request2:%s",request);
            mOutputStream.write(request.getBytes(charsetName));
            response = Response.parseResponse(mBufferedReader);
			mLogger.log("response:%s",response.status);
            return response.status == 200;
        }
        return false;
    }

    public HashMap<String,String> mapValues(String value){
        if(value.contains(",")){
            HashMap<String,String> mapValues = new NetParamsMap();
            for(String s:value.split(",")){
                String[] ss= s.split("=");
                mapValues.put(ss[0].trim(),ss[1].trim());
            }
            return mapValues;
        }
        return null;
    }

	public Response sendRequestDESCRIBE() throws IOException {
		String request = "DESCRIBE "+ mUrl +" RTSP/1.0\r\n"+
				"Accept: "+"application/sdp"+"\r\n"+
                addHeaders()+authorization("DESCRIBE")+end;
		mLogger.log("request:%s",request);
        mOutputStream.write(request.getBytes(charsetName));
		Response response = Response.parseResponse(mBufferedReader);
		mLogger.log("response:%s",response.status);
		if(response.status == 200){
			handleRequestDESCRIBE(response);
		}else if(response.status == 401){
			mAuthorization = response.headers.get("www-authenticate");
			request = "DESCRIBE "+ mUrl +" RTSP/1.0\r\n"+
					"Accept: "+"application/sdp"+"\r\n"+
					addHeaders()+authorization("DESCRIBE")+end;
			mLogger.log("request2:%s",request);
			mOutputStream.write(request.getBytes(charsetName));
			response = Response.parseResponse(mBufferedReader);
			mLogger.log("response:%s",response.status);
			if(response.status == 200){
				handleRequestDESCRIBE(response);
			}
		}
		return response;
	}

	protected void handleRequestDESCRIBE(Response response) throws IOException {
		mSdp = response.content;
		mRtspSdpParser = new RtspSdpParser();
		if(!TextUtils.isEmpty(mSdp)){
			mRtspSdpParser.parserSdp(mSdp);
		}
	}

	public void sendRtspRequest(String rtsp) throws IOException {
    	String method = rtsp.substring(0,rtsp.indexOf(" "));
    	if(!TextUtils.isEmpty(method)){
    		StringBuilder sb = new StringBuilder(rtsp);
    		sb.append(addHeaders()).append(authorization(method)).append(end);
    		String request = sb.toString();
			mLogger.log("request:%s",request);
    		send(request);
    	}
	}

	public void send(String request) throws IOException {
		mOutputStream.write(request.getBytes(charsetName));
	}

	Runnable mHeartBeatRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				String request = "OPTIONS " + mUrl + " RTSP/1.0\r\n" + addHeaders()+end;
				mOutputStream.write(request.getBytes(charsetName));
				Handler handler = SystemServer.getMainHandler();
				handler.postDelayed(this,mTimeOut*1000);
			} catch (IOException e) {
				//socket closed
			}
		}
	};

	private void runHeartBeat(){
		Handler handler = SystemServer.getMainHandler();
		handler.removeCallbacks(mHeartBeatRunnable);
		handler.postDelayed(mHeartBeatRunnable, mTimeOut*1000);
	}

	private void stopHeartBeat(){
		Handler handler = SystemServer.getMainHandler();
		handler.removeCallbacks(mHeartBeatRunnable);
	}

    public Response sendRequestAnnounce(String sdp) throws IllegalStateException, IOException {
        String body = sdp;
        String request = "ANNOUNCE "+ mUrl +" RTSP/1.0\r\n" +
                addHeaders()+
                "Content-Type: application/sdp\r\n" +  
                "Content-Length: " + body.length() + "\r\n\r\n" +  
                body;
		mLogger.log("request:%s",request);
        mOutputStream.write(request.getBytes("UTF-8"));
        Response response = Response.parseResponse(mBufferedReader);
		mLogger.log("response:%s",response.status);
        if (response.headers.containsKey("server")) {
			DebugLog.info("RTSP server name:" + response.headers.get("server"));
        } else {
			DebugLog.info("RTSP server name unknown");
        }  
        if (response.status == 401) {  
            String nonce, realm;
			if (mUserName == null || mPassword == null) {
				throw new IllegalStateException("Authentication is enabled and setCredentials(String,String) was not called !");
			}
			mAuthorization = response.headers.get("www-authenticate");
			Matcher m;
			try {
				m = Response.rexegAuthenticate.matcher(response.headers.get("www-authenticate"));
				m.find();
			} catch (Exception e) {
				throw new IOException("Invalid response from server");
			}
            request = "ANNOUNCE "+ mUrl +" RTSP/1.0\r\n" +
                    "Content-Type: application/sdp"+ "\r\n" +  
                    "Content-Length: " + body.length() + "\r\n" +
					addHeaders()+
					authorization2("ANNOUNCE")+
                    body+ "\r\n\r\n";
			mLogger.log("request:%s",request);
            mOutputStream.write(request.getBytes("UTF-8"));
            response = Response.parseResponse(mBufferedReader);
			mLogger.log("request:%s",response.status);
            if (response.status == 401) throw new RuntimeException("Bad credentials !");  
        } else if (response.status == 403) {  
            throw new RuntimeException("Access forbidden !");
        }
        return response;
    } 
    
    /**
	 * 推流setup
     * Forges and sends the SETUP request  
     */  
    public boolean sendRequestSetup() throws IllegalStateException, IOException {
    	int index = 0;
        for (int i= 0;i<2;i++) {
            int trackId = i;  
            String interleaved = index+"-"+(++index);
            index++;
            String request = "SETUP "+ mUrl +"/trackID="+trackId+" RTSP/1.0\r\n"
            		+ "Transport: RTP/AVP/TCP;unicast;mode=record;interleaved="+interleaved+"\r\n"
					+"x-Dynamic-Rate: 0\r\n"
					+ addHeaders()
                    +end;
			mLogger.log("request:%s",request);
            mOutputStream.write(request.getBytes(charsetName));
            Response response = Response.parseResponse(mBufferedReader);//
			mLogger.log("response:%s",response.status);
            if (i == 0){
            	try {
            		String session = response.headers.get("session").trim();
                    String sessionID = parseSession(session);
                    if(!TextUtils.isEmpty(sessionID)){
                    	mSessionID = sessionID;
					}
                    mTimeOut = parseTimeOut(session);
					mLogger.log("mSessionID: "+ mSessionID+ "response.status:"+response.status);
                } catch (Exception e) {  
                    e.printStackTrace();
                }  
            }  
            if (response.status != 200){
				mLogger.log("return for resp :" +response.status);
                return false;
            }
        }
        return true;  
    }

    private String parseSession(String session){
		if(session.contains(";")) {
			return session.split(";")[0];
		}
		return session;
	}

    private int parseTimeOut(String session){
    	try{
			if(session.contains(";")){
				HashMap<String,String> values = new NetParamsMap();
				for(String value:session.split(";")){
					if(value.contains("=")){
						String[] keyValue = value.split("=");
						values.put(keyValue[0],keyValue[1]);
					}
				}
				return Integer.valueOf(values.get("timeout"));
			}
		}catch (Exception e){
    		e.printStackTrace();
		}
		return 60;
	}

	public Collection<String> getTrackIds() {
		return mRtspSdpParser.getTrackIds();
	}

	public String findServerUrl(){
    	final String serverIp = mRtspSdpParser.getServerIp();
    	if(TextUtils.isEmpty(serverIp)){
			return mUrl;
		}
		if("0.0.0.0".equals(serverIp)
				||"127.0.0.1".equals(serverIp)){
			return mUrl;
		}
		try{
			URI uri = URI.create(mUrl);
			String host = uri.getHost();
			return mUrl.replace(host,serverIp);
		}catch (Exception e){
    		e.printStackTrace();
    		return mUrl;
		}
	}

	/**
	 * 拉流
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public boolean sendRequestSetup2() throws IllegalStateException, IOException {
		ArrayList<String> trackIds = mRtspSdpParser.getTrackIds();
		if(trackIds==null||trackIds.isEmpty()){
			trackIds = new ArrayList<>();
			trackIds.add("trackId=0");
			trackIds.add("trackId=1");
		}

		int index = 0;
		for (String trackId:trackIds) {
			String interleaved = index+"-"+(++index);
			index++;
			StringBuffer sb = new StringBuffer();
			sb.append(String.format("SETUP %s/%s RTSP/1.0",findServerUrl(),trackId)).append(end)
					.append(addHeaders()).append(authorization("SETUP"))
					.append(String.format("Transport: RTP/AVP/TCP;unicast;interleaved=%s", interleaved)).append(end)
					//.append("x-Dynamic-Rate: 0").append(end)
					.append(end);
			String request = sb.toString();
			mLogger.log("request:%s",request);
			mOutputStream.write(request.getBytes("UTF-8"));
			Response response = Response.parseResponse(mBufferedReader);
			mLogger.log("response:%s",response.status);
			try {
				String session = response.headers.get("session");
				if(session!=null){
					session = session.trim();
					String sessionID = parseSession(session);
					if(!TextUtils.isEmpty(sessionID)){
						mSessionID = sessionID;
					}
					mTimeOut = parseTimeOut(session);
					mLogger.log("mSessionID: "+ mSessionID+ ",response.status:"+response.status);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (response.status != 200){
				mLogger.log("return for resp :%s", response.status);
				return false;
			}
		}
		return true;
	}
    
    /** 
     * Forges and sends the RECORD request  
     */  
    public boolean sendRequestPlay() throws IllegalStateException, SocketException, IOException {  
        String request = "PLAY "+ findServerUrl() +" RTSP/1.0\r\n" +
				"Range: npt=0.000-\r\n" +
                addHeaders()+
                authorization("PLAY")+
				end;
		mLogger.log("request:%s",request);
        mOutputStream.write(request.getBytes("UTF-8"));
        Response response = Response.parseResponse(mBufferedReader);
		mLogger.log("response:%s",response.status);
        if (response.status != 200){
            return false;  
        }
		runHeartBeat();
        return true;
    }

	/**
	 * Forges and sends the RECORD request
	 */
	public boolean sendRequestFORCEIFRAME() throws IllegalStateException, SocketException, IOException {
		String request = "FORCEIFRAME "+ mUrl +" RTSP/1.0\r\n" +
				addHeaders()+
				"forceiframe: 1\r\n" +
				authorization("FORCEIFRAME")+
				end;
		mLogger.log("request:%s",request);
		mOutputStream.write(request.getBytes("UTF-8"));
		Response response = Response.parseResponse(mBufferedReader);
		mLogger.log("response:%s",response.status);
		if (response.status != 200){
			return false;
		}
		runHeartBeat();
		return true;
	}
    
    /**
	 * Forges and sends the TEARDOWN request 
	 */
	public void sendRequestTeardown() throws IOException {
		String request = "TEARDOWN "+ mUrl +" RTSP/1.0\r\n" + addHeaders()+end;
		mLogger.log("request:%s",request);
		mOutputStream.write(request.getBytes("UTF-8"));
	}

	public InputStream getInputStream() {
		return mInputStream;
	}

    final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for ( int j = 0; j < bytes.length; j++ ) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/** Needed for the Digest Access Authentication. */
	private String computeMd5Hash(String buffer) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			return bytesToHex(md.digest(buffer.getBytes("UTF-8")));
		} catch (NoSuchAlgorithmException ignore) {
		} catch (UnsupportedEncodingException e) {}
		return "";
	}

	public void send(int b) throws IOException {
		mOutputStream.write(b);
	}

	public void send(byte[] packet) throws IOException {
		mOutputStream.write(packet);
	}

	public void send(byte[] b, int off, int len) throws IOException {
		mOutputStream.write(b,off,len);
	}

	public void reconnect(int timeout) throws IOException {
		SocketAddress address = new InetSocketAddress(mHost,mPort);
		mSocket.connect(address,timeout);
	}

	public static class Response {
		private static final String TAG = "Response";
		// Parses method & uri
		public static final Pattern regexStatus = Pattern.compile("RTSP/\\d.\\d (\\d+) (\\w+)",Pattern.CASE_INSENSITIVE);
		// Parses a request header
		public static final Pattern rexegHeader = Pattern.compile("(\\S+):(.+)",Pattern.CASE_INSENSITIVE);
		// Parses a WWW-Authenticate header
		public static final Pattern rexegAuthenticate = Pattern.compile("realm=\"(.+)\",\\s+nonce=\"(\\w+)\"",Pattern.CASE_INSENSITIVE);
		// Parses a Session header
		public static final Pattern rexegSession = Pattern.compile("(\\d+)",Pattern.CASE_INSENSITIVE);
		// Parses a Transport header
		public static final Pattern rexegTransport = Pattern.compile("client_port=(\\d+)-(\\d+).+server_port=(\\d+)-(\\d+)",Pattern.CASE_INSENSITIVE);
		public int status;
		public HashMap<String,String> headers = new HashMap<String,String>();
		public String content;
		public String responseStr;

		/** Parse the method, uri & headers of a RTSP request */
		public static Response parseResponse(BufferedReader input) throws IOException, IllegalStateException, SocketException {
			Response response = new Response();
			String line;
			Matcher matcher;
			// Parsing request method & uri
			if ((line = input.readLine())==null){
				throw new SocketException("Connection lost");
			}

			while(!line.startsWith("RTSP")){
				line = input.readLine();
				if(line == null){
					throw new SocketException("Connection lost");
				}
			}

			StringBuffer responseStr = new StringBuffer();
			responseStr.append(line).append(end);
			matcher = regexStatus.matcher(line);
			matcher.find();
			response.status = Integer.parseInt(matcher.group(1));

			// Parsing headers of the request
			while ( (line = input.readLine()) != null) {
				if (line.length()>3) {
				    responseStr.append(line).append(end);
					matcher = rexegHeader.matcher(line);
					if(matcher.find()){
						response.headers.put(matcher.group(1).toLowerCase(Locale.US),
								matcher.group(2).trim());
					}
				} else {
					break;
				}
			}

            DebugLog.info("Response:"+responseStr);
			if(response.headers.containsKey("content-length")){
				int content_len = Integer.valueOf(response.headers.get("content-length"));
				if(content_len>0){
					response.content = parseContent(input,content_len);
				}
			}

			if(!TextUtils.isEmpty(response.content)){
                DebugLog.info("Response content:\r\n"+response.content);
            }

			response.responseStr = responseStr.toString();
			return response;
		}
	}

	public static String parseContent(BufferedReader br,int content_len) throws IOException{
		try{
			char[] buf = new char[content_len];
			int len = br.read(buf,0,content_len);
			return new String(buf,0,len);
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

}