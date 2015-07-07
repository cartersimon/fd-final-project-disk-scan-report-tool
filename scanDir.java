package dirList;

import java.io.*;
import java.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import javax.mail.*;
import javax.mail.internet.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/scanDir")
public class scanDir extends HttpServlet {
	static String msg = "";  //initialise email content variable
	static int highestJobNum = 0;  //variable to hold highest job number found
	static String DVDJOBS = "C:/Project/DVDjobs99.xls"; // path to DVDJOBS Excel file
	static String STORAGE = "C:/t-temp/";  //location to scan
       
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String textbox = request.getParameter("inputedEmail");  //store entered email
	    response.setContentType("text/html");
	    PrintWriter out = response.getWriter();
		
		//check if DVDJOBS readable, if not exit with response page
		File dvdjobs = new File(DVDJOBS);
		if(!dvdjobs.exists()) { 
			String dvdjobserror = htmlHeader();  //variable to hold html
			dvdjobserror += "<h3>Unfortunately, DVDJOBS could not be read.</h3>\n" +
			"<h3>Please contact the systems department.</h3>\n";
			dvdjobserror += htmlFooter();
			out.println(dvdjobserror);  //output error page
			return;  //exit program
		}
		File array = new File(STORAGE);
		
		//check if storage is readable, if not exit with response page
		if(!array.exists()) { 
			String arrayerror = htmlHeader();  //variable to hold html
			arrayerror += "<h3>Unfortunately, the array could not be read.</h3>\n" +
			"<h3>Please contact the systems department.</h3>\n";
			arrayerror += htmlFooter();
			out.println(arrayerror);  //output error page
			return;  //exit program
		}
		String[] masterList = createMasterlist(); //store ALL jobnums & titles from excel
		
		//create new request in database and return request_id for use in runScan() method
		int curRequestID = Database.newRequest(textbox);
		runScan(masterList, curRequestID);  //initiate scan of storage
		
		//read from DB and output to browser
		String report = htmlHeader(curRequestID) + 
			Database.createReport(curRequestID) +
			htmlFooter();
	    out.println(report);
		
		sendMail(msg, textbox);  //send email
		msg = "";  //reinitialise email content variable
	}//end doGet()

	public static void runScan(String[] masterList, int request_id)
	{
		File file;  //variable to hold current file/folder name
		File dir = new File(STORAGE);	//variable to hold storage location
		int projFound = 0;	//variable to hold count of live titles (system info only)
		msg += "Scan Tool Results\n\nJob Number\tTitle\n----------\t-----";  //header for email
		
		String[] list = dir.list();	//holds all files/folders at location
			if (list.length > 0)	//ensures something found at location
			{
				Arrays.sort(list);	//sort here???  Or could once have valid items??? 
				for (int i = 0; i < list.length; i++) 	//check each item found 
				{
					file = new File(STORAGE + list[i]);	//check an item
					  if (file.isDirectory()) 	//do the following if item's a directory
					  {
						  String curDir = list[i];	//string var of item to allow comparison 
						  
						  if (curDir.matches("\\d{4}sk\\d{1,3}[a-zA-Z]"))  //name matches convention?
						  {
							  projFound++;	
							  //remove sku & convert to int to allow compare
							  int project = Integer.parseInt(curDir.replaceAll("sk.*",""));
							  if (project > highestJobNum){  //no entry for project in dvdjobs
								  Database.storeItem(curDir, "WARNING: No entry in DVDJOBS", request_id);
								  msg += "\n"+ list[i] + "\tWARNING: No entry in DVDJOBS";
							  }else if(masterList[project] == ""){  //dvdjobs entry has no film name
								  Database.storeItem(curDir, "WARNING: No title in DVDJOBS", request_id);
								  msg += "\n"+ list[i] + "\tWARNING: No title in DVDJOBS";
							  }else{  
							  Database.storeItem(curDir, masterList[project], request_id);  //store entry in database
							  msg += "\n"+ list[i] + "\t" + masterList[project];  //store entry in email variable
							  }
						  }  
					  }
				}
				System.out.println(projFound + " projects found.");	//confirm how many projs found (system info only)
			}
	}//end runScan()

	public static String[] createMasterlist() throws FileNotFoundException, IOException
	{
		POIFSFileSystem fileSystem = new POIFSFileSystem(new FileInputStream(DVDJOBS));
		HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);
		HSSFSheet sheet = workbook.getSheet("DVD1998"); //Get Excel Sheet
		String[] masterlist;
		
		for (Row row : sheet) // loop finds highest job# to initialise masterlist
		{
		    Cell jobNum = row.getCell(1); // load a cell from 2nd column
		    if(row.getRowNum()==0)
		    	continue; // 1st row is the header row, skip it
		    highestJobNum = Integer.parseInt(jobNum.getStringCellValue());
		} // returns string, convert to int to initialise  
		masterlist = new String[highestJobNum + 1];	// +1 to allow for index 0
		for (Row row : sheet) 
		{
		    Cell jobNum = row.getCell(1); // load cell from 2nd column
		    Cell title = row.getCell(3, Row.CREATE_NULL_AS_BLANK);  //allows prog to continue if entry is null
		    if(row.getRowNum()==0)
		    	continue; //skip the header row
		    int jobNumAsInt = Integer.parseInt(jobNum.getStringCellValue()); //convert to int
		    masterlist[jobNumAsInt] = title.getStringCellValue();  //store film name in master list
		}
		return masterlist;
	}//end createMasterlist()

	private static String htmlHeader(){  //standard header for web page
		String output =
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \n" +
			"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
			"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n" +
			"<!-- Author: Simon Carter.  Results page for the Deluxe Media Scan Tool -->\n" +
			"<head>\n" +
			"<title>Deluxe Media Scan Tool: Results</title>\n" +
			"<link rel=\"stylesheet\" type=\"text/css\" href=\"dscan.css\" />\n" +
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" +
			"</head>\n" +
			"<body>\n" +
			"<div id=\"main\">\n" +
			"<div id=\"header\">\n" +
			"<h1> Deluxe Media <br /> Scan Tool - Results</h1>\n" +
			"</div>  <!-- end header -->\n" +
			"<hr />\n" +
			"<div id=\"contentcontainer\">\n";
		return output;
	}//end htmlHeader()
	
	private static String htmlHeader(int rID){  //web page header with timestamp and email address
		String output =
			htmlHeader() + 
			"<p>\n" + Database.getTimestamp(rID) + "\n</p>\n" +
			"<h4>\n";
		String emailTmp = Database.getEmailAddr(rID);  //fetch email from database
		if (emailTmp.length() == 0)  //if no email entered issue warning
 			output += "WARNING: No email supplied, report sent to default address\n</h4>\n";
 		else
 			output += "This report has been sent to: " + emailTmp + "\n</h4>\n";
		return output;
	}//end htmlHeader(int rID)
	
	private static String htmlFooter(){  //footer for web page
		String output = 
			"<div id=\"footer\">	\n" +
			"<p>&#169; Deluxe Media 2013. All rights reserved. 7 Soho Square, London, " +
			"W1D 3QB. Tel: 020 7534 7300</p>\n" +
			"</div>  <!-- end footer -->\n" +
			"</div>  <!--end contentcontainer -->\n" +
			"</div>  <!--end main -->\n" +
			"</body>\n" +
			"</html>";
		return output;
	}//end htmlFooter()
	
	public static void sendMail(String msgContent, String emailAddr)
	{    
		boolean emailSupplied = false;  //variable to show whether email address was supplied
		
		if (emailAddr == ""){  //if no email address supplied set to default address
			emailAddr = "si_carter76@hotmail.com";  //set default address
		}else{
			emailSupplied = true;
		}		
		String to = emailAddr;
	    String from = "simon.carter@bydeluxe.com"; // Sender's email ID needs to be mentioned
	    Properties properties = System.getProperties(); // Get system properties
	    properties.setProperty("mail.smtp.host", "94.31.1.11"); // Setup mail server      
	    Session session = Session.getDefaultInstance(properties); // Get the default Session object.
	    try{
         
	    	MimeMessage message = new MimeMessage(session); // Create a default MimeMessage object.
	    	message.setFrom(new InternetAddress(from)); // Set From: header field of the header.
	    	message.addRecipient(Message.RecipientType.TO,
                                  new InternetAddress(to)); // Set To: header field of the header.
	    	message.setSubject("arrayT report"); // Set Subject: header field
	        if (emailSupplied){ // Now set the message content
	        	 message.setText(msgContent);
	        }else{  //issue warning if no address supplied
	        	 message.setText("WARNING: No email address was supplied\n" + msgContent);
	        }
	        Transport.send(message); // Send message
	        System.out.println("Sent message successfully....");

		}catch (MessagingException e) {  //catch errors messages
			e.printStackTrace();
		}
	}//end sendMail()	
}//end scanDir class
