package org.red5.io.object;

/**
 * Serializer options
 */
public interface SerializerOpts {

	public enum Flag {
		Enabled, Disabled, Default
	}

	public enum SerializerOption {
		SerializeClassName
	}

    /**
     * Return serializer options flag
     * @param opt         Serializer option
     * @return            Option flag (enabled, disabled or default)
     */
    public Flag getSerializerOption(SerializerOption opt);
}
