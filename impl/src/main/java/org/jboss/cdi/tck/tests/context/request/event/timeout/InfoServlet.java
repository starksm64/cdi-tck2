/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.cdi.tck.tests.context.request.event.timeout;

import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.cdi.tck.util.Timer;
import org.jboss.cdi.tck.util.Timer.StopCondition;

/**
 * @author Martin Kouba
 *
 */
@SuppressWarnings("serial")
@WebServlet("/info")
public class InfoServlet extends HttpServlet {

    private final ApplicationScopedObserver observingBean;

    @Inject
    TimeoutService timeoutService;

    @Inject
    public InfoServlet(ApplicationScopedObserver observingBean) {
        this.observingBean = observingBean;
    }



    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        SynchronousQueue<Boolean> queue = new SynchronousQueue<Boolean>();
        timeoutService.start(queue);

        Boolean initializedResult;
        try {
            initializedResult = queue.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ServletException(e);
        }

        resp.getWriter().append("Initialized:" + initializedResult);
        resp.getWriter().append("\n");

        // wait for the request scope created for the async method invocation to be destroyed
        try {
            new Timer().setDelay(5, TimeUnit.SECONDS).addStopCondition(new StopCondition() {
                public boolean isSatisfied() {
                    return observingBean.isDestroyedCalled();
                }
            }).start();
        } catch (InterruptedException e) {
            throw new ServletException(e);
        }

        resp.getWriter().append("Destroyed:" + observingBean.isDestroyedCalled());
        resp.setContentType("text/plain");
    }

}
