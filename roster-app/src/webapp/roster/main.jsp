<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://sakaiproject.org/jsf/sakai" prefix="sakai"%>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%
response.setContentType("text/html; charset=UTF-8");
%>
<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
   <jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.tool.roster.bundle.Messages"/>
</jsp:useBean>
<f:view>

<sakai:view title="#{msgs.facet_roster_list}" toolCssHref="/sakai-roster-tool/css/roster.css">
	<%="<script src=\"js/roster.js\"></script>"%>
		<h:form id="roster_form">
			<t:aliasBean alias="#{viewBean}" value="#{overview}">
				<%@include file="inc/nav.jspf" %>
			</t:aliasBean>

			<h:outputText value="#{msgs.title_msg}"
				rendered="#{overview.renderModifyMembersInstructions}" styleClass="instruction"
				style="display: block;" />

            <h:outputText value="#{msgs.title_msg_ta}"
                          rendered="#{overview.renderModifyMembersInstructions}" styleClass="instruction"
                          style="display: block;" />

            <h:outputLink rendered="#{overview.renderPrivacyMessage}"
                          value="#{msgs.title_missing_participants_link}" target="_blank">
                <h:outputText value="#{msgs.title_missing_participants}" />
            </h:outputLink>

            <%@include file="inc/filter.jspf" %>
			
            <t:dataTable cellpadding="0" cellspacing="0"
                         id="rosterTable"
                         value="#{overview.participants}"
                         var="participant"
                         sortColumn="#{prefs.sortColumn}"
                         sortAscending="#{prefs.sortAscending}"
                         styleClass="listHier rosterTable">
                <h:column>
                    <f:facet name="header">
                        <t:commandSortHeader columnName="sortName" immediate="true" arrow="true">
                            <h:outputText value="#{msgs.facet_name}" />
                        </t:commandSortHeader>
                    </f:facet>
                    <h:commandLink action="#{profileBean.displayProfile}" value="#{participant.user.sortName}" title="#{msgs.show_profile}" rendered="#{overview.renderProfileLinks}">
                        <f:param name="participantId" value="#{participant.user.id}" />
                        <f:param name="returnPage" value="overview" />
                    </h:commandLink>
                    <h:outputText value="#{participant.user.sortName}" rendered="#{ ! overview.renderProfileLinks}" />
                </h:column>
                <h:column>
                    <f:facet name="header">
                        <t:commandSortHeader columnName="displayId" immediate="true" arrow="true">
                            <h:outputText value="#{msgs.facet_userId}" />
                        </t:commandSortHeader>
                    </f:facet>
                    <h:outputText value="#{participant.user.displayId}"/>
                </h:column>
                <h:column>
                    <f:facet name="header">
                        <t:commandSortHeader columnName="email" immediate="true" arrow="true">
                            <h:outputText value="#{msgs.facet_email}" />
                        </t:commandSortHeader>
                    </f:facet>
                    <h:outputLink value="mailto:#{participant.user.email}"><h:outputText value="#{participant.user.email}"/></h:outputLink>
                </h:column>
                <h:column>
                    <f:facet name="header">
                        <t:commandSortHeader columnName="role" immediate="true" arrow="true">
                            <h:outputText value="#{msgs.facet_role}" />
                        </t:commandSortHeader>
                    </f:facet>
                    <h:outputText value="#{participant.roleTitle}"/>
                </h:column>
		        
            </t:dataTable>

      <t:div styleClass="instruction">
                <h:outputFormat value="#{msgs.no_participants_msg}" rendered="#{empty filter.participants && filter.searchFilterString eq filter.defaultSearchText}" >
                     <f:param value="#{filter.sectionFilterTitle}"/>
                </h:outputFormat>
                <h:outputFormat value="#{msgs.no_participants_in_section}" rendered="#{empty filter.participants &&  filter.searchFilterString != filter.defaultSearchText && filter.sectionFilterTitle!=null}" >
                    <f:param value="#{filter.searchFilterString}"/>
                    <f:param value="#{filter.sectionFilterTitle}"/>
                </h:outputFormat>
                   <h:outputFormat value="#{msgs.no_participants_in_anysections}" rendered="#{empty filter.participants &&  filter.searchFilterString != filter.defaultSearchText && filter.sectionFilterTitle == null}" >
                    <f:param value="#{filter.searchFilterString}"/>                    
                </h:outputFormat>
            </t:div>
        </h:form>
</sakai:view>

</f:view>
