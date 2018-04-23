package org.smartrplace.sim.resource.impl;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.LoggingUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;

class ResourceSimulation implements TimerListener {

	private final SingleValueResource resource;
	private final ReadOnlyTimeSeries input;
	private long time = -1;
	private final Timer timer;
	
	ResourceSimulation(SingleValueResource resource, ReadOnlyTimeSeries input, ApplicationManager appMan) {
		this.resource = resource.getLocationResource();
		this.input = input;
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
			timerElapsed(timer);
			return;
		}
		timer.stop();
		time = next.getTimestamp();
		setValue(resource, next.getValue());
		final SampledValue coming = input.getNextValue(time + 1);
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
	
}
