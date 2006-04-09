package org.red5.server.stream;

import static org.red5.server.api.stream.IBroadcastStream.TYPE;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.flv.IFLV;
import org.red5.io.flv.IFLVService;
import org.red5.io.flv.IWriter;
import org.red5.io.flv.impl.FLVService;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IBroadcastStreamService;
import org.red5.server.api.stream.IOnDemandStream;
import org.red5.server.api.stream.IOnDemandStreamService;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.api.stream.ISubscriberStreamService;
import org.red5.server.net.rtmp.message.Status;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

public class ScopeWrappingStreamManager
	implements IBroadcastStreamService, IOnDemandStreamService, ISubscriberStreamService {

	protected static Log log =
        LogFactory.getLog(ScopeWrappingStreamManager.class.getName());
	
	private IScope scope;
	private String streamDir = "streams";
	private IFLVService flvService;

	public ScopeWrappingStreamManager(IScope scope) {
		this.scope = scope;
		setFlvService(new FLVService());
	}
	
	public void setFlvService(IFLVService flvService) {
		this.flvService = flvService;
	}

	public boolean hasBroadcastStream(String name) {
		return scope.hasChildScope(name);
	}
	
	public Iterator<String> getBroadcastStreamNames() {
		return scope.getBasicScopeNames(TYPE);
	}
	
	public IBroadcastStream getBroadcastStream(String name) {
		BroadcastStreamScope result = (BroadcastStreamScope) scope.getBasicScope(TYPE, name);
		if (result != null)
			return result;
		
		// Create new stream
		// XXX: this is only the correct connection if the stream is created by the
		//      real subscriber, not if it's a temporary stream!
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof RTMPConnection))
			return null;
		
		result = new BroadcastStreamScope(scope, (RTMPConnection) conn, name);
		scope.addChildScope(result);
		return result;
	}
	
	public boolean hasOnDemandStream(String name) {
		try {
			File file = scope.getResources("streams/" + name)[0].getFile();
			if (file.exists())
				return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public IOnDemandStream getOnDemandStream(String name) {
		FileStreamSource source = null;
		try {
			File file = scope.getResources("streams/" + name)[0].getFile();
			IFLV flv = flvService.getFLV(file);
			source = new FileStreamSource(flv.reader());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (source == null)
			return null;
		
		return new OnDemandStream(scope, source);
	}
	
	public ISubscriberStream getSubscriberStream(String name) {
		// TODO: implement this
		return null;
	}
	
	/*
	public void publishStream(Stream stream){
		
		// If we have a read mode stream, we shouldnt be publishing return
		if(stream.getMode().equals(Stream.MODE_READ)) return;
		
		BroadcastStream multi = (BroadcastStream) published.get(stream.getName());
		if (multi == null)
			// sink doesn't exist, create new
			multi = new BroadcastStream(scope);
			
		stream.setUpstream(multi);
		published.put(stream.getName(),multi);
		
		// If the mode is live, we dont need to do anything else
		if(stream.getMode().equals(Stream.MODE_LIVE)) return;
		
		// The mode must be record or append
		try {
			Resource res = scope.getResource("streams/" + stream.getName()+".flv");
			if(stream.getMode().equals(Stream.MODE_RECORD) && res.exists()) 
				res.getFile().delete();
			if(!res.exists()) res = scope.getResource("streams/").createRelative(stream.getName()+".flv");
			if(!res.exists()) res.getFile().createNewFile(); 
			File file = res.getFile();
			IFLV flv = flvService.getFLV(file);
			IWriter writer = null; 
			if(stream.getMode().equals(Stream.MODE_RECORD)) 
				writer = flv.writer();
			else if(stream.getMode().equals(Stream.MODE_APPEND))
				writer = flv.append();
			multi.subscribe(new FileStreamSink(scope, writer));
		} catch (IOException e) {
			log.error("Error recording stream: "+stream, e);
		}
	}
	
	public void deleteStream(Stream stream){
		if (stream.getUpstream() != null && published.containsKey(stream.getName())) {
			// Notify all clients that stream is no longer published
			BroadcastStream multi = (BroadcastStream) published.get(stream.getName());
			Status unpublish = new Status(Status.NS_PLAY_UNPUBLISHNOTIFY);
			unpublish.setClientid(stream.getStreamId());
			unpublish.setDetails(stream.getName());
			Iterator it = multi.streams.iterator();
			while (it.hasNext()) {
				Stream s = (Stream) it.next();
				s.getDownstream().getData().sendStatus(unpublish);
			}
			published.remove(stream.getName());
		}
		stream.close();
	}
	
	public boolean isPublishedStream(String name){
		return published.containsKey(name);
	}
	
	public boolean isFileStream(String name) {
		if (this.isPublishedStream(name))
			// A stream cannot be published and file based at the same time
			return false;
		
		try {
			File file = scope.getResources("streams/" + name)[0].getFile();
			if (file.exists())
				return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public IStreamSource lookupStreamSource(String name){
		return createFileStreamSource(name);
	}

	protected IStreamSource createFileStreamSource(String name){
		Resource[] resource = null;
		FileStreamSource source = null;
		try {
			File file = scope.getResources("streams/" + name)[0].getFile();
			IFLV flv = flvService.getFLV(file);
			source = new FileStreamSource(flv.reader());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return source;
	}
	*/
}
