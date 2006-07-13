package org.red5.server.adapter;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import static org.red5.server.api.ScopeUtils.getScopeService;
import static org.red5.server.api.ScopeUtils.isApp;
import static org.red5.server.api.ScopeUtils.isRoom;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.IStreamableFile;
import org.red5.io.IStreamableFileFactory;
import org.red5.io.IStreamableFileService;
import org.red5.io.ITagReader;
import org.red5.io.StreamableFileFactory;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.service.ServiceUtils;
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
import org.red5.server.stream.IProviderService;
import org.red5.server.stream.ProviderService;
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
		/*
		try {
			Thread.currentThread().sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		if (!super.connect(conn, scope, params))
			return false;
		boolean success = false;
		if(isApp(scope)) success = appConnect(conn, params);
		else if(isRoom(scope)) success = roomConnect(conn, params);
		return success;
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

	/**
	 * Try to measure bandwidth of current connection.
	 * 
	 * This is required for some FLV player to work because they require
	 * the "onBWDone" method to be called on the connection.
	 */
	public void measureBandwidth() {
		measureBandwidth(Red5.getConnectionLocal());
	}
	
	/**
	 * Try to measure bandwidth of given connection.
	 * 
	 * This is required for some FLV player to work because they require
	 * the "onBWDone" method to be called on the connection.
	 * 
	 * @param conn
	 * 			the connection to measure the bandwidth for 
	 */
	public void measureBandwidth(IConnection conn) {
		// dummy for now, this makes flv player work
		// they dont wait for connected status they wait for onBWDone
		ServiceUtils.invokeOnConnection(conn, "onBWDone", new Object[]{});
		/*
		ServiceUtils.invokeOnConnection(conn, "onBWCheck", new Object[] {}, new IPendingServiceCallback() {
			public void resultReceived(IPendingServiceCall call) {
				log.debug("onBWCheck 1 result: " + call.getResult());
			}
		});
		int[] filler = new int[1024];
		ServiceUtils.invokeOnConnection(conn, "onBWCheck", new Object[] { filler }, new IPendingServiceCallback() {
			public void resultReceived(IPendingServiceCall call) {
				log.debug("onBWCheck 2 result: " + call.getResult());
				ServiceUtils.invokeOnConnection(conn, "onBWDone", new Object[] { new Integer(1000), new Integer(300), new Integer(6000), new Integer(300) }, new IPendingServiceCallback() {
					public void resultReceived(IPendingServiceCall call) {
						log.debug("onBWDone result: " + call.getResult());
					}
				});
			}
		});*/
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

	public Set<String> getSharedObjectNames(IScope scope) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.getSharedObjectNames(scope);
	}

	public boolean hasSharedObject(IScope scope, String name) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.hasSharedObject(scope, name);
	}
	
	/* Wrapper around the stream interfaces */
	
	public boolean hasBroadcastStream(IScope scope, String name) {
		IProviderService service = (IProviderService) getScopeService(scope, IProviderService.KEY, ProviderService.class); 
		return (service.getLiveProviderInput(scope, name, false) != null);
	}

	public IBroadcastStream getBroadcastStream(IScope scope, String name) {
		log.warn("This won't work until the refactoring of the streaming code is complete.");
		IBroadcastStreamService service = (IBroadcastStreamService) getScopeService(scope, IBroadcastStreamService.BROADCAST_STREAM_SERVICE, StreamService.class); 
		return service.getBroadcastStream(scope, name);
	}
	
	public List<String> getBroadcastStreamNames(IScope scope) {
		IProviderService service = (IProviderService) getScopeService(scope, IProviderService.KEY, ProviderService.class); 
		return service.getBroadcastStreamNames(scope);
	}
	
	public boolean hasOnDemandStream(IScope scope, String name) {
		IProviderService service = (IProviderService) getScopeService(scope, IProviderService.KEY, ProviderService.class); 
		return (service.getVODProviderInput(scope, name) != null);
	}

	public IOnDemandStream getOnDemandStream(IScope scope, String name) {
		log.warn("This won't work until the refactoring of the streaming code is complete.");
		IOnDemandStreamService service = (IOnDemandStreamService) getScopeService(scope, IOnDemandStreamService.ON_DEMAND_STREAM_SERVICE, StreamService.class); 
		return service.getOnDemandStream(scope, name);
	}

	public ISubscriberStream getSubscriberStream(IScope scope, String name) {
		log.warn("This won't work until the refactoring of the streaming code is complete.");
		ISubscriberStreamService service = (ISubscriberStreamService) getScopeService(scope, ISubscriberStreamService.SUBSCRIBER_STREAM_SERVICE, StreamService.class); 
		return service.getSubscriberStream(scope, name);
	}
	
	/*
	 * Wrapper around ISchedulingService
	 * 
	 * NOTE: We store this service in the scope as it can be
	 * shared across all rooms of the applications.
	 */
	public String addScheduledJob(int interval, IScheduledJob job) {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.addScheduledJob(interval, job);
	}

	public String addScheduledOnceJob(long timeDelta, IScheduledJob job) {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.addScheduledOnceJob(timeDelta, job);
	}
	
	public String addScheduledOnceJob(Date date, IScheduledJob job) {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.addScheduledOnceJob(date, job);
	}

	public void removeScheduledJob(String name) {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		service.removeScheduledJob(name);
	}
	
	public List<String> getScheduledJobNames() {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.getScheduledJobNames();
	}

	// NOTE: Method added to get flv player to work.
	public double getStreamLength(String name){
		IProviderService provider = (IProviderService) getScopeService(scope, IProviderService.KEY, ProviderService.class); 
		File file = provider.getVODProviderFile(scope, name);
		if(file == null) return 0;
		
		double duration = 0;
		
		IStreamableFileFactory factory = (IStreamableFileFactory) ScopeUtils.getScopeService(scope, IStreamableFileFactory.KEY, StreamableFileFactory.class);
		IStreamableFileService service = factory.getService(file);
		if (service == null) {
			log.error("No service found for " + file.getAbsolutePath());
			return 0;
		}
		try {
			IStreamableFile streamFile = service.getStreamableFile(file);
			ITagReader reader = streamFile.getReader();
			duration = reader.getDuration() / 1000;
			reader.close();
		} catch (IOException e) {
			log.error("error read stream file " + file.getAbsolutePath(), e);
		}
		
		return duration;
	}
	
}