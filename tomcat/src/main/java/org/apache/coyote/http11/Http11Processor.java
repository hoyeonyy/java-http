package org.apache.coyote.http11;

import com.techcourse.db.InMemoryUserRepository;
import com.techcourse.exception.UncheckedServletException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import org.apache.coyote.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http11Processor implements Runnable, Processor {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);

    private final Socket connection;

    public Http11Processor(final Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.info("connect host: {}, port: {}", connection.getInetAddress(), connection.getPort());
        process(connection);
    }

    @Override
    public void process(final Socket connection) {
        try (final var inputStream = connection.getInputStream();
             final var outputStream = connection.getOutputStream()) {

            HttpRequest httpRequest = new HttpReader(inputStream).getHttpRequest();
            RequestLine requestLine = httpRequest.requestLine();

            if (requestLine.isGet() && requestLine.isRoot()) {
                responseRoot(outputStream, requestLine);
            }
            if (requestLine.isGet() && requestLine.isIndex()) {
                responseIndex(outputStream, requestLine);
            }
            if (requestLine.isGet() && requestLine.hasCss()) {
                responseCss(outputStream, requestLine);
            }
            if (requestLine.isGet() && requestLine.hasJs()) {
                responseJs(outputStream, requestLine);
            }
            if (requestLine.isGet() && requestLine.hasLogin()) {
                responseLogin(outputStream, requestLine);
            }
        } catch (IOException | UncheckedServletException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void responseRoot(OutputStream outputStream, RequestLine requestLine) throws IOException {
        final var responseBody = "Hello world!";

        final var response = String.join("\r\n",
                requestLine.getProtocol() + " 200 OK",
                "Content-Type: text/html;charset=utf-8",
                "Content-Length: " + responseBody.getBytes().length,
                "",
                responseBody);

        outputStream.write(response.getBytes());
        outputStream.flush();
    }

    private void responseIndex(OutputStream outputStream, RequestLine requestLine) throws IOException {
        URL url = getClass().getClassLoader().getResource("static/index.html");
        String responseBody = new String(Files.readAllBytes(new File(url.getFile()).toPath()));

        final var response = String.join("\r\n",
                requestLine.getProtocol() + " 200 OK",
                "Content-Type: text/html;charset=utf-8",
                "Content-Length: " + responseBody.getBytes().length,
                "",
                responseBody);

        outputStream.write(response.getBytes());
        outputStream.flush();
    }

    private void responseCss(OutputStream outputStream, RequestLine requestLine) throws IOException {
        URL url = getClass().getClassLoader().getResource("static" + requestLine.getRequestUrl());
        String responseBody = new String(Files.readAllBytes(new File(url.getFile()).toPath()));

        final var response = String.join("\r\n",
                requestLine.getProtocol() + " 200 OK",
                "Content-Type: text/css;charset=utf-8",
                "Content-Length: " + responseBody.getBytes().length,
                "",
                responseBody);

        outputStream.write(response.getBytes());
        outputStream.flush();
    }

    private void responseJs(OutputStream outputStream, RequestLine requestLine) throws IOException {
        URL url = getClass().getClassLoader().getResource("static" + requestLine.getRequestUrl());
        String responseBody = new String(Files.readAllBytes(new File(url.getFile()).toPath()));

        final var response = String.join("\r\n",
                requestLine.getProtocol() + " 200 OK",
                "Content-Type: text/js;charset=utf-8",
                "Content-Length: " + responseBody.getBytes().length,
                "",
                responseBody);

        outputStream.write(response.getBytes());
        outputStream.flush();
    }

    private void responseLogin(OutputStream outputStream, RequestLine requestLine) throws IOException {
        if (requestLine.hasQuestion()) {
            int queryStartIndex = requestLine.getRequestUrl().indexOf("?");
            String queryString = requestLine.getRequestUrl().substring(queryStartIndex + 1);
            String[] pairs = queryString.split("&");
            String[] keyValue = pairs[0].split("=");
            String account = keyValue[1];

            InMemoryUserRepository.findByAccount(account)
                    .ifPresent(user -> log.info(user.toString()));
        }

        URL url = getClass().getClassLoader().getResource("static/login.html");

        String responseBody = new String(Files.readAllBytes(new File(url.getFile()).toPath()));

        final var response = String.join("\r\n",
                requestLine.getProtocol() + " 200 OK",
                "Content-Type: text/html;charset=utf-8",
                "Content-Length: " + responseBody.getBytes().length,
                "",
                responseBody);

        outputStream.write(response.getBytes());
        outputStream.flush();
    }
}
