package org.red5.server.so;

import org.red5.server.api.event.IEventListener;

public class FlexSharedObjectMessage extends SharedObjectMessage {

    /**
     * Creates Flex2 Shared Object event with given name, version and persistence flag
     *
     * @param name          Event name
     * @param version       SO version
     * @param persistent    SO persistence flag
     */
    public FlexSharedObjectMessage(String name, int version, boolean persistent) {
		this(null, name, version, persistent);
	}

    /**
     * Creates Flex2 Shared Object event with given listener, name, SO version and persistence flag
     *
     * @param source         Event listener
     * @param name           Event name
     * @param version        SO version
     * @param persistent     SO persistence flag
     */
    public FlexSharedObjectMessage(IEventListener source, String name, int version,
			boolean persistent) {
    	super(source, name, version, persistent);
    	
    }

	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_FLEX_SHARED_OBJECT;
	}


}
