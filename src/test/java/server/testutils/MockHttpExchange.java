package server.testutils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class MockHttpExchange extends HttpExchange {
    private String method;
    private URI uri;
    private Headers requestHeaders;
    private InputStream requestBody;
    private ByteArrayOutputStream responseBody;
    private int responseCode;

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public MockHttpExchange(String method, String path, String body) {
        this.method = method;
        this.uri = URI.create(path);
        this.requestBody = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        this.responseBody = new ByteArrayOutputStream();
        this.requestHeaders = new Headers();
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Headers exposure is acceptable for testing purposes"
    )
    @Override
    public Headers getRequestHeaders() {
        return requestHeaders;
    }
    @Override
    public Headers getResponseHeaders() {
        return new Headers();
    }
    @Override
    public URI getRequestURI() {
        return uri;
    }
    @Override
    public String getRequestMethod() {
        return method;
    }

    @Override
    public HttpContext getHttpContext() {
        return null;
    }

    @Override
    public InputStream getRequestBody() {
        return requestBody;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "OutputStream exposure is acceptable for testing purposes"
    )
    @Override
    public OutputStream getResponseBody() {
        return responseBody;
    }
    @Override
    public void sendResponseHeaders(int rCode, long responseLength) {
        this.responseCode = rCode;
    }
    @Override
    public InetSocketAddress getRemoteAddress() {return null;}
    @Override
    public void close() {}
    @Override
    public Object getAttribute(String name) {return null;}
    @Override
    public void setAttribute(String name, Object value) {}
    @Override
    public void setStreams(InputStream i, OutputStream o) {}
    @Override
    public HttpPrincipal getPrincipal() {return null;}
    @Override
    public InetSocketAddress getLocalAddress() {return null;}
    @Override
    public String getProtocol() {return "";}


    public int getResponseCode() {
        return responseCode;
    }
    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public String getResponseBodyAsString() {
        return responseBody.toString(StandardCharsets.UTF_8);
    }
    public MockHttpExchange withHeader(String key, String value) {
        requestHeaders.add(key, value);
        return this;
    }
    public MockHttpExchange withStudentId(int studentId) {
        requestHeaders.add("StudentId", String.valueOf(studentId));
        return this;
    }
    public MockHttpExchange withRole(String role){
        return withHeader("Role", role);
    }
}
