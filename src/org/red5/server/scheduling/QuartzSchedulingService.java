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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.jmx.mxbeans.QuartzSchedulingServiceMXBean;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Scheduling service that uses Quartz as backend.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ManagedResource(objectName = "org.red5.server:name=schedulingService,type=QuartzSchedulingService")
public class QuartzSchedulingService implements ISchedulingService, QuartzSchedulingServiceMXBean, InitializingBean, DisposableBean {

	private static Logger log = Red5LoggerFactory.getLogger(QuartzSchedulingService.class);

	/**
	 * Number of job details
	 */
	protected AtomicLong jobDetailCounter = new AtomicLong(0);

	/**
	 * Creates schedulers.
	 */
	protected SchedulerFactory factory;

	/**
	 * Service scheduler
	 */
	protected Scheduler scheduler;

	/**
	 * Instance id
	 */
	protected String instanceId;

	/** Constructs a new QuartzSchedulingService. */
	public void afterPropertiesSet() throws Exception {
		log.debug("Initializing...");
		try {
			//create the standard factory if we dont have one
			if (factory == null) {
				factory = new StdSchedulerFactory();
			}
			if (instanceId == null) {
				scheduler = factory.getScheduler();
			} else {
				scheduler = factory.getScheduler(instanceId);
			}
			//start the scheduler
			if (scheduler != null) {
				scheduler.start();
			} else {
				log.error("Scheduler was not started");
			}
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void setFactory(SchedulerFactory factory) {
		this.factory = factory;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

//	protected void registerJMX() {
//		//register with jmx server
//		MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
//		try {
//			ObjectName oName = null;
//			if (instanceId == null) {
//				oName = new ObjectName("org.red5.server:name=" + this.getClass().getName());
//			} else {
//				oName = new ObjectName("org.red5.server:name=" + this.getClass().getName() + ",instanceId=" + instanceId);
//			}
//	        mbeanServer.registerMBean(this, oName);
//		} catch (Exception e) {
//			log.warn("Error on jmx registration", e);
//		}		
//	}

	/** {@inheritDoc} */
	public String addScheduledJob(int interval, IScheduledJob job) {
		String result = getJobName();

		// Create trigger that fires indefinitely every <interval> milliseconds
		SimpleTrigger trigger = new SimpleTrigger("Trigger_" + result, null, new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, interval);
		scheduleJob(result, trigger, job);
		return result;
	}

	/** {@inheritDoc} */
	public String addScheduledOnceJob(Date date, IScheduledJob job) {
		String result = getJobName();

		// Create trigger that fires once at <date>
		SimpleTrigger trigger = new SimpleTrigger("Trigger_" + result, null, date);
		scheduleJob(result, trigger, job);
		return result;
	}

	/** {@inheritDoc} */
	public String addScheduledOnceJob(long timeDelta, IScheduledJob job) {
		// Create trigger that fires once in <timeDelta> milliseconds
		return addScheduledOnceJob(new Date(System.currentTimeMillis() + timeDelta), job);
	}

	/** {@inheritDoc} */
	public String addScheduledJobAfterDelay(int interval, IScheduledJob job, int delay) {
		String result = getJobName();
		// Initialize the start time to now and add the delay.
		long startTime = System.currentTimeMillis() + delay;
		// Create trigger that fires indefinitely every <internval> milliseconds.
		SimpleTrigger trigger = new SimpleTrigger("Trigger_" + result, null, new Date(startTime), null, SimpleTrigger.REPEAT_INDEFINITELY, interval);
		// Schedule the job with Quartz.
		scheduleJob(result, trigger, job);
		// Return the job name.
		return result;
	}

	/**
	 * Getter for job name.
	 *
	 * @return  Job name
	 */
	public String getJobName() {
		String result = "ScheduledJob_" + jobDetailCounter.getAndIncrement();
		return result;
	}

	/** {@inheritDoc} */
	public List<String> getScheduledJobNames() {
		List<String> result = new ArrayList<String>();
		if (scheduler != null) {
			try {
				for (String name : scheduler.getJobNames(null)) {
					result.add(name);
				}
			} catch (SchedulerException ex) {
				throw new RuntimeException(ex);
			}
		} else {
			log.warn("No scheduler is available");
		}
		return result;
	}

	/** {@inheritDoc} */
	public void pauseScheduledJob(String name) {
		try {
			scheduler.pauseJob(name, null);
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	/** {@inheritDoc} */
	public void resumeScheduledJob(String name) {
		try {
			scheduler.resumeJob(name, null);
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void pauseScheduledTrigger(String name) {
		try {
			scheduler.pauseTrigger("Trigger_" + name, null);
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void resumeScheduledTrigger(String name) {
		try {
			scheduler.resumeTrigger("Trigger_" + name, null);
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	/** {@inheritDoc} */
	public void removeScheduledJob(String name) {
		try {
			scheduler.deleteJob(name, null);
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Schedules job
	 * @param name               Job name
	 * @param trigger            Job trigger
	 * @param job                Scheduled job object
	 *
	 * @see org.red5.server.api.scheduling.IScheduledJob
	 */
	private void scheduleJob(String name, Trigger trigger, IScheduledJob job) {
		if (scheduler != null) {
			// Store reference to applications job and service
			JobDetail jobDetail = new JobDetail(name, null, QuartzSchedulingServiceJob.class);
			jobDetail.getJobDataMap().put(QuartzSchedulingServiceJob.SCHEDULING_SERVICE, this);
			jobDetail.getJobDataMap().put(QuartzSchedulingServiceJob.SCHEDULED_JOB, job);
			try {
				scheduler.scheduleJob(jobDetail, trigger);
			} catch (SchedulerException ex) {
				throw new RuntimeException(ex);
			}
		} else {
			log.warn("No scheduler is available");
		}
	}

	public void destroy() throws Exception {
		if (scheduler != null) {
			log.debug("Destroying...");
			scheduler.shutdown();
		}
	}

}
