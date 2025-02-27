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

import cn.hippo4j.common.config.ApplicationContextHolder;
import cn.hippo4j.common.web.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

/**
 * Web thread pool handler choose.
 *
 * @author chen.ma
 * @date 2022/1/20 20:15
 */
@Slf4j
public class WebThreadPoolHandlerChoose {

    /**
     * Choose the web thread pool service bean.
     *
     * @return
     */
    public WebThreadPoolService choose() {
        WebThreadPoolService webThreadPoolService;
        try {
            webThreadPoolService = ApplicationContextHolder.getBean(WebThreadPoolService.class);
        } catch (Exception ex) {
            throw new ServiceException("Web thread pool service bean not found.", ex);
        }
        return webThreadPoolService;
    }
}
