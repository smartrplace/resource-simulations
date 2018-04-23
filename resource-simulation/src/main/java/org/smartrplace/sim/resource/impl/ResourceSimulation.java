package org.smartrplace.sim.resource.impl;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.LoggingUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;

// TODO clean up schedule values at some point...
class ResourceSimulation implements TimerListener {

	private final SingleValueResource resource;
	private final ReadOnlyTimeSeries input;
	private final boolean additive;
	private final Schedule forecast;
	private final CyclicIterator iterator;
	private final ConfigPattern config;
	private final Timer timer;
	
	// state; time in model schedule
	private long time = -1;
	private float offset = 0;
	
	ResourceSimulation(ConfigPattern config, ReadOnlyTimeSeries input, ApplicationManager appMan) {
		this.config = config;
		this.resource = config.target.getLocationResource();
		this.input = input;
		this.additive = config.additive.isActive() && config.additive.getValue();
		if (!config.forecast.isActive()) {
			this.forecast = null;
			this.iterator = null;
		} else {
			final String scheduleName = config.forecast.getValue();
			Schedule sched0 = resource.getSubResource(scheduleName);
			if (sched0 == null)
				sched0 = resource.getSubResource(scheduleName, AbsoluteSchedule.class).create();
			this.forecast = sched0;
			sched0.activate(false);
			this.iterator = new CyclicIterator(input, appMan.getFrameworkTime());
		}
		this.timer = appMan.createTimer(10000000, this);
		timerElapsed(timer);
		resource.activate(false);
		LoggingUtils.activateLogging(resource, -2);
		appMan.getLogger().debug("Simulation started for resource {}", resource);
	}

	@Override
	public void timerElapsed(Timer timer) {
		final SampledValue next = input.getNextValue(time + 1);
		if (next == null) {
			time = -1;
			offset = ValueResourceUtils.getFloatValue(resource);
			timerElapsed(timer);
			return;
		}
		timer.stop();
		time = next.getTimestamp();
		final Value value;
		if (additive)
			value = new FloatValue(offset + next.getValue().getFloatValue());
		else 
			value = next.getValue();
		setValue(resource, value);
		final SampledValue coming = input.getNextValue(time + 1);
		final long now = timer.getExecutionTime();
		if (forecast != null) {
			final long horizon = getForecastHorizon(config);
			final SampledValue last = forecast.getPreviousValue(Long.MAX_VALUE);
			final long startTime;
			if (last == null) {
				startTime = now;
			} else if (last.getTimestamp() >= now + horizon) {
				startTime = Long.MAX_VALUE;
			} else {
				startTime = Math.max(now, last.getTimestamp() + 1);
			}
			if (startTime < now +horizon) {
				final List<SampledValue> forecastValues = new ArrayList<>();
				while (true) {
					final SampledValue next0 = iterator.next();
					if (next0.getTimestamp() < startTime)
						continue;
					forecastValues.add(next0);
					if (next0.getTimestamp() >= now + horizon)
						break;
				}
				if (!forecastValues.isEmpty())
					forecast.addValues(forecastValues);
			}
		}
		final long diff = coming == null ? 5000 : (coming.getTimestamp() - time); 
		timer.setTimingInterval(diff);
		timer.resume();
	}
	
	void close() {
		timer.destroy();
	}
	
	private static void setValue(final SingleValueResource resource, final Value value) {
		if (resource instanceof BooleanResource) {
			if (!(value instanceof BooleanValue)) {
				// workaround... it is not possible to convert FloatValue to BooleanValue
				((BooleanResource) resource).setValue(Math.abs(value.getFloatValue()) > 0.0001); 
			} else {
				((BooleanResource) resource).setValue(value.getBooleanValue());
			}
		} else {
			ValueResourceUtils.setValue(resource, value);
		}
	}
	
	private static long getForecastHorizon(final ConfigPattern config) {
		if (config.forecastHorizon.isActive()) {
			final long try0 = config.forecastHorizon.getValue();
			if (try0 > 0)
				return try0;
		}
		return 24 * 60 * 60 * 1000; // one day
	}
	
}
