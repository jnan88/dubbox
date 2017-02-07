package com.alibaba.dubbo.rpc.protocol.jersey;

import javax.servlet.ServletContext;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.jersey.server.ResourceConfig;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;

public abstract class JerseyBaseRestServer implements JerseyServer {
	private org.glassfish.jersey.servlet.ServletContainer container = new org.glassfish.jersey.servlet.ServletContainer();
	@Override
	public void start(URL url) {
		loadProviders(url.getParameter(Constants.EXTENSION_KEY, ""));
		doStart(url);

	}

	@Override
	public void deploy(Class resourceDef, Object resourceInstance, String contextPath) {
		if(StringUtils.isNotEmpty(contextPath)){
			getServletContext().setInitParameter("jersey.config.servlet.filter.contextPath", contextPath);
			getServletContext().addServlet("MyApplication", container).addMapping(contextPath);
		}else{
			getServletContext().setInitParameter("jersey.config.servlet.filter.contextPath", "/");
			getServletContext().addServlet("MyApplication", container).addMapping("/");
		}
		
		getResourceConfig().register(resourceDef);
		
	}

	@Override
	public void undeploy(Class resourceDef) {
		getResourceConfig().getClasses().remove(resourceDef);

	}

	protected void loadProviders(String value) {
		for (String clazz : Constants.COMMA_SPLIT_PATTERN.split(value)) {
			if (!StringUtils.isEmpty(clazz)) {
				getResourceConfig().register(clazz.trim());
			}
		}
	}

	protected abstract JerseyConfig getResourceConfig();
	protected abstract ServletContext getServletContext();

	protected abstract void doStart(URL url);

}
