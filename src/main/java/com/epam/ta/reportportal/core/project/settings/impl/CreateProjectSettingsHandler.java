/*
 * Copyright 2018 EPAM Systems
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

package com.epam.ta.reportportal.core.project.settings.impl;

import com.epam.ta.reportportal.auth.ReportPortalUser;
import com.epam.ta.reportportal.core.events.MessageBus;
import com.epam.ta.reportportal.core.project.settings.ICreateProjectSettingsHandler;
import com.epam.ta.reportportal.dao.IssueGroupRepository;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.WidgetRepository;
import com.epam.ta.reportportal.entity.enums.TestItemIssueGroup;
import com.epam.ta.reportportal.entity.item.issue.IssueType;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.ValidationConstraints;
import com.epam.ta.reportportal.ws.model.project.config.CreateIssueSubTypeRQ;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.epam.ta.reportportal.commons.Predicates.equalTo;
import static com.epam.ta.reportportal.commons.validation.BusinessRule.expect;
import static com.epam.ta.reportportal.entity.enums.TestItemIssueGroup.*;
import static com.epam.ta.reportportal.ws.model.ErrorType.BAD_REQUEST_ERROR;
import static com.epam.ta.reportportal.ws.model.ErrorType.PROJECT_NOT_FOUND;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Service
@Transactional
public class CreateProjectSettingsHandler implements ICreateProjectSettingsHandler {

	private static final Map<String, String> PREFIX = ImmutableMap.<String, String>builder().put(AUTOMATION_BUG.getValue(), "ab_")
			.put(PRODUCT_BUG.getValue(), "pb_")
			.put(SYSTEM_ISSUE.getValue(), "si_")
			.put(NO_DEFECT.getValue(), "nd_")
			.put(TO_INVESTIGATE.getValue(), "ti_")
			.build();

	private ProjectRepository projectRepository;

	private WidgetRepository widgetRepository;

	private IssueGroupRepository issueGroupRepository;

	private MessageBus messageBus;

	@Autowired
	public CreateProjectSettingsHandler(ProjectRepository projectRepository, WidgetRepository widgetRepository,
			IssueGroupRepository issueGroupRepository, MessageBus messageBus) {
		this.projectRepository = projectRepository;
		this.widgetRepository = widgetRepository;
		this.issueGroupRepository = issueGroupRepository;
		this.messageBus = messageBus;
	}

	@Override
	public EntryCreatedRS createProjectIssueSubType(ReportPortalUser.ProjectDetails projectDetails, ReportPortalUser user,
			CreateIssueSubTypeRQ rq) {
		Project project = projectRepository.findById(projectDetails.getProjectId())
				.orElseThrow(() -> new ReportPortalException(PROJECT_NOT_FOUND, projectDetails.getProjectId()));

		expect(TO_INVESTIGATE.getValue().equalsIgnoreCase(rq.getTypeRef()), equalTo(false)).verify(BAD_REQUEST_ERROR,
				"Impossible to create sub-type for 'To Investigate' type."
		);
		expect(NOT_ISSUE_FLAG.getValue().equalsIgnoreCase(rq.getTypeRef()), equalTo(false)).verify(BAD_REQUEST_ERROR,
				"Impossible to create sub-type for 'Not Issue' type."
		);

		/* Check if global issue type reference is valid */
		TestItemIssueGroup expectedGroup = TestItemIssueGroup.fromValue(rq.getTypeRef())
				.orElseThrow(() -> new ReportPortalException(BAD_REQUEST_ERROR, rq.getTypeRef()));

		List<IssueType> existingSubTypes = project.getIssueTypes()
				.stream().filter(issueType -> issueType.getIssueGroup().equals(expectedGroup))
				.collect(Collectors.toList());

		expect(existingSubTypes.size() < ValidationConstraints.MAX_ISSUE_SUBTYPES, equalTo(true)).verify(BAD_REQUEST_ERROR,
				"Sub Issues count is bound of size limit"
		);

		IssueType subType = new IssueType();
		String locator = PREFIX.get(expectedGroup.getValue()) + shortUUID();
		subType.setLocator(locator);
		subType.setIssueGroup(issueGroupRepository.findByTestItemIssueGroup(expectedGroup));
		subType.setLongName(rq.getLongName());
		subType.setShortName(rq.getShortName().toUpperCase());
		subType.setHexColor(rq.getColor());
		subType.setProjects(Lists.newArrayList(project));

		project.getIssueTypes().add(subType);

		projectRepository.save(project);
		/*widgetRepository.findAllByProjectId(project.getId())
				.stream()
				.filter(widget -> widget.getWidgetType().equals(LAUNCHES_TABLE.getType()))
				.filter(widget -> widget.getContentFields()
						.stream()
						.anyMatch(s -> s.contains(subType.getIssueGroup().getTestItemIssueGroup().getValue().toLowerCase())))
				.forEach(widget -> {
					widget.getContentFields()
							.add("statistics$defects$" + subType.getIssueGroup().getTestItemIssueGroup().getValue().toLowerCase() + "$"
									+ subType.getLocator());
					widgetRepository.save(widget);
				});*/

		//messageBus.publishActivity(new DefectTypeCreatedEvent(subType, project.getId(), user.getUserId()));
		subType.getId();
		return new EntryCreatedRS(project.getIssueTypes()
				.stream()
				.filter(issueType -> issueType.getLocator().equalsIgnoreCase(locator))
				.findFirst()
				.get()
				.getId());
	}

	private static String shortUUID() {
		long l = ByteBuffer.wrap(UUID.randomUUID().toString().getBytes(Charsets.UTF_8)).getLong();
		return Long.toString(l, Character.MAX_RADIX);
	}
}
