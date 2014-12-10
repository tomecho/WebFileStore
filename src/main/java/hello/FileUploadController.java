package hello;

//import java.io.BufferedOutputStream;
//import java.io.File;
//import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import javax.sql.rowset.serial.SerialBlob;

//import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.sql.*;
/*
 * Reads from table fileupload on database testuser
 * columns are name and data
 *   name being filename in text.
 *   data is a blob form of the file
 * 
 * */
@Controller
public class FileUploadController {

    @RequestMapping(value="/upload", method=RequestMethod.GET)
    public @ResponseBody String provideUploadInfo() {
        return "You can upload a file by posting to this same URL.";
    }

    @RequestMapping(value="/upload", method=RequestMethod.POST)
    public @ResponseBody String handleFileUpload(@RequestParam("name") String name,
            @RequestParam("file") MultipartFile file){
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	if (!file.isEmpty()) {
            try { //already writes to a file
            	Class.forName("com.mysql.jdbc.Driver");  //this driver needs to be added to build path
            	conn = DriverManager.getConnection("jdbc:mysql://localhost/testuser",
            			"testuser","badpassword"); //connect to the database, user information is hardcoded here
            	stmt = conn.prepareStatement("INSERT INTO fileupload (name,data) VALUES ('" + name + "-uploaded'," + "?)"); //the statement
            	Blob data = new SerialBlob(file.getBytes());
            	stmt.setBlob(1, data);
            	stmt.executeUpdate();
            	/*  Legacy code from writing to file
                //byte[] bytes = file.getBytes();
               // BufferedOutputStream stream =
                //        new BufferedOutputStream(new FileOutputStream(new File(name + "-uploaded")));
               // stream.write(bytes);
                //stream.close();
                //return "You successfully uploaded " + name + " into " + name + "-uploaded !"; */
                return "success, you can download from here: " +
                		"<a href=\"" + name + "-uploaded\"" + " download>link</a>"; //lets switch this to a url for download
            } catch (Exception e) {
                return "You failed to upload " + name + " => " + e.getMessage();
            } finally { //close the connection no matter the error
            	try{
            		stmt.close(); } catch(Exception e) {
            			//just forget about it 
            	}
            	try {
            		conn.close(); } catch(Exception e){
            			//ignore errors again
            	}
            	
            }
        } else {
            return "You failed to upload " + name + " because the file was empty.";
        }
    }
    @RequestMapping(value="/{f}-uploaded", method=RequestMethod.GET)
    public @ResponseBody void sendFile(@PathVariable("f") String f, HttpServletResponse response){
    	Connection conn = null;
    	Statement stmt = null;
    	try {
    		Class.forName("com.mysql.jdbc.Driver");
    		conn = DriverManager.getConnection("jdbc:mysql://localhost/testuser",
    				"testuser","badpassword"); //connect to the database, user information is hardcoded here
    		stmt = conn.createStatement(); //my statement
    		ResultSet rs = stmt.executeQuery("SELECT name,data FROM fileupload WHERE name='" 
    				+ (f+"-uploaded'")); //doesnt support files with same name
    		if(rs.next()){
    			InputStream in = rs.getBlob("data").getBinaryStream();
    			response.addHeader("Content-Disposition", ("attachment;filename='"+f+"-uploaded'")); //set the file as download rather than display in browser
    			OutputStream out = response.getOutputStream();
    			byte[] buffer = new byte[131072]; //128kb
    			int bytesRead = -1;
             	while ((bytesRead = in.read(buffer)) != -1) {
             		out.write(buffer, 0, bytesRead);
             	}
             	in.close();
             	out.close();
             } else {
            	 System.out.println("test");
            	 return; //could not find file do nothing
             }
    		
    	} catch (Exception e) {
    		System.out.println(e.getClass() + " " + e.toString()); 
    	} finally { //close the connection no matter the error
        	try{
        		stmt.close(); } catch(Exception e) {
        			//just forget about it 
        	}
        	try {
        		conn.close(); } catch(Exception e){
        			//ignore errors again
        	}
        	
        }
    	//return new FileSystemResource((f+"-uploaded")); //return the file from filesystem // but were not doing it that way anymore 
    }
}