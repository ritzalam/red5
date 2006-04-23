package org.red5.server.stream;

import static org.red5.server.api.stream.IBroadcastStream.TYPE;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.flv.IFLV;
import org.red5.io.flv.IFLVService;
import org.red5.io.flv.IReader;
import org.red5.io.flv.impl.FLVService;
import org.red5.io.mp3.IMP3;
import org.red5.io.mp3.IMP3Service;
import org.red5.io.mp3.impl.MP3Service;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IBroadcastStreamService;
import org.red5.server.api.stream.IOnDemandStream;
import org.red5.server.api.stream.IOnDemandStreamService;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.api.stream.ISubscriberStreamService;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Status;

public class ScopeWrappingStreamManager
	implements IBroadcastStreamService, IOnDemandStreamService, ISubscriberStreamService {

	protected static Log log =
        LogFactory.getLog(ScopeWrappingStreamManager.class.getName());
	
	protected IScope scope;
	private String streamDir = "streams";
	protected IFLVService flvService;
	protected IMP3Service mp3Service;

	public ScopeWrappingStreamManager(IScope scope) {
		this.scope = scope;
		setFlvService(new FLVService());
		setMp3Service(new MP3Service());
	}
	
	public void setFlvService(IFLVService flvService) {
		this.flvService = flvService;
	}

	public void setMp3Service(IMP3Service mp3Service) {
		this.mp3Service = mp3Service;
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
	
	protected String getStreamDirectory() {
		return streamDir + "/";
	}
	
	protected String getStreamFilename(String name) {
		return getStreamFilename(name, null);
	}
	
	protected String getStreamFilename(String name, String extension) {
		String result = getStreamDirectory() + name;
		if (extension != null && !extension.equals(""))
			result += extension;
		return result;
	}
	
	public boolean hasOnDemandStream(String name) {
		try {
			File file = scope.getResources(getStreamFilename(name))[0].getFile();
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
			File file = scope.getResources(getStreamFilename(name))[0].getFile();
			IReader reader;
			if (file.getAbsolutePath().endsWith(".mp3")) {
				IMP3 mp3 = mp3Service.getMP3(file);
				reader = mp3.reader();
			} else {
				IFLV flv = flvService.getFLV(file);
				reader = flv.reader();
			}
			source = new FileStreamSource(reader);
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
	
	public void deleteStream(IStream stream) {
		
		log.debug("Delete stream: "+stream);
		
		if (stream instanceof IBroadcastStream) {
			// Notify all clients that stream is no longer published
			Status unpublish = new Status(Status.NS_PLAY_UNPUBLISHNOTIFY);
			unpublish.setClientid(stream.getStreamId());
			unpublish.setDetails(stream.getName());
			Iterator<ISubscriberStream> it = ((IBroadcastStream) stream).getSubscribers();
			while (it.hasNext()) {
				ISubscriberStream s = it.next();
				if (s instanceof SubscriberStream)
					((SubscriberStream) s).getDownstream().getData().sendStatus(unpublish);
			}
		}
		
		if (stream instanceof IBasicScope){
			log.info("Remove stream scope:" + stream);
			scope.removeChildScope((IBasicScope) stream);
		}
			
		stream.close();
	}
	
}
