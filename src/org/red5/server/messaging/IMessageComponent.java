package org.red5.server.messaging;

public interface IMessageComponent {
	/**
	 * 
	 * @param source
	 * @param pipe TODO
	 * @param oobCtrlMsg
	 */
	void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg);
}
