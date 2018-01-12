package io.zbus.unittests;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo; 

public class Test {

	public static void main(String[] args) throws Exception {   
		String url = "/mytopic/group1/?cmd=consume&&token=xxxyyy";
		// /myrpc/plus/1/2/?module=xxxx&token=ttttt
		UrlInfo info = HttpKit.parseUrl(url);
		System.out.println(info.path);
		System.out.println(info.params);
	}

}
