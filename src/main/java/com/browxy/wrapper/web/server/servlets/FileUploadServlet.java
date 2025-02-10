package com.browxy.wrapper.web.server.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.browxy.wrapper.web.fileUtils.FileManager;
import com.browxy.wrapper.web.response.ResponseMessageUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileUploadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(FileUploadServlet.class);
	private final String uploadPath;

	public FileUploadServlet(String uploadPath) {
		this.uploadPath = uploadPath;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!ServletFileUpload.isMultipartContent(request)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().write(ResponseMessageUtil.getStatusMessage("Request does not contain upload data", 400));
			return;
		}

		DiskFileItemFactory factory = new DiskFileItemFactory();

		File repository = new File(System.getProperty("java.io.tmpdir"));
		factory.setRepository(repository);

		ServletFileUpload upload = new ServletFileUpload(factory);

		upload.setFileSizeMax(50 * 1024 * 1024); // 50MB max file size
		upload.setSizeMax(100 * 1024 * 1024); // 100MB max total size
		int code = 400;
		String message = "file/s uploaded OK";
		try {
			List<FileItem> formItems = upload.parseRequest(request);
			String alias = request.getParameter("alias");
			String path = alias != null && !alias.trim().isEmpty() ? uploadPath + File.separator + alias : uploadPath;
			FileManager.createDirectory(path);
			for (FileItem item : formItems) {
				if (!item.isFormField()) {
					String fileName = new File(item.getName()).getName();
					String filePath = path + File.separator + fileName;
					File storeFile = new File(filePath);
					item.write(storeFile);
					FileManager.setPermissions(filePath);
				}
			}
			code = 200;
		} catch (FileUploadBase.SizeLimitExceededException e) {
			logger.error("File size exceeds the limit!:", e);
			message = "File size exceeds the limit!";
		} catch (Exception e) {
			logger.error("Error while uploading file:", e);
			message = "Error while uploading file: " + e.getMessage();
		} finally {
			response.setStatus(code == 200 ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().write(ResponseMessageUtil.getStatusMessage(message, code));
			response.flushBuffer();
			response.getWriter().close();
		}
	}
}
