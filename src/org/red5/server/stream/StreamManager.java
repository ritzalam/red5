package org.red5.server.stream;

import static org.red5.server.api.stream.IBroadcastStream.TYPE;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.IStreamableFile;
import org.red5.io.IStreamableFileService;
import org.red5.io.ITagReader;
import org.red5.io.StreamableFileFactory;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
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
import org.springframework.context.ApplicationContext;

public class StreamManager
	implements IBroadcastStreamService, IOnDemandStreamService, ISubscriberStreamService {

	protected static final String FILE_FACTORY = "streamableFileFactory";
	
	protected static Log log =
        LogFactory.getLog(StreamManager.class.getName());
	
	private String streamDir = "streams";

	protected StreamableFileFactory getFileFactory(IScope scope) {
		final IContext context = scope.getContext();
		ApplicationContext appCtx = context.getApplicationContext();
		if (!appCtx.containsBean(FILE_FACTORY))
			return new StreamableFileFactory();
		else
			return (StreamableFileFactory) appCtx.getBean(FILE_FACTORY);
	}
	
	public boolean hasBroadcastStream(IScope scope, String name) {
		return scope.hasChildScope(name);
	}
	
	public Iterator<String> getBroadcastStreamNames(IScope scope) {
		return scope.getBasicScopeNames(TYPE);
	}
	
	public IBroadcastStream getBroadcastStream(IScope scope, String name) {
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
	
	public boolean hasOnDemandStream(IScope scope, String name) {
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
	
	public IOnDemandStream getOnDemandStream(IScope scope, String name) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof RTMPConnection))
			return null;
		
		FileStreamSource source = null;
		try {
			File file = scope.getResources(getStreamFilename(name))[0].getFile();
			ITagReader reader;
			IStreamableFileService service = getFileFactory(scope).getService(file);
			if (service == null) {
				log.error("No service found for " + file.getAbsolutePath());
				return null;
			}
			IStreamableFile streamFile = service.getStreamableFile(file);
			reader = streamFile.getReader();
			source = new FileStreamSource(reader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (source == null)
			return null;
		
		return new OnDemandStream(scope, source, (RTMPConnection) conn);
	}
	
	public ISubscriberStream getSubscriberStream(IScope scope, String name) {
		// TODO: implement this
		return null;
	}
	
	public void deleteStream(IScope scope, IStream stream) {
		
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
