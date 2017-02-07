package com.alibaba.dubbo.rpc.protocol.jersey;

import org.glassfish.jersey.server.ResourceConfig;

public class ResourceConfigSingleton {

	private static final ResourceConfig rc = new ResourceConfig();
	
	public static ResourceConfig getInstance(){
		return rc;
	}
	
}
