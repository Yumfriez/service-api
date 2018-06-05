/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.epam.ta.reportportal.ws.converter.converters;

import com.epam.ta.reportportal.store.database.entity.dashboard.Dashboard;
import com.epam.ta.reportportal.store.database.entity.dashboard.DashboardWidget;
import com.epam.ta.reportportal.store.database.entity.dashboard.DashboardWidgetId;
import com.epam.ta.reportportal.store.database.entity.widget.Widget;
import com.epam.ta.reportportal.ws.model.Position;
import com.epam.ta.reportportal.ws.model.Size;
import com.epam.ta.reportportal.ws.model.dashboard.DashboardResource;

import java.util.function.Function;

/**
 * @author Pavel Bortnik
 */
public class WidgetConverter {

	public static final Function<DashboardWidget, DashboardResource.WidgetObjectModel> TO_OBJECT_MODEL = dashboardWidget -> {
		DashboardResource.WidgetObjectModel objectModel = new DashboardResource.WidgetObjectModel();
		objectModel.setWidgetId(dashboardWidget.getId().getWidgetId());
		objectModel.setWidgetPosition(new Position(dashboardWidget.getPositionX(), dashboardWidget.getPositionY()));
		objectModel.setWidgetSize(new Size(dashboardWidget.getWidth(), dashboardWidget.getHeight()));
		return objectModel;
	};

	/**
	 * Creates many-to-many object representation of dashboards and widgets
	 *
	 * @param model       Widget model object
	 * @param dashboardId Dashboard id
	 * @return many-to-many object representation
	 */
	public static DashboardWidget toDashboardWidget(DashboardResource.WidgetObjectModel model, Dashboard dashboard, Widget widget) {

		DashboardWidgetId id = new DashboardWidgetId();
		id.setDashboardId(dashboard.getId());
		id.setWidgetId(model.getWidgetId());

		DashboardWidget dashboardWidget = new DashboardWidget();
		dashboardWidget.setId(id);
		dashboardWidget.setWidgetName(widget.getName());
		dashboardWidget.setPositionX(model.getWidgetPosition().getX());
		dashboardWidget.setPositionY(model.getWidgetPosition().getY());
		dashboardWidget.setWidth(model.getWidgetSize().getWidth());
		dashboardWidget.setHeight(model.getWidgetSize().getHeight());
		dashboardWidget.setDashboard(dashboard);
		dashboardWidget.setWidget(widget);

		return dashboardWidget;
	}

}
