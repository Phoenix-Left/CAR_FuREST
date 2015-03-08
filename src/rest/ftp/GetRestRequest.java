package rest.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Produces;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.SocketOutputStream;

import rest.ftp.output.html.HtmlGenerator;
import rest.ftp.output.json.JsonGenerator;

public class GetRestRequest {

	/**
	 * Send a RETR Request to the FTP Server for the file designed by session uri and return the file as a byte array to be download
	 * @param session The session which send the RETR Request
	 * @param information Information about the FTP Request
	 * @return A byte array containing the file if it's found, null otherwise
	 */
	@Produces("application/octet-stream")
	public static byte[] getFile(FTPSession session, GetRestRequestInformation information) throws IOException {
		int length;
		byte[] buffer = null;

		InputStream stream = session.getFTPClient().retrieveFileStream(information.getURI());
		if(stream.available() == 0) {
			buffer = new byte[0];
		}
		
		while((length = stream.available()) > 0) {

			int oldLength = buffer != null ? buffer.length : 0;
			byte[] bufferInternal = new byte[length+oldLength];

			if(buffer != null)
				System.arraycopy(buffer, 0, bufferInternal, 0, buffer.length);

			stream.read(bufferInternal, oldLength, length);
			buffer = bufferInternal;

		}
		
		stream.close();
		
		return buffer;
		
	}

	/**
	 * Send a LIST Request to the FTP Server for the directory designed by session uri and return a HTML page as a byte array containing the list of the folder's files
	 * @param session The session which send the LIST Request
	 * @param information Information about the FTP Request
	 * @return A byte array which contains HTML Code to display the directory content
	 */

	public static byte[] getDirectory(FTPSession session, GetRestRequestInformation information) {
		String output = "html";
		if(information.getUriInfo().getQueryParameters().containsKey("output")) {
			output = information.getUriInfo().getQueryParameters().get("output").get(0);
		}
		
		if(output.equals("json")) {
			return JsonGenerator.generateDirectory(session, information).getBytes();
		}
		else {
			return HtmlGenerator.generateDirectory(session, information).getBytes();
		}
	}
	
	public static Map<String, FTPFile> getDirectoryList(FTPFile[] ftpFiles, GetRestRequestInformation information) {
		Map<String, FTPFile> listFile = new HashMap<String, FTPFile>();
		
		String[] folders;
		folders = information.getURI().split("/");

		// Add parent directory link
		if(!information.getURI().equals("")) {
			
			//get parent directory
			String parentDirectory = "";
			for(int i = 0; i < folders.length-1; i++) {
				parentDirectory += folders+"/";
			}
			FTPFile parentEntry = new FTPFile();
			parentEntry.setName(parentDirectory);
			parentEntry.setType(FTPFile.DIRECTORY_TYPE);
			
			listFile.put("Parent Directory", parentEntry);
		}
		
		String prefix = (information.getURI().equals("") ? "" : "/"+information.getURI());
		
		//Add directories first
		for(int i = 0; i < ftpFiles.length; i++) {
			if(ftpFiles[i].isDirectory()) {
				String name = ftpFiles[i].getName();
				ftpFiles[i].setName(prefix+(name.startsWith("/") ? name : "/"+name));
				listFile.put(name+"/", ftpFiles[i]);	
			}
		}
		
		// Then regular files
		for(int i = 0; i < ftpFiles.length; i++) {
			if(!ftpFiles[i].isDirectory()) {
				String name = ftpFiles[i].getName();
				ftpFiles[i].setName(prefix+(name.startsWith("/") ? name : "/"+name));
				listFile.put(name, ftpFiles[i]);	
			}
		}
		
		return listFile;
	}

}
