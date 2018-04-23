package org.smartrplace.sim.resource.impl.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.ElectricCurrentResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.LoggingUtils;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.sim.resource.config.ScheduledSimulationConfig;
import org.smartrplace.sim.resource.impl.ConfigPattern;
import org.smartrplace.sim.resource.impl.ResourceSimulationOrchestration;
import org.smartrplace.sim.resource.impl.Type;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.extended.resource.DefaultResourceTemplate;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirmData;
import de.iwes.widgets.html.complextable.DynamicTableData;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.checkbox.Checkbox2;
import de.iwes.widgets.html.form.checkbox.DefaultCheckboxEntry;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.popup.Popup;
import de.iwes.widgets.pattern.widget.table.PatternTable;
import de.iwes.widgets.reswidget.scheduleplot.flot.SchedulePlotFlot;
import de.iwes.widgets.reswidget.scheduleviewer.api.SchedulePresentationData;
import de.iwes.widgets.reswidget.scheduleviewer.DefaultSchedulePresentationData;
import de.iwes.widgets.template.DisplayTemplate;

class PageBuilder {

	private final static String BASE_RESOURCE = "resourceSimulations";
	private final WidgetPage<?> page;
	private final Header header;
	private final Alert alert;
	private final Header simulationsHeader;
	private final PatternTable<ConfigPattern> table;
	private final Popup newConfigPopup;
	private final TemplateDropdown<Class<? extends SingleValueResource>> typeSelector;
	private final Checkbox2 resSelectorCheckbox;
	private final TextField newPathField;
	private final TemplateDropdown<SingleValueResource> targetSelector;
	private final Dropdown primaryTypeSelector;
	private final TemplateDropdown<Type> secondaryTypeSelector;
	private final Button newConfigSubmit;
	private final Button newConfigPopupTrigger;
	private final Popup plotPopup;
	private final SchedulePlotFlot schedulePlot;
	
	private final Header templatesHeader;
	// FIXME the following two should be multiselects
	private final Dropdown templatePrimarySelector;
	private final TemplateDropdown<Type> templateSecondarySelector;
	private final SchedulePlotFlot templatePlot;
	private final Button templatePlotSubmit;
 	
	@SuppressWarnings("serial")
	PageBuilder(final ApplicationManager appMan, final WidgetPage<?> page, final TypeCache typeCache) {
		this.page = page;
		this.header = new Header(page, "header", "Resource simulations");
//		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
		header.setDefaultColor("blue");
		this.alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		this.simulationsHeader = new Header(page, "simulationsHeadeR", "Simulations");
		simulationsHeader.setDefaultHeaderType(2);
		simulationsHeader.setDefaultColor("blue");
		final RowTemplate<ConfigPattern> tableTemplate = new RowTemplate<ConfigPattern>() {
			
			final Map<String, Object> header; 
			
			{
				final Map<String, Object> header = new LinkedHashMap<>(5);
				header.put("target", "Target");
				header.put("type", "Simulation type");
				header.put("path", "Config path");
				header.put("plot", "Plot");
				header.put("delete", "Delete");
				this.header = Collections.unmodifiableMap(header);
			}

			@Override
			public Row addRow(ConfigPattern object, OgemaHttpRequest req) {
				final Row row = new Row();
				final String id = ResourceUtils.getValidResourceName(getLineId(object));
				final Label target = new Label(table, id + "_targetLabel" ,req) {
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						setText(object.target.getLocation(), req);
					}
					
				};
				row.addCell("target", target);
				final Label simType = new  Label(table, id + "_simType" ,req) {
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						final String primary = object.typePrimary.getValue();
						final String secondary = object.typeSecondary.isActive() ? object.typeSecondary.getValue() :
							"default";
						setText(primary + ": " + secondary, req);
					}
					
				};
				row.addCell("type", simType);
				row.addCell("path", object.model.getPath());
				final Button showPlot = new Button(table, id + "_plot", req) {
					
					@Override
					public void onPrePOST(String data, OgemaHttpRequest req) {
						final ReadOnlyTimeSeries timeSeries = LoggingUtils.getHistoricalData(object.target.getLocationResource());
						final Map<String, SchedulePresentationData> schedules  = Collections.singletonMap(object.target.getLocation(), 
									new DefaultSchedulePresentationData(timeSeries, Float.class, object.target.getLocation(), InterpolationMode.LINEAR));
						schedulePlot.getScheduleData(req).setSchedules(schedules);
					}
					
				};
				showPlot.setDefaultText("Show");
				showPlot.addDefaultStyle(ButtonData.BOOTSTRAP_LIGHT_BLUE);
				showPlot.triggerAction(plotPopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET, req);
				showPlot.triggerAction(schedulePlot, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				row.addCell("plot", showPlot);
				final ButtonConfirm deleteButton = new ButtonConfirm(table, id + "_deleteBtn", req) {
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						object.model.delete();
					}
					
				};
				deleteButton.setDefaultText("Delete");
				deleteButton.setDefaultCancelBtnMsg("Cancel");
				deleteButton.setDefaultConfirmBtnMsg("Delete");
				deleteButton.setDefaultConfirmMsg("Do you really want to delete the simulation?");
				deleteButton.setDefaultConfirmPopupTitle("Delete simulation");
				deleteButton.addDefaultStyle(ButtonConfirmData.CANCEL_LIGHT_BLUE);
				deleteButton.addDefaultStyle(ButtonConfirmData.CONFIRM_ORANGE);
				deleteButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);
				deleteButton.triggerAction(table, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				row.addCell("delete", deleteButton);
				return row;
			}

			@Override
			public String getLineId(ConfigPattern object) {
				return object.model.getPath();
			}

			@Override
			public Map<String, Object> getHeader() {
				return header;
			}
			
		};
		this.table = new PatternTable<>(page, "table", false, ConfigPattern.class, tableTemplate, appMan.getResourcePatternAccess());
		table.addDefaultStyle(DynamicTableData.TABLE_STRIPED);
		
		this.newConfigPopup = new Popup(page, "popup", true);
		this.typeSelector = new TemplateDropdown<>(page, "typeSelector");
		typeSelector.setDefaultItems(Arrays.<Class<? extends SingleValueResource>> asList(
				FloatResource.class,
				IntegerResource.class,
				BooleanResource.class,
				PowerResource.class,
				TemperatureResource.class,
				EnergyResource.class,
				ElectricCurrentResource.class,
				VoltageResource.class
		));
		typeSelector.setTemplate(new DisplayTemplate<Class<? extends SingleValueResource>>() {
			
			@Override
			public String getLabel(Class<? extends SingleValueResource> object, OgemaLocale locale) {
				return object.getSimpleName();
			}
			
			@Override
			public String getId(Class<? extends SingleValueResource> object) {
				return object.getName();
			}
		});
		typeSelector.selectDefaultItem(FloatResource.class);
		this.resSelectorCheckbox = new Checkbox2(page, "resSelectorCheckbox");
		resSelectorCheckbox.setDefaultCheckboxList(Arrays.asList(
				new DefaultCheckboxEntry("existing", "Simulate existing resource", true)
		));
		this.newPathField = new TextField(page, "newPathField") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final boolean existing = resSelectorCheckbox.getCheckboxList(req).get(0).isChecked();
				if (existing) {
					disable(req);
					return;
				}
				enable(req);
			}
			
		};
		
		this.targetSelector = new TemplateDropdown<SingleValueResource>(page, "targetSelector") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final boolean existing = resSelectorCheckbox.getCheckboxList(req).get(0).isChecked();
				if (!existing) {
					update(Collections.emptyList(), req);
					disable(req);
					return;
				}
				enable(req);
				final Class<? extends SingleValueResource> type = typeSelector.getSelectedItem(req);
				final List<ConfigPattern> configs = appMan.getResourcePatternAccess().getPatterns(ConfigPattern.class, AccessPriority.PRIO_LOWEST);
				final List<SingleValueResource> resources = appMan.getResourceAccess().getResources(type).stream()
					.filter(resource -> !configs.stream().filter(cfg -> cfg.target.equalsLocation(resource)).findAny().isPresent())
					.collect(Collectors.toList());
				update(resources, req);
			}
			
		};
		targetSelector.setTemplate(new DefaultResourceTemplate<>());
		this.primaryTypeSelector = new PrimaryTypeSelector(page, "primaryTypeSelector", typeCache);
		this.secondaryTypeSelector = new SecondaryTypeSelector(page, "secondaryTypeSelector", (PrimaryTypeSelector) primaryTypeSelector, typeCache);
		this.newConfigSubmit = new Button(page, "newConfigSubmit", "Create new configuration") {
			
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final Type type = secondaryTypeSelector.getSelectedItem(req);
				if (type == null)
					return;
				final boolean existing = resSelectorCheckbox.getCheckboxList(req).get(0).isChecked();
				final SingleValueResource resource;
				if (existing) {
					resource = targetSelector.getSelectedItem(req);
				} else {
					final String path = newPathField.getValue(req).trim();
					if (path.isEmpty()) {
						alert.showAlert("Resource path must not be empty", false, req);
						return;
					}
					final Resource old = appMan.getResourceAccess().getResource(path);
					if (old != null && old.exists()) {
						alert.showAlert("Resource " + path + " already exists" , false, req);
						return;
					}
					final int idx = path.lastIndexOf('/');
					if (idx < 0) {
						resource = appMan.getResourceManagement().createResource(path, typeSelector.getSelectedItem(req));
					} else {
						final String parentPath = path.substring(0, idx);
						final Resource parent = appMan.getResourceAccess().getResource(parentPath);
						if (parent == null) {
							alert.showAlert("Parent resource " + parentPath + " does not exist", false, req);
							return;
						}
						if (!parent.exists()) {
							alert.showAlert("Parent resource " + parentPath + " is virtual", false, req);
							return;
						}
						resource = parent.addDecorator(path.substring(idx+1), typeSelector.getSelectedItem(req));
						if (resource instanceof TemperatureResource)
							((TemperatureResource) resource).setCelsius(15F);
					}
					
				}
				if (resource == null) 
					return;
				@SuppressWarnings("unchecked")
				final ResourceList<ScheduledSimulationConfig> configs = appMan.getResourceManagement().createResource(BASE_RESOURCE, ResourceList.class);
				configs.setElementType(ScheduledSimulationConfig.class);
				if (!configs.isActive())
					configs.activate(false);
				// FIXME not very elegant... the target resource must be active already, although it does not contain a sensible value yet
				resource.activate(false);
				final ScheduledSimulationConfig config = configs.add();
				config.target().setAsReference(resource);
				config.typePrimary().<StringResource> create().setValue(type.primaryType);
				if (type.secondaryType != null)
					config.typeSecondary().<StringResource> create().setValue(type.secondaryType);
				config.activate(true);
				alert.showAlert("New simulation configuration created for resource "+ resource, true, req);
			}
			
		};
		newConfigPopupTrigger = new Button(page, "newConfigPopupTrigger", "Create new simulation");
		newConfigPopupTrigger.addDefaultStyle(ButtonData.BOOTSTRAP_LIGHT_BLUE);
		
		plotPopup = new Popup(page, "plotPopup", true);
		schedulePlot = new SchedulePlotFlot(page, "schedulePlot", false);
		plotPopup.setDefaultTitle("Log data plot");
		plotPopup.setDefaultWidth("80%");
		plotPopup.setBody(schedulePlot, null);
		// FIXME workaround for funny-sized plot widget
		final Button dummy = new Button(page, "plotUpdate", "Update");
		plotPopup.setFooter(dummy, null);
		dummy.triggerAction(schedulePlot, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		this.templatesHeader = new Header(page, "templatesHeader", "Templates");
		templatesHeader.setDefaultHeaderType(2);
		templatesHeader.setDefaultColor("blue");
		this.templatePrimarySelector = new PrimaryTypeSelector(page, "templatePrimarySelecotr", typeCache);
		this.templateSecondarySelector = new SecondaryTypeSelector(page, "templateSecondarySelector", (PrimaryTypeSelector) templatePrimarySelector, typeCache);
		this.templatePlot = new SchedulePlotFlot(page, "templatePlot", false) {
			
			public void onGET(OgemaHttpRequest req) {
				final OgemaWidget trigger = page.getTriggeringWidget(req);
				if (trigger == null)
					return;
				// FIXME multiselect
				final Type type = templateSecondarySelector.getSelectedItem(req);
				if (type == null) {
					getScheduleData(req).setSchedules(Collections.emptyMap());
					return;
				}
				final ReadOnlyTimeSeries timeSeries = ResourceSimulationOrchestration.getTemplateTimeseries(type, typeCache.baseFolder, typeCache.ctx.getBundle());
				if (timeSeries == null) {
					getScheduleData(req).setSchedules(Collections.emptyMap());
					return;
				}
				getScheduleData(req).setSchedules(Collections.singletonMap(type.toString(), 
						new DefaultSchedulePresentationData(timeSeries, Float.class, type.toString(), InterpolationMode.STEPS)));
				
			}
			
		};
		this.templatePlotSubmit = new Button(page, "templatePlotSubmit", "Show templates");
		page.showOverlay(true);
		
		buildPage();
		setDependencies();
	}
	
	private final void buildPage() {
		page.append(header).linebreak().append(alert)
			.append(simulationsHeader).append(table).linebreak().append(newConfigPopupTrigger).linebreak();
		
		int row = 0;
		page.append(templatesHeader)
		 	.append(new StaticTable(3, 3, new int[] {3,3,6})
		 			.setContent(row, 0, "Select primary type").setContent(row++, 1,	templatePrimarySelector)
		 			.setContent(row, 0, "Select secondary type").setContent(row++, 1, templateSecondarySelector)
		 												.setContent(row++, 1, templatePlotSubmit)
		 ).linebreak().append(templatePlot);
		
		final PageSnippet body = new PageSnippet(page, "popupBody", true);
		row = 0;
		body.append(new StaticTable(6, 2)
				.setContent(row, 0, "Select resource type").setContent(row++, 1, typeSelector)
															.setContent(row++, 1, resSelectorCheckbox)
				.setContent(row, 0, "Select target path").setContent(row++, 1, newPathField)
				.setContent(row, 0, "Select target resource").setContent(row++, 1, targetSelector)
				.setContent(row, 0, "Select simulation type (primary)").setContent(row++, 1, primaryTypeSelector)
				.setContent(row, 0, "Select simulation type (secondary)").setContent(row++, 1, secondaryTypeSelector)
			,null);
		newConfigPopup.setBody(body, null);
		newConfigPopup.setFooter(newConfigSubmit, null);
		newConfigPopup.setTitle("Create new simulation", null);
	
		page.linebreak().append(newConfigPopup).linebreak().append(plotPopup);
	}
	
	private final void setDependencies() {
		typeSelector.triggerAction(targetSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		newConfigSubmit.triggerAction(table, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		newConfigSubmit.triggerAction(newConfigPopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		newConfigPopupTrigger.triggerAction(newConfigPopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
		newConfigPopupTrigger.triggerAction(targetSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST); 
		resSelectorCheckbox.triggerAction(newPathField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		resSelectorCheckbox.triggerAction(targetSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		templatePlotSubmit.triggerAction(templatePlot, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
	}
	
	@SuppressWarnings("serial")
	private static class PrimaryTypeSelector extends Dropdown {
		
		private final TypeCache typeCache;
		
		PrimaryTypeSelector(WidgetPage<?> page, String id, TypeCache typeCache) {
			super(page, id);
			this.typeCache = typeCache;
		}
		
		public void onGET(OgemaHttpRequest req) {
			final Map<String,String> map = typeCache.getSupportedTypes().keySet()
				.stream()
				.collect(Collectors.toMap(Function.identity(), Function.identity()));
			update(map, req);
		}
		
	}

	@SuppressWarnings("serial")
	private static class SecondaryTypeSelector extends TemplateDropdown<Type> {
			
		private final PrimaryTypeSelector primarySelector;
		private final TypeCache typeCache;
		
		SecondaryTypeSelector(WidgetPage<?> page, String id, PrimaryTypeSelector primarySelector, TypeCache typeCache) {
			super(page, id);
			this.primarySelector = primarySelector;
			this.typeCache = typeCache;
			primarySelector.triggerAction(this, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			this.setTemplate(new DisplayTemplate<Type>() {

				@Override
				public String getId(Type object) {
					return object.toString();
				}

				@Override
				public String getLabel(Type object, OgemaLocale locale) {
					final String secondary = object.secondaryType;
					if (secondary == null)
						return "default";
					return secondary;
				}
			});
		}

		public void onGET(OgemaHttpRequest req) {
			final String primary = primarySelector.getSelectedValue(req);
			if (primary == null || !typeCache.getSupportedTypes().containsKey(primary)) {
				update(Collections.emptyList(), req);
				return;
			}
			update(typeCache.getSupportedTypes().get(primary), req);
		}
			
		
	}
	
}
