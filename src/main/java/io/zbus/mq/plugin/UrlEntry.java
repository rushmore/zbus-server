package io.zbus.mq.plugin;

public class UrlEntry{
	public String url; //url path
	public String mq;
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mq == null) ? 0 : mq.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
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
		UrlEntry other = (UrlEntry) obj;
		if (mq == null) {
			if (other.mq != null)
				return false;
		} else if (!mq.equals(other.mq))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}   
}