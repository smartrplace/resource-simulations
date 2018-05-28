/**
 * Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.sim.resource.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;

// TODO support additive parameter
class CyclicIterator implements Iterator<SampledValue> {
	
	private final ReadOnlyTimeSeries base;
	private Iterator<SampledValue> it;
	// value of base
	private SampledValue last;
	private long lastT;
	private long lastDiff;
	private float lastValue;
	
	CyclicIterator(ReadOnlyTimeSeries timeSeries, final long startTime) {
		this.base = timeSeries;
		this.it = timeSeries.iterator();
		lastT = startTime;
		if (it.hasNext()) {
			last = it.next();
			lastDiff = 1000; // irrelevant except in edge cases
		}
	}

	@Override
	public boolean hasNext() {
		return !base.isEmpty();
	}

	@Override
	public SampledValue next() {
		if (!it.hasNext()) {
			it = base.iterator();
			if (!it.hasNext())
				throw new NoSuchElementException("No further element");
			final SampledValue first = it.next();
			lastT = lastT + lastDiff;
			last = first;
			final SampledValue next = new SampledValue(first.getValue(), lastT, first.getQuality());
			return next;
		}
		final SampledValue next = it.next();
		lastDiff = next.getTimestamp() - last.getTimestamp();
		lastT = lastT + lastDiff;
		last = next;
		final SampledValue computed = new SampledValue(next.getValue(), lastT, next.getQuality());
		return computed;
	}

}
