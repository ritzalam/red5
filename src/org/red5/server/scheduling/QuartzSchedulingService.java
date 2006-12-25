package org.red5.server.scheduling;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Scheduling service that uses Quartz as backend. 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class QuartzSchedulingService implements ISchedulingService {
    /**
     * Logger
     */
	private Log log = LogFactory
			.getLog(QuartzSchedulingService.class.getName());

    /**
     * Creates schedulers
     */
    private static SchedulerFactory schedFact = new StdSchedulerFactory();

    /**
     * Service scheduler
     */
    private Scheduler scheduler;

    /**
     * Number of job details
     */
    private long jobDetailCounter;

	/** Constructs a new QuartzSchedulingService. */
    public QuartzSchedulingService() {
		try {
			scheduler = schedFact.getScheduler();
			scheduler.start();
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
     * Getter for job name.
     *
     * @return  Job name
     */
    private synchronized String getJobName() {
		String result = "ScheduledJob_" + jobDetailCounter;
		jobDetailCounter++;
		return result;
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
		// Store reference to applications job and service 
		JobDetail jobDetail = new JobDetail(name, null,
				QuartzSchedulingServiceJob.class);
		jobDetail.getJobDataMap().put(
				QuartzSchedulingServiceJob.SCHEDULING_SERVICE, this);
		jobDetail.getJobDataMap().put(QuartzSchedulingServiceJob.SCHEDULED_JOB,
				job);

		try {
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	/** {@inheritDoc} */
    public String addScheduledJob(int interval, IScheduledJob job) {
		String result = getJobName();

		// Create trigger that fires indefinitely every <interval> milliseconds
		SimpleTrigger trigger = new SimpleTrigger("Trigger_" + result, null,
				new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, interval);
		scheduleJob(result, trigger, job);
		return result;
	}

	/** {@inheritDoc} */
    public String addScheduledOnceJob(long timeDelta, IScheduledJob job) {
		// Create trigger that fires once in <timeDelta> milliseconds
		return addScheduledOnceJob(new Date(System.currentTimeMillis()
				+ timeDelta), job);
	}

	/** {@inheritDoc} */
    public String addScheduledOnceJob(Date date, IScheduledJob job) {
		String result = getJobName();

		// Create trigger that fires once at <date>
		SimpleTrigger trigger = new SimpleTrigger("Trigger_" + result, null,
				date);
		scheduleJob(result, trigger, job);
		return result;
	}

	/** {@inheritDoc} */
    public void removeScheduledJob(String name) {
		try {
			scheduler.deleteJob(name, null);
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
	}

	/** {@inheritDoc} */
    public List<String> getScheduledJobNames() {
		List<String> result = new ArrayList<String>();
		try {
			for (String name : scheduler.getJobNames(null)) {
				result.add(name);
			}
		} catch (SchedulerException ex) {
			throw new RuntimeException(ex);
		}
		return result;
	}

}
