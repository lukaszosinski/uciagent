
package WebSocket;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class UCIWebSocket {
    private WebSocket webSocket = null;
    private String authorizationToken = null;
    private HttpURLConnection httpLoginConnection = null;
    private boolean connected = false;



    public UCIWebSocket(String url) {
        String engineAddress = new String(url + "ws_engine");
        String loginAddress = new String(url + "user/login");

        httpLoginConnection = getLoginConnection(loginAddress);
        JSONObject loginData = getLoginDataAsJSON();
        authorizationToken = loginAndGetAuthToken(loginData);
        webSocket = openEngineWebSocket(engineAddress);
    }

    public void send(String message) {
        webSocket.send(message);
    }

    private HttpURLConnection getLoginConnection(String loginAdress) {
        HttpURLConnection con = null;
        try {
            URL loginURL = new URL(loginAdress);
            con = (HttpURLConnection) loginURL.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestMethod("POST");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return con;
    }

    private JSONObject getLoginDataAsJSON() {
        JSONObject loginData   = new JSONObject();
        loginData.put("login", "test");
        loginData.put("password", "lukasz");
        return loginData;
    }

    private String loginAndGetAuthToken(JSONObject loginData) {
        try {
            OutputStreamWriter wr = new OutputStreamWriter(httpLoginConnection.getOutputStream());
            wr.write(loginData.toString());
            wr.flush();

            StringBuilder sb = new StringBuilder();
            int HttpResult = httpLoginConnection.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(httpLoginConnection.getInputStream(), "utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                JSONObject JSONResponse = new JSONObject(sb.toString());
                authorizationToken = JSONResponse.getString("token");
                return authorizationToken;
            } else {
                System.out.println(httpLoginConnection.getResponseMessage());
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private WebSocket openEngineWebSocket(String engineAddress) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + authorizationToken)
                .url(engineAddress)
                .build();
        return client.newWebSocket(request, new WebSocketListener());
    }



    private final class WebSocketListener extends okhttp3.WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            connected = true;
            System.out.println("Web socket opened");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            System.out.println("Message received " + text);
            //EventBus.getDefault().post(new MessageReceivedEvent(text));
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            connected = false;
            System.out.println("Web socket closed reason: " + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            connected = false;
            System.out.println("Web socket failed: " + t.getMessage());

        }
    }
}
