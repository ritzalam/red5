package org.red5.server.net.rtmp_refactor.status;

public interface StatusCodes {

	public static final String NC_CALL_FAILED = "NetConnection.Call.Failed";
	public static final String NC_CALL_BADVERSION = "NetConnection.Call.BadVersion"; // Why small c in call
	
	public static final String NC_CONNECT_APPSHUTDOWN = "NetConnection.Connect.AppShutdown";
	public static final String NC_CONNECT_CLOSED = "NetConnection.Connect.Closed";
	public static final String NC_CONNECT_FAILED = "NetConnection.Connect.Failed";
	public static final String NC_CONNECT_REJECTED = "NetConnection.Connect.Rejected";
	public static final String NC_CONNECT_SUCCESS = "NetConnection.Connect.Success";
	
	public static final String NS_CLEAR_SUCCESS = "NetStream.Clear.Success";
	public static final String NS_CLEAR_FAILED = "NetStream.Clear.Failed";
	
	public static final String NS_PUBLISH_START = "NetStream.Publish.Start";
	public static final String NS_PUBLISH_BADNAME = "NetStream.Publish.BadName";
	public static final String NS_FAILED = "NetStream.Failed";
	public static final String NS_UNPUBLISHED_SUCCESS = "NetStream.Unpublish.Success";
	
	public static final String NS_RECORD_START = "NetStream.Record.Start";
	public static final String NS_RECOED_NOACCESS = "NetStream.Record.NoAccess";
	public static final String NS_RECORD_STOP = "NetStream.Record.Stop";
	public static final String NS_RECORD_FAILED = "NetStream.Record.Failed";
	
	public static final String NS_PLAY_INSUFFICIENT_BW = "NetStream.Play.InsufficientBW";
	public static final String NS_PLAY_START = "NetStream.Play.Start";
	public static final String NS_PLAY_STREAMNOTFOUND = "NetStream.Play.StreamNotFound";
	public static final String NS_PLAY_STOP = "NetStream.Play.Stop";
	public static final String NS_PLAY_FAILED = "NetStream.Play.Failed";
	public static final String NS_PLAY_RESET = "NetStream.Play.Reset";
	public static final String NS_PLAY_PUBLISHNOTIFY = "NetStream.Play.PublishNotify";
	public static final String NS_PLAY_UNPUBLISHNOTIFY = "NetStream.Play.UnpublishNotify";
	
	public static final String NS_DATA_START = "NetStream.Data.Start";
	
	public static final String APP_SCRIPT_ERROR = "Application.Script.Error";
	public static final String APP_SCRIPT_WARNING ="Application.Script.Warning";
	public static final String APP_RESOURCE_LOWMEMORY = "Application.Resource.LowMemory";
	public static final String APP_SHUTDOWN = "Application.Shutdown";
	public static final String APP_GC = "Application.GC";
	
}
