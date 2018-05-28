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
package org.smartrplace.sim.resource.impl.gui;

import java.util.Map;
import org.ogema.core.application.ApplicationManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.smartrplace.sim.resource.impl.ResourceSimulationOrchestration;
import de.iwes.widgets.api.widgets.LazyWidgetPage;
import de.iwes.widgets.api.widgets.WidgetPage;

@Component(
		service=LazyWidgetPage.class,
		property= {
				LazyWidgetPage.BASE_URL + "=/org/smartrplace/sim/resourcesim",
				LazyWidgetPage.RELATIVE_URL + "=index.html",
				LazyWidgetPage.START_PAGE + "=true"
		}
)
public class ResourceSimulationPage implements LazyWidgetPage {

	private volatile TypeCache typeCache;
	
	@Activate
	protected void activate(BundleContext ctx, Map<String, ?> properties) {
		// FIXME copied form app start class
		String baseFolder = null;
		if (properties.containsKey("basefolder")) {
			baseFolder = (String) properties.get("basefolder"); 
		} else {
			baseFolder = ctx.getProperty(ResourceSimulationOrchestration.BASE_FOLDER_PROPERTY);
		}
		if (baseFolder == null)
			baseFolder = ResourceSimulationOrchestration.DEFAULT_BASE_FOLDER;
		baseFolder = baseFolder.replace('\\', '/');
		if (baseFolder.endsWith("/"))
			baseFolder = baseFolder.substring(0, baseFolder.length()-1);
		this.typeCache = new TypeCache(ctx, baseFolder);
	}
	
	@Deactivate
	protected void deactivate() {
		this.typeCache = null;
	}

	@Override
	public void init(final ApplicationManager appMan, final WidgetPage<?> page) {
		new PageBuilder(appMan, page, typeCache);
	}

}
