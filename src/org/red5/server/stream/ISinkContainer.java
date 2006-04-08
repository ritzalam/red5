package org.red5.server.stream;

public interface ISinkContainer {

	public void connect(IStreamSink stream);

	public void disconnect(IStreamSink stream);

}
