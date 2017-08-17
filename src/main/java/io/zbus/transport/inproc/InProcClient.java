package io.zbus.transport.inproc;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.AbstractClient;
import io.zbus.transport.AttributeMap;
import io.zbus.transport.Id;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Server;
import io.zbus.transport.Session;

public class InProcClient<REQ extends Id, RES extends Id> extends AbstractClient<REQ, RES> {
	private static final Logger log = LoggerFactory.getLogger(InProcClient.class);
	private final IoAdaptor serverIoAdaptor;
	
	public InProcClient(IoAdaptor serverIoAdaptor) {
		this.serverIoAdaptor = serverIoAdaptor;
	}
	
	public InProcClient(Server server) {
		this.serverIoAdaptor = server.getIoAdatpr();
	}

	protected String serverAddress() {
		return "Server-InProc";
	}
 
	public synchronized void connectAsync() {
		if(this.session != null) return; 
		this.session = new InprocSession();
		try {
			sessionCreated(this.session);
		} catch (IOException e) {
			//ignore
		}  
	}
	

	@Override
	public void connectSync(long timeout) throws IOException, InterruptedException {
		if (hasConnected())
			return;

		synchronized (this) {
			if (!hasConnected()) {
				connectAsync();
				activeLatch.await(timeout, TimeUnit.MILLISECONDS);

				if (hasConnected()) {
					return;
				}
				String msg = String.format("Connection(%s) timeout", serverAddress());
				log.warn(msg);
				cleanSession();
			}
		}
	}
	
	@Override
	public void sendMessage(REQ req) throws IOException, InterruptedException {
		if(!hasConnected()){
			connectSync(connectTimeout);  
			if(!hasConnected()){
				String msg = String.format("Connection(%s) timeout", serverAddress()); 
				throw new IOException(msg);
			}
		}  
		serverIoAdaptor.onMessage(req, this.session);
	}
	
	class InprocSession extends AttributeMap implements Session {
		private final String id; 

		public InprocSession() { 
			this.id = UUID.randomUUID().toString();
			try {
				serverIoAdaptor.sessionCreated(this);
			} catch (IOException e) {
				// ignore
			}
		}

		@Override
		public String id() {
			return id;
		}

		public String remoteAddress() {
			return "InProc-Client-" + id();
		}

		public String localAddress() {
			return "InProc-Server-" + id();
		}

		public void write(Object msg) {
			try {
				InProcClient.this.onMessage(msg, this);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}

		@Override
		public void close() throws IOException {
			//ignore
		}

		@Override
		public boolean active() {
			return true;
		}

		@Override
		public String toString() {
			return "Session [" + "remote=" + remoteAddress() + ", active=" + active() + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Session other = (Session) obj;
			if (id == null) {
				if (other.id() != null)
					return false;
			} else if (!id.equals(other.id()))
				return false;
			return true;
		}
	}
}

