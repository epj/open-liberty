/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.web.server.version20.container;

import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletRegistration;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;

@ConfigurationProperties(prefix = "server.liberty", ignoreUnknownFields = true)
public class LibertyReactiveWebServerFactory extends AbstractReactiveWebServerFactory implements ApplicationContextAware, LibertyFactoryBase {
    private boolean useDefaultHost = true;
    private ApplicationContext context;
    private final AtomicReference<LibertyWebServer> usingDefaultHost = new AtomicReference<>();

    @Override
    public WebServer getWebServer(HttpHandler httpHandler) {
        ServletContextInitializer[] initializers = { (c) -> {
            ServletHttpHandlerAdapter servlet = new ServletHttpHandlerAdapter(httpHandler);
            ServletRegistration.Dynamic registration = c.addServlet("http-handler-adapter", servlet);
            registration.setLoadOnStartup(1);
            registration.addMapping("/");
            registration.setAsyncSupported(true);
        } };
        return new LibertyWebServer(this, this, initializers);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    String getApplicationID() {
        return context.getId();
    }

    public void setUseDefaultHost(boolean useDefaultHost) {
        this.useDefaultHost = useDefaultHost;
    }

    @Override
    public boolean shouldUseDefaultHost(LibertyWebServer container) {
        // only use default host if configured to and
        // this is the root application context
        return useDefaultHost && context.getParent() == null && usingDefaultHost.compareAndSet(null, container);
    }

    @Override
    public void stopUsingDefaultHost(LibertyWebServer container) {
        usingDefaultHost.compareAndSet(container, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.springboot.support.web.server.version20.container.LibertyFactoryBase#getContextPath()
     */
    @Override
    public String getContextPath() {
        return "/";
    }
}
