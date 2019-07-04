/* author - Shubham Garg*/

package com.adobe.migeration.core.utils;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.google.common.collect.Lists;
import com.adobe.migeration.core.commonconstants.CommonConstants;
import com.adobe.migeration.core.service.impl.CreateJCRPageImpl;
import com.adobe.migeration.core.tagMapper.TeamSiteTagsMapping;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.jcr.Session;

import static com.adobe.migeration.core.service.impl.MigeratePageXMLServiceImpl.dam_path_in_aem;

@Component(immediate = true,
        configurationPid = "com.adobe.migeration.core.utils.AdobeMigrationUtil",
        service = AdobeMigrationUtil.class)
public class AdobeMigrationUtil {

    private static Logger logger = LoggerFactory.getLogger(AdobeMigrationUtil.class);
    private static final Logger link_logger = LoggerFactory.getLogger("migration-adobe-links");
    private static final Logger malformed_link_logger = LoggerFactory.getLogger("malformed-links");
    public static final Pattern pattern = Pattern.compile("\\s");

    private static Map<String, String> setTags() {
        Map<String, String> docListTagMap = new HashMap<>();
             /*docListTagMap.put("JRC_Product_Group_Name", "jrcprodname");
             docListTagMap.put("JRC_Product_Group", "jrcprodgrp");
             docListTagMap.put("Content_Types", "contenttype");
             docListTagMap.put("FANN_Product_Groups", "fannprodgrp");
             docListTagMap.put("FANN", "fannprodname");
             docListTagMap.put("General_Subjects", "gensubjects");
            */
        //Shared By Ankur
       // docListTagMap.put("JRC_Product_Group_Name", "jrcprodname");
       // docListTagMap.put("JRC_Product_Group", "jrcprodgrp");
       // docListTagMap.put("BPM_Product_Groups", "bpmprodgrp");
        docListTagMap.put("Brand_Groups", "brandgroup");
        docListTagMap.put("BPM_Product_Names", "bpmproductgroupname");
        return docListTagMap;
    }

    public static String getTagVal(String tagID) {
        try {
            String tag = tagID.substring(tagID.indexOf(":") + 1).split("/")[0];
            String val = setTags().containsKey(tag) ? setTags().get(tag) : "";
            logger.info("Tags Check In Map :" + tag + " And Its Val " + val);
            return val;
        } catch (Exception ex) {
            logger.error("Exception in Getting Tags Val :" + ex.getMessage());
        }
        return "";
    }


    public static String getDamPath(String teamSiteImagePath, ResourceResolver resolver) {
        link_logger.info("TeamSite Asset Path #### {}",teamSiteImagePath);
        teamSiteImagePath = teamSiteImagePath.trim();
        Matcher matcher = pattern.matcher(teamSiteImagePath);
        boolean found = matcher.find();
        if(found){
            teamSiteImagePath = teamSiteImagePath.replaceAll("\\s+","-");
        }
        teamSiteImagePath = teamSiteImagePath.replace("%20","-");
        teamSiteImagePath = teamSiteImagePath.replace("[",StringUtils.EMPTY);
        teamSiteImagePath = teamSiteImagePath.replace("]",StringUtils.EMPTY);
        String assetPath = teamSiteImagePath;
        assetPath = getAemPath(assetPath, resolver);
        link_logger.info("AEM Path ---> " + assetPath);
        return assetPath;
    }

	public static String getAemPath(String assetPath, ResourceResolver resolver) {
		//External replacement
        if(assetPath.matches("http://adobe.corp.adobe\\.com/.*")) {
        	assetPath = assetPath.replace("http://adobe.corp.adobe.com",StringUtils.EMPTY);
        } else if(assetPath.matches("http://adobereview.corp.adobe\\.com/.*")) {
        	assetPath = assetPath.replace("http://adobereview.corp.adobe.com",StringUtils.EMPTY);
        } else if(assetPath.startsWith("$URL_PREFIX[$1]$1$1$1")) {
        	assetPath = assetPath.replace("$URL_PREFIX[$1]$1$1$1",StringUtils.EMPTY);
        	if(!assetPath.startsWith("/")) {
        		assetPath = "/" + assetPath;
        	}
        } else if(assetPath.startsWith("$URL_PREFIX[$1]$1")) {
        	assetPath = assetPath.replace("$URL_PREFIX[$1]$1",StringUtils.EMPTY);
        	if(!assetPath.startsWith("/")) {
        		assetPath = "/" + assetPath;
        	}
        } else if(assetPath.startsWith("$URL_PREFIX")) {
        	assetPath = assetPath.replace("$URL_PREFIX",StringUtils.EMPTY);
        } else if (assetPath.contains("/news/source_files/images/adobenews")){    		
        	assetPath = assetPath.substring(assetPath.indexOf("internal"), assetPath.length());    		        
    	} else if (assetPath.startsWith("https://np1appl")){
    		String temp = assetPath.replace("https://np1appl",StringUtils.EMPTY);
    		assetPath = temp.substring(temp.indexOf("/"));
    	}
        //Internal adjustment
        if(assetPath.startsWith("/")) {
        	if(assetPath.contains("//")) {
        		assetPath = assetPath.replace("//","/");
        	}
        	if(assetPath.startsWith("/internal/PS")) {
        		assetPath = assetPath.replace("/internal/PS","/content/dam/ps/internal");
        	}else if(assetPath.startsWith("/internal/ps")) {
        		assetPath = assetPath.replace("/internal/ps","/content/dam/ps/internal");
        	} else if(assetPath.startsWith("/internal/adobe")){
        		assetPath = assetPath.replace("/internal/adobe", dam_path_in_aem+"/internal");
            } else if(assetPath.startsWith("/default/main/adobe")){
            	assetPath = assetPath.replace("/default/main/adobe", dam_path_in_aem+"/internal");
            } else if(assetPath.startsWith("/internal/")) {
            	assetPath = assetPath.replace("/internal", dam_path_in_aem+"/internal");
            } else if(!assetPath.startsWith("/internal/") && assetPath.contains(".page")) {
            	if(assetPath.startsWith("/sites/")) {
            		assetPath = assetPath.replace("/sites", StringUtils.EMPTY);
            		assetPath = fullTextSearch(assetPath, resolver);
            	} else {
            		assetPath = fullTextSearch(assetPath, resolver);
            	}
            } else if(assetPath.startsWith("/internal/")) {
            	assetPath = dam_path_in_aem+"/internal" + assetPath;
            }
        }
        link_logger.info("Inside getAemPath method. Final AEM Path ---> " + assetPath);
		return assetPath;
	}

    public static String getFileName(String fileName) {
        StringBuilder strBuilder = new StringBuilder();
        String[] splitArr = null;
        splitArr = fileName.split("-");
        if (fileName.indexOf(CommonConstants.SPACE) != -1) {
            splitArr = fileName.split("\\s");
        }
        if (splitArr != null) {
            for (String tokens : splitArr) {
                strBuilder.append(" " + StringUtils.capitalize(tokens));
            }
        }
        return strBuilder.toString();
    }
    
    public static String toAEMDate(String date)  {
        String   upDate ="";
      
           if (StringUtils.isNotBlank(date)) {
        	   try {
                   Date dd = new SimpleDateFormat("dd-MMM-yy").parse(date);
                   upDate = new SimpleDateFormat("yyyy-MM-dd").format(dd) + CommonConstants.Time_Stamp;
                   if(!upDate.contains("2019") && !upDate.contains("2020")) {
                       upDate = "2021"+ upDate.substring(upDate.indexOf("-"));
                   }
               if(!upDate.contains("2019") && !upDate.contains("2020")) {
            	   upDate = "2023"+ upDate.substring(upDate.indexOf("-"));
               }
               } catch ( ParseException e) {
				logger.error("Date Format Exception toAEMDate method "+e.getMessage());
			}
           }
       
        return upDate;
    }

    public static Calendar toAEMCal(String date)  {
            try {
                if (StringUtils.isNotBlank(date)) {
                    Date dd = new SimpleDateFormat("dd-MMM-yy").parse(date);
                    String dteTim = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.0").format(dd);
                    LocalDateTime ldt = LocalDateTime.parse(dteTim,DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss.0"));
                    ldt = ldt.plusHours(8);
                    ZonedDateTime utc = ldt.atZone(ZoneId.of("UTC"));
                    Calendar cal = GregorianCalendar.from(utc);
                    return cal;
                }
            }catch (DateTimeParseException | ParseException par){
               //logger.error("Error in mIgration UTil toAEMCal():"+par.getMessage());
            }
            return null;
    }



    public static List<String> getTagCollection(String tsfacetProGrps) {
        return Arrays.stream(tsfacetProGrps.split("~")).map(MigrationUtil::collectAEMNode).collect(Collectors.toList());
    }

    public static String collectAEMNode(String tsTag){
        return TeamSiteTagsMapping.getTagFromKey(tsTag);
    }

    public static String getAEMPath(String brws,ResourceResolver resolver) {
        String aemPath = brws;
       if(CreateJCRPageImpl.assetPattern.matcher(aemPath).find()){
           aemPath =getDamPath(aemPath,resolver);
       }
       else {
    	   aemPath = getAemPath(aemPath, resolver);
    	   link_logger.info("AEM Href-Link " + aemPath);
       }
        return aemPath;
    }

    public static List<CSVRecord> readCSVFile(String filepath) throws IOException {
        File file = new File(filepath);
        CSVFormat format  = CSVFormat.RFC4180.withFirstRecordAsHeader();
        CSVParser csvRecords = CSVParser.parse(file, StandardCharsets.UTF_8, format);
        List<CSVRecord> csvRecordList = csvRecords.getRecords();
        return csvRecordList;
    }
    
    public static String fullTextSearch(String pagePath, ResourceResolver resolver) {
    	String aemPath = pagePath;
    	String[] strArray = aemPath.split(".page");
    	String nodePath = strArray[0].trim();
    	nodePath = nodePath.replace("%20","-");
    	nodePath = nodePath.replaceAll("\\s+","-");
    	String nodeName = nodePath.substring(nodePath.lastIndexOf("/")+1);
    	Map<String, String> map = new HashMap<String, String>();
		map.put("path", "/content/adobe");
		map.put("type", "cq:Page");
		map.put("nodename", nodeName);
		map.put("p.limit", "-1");
		//map.put("fulltext", nodePath);
		QueryBuilder queryBuilder = getService(QueryBuilder.class);
		PredicateGroup predicates = PredicateGroup.create(map);
        Query query = queryBuilder.createQuery(predicates, resolver.adaptTo(Session.class));
        //query.setHitsPerPage(0);
        Iterator<Resource> rsIterator = query.getResult().getResources();
		//Iterator<Resource> rsIterator = null != queryService ? queryService.getResultFromPredicate(map, resolver.adaptTo(Session.class)) : Collections.emptyIterator();
		List<Resource> list = Lists.newArrayList(rsIterator);
		int resultCount = null != list ? list.size() : 0;
		for(Resource res : list) {
			String path = res.getPath();
			if(path.endsWith(nodePath)) {
				if(strArray.length == 2) {
					aemPath = path + ".html" + strArray[1];
				} else {
					aemPath = path + ".html";
				}
				break;
			}
		}
		/*while (rsIterator.hasNext()) {
			String path = rsIterator.next().getPath();
			if(path.endsWith(nodePath)) {
				if(strArray.length == 2) {
					aemPath = path + ".html" + strArray[1];
				} else {
					aemPath = path + ".html";
				}
				break;
			}
		}*/
		if(aemPath.equals(pagePath)) {
			malformed_link_logger.info("Pages found: "+ resultCount +" Correct link could not be formed for: "+pagePath);
			aemPath = pagePath.replace(".page", ".html");
			aemPath = "/content/adobe" + aemPath;
		}
        return aemPath;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T getService(Class<T> serviceClass) {
        BundleContext bContext = FrameworkUtil.getBundle(serviceClass).getBundleContext();
        ServiceReference sr = bContext.getServiceReference(serviceClass.getName());
            return (T)serviceClass.cast(bContext.getService(sr));
    }
}
