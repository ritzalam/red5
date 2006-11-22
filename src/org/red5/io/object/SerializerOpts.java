package org.red5.io.object;

public interface SerializerOpts {

	public enum Flag {
		Enabled, Disabled, Default
	}

	public enum SerializerOption {
		SerializeClassName
	}

	public Flag getSerializerOption(SerializerOption opt);
}
