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

/**
 * ApplicationAdapter class serves as a base class for your Red5 applications.
 * It provides methods to work with SharedObjects and streams, as well as connections and scheduling services.
 * 
 * ApplicationAdapter is an application level IScope. To handle streaming processes in your application
 * you should implement {@link IStreamAwareScopeHandler} interface and implement handling methods.
 *
 */
public class ApplicationAdapter extends StatefulScopeWrappingAdapter
	implements ISharedObjectService, IBroadcastStreamService, IOnDemandStreamService, ISubscriberStreamService,
		ISchedulingService {
	
	/**
	 * Logger object
	 */
	protected static Log log =
        LogFactory.getLog(ApplicationAdapter.class.getName());

	/**
	 * Reject the currently connecting client without a special error message. This method throws {@link ClientRejectedException}
	 * exception.
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
	
	/**
	 * Returns connection result for given scope and parameters. Whether the scope is room or app level scope,
	 * this method distinguishes it and acts accordingly. You override {@link ApplicationAdapter#appConnect(IConnection, Object[]))} 
	 * or {@link ApplicationAdapter#roomConnect(IConnection, Object[])}} in your application to make it act the way you want.
	 * 
	 * @param	conn	Connection object
	 * @param	scope	Scope
	 * @param	params	List of params passed to connection handler
	 * @return			<code>true</code> if connect is successful, <code>false</code> otherwise
	 */
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
	
	/**
	 * Starts scope. Scope can be both application or room level.
	 * 
	 * @param		scope		Scope object
	 * @return		<code>true</code> if scope can be started, <code>false</code> otherwise. 
	 * 				See {@link AbstractScopeAdapter#start(IScope)} for details.
	 */
	public boolean start(IScope scope) {
		if (!super.start(scope))
			return false;
		if(isApp(scope)) return appStart(scope);
		else if(isRoom(scope)) return roomStart(scope);
		else return false;
	}
	
	/**
	 * Returns disconnection result for given scope and parameters. Whether the scope is room or app level scope,
	 * this method distinguishes it and acts accordingly.
	 * 
	 * @param	conn	Connection object
	 * @param	scope	Scope
	 * @param	params	List of params passed to connection handler
	 * @return			<code>true</code> if disconnect is successful, <code>false</code> otherwise
	 */	
	public void disconnect(IConnection conn, IScope scope) {
		log.debug("disconnect");
		if(isApp(scope)) appDisconnect(conn);
		else if(isRoom(scope)) roomDisconnect(conn);
		super.disconnect(conn, scope);
	}
	
	/**
	 * Stops scope handling (that is, stops application if given scope is app level scope and stops room handling
	 * if given scope has lower scope level). This method calls {@link ApplicationAdapter#appStop(IScope)} or
	 * {@link ApplicationAdapter#roomStop(IScope))} handlers respectively.
	 * 
	 * @param		scope		Scope to stop
	 */
	public void stop(IScope scope) {
		if(isApp(scope)) appStop(scope);
		else if(isRoom(scope)) roomStop(scope);
		super.stop(scope);
	}
	
	/**
	 * Adds client to scope. Scope can be both application or room. Can be applied to both application scope and scopes of lower level. 
	 * This method calls {@link ApplicationAdapter#appJoin(IClient, IScope)} or
	 * {@link ApplicationAdapter#roomJoin(IClient, IScope))} handlers respectively.
	 * 
	 * @param		client		Client object
	 * @param		scope		Scope object
	 */
	public boolean join(IClient client, IScope scope) {
		if (!super.join(client, scope))
			return false;
		if(isApp(scope)) return appJoin(client, scope);
		else if(isRoom(scope)) return roomJoin(client, scope);
		else return false;
	}
	
	/**
	 * Disconnects client from scope. Can be applied to both application scope and scopes of lower level. 
	 * This method calls {@link ApplicationAdapter#appLeave(IClient, IScope)} or
	 * {@link ApplicationAdapter#roomLeave(IClient, IScope)} handlers respectively.
	 * 
	 * @param	client	Client object
	 * @param	scope	Scope object
	 */
	public void leave(IClient client, IScope scope) {
		log.debug("leave");
		if(isApp(scope)) appLeave(client, scope);
		else if(isRoom(scope)) roomLeave(client, scope);
		super.leave(client, scope);
	}
	
	/**
	 * Called once on scope (that is, application or application room) start. 
	 * You override {@link ApplicationAdapter#appStart(IScope)} or {@link ApplicationAdapter#roomStart(IScope)}} in your application
	 * to make it act the way you want.
	 * 
	 * @param	scope	Scope object
	 * @return			<code>true</code> if scope can be started, <code>false</code> otherwise
	 */
	public boolean appStart(IScope app){
		log.debug("appStart: " + app);
		return true;
	}
	
	/**
	 * Handler method. Called when application is stopped.
	 * 
	 * @param 	app		Scope object
	 */
	public void appStop(IScope app){
		// do nothing
		log.debug("appStop: " + app);
	}
	
	/**
	 * Handler method. Called when room scope is started.
	 * 
	 * @param room	Room scope
	 * @return		Boolean value
	 */
	public boolean roomStart(IScope room){
		log.debug("roomStart: " + room);
		// TODO : Get to know what does roomStart return mean
		return true;
	}
	
	/**
	 * Handler method. Called when room scope is stopped.
	 * 
	 * @param room	Room scope.
	 */
	public void roomStop(IScope room){
		log.debug("roomStop: " + room);
		//	do nothing
	}
	
	/**
	 * Handler method. Called every time new client connects (that is, new IConnection object is created after call
	 * from a SWF movie) to the application.
	 * 
	 * You override this method to pass additional data from client to server application using <code>NetConnection.connect</code> method.
	 * 
	 * EXAMPLE: 
	 * 
	 * In this simple example we pass user's skin of choice identifier from client to th server.
	 * 
	 * Client-side: 
	 * 
	 * <code>NetConnection.connect( "rtmp://localhost/killerred5app", "silver" );</code>
	 * 
	 * Server-side:
	 * 
	 * <code>if (params.length > 0) System.out.println( "Theme selected: " + params[0] );</code>
	 * 
	 * @param conn		Connection object
	 * @param params	List of parameters after connection URL passed to <code>NetConnection.connect</code> method.
	 * @return			Boolean value
	 */
	public boolean appConnect(IConnection conn, Object[] params){
		log.debug("appConnect: "+conn);
		return true;
	}
	
	/**
	 * Handler method. Called every time new client connects (that is, new IConnection object is created after call
	 * from a SWF movie) to the application.
	 * 
	 * You override this method to pass additional data from client to server application using <code>NetConnection.connect</code> method.
	 * 
	 * See {@link ApplicationAdapter#appConnect(IConnection, Object[])} for code example.
	 * 
	 * @param conn		Connection object
	 * @param params	List of params passed to room scope
	 * @return			Boolean value
	 */
	public boolean roomConnect(IConnection conn, Object[] params){
		log.debug("roomConnect: "+conn);
		return true;
	}
	
	/**
	 * Handler method. Called every time client disconnects from the application.
	 * 
	 * @param conn	Disconnected connection object
	 */
	public void appDisconnect(IConnection conn){
		// do nothing
		log.debug("appDisconnect: "+conn);
	}
	
	/**
	 * Handler method. Called every time client disconnects from the room.
	 * 
	 * @param conn	Disconnected connection object
	 */
	public void roomDisconnect(IConnection conn){
		// do nothing
		log.debug("roomDisconnect: "+conn);
	}
	
	public boolean appJoin(IClient client, IScope app){
		log.debug("appJoin: "+client+" >> "+app);
		return true;
	}

	/**
	 * Handler method. Called every time client leaves application scope.
	 * 
	 * @param client	Client object that left
	 * @param app		Application scope
	 */
	public void appLeave(IClient client, IScope app){
		log.debug("appLeave: "+client+" << "+app);
	}
	
	public boolean roomJoin(IClient client, IScope room){
		log.debug("roomJoin: "+client+" >> "+room);
		return true;
	}

	/**
	 * Handler method. Called every time client leaves room scope.
	 * 
	 * @param conn	Disconnected connection object
	 */
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
	
	/**
	 * Creates a new shared object for given scope. Server-side shared objects (also known as Remote SO) are
	 * special kind of objects those variable are synchronized between clients. To get an instance of RSO at client-side,
	 * use <code>SharedObject.getRemote()</code>. 
	 * 
	 * SharedObjects can be persistent and transient. Persistent RSO are statuful, i.e. store their data between sessions. 
	 * If you need to store some data on server while clients go back and forth use persistent SO (just use <code>true</code> ), 
	 * otherwise perfer usage of transient for extra performance.
	 * 
	 * @param	scope			Scope that shared object belongs to
	 * @param	name			Name of SharedObject
	 * @param	persistent		Whether SharedObject instance should be persistent or not
	 * @return					New shared object instance
	 */
	public boolean createSharedObject(IScope scope, String name, boolean persistent) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.createSharedObject(scope, name, persistent);
	}
	
	/**
	 * Returns shared object from given scope by name.
	 * 
	 * @param	scope			Scope that shared object belongs to
	 * @param	name			Name of SharedObject
	 * @return					Shared object instance with name given
	 */
	public ISharedObject getSharedObject(IScope scope, String name) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.getSharedObject(scope, name);
	}

	/**
	 * Returns shared object from given scope by name.
	 * 
	 * @param	scope			Scope that shared object belongs to
	 * @param	name			Name of SharedObject
	 * @param	persistent		Whether SharedObject instance should be persistent or not
	 * @return					Shared object instance with name given
	 */
	public ISharedObject getSharedObject(IScope scope, String name, boolean persistent) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.getSharedObject(scope, name, persistent);
	}
	
	/**
	 * Returns available SharedObject names as List
	 * 
	 * @param	scope	Scope that SO belong to
	 */
	public Set<String> getSharedObjectNames(IScope scope) {
		ISharedObjectService service = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class); 
		return service.getSharedObjectNames(scope);
	}
	
	/**
	 * Checks whether there's a SO with given scope and name
	 * 
	 * @param	scope	Scope that SO belong to
	 * @param	name	Name of SharedObject
	 */	
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
	
	/**
	 * Returns list of stream names broadcasted in <pre>scope</pre>. Broadcast stream name is somewhat different
	 * from server stream name. Server stream name is just an ID assigned by Red5 to every created stream. Broadcast stream name
	 * is the name that is being used to subscribe to the stream at client side, that is, in <code>NetStream.play</code> call.
	 * 
	 * @param	scope	Scope to retrieve broadcasted stream names
	 * @return	List of broadcasted stream names.
	 */
	public List<String> getBroadcastStreamNames(IScope scope) {
		IProviderService service = (IProviderService) getScopeService(scope, IProviderService.KEY, ProviderService.class); 
		return service.getBroadcastStreamNames(scope);
	}
	
	/**
	 * Check whether scope has VOD stream with given name or not
	 * 
	 * @param	scope	Scope
	 * @param	name	VOD stream name
	 * 
	 * @return	<code>true</code> if scope has VOD stream with given name, <code>false</code> otherwise.
	 */
	public boolean hasOnDemandStream(IScope scope, String name) {
		IProviderService service = (IProviderService) getScopeService(scope, IProviderService.KEY, ProviderService.class); 
		return (service.getVODProviderInput(scope, name) != null);
	}
	
	/**
	 * Returns VOD stream with given name from specified scope.
	 * 
	 * @param	scope	Scope object
	 * @param	name	VOD stream name
	 * 
	 * @return	IOnDemandStream object that represents stream that can be played on demand, seekable and so forth. See {@link IOnDemandStream} for details.
	 */
	public IOnDemandStream getOnDemandStream(IScope scope, String name) {
		log.warn("This won't work until the refactoring of the streaming code is complete.");
		IOnDemandStreamService service = (IOnDemandStreamService) getScopeService(scope, IOnDemandStreamService.ON_DEMAND_STREAM_SERVICE, StreamService.class); 
		return service.getOnDemandStream(scope, name);
	}
	
	/**
	 * Returns subscriber stream with given name from specified scope. Subscriber stream is a stream that clients can subscribe to.
	 * 
	 * @param	scope	Scope
	 * @param	name	Stream name
	 * 
	 * @return	ISubscriberStream object
	 */
	public ISubscriberStream getSubscriberStream(IScope scope, String name) {
		log.warn("This won't work until the refactoring of the streaming code is complete.");
		ISubscriberStreamService service = (ISubscriberStreamService) getScopeService(scope, ISubscriberStreamService.SUBSCRIBER_STREAM_SERVICE, StreamService.class); 
		return service.getSubscriberStream(scope, name);
	}
	
	/**
	 * Wrapper around ISchedulingService, adds a scheduled job to be run periodically. 
	 * We store this service in the scope as it can be shared across all rooms of the applications.
	 * 
	 * @param	interval	Time inverval to run the scheduled job
	 * @param	job			Scheduled job object
	 * 
	 * @return	Name of the scheduled job
	 */
	public String addScheduledJob(int interval, IScheduledJob job) {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.addScheduledJob(interval, job);
	}
	
	/**
	 * Adds a scheduled job that's gonna be executed once. Please note
	 * that the jobs are not saved if Red5 is restarted in the meantime.
	 * 
	 * @param	timeDelta	Time offset in milliseconds from the current date when given job should be run
	 * @param	job			Scheduled job object
	 * 
	 * @return	Name of the scheduled job
	 */
	public String addScheduledOnceJob(long timeDelta, IScheduledJob job) {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.addScheduledOnceJob(timeDelta, job);
	}
	
	/**
	 * Adds a scheduled job that's gonna be executed once on given date. Please note
	 * that the jobs are not saved if Red5 is restarted in the meantime.
	 * 
	 * @param	date	When to run scheduled job
	 * @param	job		Scheduled job object
	 * 
	 * @return	Name of the scheduled job
	 */
	public String addScheduledOnceJob(Date date, IScheduledJob job) {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.addScheduledOnceJob(date, job);
	}
	
	/**
	 * Removes scheduled job from scheduling service list
	 * 
	 * @param	name	Scheduled job name
	 */
	public void removeScheduledJob(String name) {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		service.removeScheduledJob(name);
	}
	
	/**
	 * Retuns list of scheduled job names
	 * 
	 * @return	List of scheduled job names as list of Strings.
	 */
	public List<String> getScheduledJobNames() {
		ISchedulingService service = (ISchedulingService) getScopeService(scope, ISchedulingService.SCHEDULING_SERVICE, QuartzSchedulingService.class);
		return service.getScheduledJobNames();
	}

	// NOTE: Method added to get flv player to work.
	/**
	 * Returns stream length. This is a hook so do not count on this method 'cause situation may one day.
	 * 
	 * @param	name	Stream name
	 * @return	Stream length in seconds (?)
	 */
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
			duration = (double) reader.getDuration() / 1000;
			reader.close();
		} catch (IOException e) {
			log.error("error read stream file " + file.getAbsolutePath(), e);
		}
		
		return duration;
	}
	
}