
package WebSocket;


import Models.EngineModel;
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
    private WebSocket webSocket;
    private String serverURL;
    private String authorizationToken;
    private String engineName;
    private Boolean showEngineName;


    private boolean connected = false;


    public UCIWebSocket(EngineModel model) {
        serverURL = model.address;
        engineName = model.name;
        showEngineName = model.showEngineName;

        String engineAddress = serverURL + "ws_engine";
        String loginAddress = serverURL + "user/login";


        HttpURLConnection loginConnection = getPOSTConnectionForURL(loginAddress);
        JSONObject loginData = getLoginDataAsJSON(model.userName, model.password);
        JSONObject response = processAndGetJSONResponse(loginData, loginConnection);
        authorizationToken = response.getString("token");
        webSocket = openEngineWebSocket(engineAddress);
        this.start();
    }

    public boolean start() {
        String startAddress = serverURL + "engine/start";
        return startOrStopEngine(startAddress);
    }

    public boolean stop() {
        String stopAddress = serverURL + "engine/stop";
        return startOrStopEngine(stopAddress);
    }

    private boolean startOrStopEngine(String address) {
        JSONObject data = new JSONObject();
        data.put("engine", engineName);
        HttpURLConnection engineStartConnection = getPOSTConnectionForURL(address);
        JSONObject response = processAndGetJSONResponse(data, engineStartConnection);
        return (Boolean) response.get("success");
    }

    public void send(String message) {
        webSocket.send(message);
    }

    public void close() {
        webSocket.close(1000, "OK");
    }

    private HttpURLConnection getPOSTConnectionForURL(String url) {
        HttpURLConnection con = null;
        try {
            URL loginURL = new URL(url);
            con = (HttpURLConnection) loginURL.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + authorizationToken);
            con.setRequestMethod("POST");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return con;
    }

    private JSONObject getLoginDataAsJSON(String userName, String password) {
        JSONObject loginData   = new JSONObject();
        loginData.put("login", userName);
        loginData.put("password", password);
        return loginData;
    }

    private JSONObject processAndGetJSONResponse(JSONObject data, HttpURLConnection connection) {
        try {
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(data.toString());
            wr.flush();

            StringBuilder sb = new StringBuilder();
            int HttpResult = connection.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                return new JSONObject(sb.toString());
            } else {
                System.out.println(connection.getResponseMessage());
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

    public boolean isConnected() {
        return connected;
    }


    private final class WebSocketListener extends okhttp3.WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            connected = true;
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            System.out.println(showEngineName ? text + " : " + engineName : text);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            connected = false;
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            connected = false;
            System.out.println("Web socket failed: " + t.getMessage());

        }
    }
}
