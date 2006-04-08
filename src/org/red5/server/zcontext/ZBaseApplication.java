package org.red5.server.zcontext;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.net.rtmp.status.StatusObjectService;
import org.red5.server.persistence2.IPersistable;
import org.red5.server.persistence2.IPersistentStorage;
import org.red5.server.persistence2.RamPersistence;
import org.red5.server.so.SharedObject;
import org.red5.server.stream.IStreamSource;
import org.red5.server.stream.Stream;
import org.red5.server.stream.StreamManager;
import org.red5.server.stream.TemporaryStream;
import org.red5.server.stream.VideoCodecFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ZBaseApplication 
	implements ApplicationContextAware {

	//private StatusObjectService statusObjectService = null;
	private ApplicationContext appCtx = null;
	private HashSet clients = new HashSet();
	private StreamManager streamManager = null;
	// Persistent shared objects are configured through red5.xml
	private IPersistentStorage soPersistence = null;
	// Non-persistent shared objects are only stored in memory
	private RamPersistence soTransience = new RamPersistence(); 
	private HashSet listeners = new HashSet();
	private VideoCodecFactory videoCodecs = null;
	
	protected static Log log =
        LogFactory.getLog(ZBaseApplication.class.getName());
	
	public void setApplicationContext(ApplicationContext appCtx){
		this.appCtx = appCtx;
	}
	
	public void setStreamManager(StreamManager streamManager){
		this.streamManager = streamManager;
	}
	
	public void setSharedObjectPersistence(IPersistentStorage soPersistence) {
		this.soPersistence = soPersistence;
	}
	
	public void setVideoCodecFactory(VideoCodecFactory factory) {
		this.videoCodecs = factory;
	}
	
	/*
	public void setStatusObjectService(StatusObjectService statusObjectService){
		this.statusObjectService = this.statusObjectService;
	}
	*/
	
	private StatusObject getStatus(String statusCode){
		// TODO: fix this, getting the status service out of the thread scope is a hack
		//final StatusObjectService statusObjectService = ZScope.getStatusObjectService();
		//return statusObjectService.getStatusObject(statusCode);
		return null;
	}
	
	public final void initialize(){
		log.debug("Calling onAppStart");
		onAppStart();
	}
	
	public final StatusObject connect(List params){
		IConnection connection = Red5.getConnectionLocal();
		final IClient client = connection.getClient();
		log.debug("Calling onConnect");
		if(onConnect(client, params)){
			clients.add(client);
			RTMPConnection conn = (RTMPConnection) connection;
			Ping ping = new Ping();
			ping.setValue1((short)0);
			ping.setValue2(0);
			conn.ping(ping);
			return getStatus(StatusObjectService.NC_CONNECT_SUCCESS);
		} else {
			return getStatus(StatusObjectService.NC_CONNECT_REJECTED);
		}
	}
	
	public final void disconnect(){
		final IConnection connection = Red5.getConnectionLocal();
		final IClient client = connection.getClient();
		clients.remove(client);
		if (this.soPersistence != null) {
			// Unregister client from shared objects
			Iterator it = this.soPersistence.getObjects();
			while (it.hasNext()) {
				ISharedObject so = (ISharedObject) it.next();
				//so.unregister(connection);
			}
		}

	}
	
	// -----------------------------------------------------------------------------
	
	public int createStream(){
		// Reserve a slot for the new stream and send id of new stream to client
		// This stream id will be checked by publish below
		final IConnection conn = Red5.getConnectionLocal();
		// TODO: create stream through streamhandler?
		Stream stream = ((RTMPConnection) conn).createNewStream();
		if (stream == null) {
			// XXX: no more stream slots available, return error to the client...
		}
		return stream.getStreamId(); 
	}
	
	public void play(String name){
		 play(name, new Double(-2000.0));
	}
	
	public void play(String name, Double number){
		final Stream stream = null; //ZScope.getStream();
		if (stream == null) {
			// XXX: invalid request, we must return an error to the client here...
			// NetStream.Play.Failed
		}
		
		// it seems as if the number is sent multiplied by 1000
		int num = (int)(number.doubleValue() / 1000.0);
		if (num < -2)
			num = -2;
		stream.setName(name);
		log.debug("play: "+name);
		log.debug("stream: "+stream);
		log.debug("number:"+number);
		
		// According the documentation of NetStream.play, the number has the following
		// meanings:
		//
		// -2 (default)
		// try to play live stream <name>, if none exists, play recorded stream,
		// if no rec. stream exists, create live stream and begin playing once
		// someone publishes to it
		//
		// -1
		// play live stream, if it doesn't exist, wait for it
		//
		// 0 or positive number
		// play recorded stream at <number> seconds from the beginning
		//
		// any negative number but -1 and -2
		// use same behaviour as -2
		//
		
		boolean isPublishedStream = streamManager.isPublishedStream(name);
		boolean isFileStream = streamManager.isFileStream(name);
		// decision: 0 for Live, 1 for File, 2 for Wait, 3 for N/A
		int decision = 3;
		
		switch (num) {
		case -2:
			if (isPublishedStream) {
				decision = 0;
			} else if (isFileStream) {
				decision = 1;
			} else {
				decision = 2;
			}
			break;
			
		case -1:
			if (isPublishedStream) {
				decision = 0;
			} else {
				// TODO: Wait for stream to be created until timeout, otherwise continue
				// with next item in playlist (see Macromedia documentation)
				// NOTE: For now we create a temporary stream
				decision = 2;
			}
			break;
			
		default:
			if (isFileStream) {
				decision = 1;
			} else {
				// TODO: Wait for it, then continue with next item in playlist (?)
			}
			break;
		}
		
		switch (decision) {
		case 0:
			streamManager.connectToPublishedStream(stream);
			stream.start();
			break;
		case 1:
			final IStreamSource source = streamManager.lookupStreamSource(name);
			log.debug(source);
			stream.setSource(source);
			
			//Integer.MAX_VALUE;
			//stream.setNSId();
			// TODO: Seek to requested start
			stream.start();
			break;
		case 2:
			streamManager.publishStream(new TemporaryStream(name, Stream.MODE_LIVE));
			streamManager.connectToPublishedStream(stream);
			stream.start();
			break;
		default:
			break;
		}
		//streamManager.play(stream, name);
		//return getStatus(StatusObjectService.NS_PLAY_START);
	}
	
	public StatusObject publish(String name, String mode){
		final Stream stream = null; //ZScope.getStream();
		if (stream == null) {
			log.debug("No stream created before publishing or published to wrong stream.");
			return getStatus(StatusObjectService.NS_PUBLISH_BADNAME);
		}
		
		// TODO: check if a stream with this name is already published by someone else
		
		stream.setName(name);
		stream.setMode(mode);
		stream.setVideoCodecFactory(this.videoCodecs);
		streamManager.publishStream(stream);
		stream.publish();		
		log.debug("publish: "+name);
		log.debug("stream: "+stream);
		log.debug("mode:"+mode);
		return getStatus(StatusObjectService.NS_PUBLISH_START);
	}
	
	
	public void pause(boolean pause, int time){
		log.info("Pause called: "+pause+" true:"+time);
		final Stream stream = null; // ZScope.getStream();
		if (stream == null) {
			// XXX: invalid request, we must return an error to the client here...
		}
		if(pause) stream.pause();
		else stream.resume();
	}
	
	public void deleteStream(int number){
		IConnection conn = Red5.getConnectionLocal();
		// TODO: change to not depend on BaseConnection
		Stream stream = ((RTMPConnection) conn).getStreamById(number);
		if (stream == null) {
			// XXX: invalid request, we must return an error to the client here...
		}
		((RTMPConnection) conn).deleteStreamById(number);
		log.debug("Delete stream: "+stream+" number: "+number);
		streamManager.deleteStream(stream);
	}
	
	public void closeStream(){
		final Stream stream = null;//ZScope.getStream();
		if (stream == null) {
			// XXX: invalid request, we must return an error to the client here...
		}
		stream.stop();
	}
	// publishStream ?
	
	// -----------------------------------------------------------------------------
	
	public void onAppStart(){
		
	}
	
	public void onAppStop(){
		
	}
	
	public boolean onConnect(IClient client, List params){
		// always ok, override
		return true;
	}
		
	// -----------------------------------------------------------------------------
	
	public ISharedObject getSharedObject(String name, boolean persistent) {
		IPersistentStorage persistence = this.soPersistence;
		if (!persistent) {
			persistence = this.soTransience;
		}
			
		if (persistence == null) {
			// XXX: maybe we should thow an exception here as a non-persistent SO doesn't make any sense...
			return null; //new SharedObject(name, false, null);
		}
		
		ISharedObject result;
		try {
			result = (ISharedObject) persistence.loadObject(SharedObject.PERSISTENT_ID_PREFIX + name);
		} catch (IOException e) {
			log.error("Could not load shared object.", e);
			result = null;
		}
		if (result == null) {
			// Create new shared object with given name
			log.info("Creating new shared object " + name);
			result = null;//new SharedObject(name, persistent, persistence);
			try {
				persistence.storeObject((IPersistable) result);
			} catch (IOException e) {
				log.error("Could not store shared object.", e);
			}
		}
		
		return result;
	}
	
}
