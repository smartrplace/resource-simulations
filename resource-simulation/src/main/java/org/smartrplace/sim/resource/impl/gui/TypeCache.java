package org.smartrplace.sim.resource.impl.gui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.smartrplace.sim.resource.impl.ResourceSimulationOrchestration;
import org.smartrplace.sim.resource.impl.Type;

class TypeCache {
	
	final BundleContext ctx;
	final String baseFolder;
	
	/*
	 * Access via #getSupportedTypes
	 */
	private volatile SoftReference<Map<String, List<Type>>> supportedTypes = new SoftReference<Map<String, List<Type>>>(null);

	TypeCache(BundleContext ctx, String baseFolder) {
		this.ctx = ctx;
		this.baseFolder = baseFolder;
	}
	
	Map<String, List<Type>> getSupportedTypes() {
		Map<String, List<Type>> types = supportedTypes.get();
		if (types != null)
			return types;
		synchronized (this) {
			types = supportedTypes.get();
			if (types != null)
				return types;
			types = getTypesInternal();
			supportedTypes = new SoftReference<Map<String, List<Type>>>(types);
		}
		return types;
	}
	
	private Map<String, List<Type>> getTypesInternal() {
		final Map<String, List<Type>> types = new HashMap<>();
		try {
			final Path path = Paths.get(baseFolder);
			if (Files.isDirectory(path)) {
				final List<Stream<Path>> streams = new ArrayList<>(); 
 				try (final Stream<Path> stream = Files.list(path)) {
					stream
						.filter(folder -> Files.isDirectory(folder))
						.flatMap(folder -> {
							 // Files.list needs to be closed and throws IO... super-annoying
							try {
								final Stream<Path> substream = Files.list(folder);
								streams.add(substream);
								return substream;
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							}
						 })
						.filter(file -> Files.isRegularFile(file))
						.filter(file -> file.getFileName().toString().toLowerCase().endsWith(".csv"))
						.map(file -> getType(file))
						.filter(type -> type != null)
						.forEach(type -> {
							if (!types.containsKey(type.primaryType))
								types.put(type.primaryType, new ArrayList<>(4));
							types.get(type.primaryType).add(type);
						});
				} finally {
					for (Stream<Path> substream : streams) {
						try {
							substream.close();
						} catch (Exception ignore) {}
					}
				}
			}
		} catch (SecurityException | UncheckedIOException | IOException e) {
			LoggerFactory.getLogger(ResourceSimulationPage.class)
				.error("Failed to load simulation templates from rundir folder",e);
		}
		
		try {
			final Enumeration<URL> urls = ctx.getBundle().findEntries(ResourceSimulationOrchestration.DEFAULT_BASE_FOLDER, "*", false);
			if (urls != null) {
				while (urls.hasMoreElements()) {
					final URL url = urls.nextElement();
					final String path = url.getPath();
					if (!path.endsWith("/"))
						continue;
					final String[] arr = path.substring(0, path.length()-1).split("/");
					final String primaryType = arr[arr.length-1];
					final Enumeration<URL> suburls = ctx.getBundle().findEntries(path, "*.csv", false);
					if (suburls == null)
						continue;
					while (suburls.hasMoreElements()) {
						final String[] subpath = suburls.nextElement().getPath().split("/");
						final String secondaryType = subpath[subpath.length-1];
						if (!secondaryType.toLowerCase().endsWith(".csv"))
							continue;
						final Type type = getType(primaryType, secondaryType);
						if (!types.containsKey(type.primaryType))
							types.put(type.primaryType, new ArrayList<>(4));
						types.get(type.primaryType).add(type);
					}
				}
			}
		} catch (SecurityException | IllegalStateException e) {
			LoggerFactory.getLogger(ResourceSimulationPage.class)
			.error("Failed to load simulation templates from bundle",e);
		}
		return types;
	}
	
	private static Type getType(final Path path) {
		final Path parent = path.getParent();
		if (parent == null)
			return null;
		return getType(parent.getFileName().toString(), path.getFileName().toString());
	}
	
	private static Type getType(final String primary, final String secondary) {
		final String secondaryType = (secondary.toLowerCase().equals("default.csv")) ? null :
			secondary.substring(0, secondary.length() - ".csv".length());
		return new Type(primary, secondaryType);	
	}
	
	
}
