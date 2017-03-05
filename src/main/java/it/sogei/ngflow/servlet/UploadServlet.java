package it.sogei.ngflow.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.storage.ObjectStorageService;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payload;
import org.openstack4j.openstack.OSFactory;

import it.sogei.ngflow.upload.FlowInfo;
import it.sogei.ngflow.upload.FlowInfoStorage;
import it.sogei.ngflow.upload.HttpUtils;

/**
 *
 * This is a servlet demo, for using Flow.js to upload files.
 *
 * by fanxu123
 */

@WebServlet("/upload")
public class UploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	/*
	 * Change this to your upload folder, by default we will use unix  /tmp
	 */
	public static final String UPLOAD_DIR = "/tmp/temp-upload";
	
	public static final String MYDOMAIN = "http://26.2.234.112:9080";

	
	//objectStorage
	//Get these credentials from Bluemix by going to your Object Storage service, and clicking on Service Credentials:
		private static final String USERNAME = "admin_9757dce54df22d39aebe60045e8949690d5ad7fe";
		private static final String PASSWORD = "p?v.}M2N*1nQ6YQ(";
		private static final String DOMAIN_ID = "1191759";
		private static final String PROJECT_ID = "80e33159813f48739f09570464e566c4";


	/*
	 * In ORDER to allow CORS  to multiple domains you can set a list of valid domains here
	 */
	private List<String> authorizedUrl = Arrays.asList(
			"http://26.2.234.112",
			"https://26.2.169.56", 
			"http://localhost", 
			"https://mybluemix.net", 
			"https://upload-frontapp.mybluemix.net",
			"https://upload-flowjs-java.mybluemix.net"
			);
	
	public void doOptions(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
    //The following are CORS headers. Max age informs the 
    //browser to keep the results of this call for 1 day.
	resp.setHeader("Cache-control", "no-cache, no-store");
    resp.setHeader("Access-Control-Allow-Origin", "https://upload-frontapp.mybluemix.net");
    resp.setHeader("Access-Control-Allow-Credentials", "true");
    resp.setHeader("Access-Control-Allow-Methods", "GET, POST");
    resp.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type");
    resp.setHeader("Access-Control-Max-Age", "86400");
    //Tell the browser what requests we allow.
    resp.setHeader("Allow", "GET, HEAD, POST, TRACE, OPTIONS");
}
	
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setHeader("Cache-control", "no-cache, no-store");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "-1");
		response.setHeader("Access-Control-Allow-Origin", "http://upload-frontapp.mybluemix.net");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Methods","POST, HEAD, GET, DELETE, PUT, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type");
		response.setHeader("Access-Control-Max-Age", "86400");
		
		System.out.println(">> Do Post v 0.5");
		
		System.out.println(">> requestURL: " +request.getRequestURL());

		PrintWriter out = response.getWriter();
		
		int flowChunkNumber = getflowChunkNumber(request);

		FlowInfo info = getFlowInfo(request);


		// Save to file
		final InputStream is = request.getInputStream();
		long readed = 0;
		long content_length = request.getContentLength();
		byte[] bytes = new byte[1024 * 100];
		
		RequestDispatcher rd = request.getRequestDispatcher("objectStorage");
//		request.setAttribute("container", info.flowIdentifier+"-container");
//		request.setAttribute("file", info.flowIdentifier+"."+flowChunkNumber);
//		
//		System.out.println(">>> chiamata servlet object storage");
//		rd.forward(request,response);
		
//////salvataggio su Object Storage
		
		ObjectStorageService objectStorage = authenticateAndGetObjectStorageService();

		//response.setContentType("application/json");
//		response.setHeader("Cache-control", "no-cache, no-store");
//		response.setHeader("Pragma", "no-cache");
//		response.setHeader("Expires", "-1");
//		response.setHeader("Access-Control-Allow-Origin", "http://upload-frontapp.mybluemix.net");
//		response.setHeader("Access-Control-Allow-Credentials", "true");
//		response.setHeader("Access-Control-Allow-Methods","POST, HEAD, GET, DELETE, PUT, OPTIONS");
//		response.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type");
//		response.setHeader("Access-Control-Max-Age", "86400");
		
		System.out.println("---- Storing file in ObjectStorage...");

		String containerName = info.flowIdentifier+"-container";

		String fileName = info.flowIdentifier+"."+flowChunkNumber;
		
		System.out.println(">> cantianerName: "+containerName);
		System.out.println(">> file : " +fileName);
		
		if(containerName == null || fileName == null){
			//No file was specified to be found, or container name is missing
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			System.out.println("Container o fileName null");
			return;
		}	
		
		if (objectStorage.containers().create(containerName).isSuccess()) {
			//final InputStream fileStream = is;
			//System.out.println(">> fileStream : " +fileStream);
			
			Payload<InputStream> payload = new PayloadClass(is);
			
			objectStorage.objects().put(containerName, fileName, payload);
			
			System.out.println(">> Successfully stored file in ObjectStorage!");
		
		}
		else{
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			System.out.println("Errore nella creazione del container: "+containerName );
			return;
		}
		
		
 ////////////
		


		System.out.println(">>> post salvataggio object storage");
		// Mark as uploaded.
		info.uploadedChunks.add(new FlowInfo.flowChunkNumber(flowChunkNumber));
		System.out.println("flowChunkNumber:" +flowChunkNumber);
		String archivoFinal = info.checkIfUploadFinished();
		System.out.println("archivoFinal:" +archivoFinal);
		System.out.println("info:" +info);
		
		
		response.setContentType("application/json");
		
		
		if (archivoFinal != null) { // Check if all chunks uploaded, and
			// change filename
			System.out.println("Ho finito");
			FlowInfoStorage.getInstance().remove(info);
			response.getWriter().print("All finished.");
			System.out.println("info:" +info);
			System.out.println("inflowFilePathnfo:" +info.flowFilePath);
			

		} else {
			response.getWriter().print( info.flowIdentifier +">> Uploaded chunk "+flowChunkNumber);
			System.out.println("Uploaded chunk: " +info.uploadedChunks);
			System.out.println("info:" +info);
			System.out.println("inflowFilePathnfo:" +info.flowFilePath);

		}
		// out.println(myObj.toString());

		out.close();
	}
		
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		response.setContentType("application/json");
		response.setHeader("Cache-control", "no-cache, no-store");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "-1");
		response.setHeader("Access-Control-Allow-Origin","http://upload-frontapp.mybluemix.net");
		response.setHeader("Access-Control-Allow-Methods","POST, HEAD, GET, DELETE, PUT, OPTIONS");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type");
		response.setHeader("Access-Control-Max-Age", "86400");
		int flowChunkNumber = getflowChunkNumber(request);
		System.out.println("Do Get");
		

		System.out.println(">> requestURL: "+request.getRequestURL());
		PrintWriter out = response.getWriter();

		FlowInfo info = getFlowInfo(request);
		
		Object fcn = new FlowInfo.flowChunkNumber(flowChunkNumber);
		System.out.println(" >>> fcn "+fcn.toString());
		System.out.println(" >>> flowChunkNumber "+flowChunkNumber);
		System.out.println(" >>> uploadedChuncks "+info.uploadedChunks.toArray().length);
		System.out.println(" >>> info "+info.flowChunkSize 
				+ " - filename " + info.flowFilename 
				+ " - filepath" + info.flowFilePath
				+ " - identifier " + info.flowIdentifier
				+ " - relativepath " + info.flowRelativePath
				+ " - totalsize " + info.flowTotalSize
				+ " - uploadedchunks " + info.uploadedChunks);
		
		if (info.uploadedChunks.contains(fcn)) {
			System.out.println("Do Get arriba");
			response.getWriter().print("Uploaded."); // This Chunk has been
														// Uploaded.
		} else {
			System.out.println("Chunk Not uploaded");
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		
		
		out.close();
		
		
//		String containerName = request.getParameter("container");
//
//		String fileName = request.getParameter("file");
//
//		if(containerName == null || fileName == null){ //No file was specified to be found, or container name is missing
//			response.sendError(HttpServletResponse.SC_NOT_FOUND);
//			System.out.println("Container name or file name was not specified.");
//			return;
//		}
//
//		SwiftObject pictureObj = objectStorage.objects().get(containerName,fileName);
//
//		if(pictureObj == null){ //The specified file was not found
//			response.sendError(HttpServletResponse.SC_NOT_FOUND);
//			System.out.println("File not found.");
//			return;
//		}
//
//		String mimeType = pictureObj.getMimeType();
//
//		DLPayload payload = pictureObj.download();
//
//		InputStream in = payload.getInputStream();
//
//		response.setContentType(mimeType);
//
//		OutputStream out = response.getOutputStream();
//
//		IOUtils.copy(in, out);
//		in.close();
//		out.close();
//
//		System.out.println("Successfully retrieved file from ObjectStorage!");
		
	}

	private int getflowChunkNumber(HttpServletRequest request) {
		System.out.println("--- getFlowChunkNumber ---");
		System.out.println("flowchunknumber: " +HttpUtils.toInt(request.getParameter("flowChunkNumber"), -1));
		
		return HttpUtils.toInt(request.getParameter("flowChunkNumber"), -1);
	}

	private FlowInfo getFlowInfo(HttpServletRequest request)
			throws ServletException {

		System.out.println("---- getFlowInfo -----");

		System.out.println(">> request param: " 
				+ "flowchunksize: "+request.getParameter("flowChunkSize")
				+ "flowtotalsize: "+request.getParameter("flowTotalSize")
				+ "flowidentifier" +request.getParameter("flowIdentifier")
				+ "flowFilename" +request.getParameter("flowFilename")
				+ "flowrelativepath " +request.getParameter("flowRelativePath"));	
		
		String base_dir = UPLOAD_DIR;
      
		int FlowChunkSize = HttpUtils.toInt(
				request.getParameter("flowChunkSize"), -1);
		
		long FlowTotalSize = HttpUtils.toLong(
				request.getParameter("flowTotalSize"), -1); //flowTotalChunks
		
		System.out.println(">> flowtotalsize: " +FlowTotalSize);
		
		String FlowIdentifier = request.getParameter("flowIdentifier");
		String FlowFilename = request.getParameter("flowFilename");
		String FlowRelativePath = request.getParameter("flowRelativePath");
		
		// Here we add a ".temp" to every upload file to indicate NON-FINISHED
//		String FlowFilePath = new File(base_dir, FlowFilename)
//				.getAbsolutePath() + ".temp";
		
		
		// modificato per ObjectStore
		String FlowFilePath = FlowFilename + ".temp";
		
		System.out.println(">> FlowFilePath: " +FlowFilePath);
		
		FlowInfoStorage storage = FlowInfoStorage.getInstance();

		FlowInfo info = storage.get(FlowChunkSize, FlowTotalSize,
				FlowIdentifier, FlowFilename, FlowRelativePath, FlowFilePath);
		if (!info.valid()) {
			storage.remove(info);
			throw new ServletException("Invalid request params.");
		}
		return info;
	}

	
	private ObjectStorageService authenticateAndGetObjectStorageService() {
		String OBJECT_STORAGE_AUTH_URL = "https://identity.open.softlayer.com/v3";

		Identifier domainIdentifier = Identifier.byName(DOMAIN_ID);

		System.out.println("Authenticating...");

		OSClientV3 os = OSFactory.builderV3()
				.endpoint(OBJECT_STORAGE_AUTH_URL)
				.credentials(USERNAME,PASSWORD, domainIdentifier)
				.scopeToProject(Identifier.byId(PROJECT_ID))
				.authenticate();

		System.out.println("Authenticated successfully!");
		System.out.println(os.objectStorage().containers().list());
		ObjectStorageService objectStorage = os.objectStorage();

		return objectStorage;
	}
	
	private class PayloadClass implements Payload<InputStream> {
		private InputStream stream = null;

		public PayloadClass(InputStream stream) {
			this.stream = stream;
		}

		@Override
		public void close() throws IOException {
			stream.close();
		}

		@Override
		public InputStream open() {
			return stream;
		}

		@Override
		public void closeQuietly() {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}

		@Override
		public InputStream getRaw() {
			return stream;
		}
	}
	
}
