/* @author Shubham Garg*/

package com.adobe.migeration.core.servlets;


import com.google.common.collect.Lists;
import com.adobe.migeration.core.service.impl.QueryService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ValueMap;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import com.adobe.migeration.core.utils.MigrationUtil;

@Component(
        immediate = true,
        service = Servlet.class,
        property = {"sling.servlet.paths=/bin/update/jcr/property"},
        configurationPid = "com.adobe.migeration.core.servlets.UpdateJcrProperty"
)
public class UpdateJcrProperty extends SlingSafeMethodsServlet {

    @Reference
    QueryService queryService;

    Predicate<Resource> chkRes =  resource -> !resource.getPath().contains("/content/adobe/en-US");

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter pw = response.getWriter();
        Session session = request.getResourceResolver().adaptTo(Session.class);
        
        int index = 0 ;
        int index1 = 0 ;        	
		String pathInAEM = request.getParameter("aempath");		
		if(StringUtils.isBlank(pathInAEM)){
			pw.write("<b style=\"color:red;\">" + "Please provide valid AEM path against the request param 'aempath'. For eg '/content/adobe'" + "</b>");
			return;
		}
		
        Map<String, String> prdicateMap = createPrdicateMap(pathInAEM);
        Iterator<Resource> resultFromPredicate = queryService.getResultFromPredicate(prdicateMap, session);
        List<Resource> resourceLst   = Lists.newArrayList(resultFromPredicate);

        //resourceLst.stream().filter(chkRes).map(UpdateCompProperty::updateRes);
		
        for(Resource resource : resourceLst ){
               /* pw.write(" Path Resorce "+resource.getPath()+"<br>");
                String expandCollapse = resource.getValueMap().get("expandCollapse", String.class);
                pw.write(" Path Resorce expandCollapse "+expandCollapse+"<br>");*/
                /*if(expandCollapse.equalsIgnoreCase("false")){
                    resource.adaptTo(ModifiableValueMap.class).put("expandCollapse","true");
                }else if(expandCollapse.equalsIgnoreCase("true")){
                    resource.adaptTo(ModifiableValueMap.class).put("expandCollapse","false");
                }*/
        	

        	 //for updating urls in sitemap       	
        	
        	/*if (resource.getPath().contains("sitemapconfig") && !resource.getPath().endsWith("links") && resource.isResourceType("adobe/components/content/links")) {
        		 //pw.write(" Path Resorce "+resource.getPath()+"<br>");
        		  String urlValue = resource.getValueMap().get("url", String.class);       		  
        		 try {
	                if(null != urlValue) {
	                	if(urlValue.contains("adobedata") && !urlValue.startsWith("/content/")) {
	                		
	                		pw.write(" Path Resorce "+resource.getPath()+"<br>");
		                	pw.write(" Old value =============== "+urlValue+"<br>");
		                	if(urlValue.contains("internal"))
		                	urlValue = urlValue.substring(urlValue.indexOf("/internal/"), urlValue.length());
						String correctedVal = MigrationUtil.getAemPath(urlValue, request.getResourceResolver());
						correctedVal = correctedVal.replaceAll("%20","-");
						correctedVal = correctedVal.replaceAll("\\s+","-");
						if (session.nodeExists(correctedVal)) {
							index++;
							resource.adaptTo(ModifiableValueMap.class).put("url", correctedVal);
							
		                	pw.write(" New value =============== "+correctedVal+"<br>");
						}
												
						
	                		
	                	}
	                	
	                	if(urlValue.contains(".page") || urlValue.contains(".asp") || urlValue.contains(".pdf") || urlValue.contains(".doc")) {
	                		index1++;
	                		//urlValue = urlValue.replaceAll("\\s+","-");
	                		pw.write(" Path Resorce "+resource.getPath()+"<br>");
	                		pw.write(" value ++++++++++++++++ "+urlValue+"<br>");
	                	}
  	                //resource.adaptTo(ModifiableValueMap.class).put("url",urlValue);
	                }
        		 } catch (RepositoryException e) {
        			 pw.write(" exception occur at ==  "+resource.getPath()+"<br>");
				}
        	 }*/
        	
        	
        	//for updating news pages property
        	try {
        		String jcrTitleVal = "";
        		String pageTitleVal = "";
        		String navTitleVal = "";
        		resource = resource.getChild("jcr:content");
        		if(null != resource && resource.getPath().contains("/news/") /*&& "adobeNews".equalsIgnoreCase(resource.getValueMap().get("jcr:title", String.class))*/) {        	
        			//pw.write(" Path Resorce "+resource.getPath()+"<br>");        		
        			jcrTitleVal = resource.getValueMap().get("jcr:title", String.class);
        			pageTitleVal =  resource.getValueMap().get("pageTitle", String.class);
        			navTitleVal =  resource.getValueMap().get("navTitle", String.class);
        			if(null != jcrTitleVal && null != pageTitleVal && jcrTitleVal.equalsIgnoreCase(pageTitleVal)) {
        				pw.write("jcrTitle="+jcrTitleVal+" and PageTitle="+pageTitleVal+" are same for path >>>>> "+resource.getPath()+"<br>");
        				if (resource.getValueMap().containsKey("jcr:title")) {
        					resource.adaptTo(ModifiableValueMap.class).remove("jcr:title");        		
        				}	
        				if (resource.getValueMap().containsKey("pageTitle")) {
        					resource.adaptTo(ModifiableValueMap.class).remove("pageTitle");        		
        				}
        				session.save();
        				if(null != navTitleVal) {
        					resource.adaptTo(ModifiableValueMap.class).put("jcr:title", navTitleVal);
        					resource.adaptTo(ModifiableValueMap.class).put("pageTitle", "adobeNews");
        					index++;
        				}

        				//index++;
        			}else {

        				pw.write("jcrTitle="+jcrTitleVal+" and PageTitle="+pageTitleVal+" are not same for path >>>>> "+resource.getPath()+"<br>");
        				if (resource.getValueMap().containsKey("jcr:title")) {
        					resource.adaptTo(ModifiableValueMap.class).remove("jcr:title");        		
        				}	
        				if (resource.getValueMap().containsKey("pageTitle")) {
        					resource.adaptTo(ModifiableValueMap.class).remove("pageTitle");        		
        				}
        				session.save();
        				if(null != navTitleVal) {
        					resource.adaptTo(ModifiableValueMap.class).put("jcr:title", navTitleVal);
        					resource.adaptTo(ModifiableValueMap.class).put("pageTitle", "adobeNews");
        					index1++;
        				}


        			}

        			//pw.write(" value =============== "+  resource.getValueMap().get("jcr:title", String.class) +"<br>");	
        		}
        		/*else {
        		pw.write("jcrTitle != adobeNews for path ---------------- "+resource.getPath()+"<br>");
        	}*/
        	} catch (RepositoryException e) {
   			 pw.write(" exception occur at ==  "+resource.getPath()+"<br>");
			}
        	
        	//for getting all resource path for pages
              
        	//resource = resource.getParent();
        	/*String type = "";
        	if(resource.getPath().contains("/content/adobe") && !resource.getPath().contains("/document-listing/")){
        		String collectionNameVal = resource.getValueMap().get("collectionName", String.class);
            	
            	type = resource.getValueMap().get("sling:resourceType", String.class);
            	 if(type.equalsIgnoreCase("adobecommon/components/content/documentlist")) {
            		 index++;            		 
            	 }else if(type.equalsIgnoreCase("adobecommon/components/content/contentlistbundle")) {
            		 index1++;
            	 }
            	pw.write("Resource ===== "+resource.getPath()+" --and-- ");
            	pw.write("collectionName = "+ collectionNameVal +"<br>");
            	} else if(resource.getName().equalsIgnoreCase("sitemapconfig0")){
            	index1++;
            	//pw.write("Resource title = "+title +"<br>");
            	pw.write("Resource Path +++++++++++++ "+resource.getPath()+"<br>");
            	}*/
        	     
            //pw.write("Page count below ---------------------------------------------");
             
        	//get total page count
            //index=0;
        	/*if(resource != null ){
            	index++;
            	//pw.write("Resource title = "+title +"<br>");
            	pw.write(""+resource.getPath()+"<br>");
            	resource.adaptTo(ModifiableValueMap.class).put("jcr:title", "Contact Us");
            	pw.write("New Value ======= "+resource.getValueMap().get("jcr:title", String.class)+"<br>");
            	} */
        	
        	
            //for removing particular jcr nodes
        	/*try {              
        	javax.jcr.Node node = null;
            if (resource.getPath().contains("jcr:content/rightContent/hw_related_articles") && resource.getName().equalsIgnoreCase("hw_related_articles")) {
            	index++;
       		 pw.write("Removed Resorce Path === "+resource.getPath()+"<br>");
       		 node = resource.adaptTo(Node.class);
       		 node.remove();
       		session.save();
            }
        	  }catch (RepositoryException e) {
				// TODO: handle exception
        		  System.out.println(e);
        		  pw.write("Not Removed Resorce Path === "+resource.getPath()+"<br>");
			}*/
        }
        
        request.getResourceResolver().commit();

            pw.write("done with index count = "+ index + " and index1 +++ "+ index1);
            pw.close();

    }

    private Map<String,String> createPrdicateMap(String pathInAEM) {

        Map<String,String> predicateMap = new HashMap<>();
        predicateMap.put("path",pathInAEM);
        /*predicateMap.put("property","jcr:title");
        predicateMap.put("property.operation", "equals");
        predicateMap.put("property.value","sitemapconfig");*/
        
        //predicateMap.put("property","sling:resourceType");
        //predicateMap.put("property.operation", "equals");
        /*predicateMap.put("property.value","adobe/components/content/links");*/
        //predicateMap.put("property.value","adobecommon/components/pages/contact-us");
        /*predicateMap.put("property.value","adobecommon/components/content/documentlist");
        
        predicateMap.put("property","sling:resourceType");
        predicateMap.put("property.operation", "equals");
        predicateMap.put("property.value","adobecommon/components/content/contentlistbundle");*/
        
      /*  
        predicateMap.put("1_group." + 1 + "_property", "sling:resourceType");
        predicateMap.put("1_group." + 1 + "_property.value", "adobecommon/components/pages/contact-us");
        predicateMap.put("1_group." + 1 + "_property.operation", "equals");
        predicateMap.put("1_group." + 2 + "_property", "jcr:title");
        predicateMap.put("1_group." + 2 + "_property.value", "Contact Us");
        
        predicateMap.put("1_group." + 2 + "_property.operation", "equals");       
        predicateMap.put("1_group.p.and", "true");*/
        
        /*predicateMap.put("property","jcr:title");
        predicateMap.put("property.operation", "equals");
        predicateMap.put("property.value","default");*/
        
        predicateMap.put("property","jcr:primaryType");
        predicateMap.put("property.operation", "equals");
        predicateMap.put("property.value","cq:Page");
        
        predicateMap.put("p.limit","-1");

        
        return predicateMap;

    }



   /* private static Resource updateRes (Resource resource){

        resource.getPath()
    }*/
}

