/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.hippo4j.core.executor.web;

import cn.hippo4j.common.model.PoolBaseInfo;
import cn.hippo4j.common.model.PoolParameter;
import cn.hippo4j.common.model.PoolParameterInfo;
import cn.hippo4j.common.model.PoolRunStateInfo;
import cn.hippo4j.core.executor.state.AbstractThreadPoolRuntime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.hippo4j.common.constant.ChangeThreadPoolConstants.CHANGE_DELIMITER;

/**
 * Tomcat web thread pool handler.
 *
 * @author chen.ma
 * @date 2022/1/19 20:57
 */
@Slf4j
@RequiredArgsConstructor
public class TomcatWebThreadPoolHandler extends AbstractWebThreadPoolService {

    private final AtomicBoolean cacheFlag = new AtomicBoolean(Boolean.FALSE);

    private static String EXCEPTION_MESSAGE;

    private final AbstractThreadPoolRuntime webThreadPoolRunStateHandler;

    @Override
    protected Executor getWebThreadPoolByServer(WebServer webServer) {
        if (cacheFlag.get()) {
            log.warn("Exception getting Tomcat thread pool. Exception message :: {}", EXCEPTION_MESSAGE);
            return null;
        }
        Executor tomcatExecutor = null;
        try {
            tomcatExecutor = ((TomcatWebServer) webServer).getTomcat().getConnector().getProtocolHandler().getExecutor();
        } catch (Exception ex) {
            cacheFlag.set(Boolean.TRUE);
            EXCEPTION_MESSAGE = ex.getMessage();
            log.error("Failed to get Tomcat thread pool. Message :: {}", EXCEPTION_MESSAGE);
        }
        return tomcatExecutor;
    }

    @Override
    public PoolBaseInfo simpleInfo() {
        PoolBaseInfo poolBaseInfo = new PoolBaseInfo();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        int corePoolSize = threadPoolExecutor.getCorePoolSize();
        int maximumPoolSize = threadPoolExecutor.getMaximumPoolSize();
        RejectedExecutionHandler rejectedExecutionHandler = threadPoolExecutor.getRejectedExecutionHandler();
        long keepAliveTime = threadPoolExecutor.getKeepAliveTime(TimeUnit.SECONDS);

        BlockingQueue<Runnable> queue = threadPoolExecutor.getQueue();
        int queueSize = queue.size();
        int remainingCapacity = queue.remainingCapacity();
        int queueCapacity = queueSize + remainingCapacity;

        poolBaseInfo.setCoreSize(corePoolSize);
        poolBaseInfo.setMaximumSize(maximumPoolSize);
        poolBaseInfo.setKeepAliveTime(keepAliveTime);
        poolBaseInfo.setQueueType(queue.getClass().getSimpleName());
        poolBaseInfo.setQueueCapacity(queueCapacity);
        poolBaseInfo.setRejectedName(rejectedExecutionHandler.getClass().getSimpleName());
        return poolBaseInfo;
    }

    @Override
    public PoolParameter getWebThreadPoolParameter() {
        PoolParameterInfo parameterInfo = null;
        try {
            parameterInfo = new PoolParameterInfo();
            ThreadPoolExecutor tomcatExecutor = (ThreadPoolExecutor) executor;
            int minThreads = tomcatExecutor.getCorePoolSize();
            int maxThreads = tomcatExecutor.getMaximumPoolSize();
            long keepAliveTime = tomcatExecutor.getKeepAliveTime(TimeUnit.SECONDS);

            parameterInfo.setCoreSize(minThreads);
            parameterInfo.setMaxSize(maxThreads);
            parameterInfo.setKeepAliveTime((int) keepAliveTime);
        } catch (Exception ex) {
            log.error("Failed to get the tomcat thread pool parameter.", ex);
        }
        return parameterInfo;
    }

    @Override
    public PoolRunStateInfo getWebRunStateInfo() {
        return webThreadPoolRunStateHandler.getPoolRunState(null, executor);
    }

    @Override
    public void updateWebThreadPool(PoolParameterInfo poolParameterInfo) {
        try {
            ThreadPoolExecutor tomcatExecutor = (ThreadPoolExecutor) executor;
            int originalCoreSize = tomcatExecutor.getCorePoolSize();
            int originalMaximumPoolSize = tomcatExecutor.getMaximumPoolSize();
            long originalKeepAliveTime = tomcatExecutor.getKeepAliveTime(TimeUnit.SECONDS);
            tomcatExecutor.setCorePoolSize(poolParameterInfo.getCoreSize());
            tomcatExecutor.setMaximumPoolSize(poolParameterInfo.getMaxSize());
            tomcatExecutor.setKeepAliveTime(poolParameterInfo.getKeepAliveTime(), TimeUnit.SECONDS);
            log.info(
                    "[TOMCAT] Changed web thread pool. " +
                            "\n    coreSize :: [{}]" +
                            "\n    maxSize :: [{}]" +
                            "\n    keepAliveTime :: [{}]",
                    String.format(CHANGE_DELIMITER, originalCoreSize, poolParameterInfo.getCoreSize()),
                    String.format(CHANGE_DELIMITER, originalMaximumPoolSize, poolParameterInfo.getMaxSize()),
                    String.format(CHANGE_DELIMITER, originalKeepAliveTime, poolParameterInfo.getKeepAliveTime()));
        } catch (Exception ex) {
            log.error("Failed to modify the Tomcat thread pool parameter.", ex);
        }
    }
}
