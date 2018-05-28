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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.units.ElectricCurrentResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.CompoundResourceEvent;
import org.ogema.core.resourcemanager.pattern.PatternChangeListener;
import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.timeseries.api.FloatTimeSeries;
import org.ogema.tools.timeseries.implementations.FloatTreeTimeSeries;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service=Application.class)
public class ResourceSimulationOrchestration implements Application, PatternListener<ConfigPattern>, PatternChangeListener<ConfigPattern> {

	public final static String DEFAULT_BASE_FOLDER = "simTemplates";
	public final static String BASE_FOLDER_PROPERTY = "org.smartrplace.sim.template.folder";
	private ApplicationManager appMan;
	private final Map<String, ResourceSimulation> simulations = new HashMap<>(8);
	// loaded on demand
	private final Map<Type, ReadOnlyTimeSeries> simulationTemplates = new HashMap<>(8);
	private volatile Bundle bundle;
	private volatile String baseFolder;
	private final static CSVFormat format = CSVFormat.DEFAULT.withDelimiter(';');
	
	@Activate
	protected void activate(final BundleContext ctx, final Map<String, ?> properties) {
		String baseFolder = null;
		if (properties.containsKey("basefolder")) {
			baseFolder = (String) properties.get("basefolder"); 
		} else {
			baseFolder = ctx.getProperty(BASE_FOLDER_PROPERTY);
		}
		if (baseFolder == null)
			baseFolder = DEFAULT_BASE_FOLDER;
		baseFolder = baseFolder.replace('\\', '/');
		if (baseFolder.endsWith("/"))
			baseFolder = baseFolder.substring(0, baseFolder.length()-1);
		this.baseFolder = baseFolder;
		this.bundle = ctx.getBundle();
	}
	
	@Deactivate
	protected void deactivate() {
		this.bundle = null;
		this.baseFolder = null;
	}
	
	@Override
	public void start(ApplicationManager appManager) {
		this.appMan = appManager;
		appManager.getResourcePatternAccess().addPatternDemand(ConfigPattern.class, this, AccessPriority.PRIO_LOWEST);
	}

	@Override
	public void stop(AppStopReason reason) {
		final ApplicationManager appMan = this.appMan;
		if (appMan != null) {
			try {
				appMan.getResourcePatternAccess().removePatternDemand(ConfigPattern.class, this);
			} catch (Exception ignore) {}
		} 
		for (ResourceSimulation sim : simulations.values())
			sim.close();
		simulations.clear();
		simulationTemplates.clear();
		this.appMan = null;
		this.bundle = null;
	}

	@Override
	public void patternChanged(ConfigPattern pattern, List<CompoundResourceEvent<?>> changes) {
		configGone(pattern);
		newConfig(pattern);
	}

	@Override
	public void patternAvailable(ConfigPattern pattern) {
		newConfig(pattern);
		appMan.getResourcePatternAccess().addPatternChangeListener(pattern, this, ConfigPattern.class);
	}

	@Override
	public void patternUnavailable(ConfigPattern pattern) {
		configGone(pattern);
		appMan.getResourcePatternAccess().removePatternChangeListener(pattern, this);
	}
	
	private void configGone(final ConfigPattern pattern) {
		final ResourceSimulation sim = simulations.remove(pattern.model.getPath());
		if (sim != null)
			sim.close();
	}
	
	private ResourceSimulation newConfig(final ConfigPattern pattern) {
		final Type type = getType(pattern);
		if (type == null) {
			appMan.getLogger().warn("Could not determine simulation type for target {}, configuration {}", pattern.target.getLocation(), pattern.model);
			return null;
		}
		final ReadOnlyTimeSeries timeSeries;
		try {
			timeSeries = getSimulationTemplate(type);
			if (timeSeries == null) {
				appMan.getLogger().warn("Simulation template for type {} not found. Cannot simulate resource {}, config {}",type, pattern.target.getLocation(), pattern.model);
				return null;
			}
		} catch (UncheckedIOException e) {
			appMan.getLogger().error("Failed to load simulation template",e.getCause());
			return null;
		}
		try {
			final ResourceSimulation sim = new ResourceSimulation(pattern, timeSeries, appMan);
			final ResourceSimulation old = simulations.put(pattern.model.getPath(), sim);
			if (old != null)
				old.close();
			return sim;
		} catch (SecurityException e) {
			return null;
		}
	}
	
	private ReadOnlyTimeSeries getSimulationTemplate(final Type type) {
		return simulationTemplates.computeIfAbsent(type, (key) -> getTemplateTimeseries(key, baseFolder, bundle));
	}
	
	public static ReadOnlyTimeSeries getTemplateTimeseries(final Type type, final String baseFolder, final Bundle bundle) {
		final String primary = type.primaryType;
		String secondary = type.secondaryType != null ? type.secondaryType : "default.csv";
		if (!secondary.toLowerCase().endsWith(".csv"))
			secondary = secondary + ".csv";
		final String path = baseFolder + "/" + primary + "/" + secondary;
		final Path file = Paths.get(path);
		final InputStream in;
		try {
			if (Files.isRegularFile(file)) {
				in = Files.newInputStream(file);
			} else {
				// does not search fragments
//				final URL url = bundle.getEntry(DEFAULT_BASE_FOLDER  + "/" + primary + "/" + secondary);
				final Enumeration<URL> urls = bundle.findEntries(DEFAULT_BASE_FOLDER  + "/" + primary, secondary, false);
				if (urls != null && urls.hasMoreElements()) {
					in = urls.nextElement().openStream();
				} else {
					return null;
				}
			}
			return parse(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private static ReadOnlyTimeSeries parse(final InputStream stream) throws IOException {
		try {
			final List<SampledValue> values = new ArrayList<>(1000);
			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
				try (final CSVParser parser = new CSVParser(reader, format)) {
					long initialTimestamp = Long.MIN_VALUE;
					long timestamp;
					float value;
					for (CSVRecord record : parser) {
						try {
							timestamp = Long.parseLong(record.get(0));
						} catch (NumberFormatException e) {
							continue;
						}
						if (initialTimestamp == Long.MIN_VALUE)
							initialTimestamp = timestamp;
						value = Float.parseFloat(record.get(1));
						values.add(new SampledValue(new FloatValue(value), timestamp - initialTimestamp, Quality.GOOD));	
					}
				}
			}
			final FloatTimeSeries timeSeries = new FloatTreeTimeSeries();
			timeSeries.addValues(values);
			return timeSeries;
		} finally {
			try {
				stream.close();
			} catch (Exception ignore) {}
		}
	}
	
	private static Type getType(ConfigPattern pattern) {
		if (pattern.typePrimary.isActive()) {
			final String primary = pattern.typePrimary.getValue();
			if (primary != null && !primary.isEmpty()) {
				final String typeSeconday = pattern.typeSecondary.isActive() ? pattern.typeSecondary.getValue() : null;
				return new Type(primary, typeSeconday);
			}
		}
		if (pattern.target instanceof PowerResource) {
			final int phase = isSubPhase(pattern.target);
			return new Type(Type.PRIMARY_POWER, phase == 0 ? null : Type.SECONDARY_POWER_PHASE + phase);
		}
		if (pattern.target instanceof TemperatureResource) {
			Room location = null;
			try {
				location = ResourceUtils.getDeviceLocationRoom(pattern.target);
			} catch (SecurityException ignore) {}
			final boolean outside = location != null && location.type().isActive() && location.type().getValue() == 0;
			return new Type(Type.PRIMARY_TEMPERATURE, outside ? Type.SECONDARY_TEMP_OUTSIDE : Type.SECONDARY_TEMP_INSIDE);
		}
		if (pattern.target instanceof VoltageResource) {
			final int phase = isSubPhase(pattern.target);
			return new Type(Type.PRIMARY_VOLTAGE, phase == 0 ? null : Type.SECONDARY_POWER_PHASE + phase);
		}
		if (pattern.target instanceof ElectricCurrentResource) {
			final int phase = isSubPhase(pattern.target);
			return new Type(Type.PRIMARY_CURRENT, phase == 0 ? null : Type.SECONDARY_POWER_PHASE + phase);			
		}
		// TODO further simulations
		return null;
	}
	
	/**
	 * 0 for non-subphase, phase (typically 1..3) otherwise, 99 for unknown phase 
	 * @param r
	 * @return
	 */
	private static int isSubPhase(Resource r) {
		try {
			Resource child = r.getLocationResource();
			Resource parent = child.getParent();
			while (parent != null) {
				if (parent instanceof ResourceList && 
						((ResourceList<?>) parent).getElementType() == ElectricityConnection.class &&
						parent.getName().equals("subPhaseConnections")) {
					if (child.getName().startsWith("subPhaseConnections_")) {
						try {
							return Integer.parseInt(child.getName().substring("subPhaseConnections_".length())) + 1;
						} catch (Exception ignore) {}
					}
					return 99;
				}
				child = parent;
				parent = parent.getParent();
			}
		} catch (SecurityException ignore) {}
		return 0;
	}

	
}
