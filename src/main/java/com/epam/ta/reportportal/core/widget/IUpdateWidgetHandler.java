/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/epam/ReportPortal
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
 
package com.epam.ta.reportportal.core.widget;

import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.widget.WidgetRQ;

/**
 * Update Widget handler
 * 
 * @author Aliaksei_Makayed
 * 
 */
public interface IUpdateWidgetHandler {

	/**
	 * Update widget with specified id
	 * 
	 * @param updateRQ
	 * @param userName
	 * @param projectName
	 * @return OperationCompletionRS
	 * @throws ReportPortalException
	 */
	OperationCompletionRS updateWidget(String widgetId, WidgetRQ updateRQ, String userName, String projectName);

}