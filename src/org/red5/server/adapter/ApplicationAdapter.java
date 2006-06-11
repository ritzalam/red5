package org.red5.server.adapter;

import static org.red5.server.api.ScopeUtils.isApp;
import static org.red5.server.api.ScopeUtils.isRoom;
import static org.red5.server.api.ScopeUtils.getScopeService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.service.IServiceHandlerProvider;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IBroadcastStreamService;
import org.red5.server.api.stream.IOnDemandStream;
import org.red5.server.api.stream.IOnDemandStreamService;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.api.stream.ISubscriberStreamService;
import org.red5.server.exception.ClientRejectedException;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.so.SharedObjectService;
import org.red5.server.stream.StreamService;

public class ApplicationAdapter extends StatefulScopeWrappingAdapter
	implements ISharedObjectService, IBroadcastStreamService, IOnDemandStreamService, ISubscriberStreamService,
		ISchedulingService {

	protected static Log log =
        LogFactory.getLog(ApplicationAdapter.class.getName());

	/**
	 * Reject the currently connecting client without a special error message.
	 *
	 */
	protected void rejectClient() {
		throw new ClientRejectedException();
	}
	
	/**
	 * Reject the currently connecting client with an error message.
	 * 
	 * The passed object will be available as "application" property of the
	 * information object that is returned to the caller. 
	 * 
	 * @param reason
	 * 			additional error message to return
	 */
	protected void rejectClient(Object reason) {
		throw new ClientRejectedException(reason);
	}
	
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		if (!super.connect(conn, scope, params))
			return false;
		if(isApp(scope)) return appConnect(conn, params);
		else if(isRoom(scope)) return roomConnect(conn, params);
		else return false;
	}

	public boolean start(IScope scope) {
		if (!super.start(scope))
			return false;
		if(isApp(scope)) return appStart(scope);
		else if(isRoom(scope)) return roomStart(scope);
		else return false;
	}

	public void disconnect(IConnection conn, IScope scope) {
		log.debug("disconnect");
		if(isApp(scope)) appDisconnect(conn);
		else if(isRoom(scope)) roomDisconnect(conn);
		super.disconnect(conn, scope);
	}

	public void stop(IScope scope) {
		if(isApp(scope)) appStop(scope);
		else if(isRoom(scope)) roomStop(scope);
		super.stop(scope);
	}

	public boolean join(IClient client, IScope scope) {
		if (!super.join(client, scope))
			return false;
		if(isApp(scope)) return appJoin(client, scope);
		else if(isRoom(scope)) return roomJoin(client, scope);
		else return false;
	}

	public void leave(IClient client, IScope scope) {
		log.debug("leave");
		if(isApp(scope)) appLeave(client, scope);
		else if(isRoom(scope)) roomLeave(client, scope);
		super.leave(client, scope);
	}

	public boolean appStart(IScope app){
		log.debug("appStart: " + app);
		return true;
	}

	public void appStop(IScope app){
		// do nothing
		log.debug("appStop: " + app);
	}
	
	public boolean roomStart(IScope room){
		log.debug("roomStart: " + room);
		return true;
	}
	
	public void roomStop(IScope room){
		log.debug("roomStop: " + room);
		//	do nothing
	}
	
	public boolean appConnect(IConnection conn, Object[] params){
		log.debug("appConnect: "+conn);
		return true;
	}
	
	public boolean roomConnect(IConnection conn, Object[] params){
		log.debug("roomConnect: "+conn);
		return true;
	}
	
	public void appDisconnect(IConnection conn){
		// do nothing
		log.debug("appDisconnect: "+conn);
	}
	
	public void roomDisconnect(IConnection conn){
		// do nothing
		log.debug("roomDisconnect: "+conn);
	}
	
	public boolean appJoin(IClient client, IScope app){
		log.debug("appJoin: "+client+" >> "+app);
		return true;
	}
	
	public void appLeave(IClient client, IScope app){
		log.debug("appLeave: "+client+" << "+app);
	}
	
	public boolean roomJoin(IClient client, IScope room){
		log.debug("roomJoin: "+client+" >> "+room);
		return true;
	}
	
	public void roomLeave(IClient client, IScope room){
		log.debug("roomLeave: "+client+" << "+room);
	}
	
	/* Wrapper around ISharedObjectService */
	
	public boolean createSharedObject(IScope scope, String name, boolean persistent) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.createSharedObject(scope, name, persistent);
	}

	public ISharedObject getSharedObject(IScope scope, String name) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.getSharedObject(scope, name);
	}

	public ISharedObject getSharedObject(IScope scope, String name, boolean persistent) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.getSharedObject(scope, name, persistent);
	}

	public Iterator<String> getSharedObjectNames(IScope scope) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.getSharedObjectNames(scope);
	}

	public boolean hasSharedObject(IScope scope, String name) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.hasSharedObject(scope, name);
	}
	
	/* Wrapper around the stream interfaces */
	
	public boolean hasBroadcastStream(IScope scope, String name) {
		IBroadcastStreamService service = (IBroadcastStreamService) getScopeService(scope, IBroadcastStreamService.BROADCAST_STREAM_SERVICE, StreamService.class); 
		return service.hasBroadcastStream(scope, name);
	}

	public IBroadcastStream getBroadcastStream(IScope scope, String name) {
		IBroadcastStreamService service = (IBroadcastStreamService) getScopeService(scope, IBroadcastStreamService.BROADCAST_STREAM_SERVICE, StreamService.class); 
		return service.getBroadcastStream(scope, name);
	}
	
	public Iterator<String> getBroadcastStreamNames(IScope scope) {
		IBroadcastStreamService service = (IBroadcastStreamService) getScopeService(scope, IBroadcastStreamService.BROADCAST_STREAM_SERVICE, StreamService.class); 
		return service.getBroadcastStreamNames(scope);
	}
	
	public boolean hasOnDemandStream(IScope scope, String name) {
		IOnDemandStreamService service = (IOnDemandStreamService) getScopeService(scope, IOnDemandStreamService.ON_DEMAND_STREAM_SERVICE, StreamService.class); 
		return service.hasOnDemandStream(scope, name);
	}

	public IOnDemandStream getOnDemandStream(IScope scope, String name) {
		IOnDemandStreamService service = (IOnDemandStreamService) getScopeService(scope, IOnDemandStreamService.ON_DEMAND_STREAM_SERVICE, StreamService.class); 
		return service.getOnDemandStream(scope, name);
	}

	public ISubscriberStream getSubscriberStream(IScope scope, String name) {
		ISubscriberStreamService service = (ISubscriberStreamService) getScopeService(scope, ISubscriberStreamService.SUBSCRIBER_STREAM_SERVICE, StreamService.class); 
		return service.getSubscriberStream(scope, name);
	}
	
	/* Wrapper around ISchedulingService */
	
	public String addScheduledJob(int interval, IScheduledJob job) {
		// NOTE: We store this service in the scope as it can be
		//       shared across all rooms of the applications.
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.addScheduledJob(interval, job);
	}

	public void removeScheduledJob(String name) {
		// NOTE: We store this service in the scope as it can be
		//       shared across all rooms of the applications.
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		service.removeScheduledJob(name);
	}
	
	public List<String> getScheduledJobNames() {
		// NOTE: We store this service in the scope as it can be
		//       shared across all rooms of the applications.
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.getScheduledJobNames();
	}

}