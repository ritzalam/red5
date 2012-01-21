/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.scheduling;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled job that is registered in the Quartz scheduler. 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class QuartzSchedulingServiceJob implements Job {
    /**
     * Scheduling service constant
     */
    protected static final String SCHEDULING_SERVICE = "scheduling_service";

    /**
     * Scheduled job constant
     */
    protected static final String SCHEDULED_JOB = "scheduled_job";

    /**
     * Logger
     */
    private Logger log = LoggerFactory.getLogger(QuartzSchedulingServiceJob.class);

    /** {@inheritDoc} */
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
		ISchedulingService service = (ISchedulingService) arg0.getJobDetail()
				.getJobDataMap().get(SCHEDULING_SERVICE);
		IScheduledJob job = (IScheduledJob) arg0.getJobDetail().getJobDataMap()
				.get(SCHEDULED_JOB);
        try {
            job.execute(service);
        } catch (Throwable e) {
            log.error("Job {} execution failed", job.toString(), e);
        }
    }

}
