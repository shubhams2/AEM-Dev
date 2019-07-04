/* @author Shubham Garg*/

package com.adobe.migeration.core.servlets;


import com.google.common.collect.Lists;

import javax.jcr.ItemExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.apache.sling.api.resource.ModifiableValueMap;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import com.adobe.granite.security.user.UserManagementService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager; 
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.JackrabbitSession ;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import java.security.Principal;
import org.apache.commons.lang3.ArrayUtils;
import com.day.cq.replication.Replicator;

@Component(
		immediate = true,
		service = Servlet.class,
		property = {"sling.servlet.paths=/bin/update/common/groups"},
		configurationPid = "com.adobe.migeration.core.servlets.UpdateCommonGroupsServlet"
		)
public class UpdateCommonGroupsServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 6672558841128128132L;
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCommonGroupsServlet.class); 

	@Reference
	UserManagementService aUserManagementService;

	PrintWriter pw = null;
	Session session = null;

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

		LOGGER.info("In servlet");

		response.setContentType("text/html");
		pw = response.getWriter();
		pw.write("In servlet </br>");
		ResourceResolver resourceResolver = request.getResourceResolver();		
		session = resourceResolver.adaptTo(Session.class);	
		String filePath = request.getParameter("filepath");		
		if(StringUtils.isBlank(filePath)){
			pw.write("<b style=\"color:red;\">" + "Please provide valid Group excel file path against the request param 'filepath'. For eg 'C://Shubham/AEM_Content/CR/Permission-branch.xlsx'" + "</b>");
			return;
		}

		try {            
			Workbook workbook = WorkbookFactory.create(new File(filePath));
			Sheet sheet = workbook.getSheetAt(0);
			String groupName = "";
			String contentPathList = "";
			Map<String, String> map = new HashMap<String, String>();
			Row row = null;
			Iterator<Cell> cellIterator = null;
			Cell cell = null;
			String cellValue = null;


			// Create a DataFormatter to format and get each cell's value as String
			DataFormatter dataFormatter = new DataFormatter();
			Iterator<Row> rowIterator = sheet.rowIterator();
			while (rowIterator.hasNext()) {
				row = rowIterator.next();
				cellIterator = row.cellIterator();

				while (cellIterator.hasNext()) {
					cell = cellIterator.next();
					cellValue = dataFormatter.formatCellValue(cell);
					if(!StringUtils.isBlank(cellValue) && !cellValue.contains("Group Name") && !cellValue.contains("Branch Name/Edit")) {
						if (!cellValue.contains("/content")) {                             
							groupName = cellValue;
						} else if (cellValue.contains("/content")) {
							contentPathList = cellValue.replace("/content/adobe","/content/dam/adobe/confidential");
							contentPathList = contentPathList+","+cellValue.replace("/content/adobe","/content/dam/adobe/internal");
							contentPathList = contentPathList+","+cellValue;                            
						}
					}                   
				}

				if(!StringUtils.isBlank(groupName) && !StringUtils.isBlank(contentPathList)) {
					map.put(groupName, contentPathList);
					groupName = "";
					contentPathList = "";
				} else {
					LOGGER.error("Data format is not correct in excel sheet.");
					pw.write("Data format is not correct in excel sheet row index == "+ cell.getRowIndex() + " </br>");                
				}

			}

			try {
			Node node = null;
			Resource res = null;
			for (Map.Entry<String, String> entry : map.entrySet()) {
				createGroupInCRX(session, entry.getKey(), entry.getValue());
			}
			}catch (RepositoryException re) {
				LOGGER.error("Repository Exception In CreateGroupFromExcelServlet" + re);
				pw.write("<b style=\"color:red;\">" + "Repository Exception In CreateGroupFromExcelServlet" + re + " </br>");
			}
			


		} catch (Exception e) {
			LOGGER.error("Exception In CreateGroupFromExcelServlet" + e);
			pw.write("<b style=\"color:red;\">" + "Exception In CreateGroupFromExcelServlet" + e + " </br>");
		}

		pw.close();

	}

	/**
	 * @param session
	 * @param groupName
	 * @param paths
	 * @throws Exception
	 */
	private void createGroupInCRX(Session session, String groupName, String paths) throws Exception {

		try {
			// Create UserManager Object
			final UserManager userManager = AccessControlUtil.getUserManager(session);
			//"adobe_common_author"
			Authorizable commonGroupAuth = userManager.getAuthorizable("adobe_common_author");
			Authorizable workflowCommonGroupAuth = userManager.getAuthorizable("workflow-users");
			
			Group commonGroup = (Group)commonGroupAuth;
			Group workflowCommonGroup = (Group)workflowCommonGroupAuth;

			// Create a Group
			Group group = null;
			Boolean flag = false ;
			if (userManager.getAuthorizable(groupName) == null && !(null == commonGroup) && !(null == workflowCommonGroup)) {
				group = userManager.createGroup(groupName.toLowerCase());
				ValueFactory valueFactory = session.getValueFactory();
				Value groupNameValue = valueFactory.createValue(groupName, PropertyType.STRING);
				group.setProperty("./profile/givenName", groupNameValue);

				//Add common group "adobe_common_author" to all new groups				
				commonGroup.addMembers(groupNameValue.toString());				
				//flag = commonGroup.addMember(userManager.getAuthorizable(groupName.toLowerCase()));
				//Add common workflow group "workflow-users" to all new groups
				workflowCommonGroup.addMembers(groupNameValue.toString());
				//flag = workflowCommonGroup.addMember(userManager.getAuthorizable(groupName.toLowerCase()));
				
				/*if(!StringUtils.isBlank(paths)) {
					setCreateEditReplicateAcl(groupName, paths, aUserManagementService, session);
				}*/

				session.save();
				LOGGER.info("---> {} Group successfully created.", group.getID());
				pw.write("---> {} Group successfully created --" + group.getID()+ " </br>");
				if(group.isMember(commonGroup) && group.isMember(workflowCommonGroup)) {
					LOGGER.info("---> {} Group created is a member of common groups ----> adobe_common_author and workflow-users");
					pw.write("---> {} Group created is a member of common groups ----> adobe_common_author and workflow-users" + " </br>");
				}
			} else {
				LOGGER.info("=====> Group already exist.. " +groupName);
				pw.write("=====> Group already exist.." + groupName + " </br>");

				//Add common group "adobe_common_author" to all new groups
				Group existingGroup  = (Group)userManager.getAuthorizable(groupName);
				if(!existingGroup.isMember(commonGroup)) {
					commonGroup.addMembers(groupName.toString());
					session.save();
					pw.write("===> Common Group added to group >> " + groupName + " </br>");
				}
				if(!existingGroup.isMember(workflowCommonGroup)) {
					workflowCommonGroup.addMembers(groupName.toString());
					session.save();
					pw.write("===> Workflow Common Group added to group >> " + groupName + " </br>");
				}
			}
		}catch (RepositoryException ee) {
			LOGGER.error("---> RepositoryException in createGroupInCRX while creating group <<<<" +groupName +">>>>" + ee);
			pw.write("<b style=\"color:red;\">" +"---> RepositoryException in createGroupInCRX while creating group <<<<" +groupName +">>>> </br>");
		}
	}

	/**
	 * @param session
	 * @param aGroupPrincipal
	 * @param aPath
	 * @param aUserManagementService
	 */
	public  void setCreateEditReplicateAcl(final String aGroupPrincipal, String aPath,
			final UserManagementService aUserManagementService, final Session aSession) {
		try {
			UserManager userManager = aUserManagementService.getUserManager(aSession);
			AccessControlManager accessControlManager = aSession.getAccessControlManager();
			Authorizable group = userManager.getAuthorizable(aGroupPrincipal);

			Privilege[] privileges = { accessControlManager.privilegeFromName(Privilege.JCR_READ),
					accessControlManager.privilegeFromName(Privilege.JCR_VERSION_MANAGEMENT),
					accessControlManager.privilegeFromName(Privilege.JCR_LOCK_MANAGEMENT),
					accessControlManager.privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES),            		
					accessControlManager.privilegeFromName(Privilege.JCR_ADD_CHILD_NODES),                    
					accessControlManager.privilegeFromName(Privilege.JCR_NODE_TYPE_MANAGEMENT),                    
					accessControlManager.privilegeFromName(Privilege.JCR_REMOVE_CHILD_NODES),
					accessControlManager.privilegeFromName(Privilege.JCR_REMOVE_NODE ),
					accessControlManager.privilegeFromName(Replicator.REPLICATE_PRIVILEGE)
			};
			AccessControlList aclList;
			String[] paths = ArrayUtils.EMPTY_STRING_ARRAY;
			if (aPath.contains(",")) {
				paths = aPath.split(",");
			}
			if (!ArrayUtils.isEmpty(paths)) {
				for (String branchPath : paths) {
					//validate branchPath for /content and /content/dam
					if(session.nodeExists(branchPath)) {


						setPrivilege(branchPath, accessControlManager, group, privileges);                        
					}else {
						//LOGGER.info("-------------> Content Path does not exist (path contains hyphen) == "+ branchPath +" -to assign privileges on the group - " + group);
						//pw.write("#####  Content Path does not exist (path contains hyphen) == "+ branchPath +" -to assign privileges on the group - " + group+" </br>");
						branchPath = branchPath.replaceAll("-", "_");
						if(session.nodeExists(branchPath)) {
							setPrivilege(branchPath, accessControlManager, group, privileges);                             
						} else {
							LOGGER.info("-------------> Content Path does not exist (with hyphen/underscore) == "+ branchPath +" -to assign privileges on the group - " + group);
							pw.write("#####  Content Path does not exist == (path contains underscore) "+ branchPath +" -to assign privileges on the group - " + group+" </br>");
						}
					}
					aSession.save();
				}
			} else {
				setPrivilege(aPath, accessControlManager, group, privileges);
				aSession.save();
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param aPath
	 * @param accessControlManager
	 * @param group
	 * @param privileges
	 * @throws PathNotFoundException
	 * @throws AccessDeniedException
	 * @throws RepositoryException
	 * @throws AccessControlException
	 * @throws LockException
	 * @throws VersionException
	 */
	private void setPrivilege(String aPath, AccessControlManager accessControlManager, Authorizable group,
			Privilege[] privileges) throws PathNotFoundException, AccessDeniedException, RepositoryException,
	AccessControlException, LockException, VersionException {
		JackrabbitAccessControlList acl = null;
		// try if there is an acl that has been set before
		for (AccessControlPolicy policy : accessControlManager.getPolicies(aPath)) {
			if (policy instanceof JackrabbitAccessControlList) {
				acl = (JackrabbitAccessControlList) policy;
				break;
			}
		}
		if (acl == null) {
			// try if there is an applicable policy
			AccessControlPolicyIterator itr = accessControlManager.getApplicablePolicies(aPath);
			while (itr.hasNext()) {
				AccessControlPolicy policy = itr.nextAccessControlPolicy();
				if (policy instanceof JackrabbitAccessControlList) {
					acl = (JackrabbitAccessControlList) policy;
					break;
				}
			}
		}

		if (acl != null) {
			/*JackrabbitSession jackrabbit= (JackrabbitSession) session;

    	    PrincipalManager principalManager = jackrabbit.getPrincipalManager();
    	    Principal principal = principalManager.getPrincipal("jackrabbit");
    	    Privilege[] privileges2 = AccessControlUtils.privilegesFromNames(accessControlManager, Privilege.JCR_ALL);*/
			acl.addEntry(group.getPrincipal(), privileges, true);
			accessControlManager.setPolicy(acl.getPath(), acl);
			session.save();
		}

	}


}
