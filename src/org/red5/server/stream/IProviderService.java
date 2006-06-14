package org.red5.server.stream;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IBroadcastStreamNew;
import org.red5.server.messaging.IMessageInput;
import org.red5.server.messaging.IProvider;

public interface IProviderService {
	public static final String KEY = "providerService";
	
	/**
	 * Get a named provider as the source of input.
	 * Live stream first, VOD stream second.
	 * @param scope
	 * @param name
	 * @return <tt>null</tt> if nothing found.
	 */
	IMessageInput getProviderInput(IScope scope, String name);
	
	/**
	 * Get a named Live provider as the source of input.
	 * @param scope
	 * @param name
	 * @return <tt>null</tt> if not found.
	 */
	IMessageInput getLiveProviderInput(IScope scope, String name, boolean needCreate);
	
	/**
	 * Get a named VOD provider as the source of input.
	 * @param scope
	 * @param name
	 * @return <tt>null</tt> if not found.
	 */
	IMessageInput getVODProviderInput(IScope scope, String name);
	
	/**
	 * Register a broadcast stream to a scope.
	 * @param scope
	 * @param name
	 * @param bs
	 * @return <tt>true</tt> if register successfully.
	 */
	boolean registerBroadcastStream(IScope scope, String name, IBroadcastStream bs);

	/**
	 * Unregister a broadcast stream of a specific name from a scope.
	 * @param scope
	 * @param name
	 * @return <tt>true</tt> if unregister successfully.
	 */
	boolean unregisterBroadcastStream(IScope scope, String name);
}
