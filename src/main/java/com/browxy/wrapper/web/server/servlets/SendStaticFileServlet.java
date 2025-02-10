package com.browxy.wrapper.web.server.servlets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.browxy.wrapper.web.fileUtils.FileManager;
import com.browxy.wrapper.web.fileUtils.MimeTypeUtil;
import com.browxy.wrapper.web.server.config.project.ProjectConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class SendStaticFileServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(SendStaticFileServlet.class);

	private String staticDirPath;
	private String staticFilePath;
	private String entryPoint;

	public SendStaticFileServlet(String staticDirPath, String staticFilePath, String entryPoint) {
		this.staticDirPath = staticDirPath;
		this.staticFilePath = staticFilePath;
		this.entryPoint = entryPoint;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String requestUri = req.getRequestURI();
		if (requestUri.startsWith("/api")) {
			req.getRequestDispatcher(req.getRequestURI()).forward(req, resp);
			return;
		}
		try {
			String filePath = requestUri.startsWith("/assets") ? this.staticDirPath + requestUri
					: this.staticDirPath + File.separator + this.staticFilePath;
			File file = new File(filePath);
			String mimeType = MimeTypeUtil.getMimeTypeByFileName(file.getName());
			if (file.exists()) {
				String content = new String(Files.readAllBytes(file.toPath()));
				if (mimeType.equals("text/html")) {
					HttpSession session = req.getSession(false);
					if(session == null) {
						session = req.getSession(true);
					}
					content = buildHtmlProjectMetadata(content);
				}
				resp.setContentType(mimeType);
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().write(content);
			} else {
				resp.setContentType("text/html");
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Static file not found");
			}
		} catch (Exception e) {
			logger.error("unable to send static content", e);
			resp.setContentType("text/html");
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "unable to send static content");
		} finally {
			resp.flushBuffer();
			resp.getWriter().close();
		}
	}

	private String buildHtmlProjectMetadata(String content) {
		ProjectConfig projectConfig = null;
        Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
		try {
			String containerBasePath = System.getProperty("containerBasePath");
			String path = containerBasePath + File.separator + "metadata" + File.separator + "project.json";
			String metadata = FileManager.readFile(path, "UTF-8");
			projectConfig = gson.fromJson(metadata, ProjectConfig.class);
			String hostSocketPort = System.getenv("HOST_SOCKET_PORT") != null ? System.getenv("HOST_SOCKET_PORT")
					: System.getProperty("PORT");
            projectConfig.setSocketPort(Integer.parseInt(hostSocketPort));
            projectConfig.setEntryPoint(this.entryPoint);
		    
		} catch (Exception e) {
			logger.error("unable to build project metatdata content", e);
		}
		String conf = projectConfig != null
				? gson.toJson(projectConfig)
				: gson.toJson(new JsonObject());
		return content.replace("%%__CONFIG__%%", conf);
	}
}
