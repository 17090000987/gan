package gan.core.event;

public class EventCode{
	
	protected static int CODE_INC = 0;
	
	public static int generateEventCode(){
		return ++CODE_INC;
	}
	
	public static final int LoginActivityLaunched		= ++CODE_INC;
	
	/**
	 * @param IMMessage
     */
	public static final int DB_SaveMessage				= ++CODE_INC;
	
	/**
	 * @param id
	 * @param Collection(IMMessage)
     */
	public static final int DB_SaveAllMessage			= ++CODE_INC;
	
	/**
	 * @param otherSideId(不传将删除所有消息)
	 * @param msgId(不传将删除otherSideId对应的消息)
     */
	public static final int DB_DeleteMessage			= ++CODE_INC;
	
	/**
	 * @param String(url)
	 * @param String(filepath)
     */
	public static final int HTTP_Download				= ++CODE_INC;
	
	/**
	 * @param type
	 * @param upfile
	 * @return url,thumburl
     */
	public static final int HTTP_PostFile				= ++CODE_INC;
	
	/**
	 * @param id
	 * @param fromType
	 * @param direction：分页加载方向，0表示时间向之前加载，1表示时间向后加载
	 * @param datetime：分页加载的基准时间，毫秒级的时间戳，向后加载就大于它，向前加载就小于它，不传表示对应加载方向上的第一页
	 * 返回结果按时间从旧向新排序
	 * @return List(XMessage)
     */
	public static final int HTTP_GetChatRecord			= ++CODE_INC;
	
	public static final int AppBackground				= ++CODE_INC;
	
	/**
	 * @param backgroundElapsedTime(long)(在后台的总时间)
     */
	public static final int AppForceground				= ++CODE_INC;
	
	/**
	 * @param Object
     */
	public static final int HandleRecentChat			= ++CODE_INC;
	
	/**
	 * @param RecentChat(may be null)
     */
	public static final int UnreadMessageCountChanged	= ++CODE_INC;
	
	/**
	 * @param List(RecentChat)
     */
	public static final int RecentChatChanged			= ++CODE_INC;
	
	/**
	 * @param XMessage
	 * @param boolean(isThumb)
     */
	public static final int DownloadMessageFile			= ++CODE_INC;
	
	/**
	 * @param XMessage
     */
	public static final int UploadMessageFile			= ++CODE_INC;
	
	/**
	 * @param XMessage
     */
	public static final int VoicePlayStarted			= ++CODE_INC;
	
	/**
	 * @param XMessage
     */
	public static final int VoicePlayPaused				= ++CODE_INC;
	
	/**
	 * @param XMessage
     */
	public static final int VoicePlayErrored			= ++CODE_INC;
	
	/**
	 * @param XMessage
     */
	public static final int VoicePlayCompletioned		= ++CODE_INC;
	
	/**
	 * @param XMessage
     */
	public static final int VoicePlayStoped				= ++CODE_INC;
	
	/**
     */
	public static final int IM_LoginStart				= ++CODE_INC;
	
	public static final int IM_Login					= ++CODE_INC;
	
	public static final int IM_LoginPwdError			= ++CODE_INC;
	
	public static final int IM_LoginOuted				= ++CODE_INC;
	
	public static final int IM_Conflict					= ++CODE_INC;
	
	public static final int IM_LoginFailure				= ++CODE_INC;
	
	/**
	 * @param IMStatus
     */
	public static final int IM_StatusQuery				= ++CODE_INC;
	
	public static final int IM_ConnectionInterrupt		= ++CODE_INC;
	
	/**
	 * @param XMessage
	 */
	public static final int IM_ReceiveMessage			= ++CODE_INC;
	
	/**
	 * @param XMessage
     */
	public static final int IM_SendMessage				= ++CODE_INC;
	
	/**
	 * @param msgId
     */
	public static final int IM_SendMessageSuccess		= ++CODE_INC;
	
	/**
	 * @param user
	 * @return PicUrlObject
     */
	public static final int IM_LoadVCard				= ++CODE_INC;
	
	/**
	 * @param BaseVCard
     */
	public static final int IM_SaveVCard				= ++CODE_INC;
	
	/**
	 * @param IMNotice
	 */
	public static final int IM_MessageNotice			= ++CODE_INC;
}
