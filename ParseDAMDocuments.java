package com.adobe.migeration.core.servlets;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Servlet;
import javax.jcr.Session;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.util.PDFTextStripperByArea;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlink;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.dam.api.Rendition;
import org.osgi.service.component.annotations.Reference;

//import org.apache.poi.xslf.usermodel.XMLSlideShow;
//import org.apache.poi.POIXMLProperties.*;
//import org.apache.poi.xslf.usermodel.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Hyperlink;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlink;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun; 

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.adobe.granite.asset.api.AssetException;
import javax.jcr.RepositoryException;


import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.io.IOUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import javax.swing.text.EditorKit;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.ChangedCharSetException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

import com.adobe.migeration.core.service.impl.CreateJCRPageImpl;
import com.adobe.migeration.core.utils.MigrationUtil;

import static com.adobe.migeration.core.service.impl.AdobeReportServiceImpl.CSV_MIME_TYPE;
import static com.adobe.migeration.core.service.impl.AdobeReportServiceImpl.DAM_MIGRATION_FOLDER_PATH;
import static com.adobe.migeration.core.service.impl.MigeratePageXMLServiceImpl.dam_path_in_aem;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a Servlet class for identifying the broken urls in the DAM asset.
 *  
 * @author Wipro
 * 
 * @version 1.0
 */
@Component(service = Servlet.class, immediate = true, property = {
		"sling.servlet.methods=" + HttpConstants.METHOD_GET,
		"sling.servlet.paths=" + "/bin/parseDamDocuments" })
public class ParseDAMDocuments extends SlingAllMethodsServlet {

	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ParseDAMDocuments.class);

	private static final Pattern pattern = Pattern.compile("\\s");
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	@Reference
	private QueryBuilder queryBuilder;

	PrintWriter out = null;
	Session session = null;
	ResourceResolver resolver = null;
	static int assetCount = 1;

	Map<String,List<String>> pdfUrlsMap = null;
	Map<String,List<String>> docUrlsMap = null;
	Map<String,List<String>> docxUrlsMap = null;
	Map<String,List<String>> htmlUrlsMap = null;
	Map<String,List<String>> xlsUrlsMap = null;

	List<String> pdfUrlList = null;
	List<String> docUrlList = null;
	List<String> docxUrlList = null;
	List<String> htmlUrlList = null;
	List<String> xlsUrlList = null;

	/**
	 * Do Get method
	 * 
	 * @param logger
	 * 
	 * @return
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
		try {
			long t1, t2;
			t1 = System.currentTimeMillis();

			response.setContentType("text/html");
			out = response.getWriter();        
			resolver = request.getResourceResolver();
			session = resolver.adaptTo(Session.class);

			pdfUrlsMap = new HashMap<>();
			docUrlsMap = new HashMap<>();
			docxUrlsMap = new HashMap<>();
			htmlUrlsMap = new HashMap<>();
			xlsUrlsMap = new HashMap<>();

			pdfUrlList = new ArrayList();
			docxUrlList = new ArrayList();
			xlsUrlList = new ArrayList();
			docUrlList = new ArrayList();
			htmlUrlList = new ArrayList();

			String aemPath = request.getParameter("aempath");          
			if(StringUtils.isBlank(aemPath)){
				out.write("<b style=\"color:red;\">" + "Please provide valid aempath asset path for excel "+ "</b>");
				return;
			}             

			Map<String, Asset> mapper = getAssetsList(aemPath, resolver);

			//Iterate through each asset to find broken links embedded inside them
			mapper.entrySet().parallelStream()
			.filter(map -> map.getKey() != null)
			.forEach(p  -> {                
				try {
					if(getAssetExtType(p.getKey().toString()) != null) {
						out.println("Asset No::"+ assetCount++);
						out.println(" & type :: " + getAssetExtType(p.getKey().toString()));
						out.println(" & path::"+ p.getValue().getPath()+"<br>");    						
						processAssets(p.getValue() , getAssetExtType(p.getKey().toString()) , p.getValue().getPath());
					}
				}catch (Exception es){
					LOG.error("Exception in lambda expression in do Get ::"+ es.getMessage());
					out.write("Exception in lambda expression in do Get ::"+ es.getMessage() + "<br>");
				}

			});

			//Generate CSV report for broken links
			out.write("Generate Report in CSV under /content/dam/migration-report" +"<br>");

			generateReport(pdfUrlsMap , "Pdf");
			pdfUrlsMap.clear();

			generateReport(xlsUrlsMap , "Excel");
			xlsUrlsMap.clear();

			generateReport(docUrlsMap , "Doc");
			docUrlsMap.clear();

			generateReport(docxUrlsMap , "Docx");
			docxUrlsMap.clear();

			generateReport(htmlUrlsMap , "Html"); 
			htmlUrlsMap.clear();

			out.write("End of Do GET method-CSV Files generated" +"<br>");	

			t2 = System.currentTimeMillis();
			out.write("parallel Stream Time Taken?= " + (t2-t1) + "\n");

		} catch (IOException ioe) {
			LOG.error("IOException occurred in ParseDAMDocument file-- " + ioe);
		} catch (Exception ex) {
			LOG.error("Exception occurred in ParseDAMDocument file-- " + ex);
		}finally {			
			assetCount = 1;
			if (out != null) {
				out.flush();
				out.close();
			}
			if (null != resolver && resolver.isLive()) {
				resolver.close();
			}

		}
	}

	private void processAssets(Asset asset , String assetType , String assetAEMPath) {
		Rendition rendition = null;
		InputStream is = null;

		try {
			//Now read asset as per extension
			if(assetType.equalsIgnoreCase("pdf")) {
				//get asset original rendition and its input stream
				rendition = asset.getOriginal();
				if(null != rendition) {
					is = rendition.getStream();							
				}						
				readPdfDocument(is, out, assetAEMPath);			
			}
			else if(assetType.equalsIgnoreCase("doc")) {
				rendition = asset.getOriginal();
				if(null != rendition) {
					is = rendition.getStream();							
				}
				readDocFile(is, out, assetAEMPath);
			}
			else if(assetType.equalsIgnoreCase("docx")) {
				rendition = asset.getOriginal();
				if(null != rendition) {
					is = rendition.getStream();							
				}
				readDocxFile(is, out, assetAEMPath);
			}					
			else if(assetType.equalsIgnoreCase("xls") || assetType.equalsIgnoreCase("xlsx") || assetType.equalsIgnoreCase("xlsm")) {
				rendition = asset.getOriginal();
				if(null != rendition) {
					is = rendition.getStream();							
				}
				readExcelFile(is, out, assetAEMPath);
			}
			else if(assetType.equalsIgnoreCase("html") || assetType.equalsIgnoreCase("htm")) {                                                                                    
				rendition = asset.getOriginal();
				if(null != rendition) {
					is = rendition.getStream();							
				}
				readHTMLFile(is, out, assetAEMPath);
			}
		} catch(Exception e){
			e.printStackTrace();
			LOG.info("Exception in processAssets::"+ e);
			out.write("<b style=\"color:red;\">" + "Exception in processAssets:: </b>"+ e + "<br>");
		}      

	}

	private String getAssetExtType(String assetName) throws Exception {
		LOG.info("--Get asset extension type------------");
		String assetExtType = "";
		if(assetName != null) {
			assetExtType = assetName.substring(assetName.lastIndexOf(".") + 1).trim();
			//out.println(assetExtType);
		}
		return assetExtType;
	}

	/*
	 * Get Assets map under given path
	 * 
	 */
	private Map<String, Asset> getAssetsList(String aemPath, ResourceResolver resolver) throws AssetException, RepositoryException {
		LOG.info(">>>>>>>>>>>>>>>>>>>>>>In getAssetsList method of ParseDAMDocument");

		Map<String, Asset> assetMap = new HashMap<String, Asset>();
		Map<String, String> map = new HashMap<String, String>();
		map.put("path", aemPath);
		map.put("type", "dam:Asset");
		map.put("p.limit", "-1");

		Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
		SearchResult result = query.getResult();
		List<Hit> list = result.getHits();

		for (Hit hit : list) {			              
			Resource resource = resolver.getResource(hit.getPath());
			Asset asset = resource.adaptTo(Asset.class);
			if(null != asset){
				assetMap.put(asset.getName() , asset);				
			}
		}
		LOG.info(">>>>>>>>>>>>>>>>>In end of getAssetsList method of ParseDAMDocument");
		out.write("Total asset size in end of getAssetsList method = "+ assetMap.size()+ "</br>");	

		return assetMap;
	}

	/*
	 * Read PDF file
	 * 
	 */
	private void readPdfDocument(InputStream is, PrintWriter out, String assetPath) {
		LOG.info("Inside readPdfDocument");
		try{
			List<String> pdfURLlistDistinct = null;			
			PDDocument doc = null;
			doc = PDDocument.load( is );		
			int pageNum = 0;
			List<PDPage> list =doc.getDocumentCatalog().getAllPages();

			for(int i = 0; i < list.size(); i++){
				PDPage page =list.get(i);
				//PDFTextStripperByArea stripper = new PDFTextStripperByArea();
				List<PDAnnotation> annotations = page.getAnnotations();
				//first setup text extraction regions
				for( int j=0; j<annotations.size(); j++ )
				{
					PDAnnotation annot = annotations.get(j);
					if( annot instanceof PDAnnotationLink )
					{
						PDAnnotationLink link = (PDAnnotationLink)annot;
						PDAction action = link.getAction();
						//String urlText = stripper.getTextForRegion( "" + j );
						if( action instanceof PDActionURI )
						{							
							PDActionURI uri = (PDActionURI)action;													
							pdfUrlList.add(uri.getURI().toString());
						}
					}
				}

			}//main for ends
			//remove duplicates from the below list
			if(pdfUrlList.size() > 0) {
				pdfURLlistDistinct = pdfUrlList.stream().distinct().collect(Collectors.toList()); 
				pdfUrlsMap.put(assetPath, pdfURLlistDistinct);
				pdfUrlList.clear();			
			}			
			is.close();
			doc.close();			
			LOG.info("Leaving readPDFFile");
		}catch(IOException ex){
			ex.printStackTrace();
			LOG.info("IOException in readPdfDocument::"+ ex);
			out.write("<b style=\"color:red;\">" + "IOException in readPdfDocument:: </b>"+ ex + "<br>");
		} catch(Exception e){
			e.printStackTrace();
			LOG.info("Exception in readPdfDocument::"+ e);
			out.write("<b style=\"color:red;\">" + "Exception in readPdfDocument:: </b>"+ e + "<br>");
		}      

	}


	/*
	 * Read Docx file
	 * 
	 */
	private void readDocxFile(InputStream is, PrintWriter out, String assetPath) {
		LOG.info("Inside readWordFile");
		try {
			List<String> docxUrlListDistinct = null;			
			XWPFDocument document = new XWPFDocument(is);
			StringBuffer text = null;
			text = new StringBuffer();

			Iterator<XWPFParagraph> i = document.getParagraphsIterator();
			while (i.hasNext()) {
				XWPFParagraph paragraph = i.next();
				for (XWPFRun run : paragraph.getRuns()) {
					if (run instanceof XWPFHyperlinkRun) {
						text.append(run.toString());
						XWPFHyperlink link = ((XWPFHyperlinkRun) run).getHyperlink(document);
						if (link != null) {				
							docxUrlList.add(link.getURL().toString());					

						}
					}
				}

			}
			//remove duplicates from the below list
			if(docxUrlList.size() > 0) {
				docxUrlListDistinct = docxUrlList.stream().distinct().collect(Collectors.toList()); 
				docxUrlsMap.put(assetPath, docxUrlListDistinct);
				docxUrlList.clear();
			}
			is.close();			
			LOG.info("Leaving readWordFile");
		}catch(IOException ex) {
			ex.printStackTrace();
			LOG.info("IOException in readDocxFile::"+ ex);
			out.write("<b style=\"color:red;\">" + "IOException in readDocxFile:: </b>"+ ex + "<br>");
		}catch(Exception e) {
			e.printStackTrace();
			LOG.info("Exception in readDocxFile::"+ e);
			out.write("<b style=\"color:red;\">" + "Exception in readDocxFile:: </b>"+ e + "<br>");
		}
	}

	/*
	 * Read Excel file
	 * 
	 */
	private void readExcelFile(InputStream is, PrintWriter out, String assetPath) {
		LOG.info("Inside readExcelFile");
		Workbook workbook = null;
		Sheet sheet = null;        
		Iterator<Cell> cellIterator = null;
		Row row = null;
		Cell cell = null;
		String cellValue = null;
		int sheetCount = 0 ;
		List<String> excelUrlListDistinct = null;

		try {
			workbook = WorkbookFactory.create(is);
			sheetCount = workbook.getNumberOfSheets();
			for (int i =0; i < sheetCount ; i++){
				sheet = workbook.getSheetAt(i);
				//List<Hyperlink> listofLinks = sheet.getHyperlinkList();         
				// Create a DataFormatter to format and get each cell's value as String
				DataFormatter dataFormatter = new DataFormatter();
				Iterator<Row> rowIterator = sheet.rowIterator();
				Hyperlink hLink = null;
				while (rowIterator.hasNext()) {
					row = rowIterator.next();
					cellIterator = row.cellIterator();

					while (cellIterator.hasNext()) {
						cell = cellIterator.next();
						cellValue = dataFormatter.formatCellValue(cell);
						if(!StringUtils.isBlank(cellValue)) {
							hLink = cell.getHyperlink();
							if (hLink == null) {
								//out.write("Cell didn't have a hyperlink!" + "</br>");
							} else {
								xlsUrlList.add(hLink.getAddress().toString());							
							}
						}                   
					}
				}
			}

			if(xlsUrlList.size() > 0) {
				excelUrlListDistinct = xlsUrlList.stream().distinct().collect(Collectors.toList());
				xlsUrlsMap.put(assetPath, excelUrlListDistinct);
				xlsUrlList.clear();
			}
			is.close();

			LOG.info("Leaving readExcelFile");
		}catch(IOException ex) {
			ex.printStackTrace();
			LOG.info("IOException in readExcelFile::"+ ex);
			out.write("<b style=\"color:red;\">" + "IOException in readExcelFile:: </b>"+ ex + "<br>");
		}catch(Exception e) {
			e.printStackTrace();
			LOG.info("Exception in readExcelFile::"+ e);
			out.write("<b style=\"color:red;\">" + "Exception in readExcelFile:: </b>"+ e + "<br>");
		}

	}

	/*
	 * Read HTML file
	 * 
	 */
	private void readHTMLFile(InputStream is, PrintWriter out, String assetPath) {
		LOG.info("Inside readHTMLFile");
		try {
			List<String> htmlUrlListDistinct = null;		
			String text = IOUtils.toString(is, StandardCharsets.UTF_8.name());

			Document doc = Jsoup.parse(text);
			Elements resultLinks = doc.select("a[href]");

			for (Element link : resultLinks) {
				String href = link.attr("href");
				//out.write("href ==" + href);
				htmlUrlList.add(href);			
			}
			//remove duplicates from the below list
			if(htmlUrlList.size() > 0) {
				htmlUrlListDistinct = htmlUrlList.stream().distinct().collect(Collectors.toList()); 
				htmlUrlsMap.put(assetPath, htmlUrlListDistinct);
				htmlUrlList.clear();
			}
			is.close();			
			LOG.info("Leaving readHTMLFile");
		}catch(IOException ex) {
			ex.printStackTrace();
			LOG.info("IOException in readHTMLFile::"+ ex);
			out.write("<b style=\"color:red;\">" + "IOException in readHTMLFile:: </b>"+ ex + "<br>");
		}catch(Exception e) {
			e.printStackTrace();
			LOG.info("Exception in readHTMLFile::"+ e);
			out.write("<b style=\"color:red;\">" + "Exception in readHTMLFile:: </b>"+ e + "<br>");
		}
	}

	/*
	 * Read Doc file
	 * 
	 */
	private void readDocFile(InputStream is, PrintWriter out, String assetPath) {
		LOG.info("Inside readDocFile");
		try {
			HWPFDocument doc = new HWPFDocument(is);
			List<String> docUrlListDistinct = null;
			WordExtractor we = new WordExtractor(doc);
			String[] paragraphs = we.getParagraphText();		
			for (String para : paragraphs) {			
				Pattern p = Pattern.compile("HYPERLINK \"([^\"]*)\"");
				Matcher m = p.matcher(para);
				while (m.find()) {	
					docUrlList.add(m.group(1));				

				}
			}
			//remove duplicates from the below list
			if(docUrlList.size() > 0) {
				docUrlListDistinct = docUrlList.stream().distinct().collect(Collectors.toList()); 
				docUrlsMap.put(assetPath, docUrlListDistinct);
				docUrlList.clear();
			}
			is.close();			
			LOG.info("Leaving readDocFile");
		}catch(IOException ex) {
			ex.printStackTrace();
			LOG.info("IOException in readDocFile::"+ ex);
			out.write("<b style=\"color:red;\">" + "IOException in readDocFile:: </b>"+ ex + "<br>");
		}
		catch(Exception e) {
			e.printStackTrace();
			LOG.info("Exception in readDocFile::"+ e);
			out.write("<b style=\"color:red;\">" + "Exception in readDocFile:: </b>"+ e + "<br>");
		}

	}


	private void generateReport(Map<String,List<String>> assetLinksMap, String fileType) {

		String correctAemPath = "";
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Asset Path");
		stringBuilder.append(",");
		stringBuilder.append("Link Text");
		stringBuilder.append(",");
		stringBuilder.append("Existing Link");
		stringBuilder.append(",");
		stringBuilder.append("Correct AEM Link");
		stringBuilder.append(",");
		stringBuilder.append("AEM Link Exists");
		stringBuilder.append(System.lineSeparator());
		int count =0;
		int brokenLinkCount = 0;

		try {
			for (Map.Entry<String, List<String>> entry : assetLinksMap.entrySet()) {			
				out.write("Iterating assetLinksMap for "+fileType+"---" + ++count +"<br>");

				/*stringBuilder.append(entry.getKey());
				stringBuilder.append(System.lineSeparator());
				int i = 0;
				 */
				for(String linkPath : entry.getValue()) {
					if(linkPath.contains("/content/dam/adobeworld") || linkPath.contains("/content/dam/ps/internal") || linkPath.contains("/content/dam/PS/internal")
							|| linkPath.contains("/content/dam/ps/confidential") || linkPath.contains("/content/dam/PS/confidential")
							|| linkPath.contains("http://adobe.corp.adobe.com") || linkPath.contains("https://adobe.corp.adobe.com")) {

						stringBuilder.append(entry.getKey());
						stringBuilder.append(",");
						stringBuilder.append("--");
						stringBuilder.append(",");
						stringBuilder.append(linkPath);
						stringBuilder.append(",");
						//get AEM equivalent path of links
						correctAemPath = getAemPath(linkPath,resolver);				

						if(StringUtils.isNotBlank(correctAemPath) && correctAemPath.startsWith("/content/dam")) {
							//no need to check case sensitivity as then only link will be considered as broken in assets.
							//correctAemPath = checkCaseSensitive(correctAemPath, resolver);
							stringBuilder.append(correctAemPath);
							stringBuilder.append(",");
							if(session.nodeExists(correctAemPath)) {
								stringBuilder.append("True");			
							} else {
								stringBuilder.append("Broken");
								brokenLinkCount++;
							}
						} else {
							stringBuilder.append("--");
							stringBuilder.append(",");
							stringBuilder.append("FALSE");
						}

						stringBuilder.append(System.lineSeparator());
					}
				}
				stringBuilder.append(System.lineSeparator());				
			}     

			String timeFormat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));		 
			String file_name = "/brokenLinkReport_"+brokenLinkCount+"_"+fileType+"_"+timeFormat+".csv";
			AssetManager assetManager = resolver.adaptTo(AssetManager.class);
			InputStream is = new ByteArrayInputStream(stringBuilder.toString().getBytes());
			Asset  asset = assetManager.createAsset(DAM_MIGRATION_FOLDER_PATH+file_name, is, CSV_MIME_TYPE, true);
			LOG.info("Broken links generated Report Path == "+ asset.getPath());
			out.write("Broken links count for file type :: " + fileType + " == " + brokenLinkCount+"<br>");
		}catch(Exception e) {
			e.printStackTrace();
			LOG.info("Exception in generateReport::"+ e);
			out.write("<b style=\"color:red;\">" + "Exception in generateReport:: </b>"+ e +"<br>");
		}
	}

	private  String getAemPath(String teamSitePath, ResourceResolver resolver) {
		LOG.info("Inside getAemPath coming assetpath---> " + teamSitePath);
		String assetPath = "";
		if(CreateJCRPageImpl.assetPattern.matcher(teamSitePath).find()){
			//aemPath =getDamPath(aemPath,resolver);
			LOG.info("TeamSite Asset Path #### {}",teamSitePath);
			teamSitePath = teamSitePath.trim();
			Matcher matcher = pattern.matcher(teamSitePath);
			boolean found = matcher.find();
			if(found){
				teamSitePath = teamSitePath.replaceAll("\\s+","-");
			}
			teamSitePath = teamSitePath.replace("%20","-");
			teamSitePath = teamSitePath.replace("[",StringUtils.EMPTY);
			teamSitePath = teamSitePath.replace("]",StringUtils.EMPTY);
			assetPath = teamSitePath;

		}

		String dam_path_in_aem = "/content/dam/adobeworld";
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
			} else if(assetPath.startsWith("/confidential/ps")) {
				assetPath = assetPath.replace("/confidential/ps","/content/dam/ps/confidential");
			}else if(assetPath.startsWith("/confidential/PS")) {
				assetPath = assetPath.replace("/confidential/PS","/content/dam/ps/confidential");
			}else if(assetPath.startsWith("/internal/adobe")){
				assetPath = assetPath.replace("/internal/adobe", dam_path_in_aem+"/internal");
			} else if(assetPath.startsWith("/default/main/adobe")){
				assetPath = assetPath.replace("/default/main/adobe", dam_path_in_aem+"/internal");
			} else if(assetPath.startsWith("/internal/")) {
				assetPath = assetPath.replace("/internal", dam_path_in_aem+"/internal");
			} else if(!assetPath.startsWith("/internal/") && assetPath.contains(".page")) {
				if(assetPath.startsWith("/sites/")) {
					assetPath = assetPath.replace("/sites", StringUtils.EMPTY);
					assetPath = MigrationUtil.fullTextSearch(assetPath, resolver);
				} else {
					assetPath = MigrationUtil.fullTextSearch(assetPath, resolver);
				}
			} else if(assetPath.startsWith("/internal/")) {
				assetPath = dam_path_in_aem+"/internal" + assetPath;
			}else if(assetPath.startsWith("/confidential/")) {
				assetPath = dam_path_in_aem+"/confidential" + assetPath;            	           	
			}
		}
		LOG.info("Inside getAemPath method. Final AEM Path correct path---> " + assetPath);
		return assetPath;
	}

	private static Set<String> resourceList = new HashSet<String>();
	private String checkCaseSensitive(String url, ResourceResolver resolver) {

		String folderPath = StringUtils.EMPTY;
		String correctUrl = url;

		resourceList.clear();
		folderPath = url.substring(0, url.lastIndexOf("/"));
		Resource parentResource = resolver.getResource(folderPath);
		if (null != parentResource) {
			Iterator<Resource> listChildren = parentResource.listChildren();
			while (listChildren.hasNext()) {
				resourceList.add(listChildren.next().getPath());
			}
			for (String resPath : resourceList) {
				if (resPath.equalsIgnoreCase(url)) {
					correctUrl = resPath;
					break;
				}
			}
		}

		return correctUrl;
	}


}

