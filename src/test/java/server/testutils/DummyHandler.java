package server.testutils;

import com.sun.net.httpserver.HttpExchange;
import server.BaseHandler;

public class DummyHandler extends BaseHandler {

    @Override
    public void handle(HttpExchange exchange) {}
}