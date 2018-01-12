package io.zbus.transport;

import java.io.IOException;

import io.zbus.kit.FileKit;

public class ServerAddress implements Cloneable {
	public String address;  //<Host>:<Port>, default to 80 if port missing
	public boolean sslEnabled;
	public transient String certificate;  //if ssl/tls enabled, certificate represents the string bytes of certificate file
	
	public transient Server server; //InProc server
	
	public transient String token;  //May need for authentication
	
	public ServerAddress(){
		
	}
	public ServerAddress(String address){
		this.address = address;
	}
	
	public ServerAddress(String address, boolean sslEnabled) {
		this.address = address;
		this.sslEnabled = sslEnabled;
	}  
	
	public ServerAddress(String address, String token) {
		this.address = address;
		this.token = token;
	}  
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public boolean isSslEnabled() {
		return sslEnabled;
	}
	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}
	public String getCertificate() {
		return certificate;
	}
	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}
	
	public void setCertFile(String certFilePath) throws IOException{
		this.certificate = FileKit.loadFile(certFilePath); 
	}
	
	public Server getServer() {
		return server;
	}
	public void setServer(Server server) {
		this.server = server;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
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
	
	public ServerAddress clone(){
		ServerAddress other = new ServerAddress();
		other.address = address;
		other.sslEnabled = sslEnabled;
		other.certificate = certificate;
		other.server = server;
		other.token = token;
		
		return other;
	} 
}