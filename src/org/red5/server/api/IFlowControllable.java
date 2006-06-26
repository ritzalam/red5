package org.red5.server.api;

/**
 * Mark an object that can be flow-controlled.
 * <p>
 * A flow-controlled object has the bandwidth config property
 * and a link to the parent controllable object.
 * <p>
 * The parent controllable object acts as the bandwidth provider
 * for this object, thus generates a tree structure, in which
 * the <tt>null</tt> parent means the host. The next depth level
 * is the <tt>IClient</tt>. The following is
 * <tt>IStreamCapableConnection</tt>. The deepest level is
 * <tt>IClientStream</tt>.
 * <p>
 * The child node consumes the parent's bandwidth. We say that
 * the child node is the bandwidth consumer while the parent is
 * the bandwidth provider.
 * <p>
 * We predefine the bandwidth configure for host and the host is
 * always a bandwidth provider. While the streams are always the
 * bandwidth consumer. The internal node is both provider and
 * consumer.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IFlowControllable {
	IFlowControllable getParentFlowControllable();
	IBandwidthConfigure getBandwidthConfigure();
	void setBandwidthConfigure(IBandwidthConfigure config);
}
