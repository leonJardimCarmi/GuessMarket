package com.guessmarket.server.servlets.events;

import com.guessmarket.dto.ApiParams;
import com.guessmarket.dto.ApiPaths;
import com.guessmarket.server.common.ApiServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * POST /events/upload  (multipart form, field "file" = the XML file)  ->  {"message", "events"}
 * The uploader becomes the market maker of every event in the file.
 * The file is never saved on the server: its content is read straight from the request into the engine.
 */
@WebServlet(name = "UploadServlet", urlPatterns = ApiPaths.UPLOAD)
@MultipartConfig(fileSizeThreshold = UploadServlet.MAX_FILE_SIZE,
        maxFileSize = UploadServlet.MAX_FILE_SIZE,
        maxRequestSize = UploadServlet.MAX_FILE_SIZE * 2L)
public class UploadServlet extends ApiServlet {
    // Up to this size the upload stays in memory (fileSizeThreshold), so Tomcat never writes it to a temporary file.
    static final int MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    protected Object handlePost(HttpServletRequest request) throws IOException, ServletException {
        String userName = currentUserName(request);
        Part filePart = requireXmlFile(request);

        List<String> loadedEvents;
        try (InputStream content = filePart.getInputStream()) { // we opened the stream, so we close it
            loadedEvents = engine().loadEventsFromXml(content, userName);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "File '" + filePart.getSubmittedFileName() + "' loaded successfully: "
                + loadedEvents.size() + " event(s) added, you are their market maker.");
        body.put("events", loadedEvents);
        return body;
    }

    private static Part requireXmlFile(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            throw new IllegalArgumentException("Upload the file as a multipart form, in a field named '" + ApiParams.FILE + "'.");
        }
        Part filePart = request.getPart(ApiParams.FILE);
        if (filePart == null || filePart.getSize() == 0) {
            throw new IllegalArgumentException("No file was sent in the field '" + ApiParams.FILE + "'.");
        }
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xml")) {
            throw new IllegalArgumentException("Only XML files can be uploaded (the file name must end with .xml).");
        }
        return filePart;
    }
}
