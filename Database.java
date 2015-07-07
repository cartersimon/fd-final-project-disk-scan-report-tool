package dirList;

import java.sql.*;
import java.text.SimpleDateFormat;

public class Database {

	public static int newRequest(String email){
		int request_id = 0;
		try {
			Statement stmt;
			ResultSet rs;

			//connect to DB
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/scanresults";
			Connection con = DriverManager.getConnection(url,"scanUser", "de1uxeUK");
			stmt = con.createStatement();		 
				 
			//add new row to request table
			SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //timestamp formatter for request		 
			stmt.executeUpdate(
			"INSERT INTO request(date_time, email) VALUES('"
			+ date.format(new java.util.Date()) + "','" + email + "')");  //grabs current system timestamp here

		 	//new row created so read its request_id
		 	stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		 	rs = stmt.executeQuery("SELECT request_id FROM request " +
				"ORDER BY request_id DESC LIMIT 1");  //get latest request_id entry

		 	if( rs.absolute(1) ){
		 		request_id = rs.getInt("request_id");  //store request_id
			}		
			 
			 //use DatabaseMetaData.getTables() to ensure result table for current year exists...
			 DatabaseMetaData dbmd = con.getMetaData();
			 ResultSet tables = dbmd.getTables(null, null, getResultTblName(), null);
			 if (!(tables.next())){
				//if table doesn't exist, create it
				stmt.executeUpdate("CREATE TABLE " + getResultTblName() +
				"(results_id INTEGER AUTO_INCREMENT NOT NULL PRIMARY KEY, " +
				"job_number VARCHAR(10) NOT NULL, title VARCHAR(100) NOT NULL, " +
				"request_id INTEGER NOT NULL, CONSTRAINT FOREIGN KEY (request_id) " +
				"REFERENCES request(request_id)) ENGINE=InnoDB");
			}		 	
			con.close();  //close connection to database
		}catch( Exception e ) {
	 		e.printStackTrace();
		}//end catch
		return request_id;
	}//end newRequest
	
	public static void storeItem(String directory, String title, int request_id){

		try {
			PreparedStatement pstmt;
			 
			//connect to DB
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/scanresults";
			Connection con = DriverManager.getConnection(url,"scanUser", "de1uxeUK");

			//use a prepared statement used to cope with quotes within a film name
			pstmt = con.prepareStatement("INSERT INTO " + getResultTblName() + 
				" (job_number, title, request_id) VALUES(?, ?, ?)");
			pstmt.setString(1, directory);  //values to go into prepared statement
			pstmt.setString(2, title);
			pstmt.setInt(3, request_id);
			pstmt.executeUpdate();  //exectue prepared statement
			con.close();  //close connection to database
		}catch( Exception e ) {
	 		e.printStackTrace();
		}
	}//end storeItem 
	
	public static String getTimestamp(int rID){
		String timeStamp = "";
		try {
			Statement stmt;
			ResultSet rs;
			
			//connect to DB
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/scanresults";
			Connection con = DriverManager.getConnection(url,"scanUser", "de1uxeUK");
		 	stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		 	rs = stmt.executeQuery("SELECT date_time FROM request WHERE request_id = '" + rID + "'");
		 	if( rs.absolute(1) ){
		 		timeStamp = rs.getString("date_time");  //fetch system time
			}		
			con.close();
			}catch( Exception e ) {
		 		e.printStackTrace();
			}
		return timeStamp;
	}//end getTimestamp()
	
	
	public static String createReport(int rID){
		String report = "";
		//find results for rID and append to report
		try {
			Statement stmt;
			ResultSet rs;

			//connect to DB
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/scanresults";
			Connection con = DriverManager.getConnection(url,"scanUser", "de1uxeUK"); 
		 		 
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			report += "<div id=\"rightcol\">\n" +
				"<h4>Title</h4>\n" +
				"<ul>\n";  //add heading to right column
			rs = stmt.executeQuery("SELECT title FROM " + getResultTblName() + " WHERE request_id = '" + rID + "'");
			while (rs.next()) {
				report += "<li>" + rs.getString("title") + "</li>\n";  //get film names
			}
			report += "</ul>\n" +
				"</div>  <!-- end rightcol -->	\n" +
				"<div id=\"leftcol\">\n" +
				"<h4>Job Number</h4>\n" +
				"<ul>\n";  //add heading to left column		 			
			rs = stmt.executeQuery("SELECT job_number FROM " + getResultTblName() + " WHERE request_id = '" + rID + "'");
			while (rs.next()) {
				report += "<li>" + rs.getString("job_number") + "</li>\n";  //get job numbers
			}
			report += "</ul>\n" +
				"</div>  <!--end leftcol -->\n" +
				"<hr />\n";  //end unordered list
			con.close();
		}catch( Exception e ) {
	 		e.printStackTrace();
		}
		return report;
	}//end createReport	

	public static String getEmailAddr(int rID){
		String emailTmp = "";
		try {
			Statement stmt;
			ResultSet rs;
			
			//connect to DB
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/scanresults";
			Connection con = DriverManager.getConnection(url,"scanUser", "de1uxeUK");
		 	stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		 	rs = stmt.executeQuery("SELECT email FROM request WHERE request_id = '" + rID + "'");
		 	if( rs.absolute(1) ){
		 		emailTmp = rs.getString("email");  //fetch email from database
			}		
		 	con.close();
		}catch( Exception e ) {
			e.printStackTrace();
		}
		return emailTmp;
	}//end getEmailAddr
	
	private static String getResultTblName(){
		 SimpleDateFormat dateR = new SimpleDateFormat("yyyy"); // formatter to get just year for table name
		 String table_name = "results_" + dateR.format(new java.util.Date()); // table name is "results_" concatenated with year 
		 return table_name;
	}//end getResultTblName	
}//end Database class
