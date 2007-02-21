/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.tool.roster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.app.roster.Participant;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.coursemanagement.api.EnrollmentSet;
import org.sakaiproject.jsf.util.LocaleUtil;
import org.sakaiproject.section.api.SectionAwareness;
import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.user.api.User;

public class FilteredProfileListingBean extends InitializableBean implements Serializable {
	private static final String STATUS_STUDENTS = "roster_status_students";
	private static final Log log = LogFactory.getLog(FilteredProfileListingBean.class);
	private static final long serialVersionUID = 1L;

	protected ServicesBean services;
	public void setServices(ServicesBean services) {
		this.services = services;
	}

	protected RosterPreferences prefs;
	public void setPrefs(RosterPreferences prefs) {
		this.prefs = prefs;
	}

	protected String statusFilter;
	protected String searchFilter = getDefaultSearchText();
	protected String sectionFilter;

	// UI Actions
	
	public void search(ActionEvent ae) {
		// Nothing needs to be done to search
	}
	
	public void clearSearch(ActionEvent ae) {
		searchFilter = getDefaultSearchText();
	}

	public List<Participant> getParticipants() {
		List<Participant> participants = services.rosterManager.getRoster();
		String defaultText = getDefaultSearchText();
		Set<String> studentRoles = getStudentRoles();
		for(Iterator<Participant> iter = participants.iterator(); iter.hasNext();) {
			Participant participant = iter.next();

			// Remove this participant if they don't  pass the status filter
			if(STATUS_STUDENTS.equals(statusFilter)) {
				// We are filtering on the collection of all student roles
				if( ! studentRoles.contains(participant.getRoleTitle())) {
					iter.remove();
					continue;
				}
			} else if(statusFilter != null  && ! statusFilter.equals(participant.getEnrollmentStatus())) {
				// We are filtering on a particular enrollment status
				iter.remove();
				continue;
			}
			
			// Remove this participant if they don't  pass the search filter
			if(searchFilter != null && ! searchFilter.equals(defaultText) && ! searchMatches(searchFilter, participant.getUser())) {
				iter.remove();
				continue;
			}
			
			// Remove this participant if they don't  pass the section filter
			if(sectionFilter != null && ! sectionMatches(sectionFilter, participant.getSectionsMap())) {
				iter.remove();
				continue;
			}			
		}
		return participants;
	}
	
	protected Set<String> getStudentRoles() {
		AuthzGroup azg = null;
		try {
			azg = services.authzService.getAuthzGroup(getSiteReference());
		} catch (GroupNotDefinedException gnde) {
			log.error("Unable to find site " + getSiteReference());
			return new HashSet<String>();
		}
		Set<String> roles = azg.getRolesIsAllowed(SectionAwareness.STUDENT_MARKER);
		if(log.isDebugEnabled()) log.debug("Student Roles = " + roles);
		return roles;
	}
	
	protected boolean searchMatches(String search, User user) {
		return user.getDisplayName().toLowerCase().startsWith(search.toLowerCase()) ||
				   user.getSortName().toLowerCase().startsWith(search.toLowerCase()) ||
				   user.getDisplayId().toLowerCase().startsWith(search.toLowerCase()) ||
				   user.getEmail().toLowerCase().startsWith(search.toLowerCase());
	}

	protected boolean sectionMatches(String sectionUuid, Map<String, CourseSection> sectionsMap) {
		for(Iterator<Entry<String, CourseSection>> iter = sectionsMap.entrySet().iterator(); iter.hasNext();) {
			Entry<String, CourseSection> entry = iter.next();
			CourseSection section = entry.getValue();
			if(section.getUuid().equals(sectionUuid)) return true;
		}
		return false;
	}
	
	public List<SelectItem> getSectionSelectItems() {
		List<SelectItem> list = new ArrayList<SelectItem>();
		// Get the available sections
		List<CourseSection> sections = services.rosterManager.getViewableSectionsForCurrentUser();
		for(Iterator<CourseSection> iter = sections.iterator(); iter.hasNext();) {
			CourseSection section = iter.next();
			list.add(new SelectItem(section.getUuid(), section.getTitle()));
		}
		return list;
	}
	
	public List<SelectItem> getStatusSelectItems() {
		List<SelectItem> list = new ArrayList<SelectItem>();
		Map<String, String> map = services.cmService.getEnrollmentStatusDescriptions(LocaleUtil.getLocale(FacesContext.getCurrentInstance()));
		
		// The UI doesn't care about status IDs... just labels
		List<String> statusLabels = new ArrayList<String>();
		for(Iterator<Entry<String, String>> iter = map.entrySet().iterator(); iter.hasNext();) {
			Entry<String, String> entry = iter.next();
			statusLabels.add(entry.getValue());
		}
		Collections.sort(statusLabels);
		for(Iterator<String> iter = statusLabels.iterator(); iter.hasNext();) {
			String statusLabel = iter.next();
			SelectItem item = new SelectItem(statusLabel, statusLabel);
			list.add(item);
		}
		return list;
	}

	/**
	 * Gets a Map of roles to the number of users in the site with those roles.
	 * @return
	 */
	public Map<String, Integer> getRoleCounts() {
		Map<String, Integer> map = new TreeMap<String, Integer>();
		for(Iterator<Participant> iter = getParticipants().iterator(); iter.hasNext();) {
			Participant participant = iter.next();
			String key = participant.getRoleTitle();
			if(map.containsKey(key)) {
				Integer prevCount = map.get(key);
				map.put(key, ++prevCount);
			} else {
				map.put(key, 1);
			}
		}
		return map;
	}

	/**
	 * We display enrollment details when there is a single EnrollmentSet associated
	 * with a site, or when multiple EnrollmentSets are children of cross listed
	 * CourseOfferings.
	 * 
	 * @return
	 */
	public boolean isDisplayEnrollmentDetails() {
		Set<EnrollmentSet> officialEnrollmentSets = services.rosterManager.getOfficialEnrollmentSetsInSite();
		int count = officialEnrollmentSets.size();
		if(count == 0) return false;
		if(count == 1) return true;

		// TODO Deal with cross listings.  Multiple cross listed courses should still show enrollment details
		return false;
	}
	
	public boolean isDisplaySectionsFilter() {
		return services.rosterManager.getViewableSectionsForCurrentUser().size() >1;
	}

	public String getSearchFilter() {
		return searchFilter;
	}

	public void setSearchFilter(String searchFilter) {
		String trimmedArg = StringUtils.trimToNull(searchFilter);
		String defaultText = getDefaultSearchText();
		if(trimmedArg == null) {
			this.searchFilter = defaultText;
		} else {
			this.searchFilter = trimmedArg;
		}
	}

	public String getSectionFilter() {
		return sectionFilter;
	}

	public void setSectionFilter(String sectionFilter) {
		this.sectionFilter = StringUtils.trimToNull(sectionFilter);
	}

	public String getStatusFilter() {
		return statusFilter;
	}

	public void setStatusFilter(String statusFilter) {
		// Don't allow this value to be set to the separater line
		if(LocaleUtil.getLocalizedString(FacesContext.getCurrentInstance(),
				InitializableBean.MESSAGE_BUNDLE, "roster_status_sep_line")
				.equals(statusFilter)) {
			this.statusFilter = null;
		} else {
			this.statusFilter = StringUtils.trimToNull(statusFilter);
		}
	}

	public String getDefaultSearchText() {
		return LocaleUtil.getLocalizedString(FacesContext.getCurrentInstance(),
				InitializableBean.MESSAGE_BUNDLE, "roster_search_text");
	}
}
