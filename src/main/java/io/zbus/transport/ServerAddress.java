package io.zbus.transport;

public class ServerAddress{
	public String address;
	public boolean sslEnabled;
	public Server server; //InProc server
	
	public ServerAddress(){
		
	}
	public ServerAddress(String address){
		this.address = address;
	}
	
	public ServerAddress(String address, boolean sslEnabled) {
		this.address = address;
		this.sslEnabled = sslEnabled;
	}
	
	@Override
	public String toString() {
		if(server != null){
			return "[InProc]" + server.getServerAddress();
		}
		return sslEnabled? "[SSL]"+address : address;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((server == null) ? 0 : server.hashCode());
		result = prime * result + (sslEnabled ? 1231 : 1237);
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
		ServerAddress other = (ServerAddress) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (server == null) {
			if (other.server != null)
				return false;
		} else if (!server.equals(other.server))
			return false;
		if (sslEnabled != other.sslEnabled)
			return false;
		return true;
	}

}