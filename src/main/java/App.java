import Models.EngineModel;
import WebSocket.UCIWebSocket;
import com.google.gson.Gson;


import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class App {
    private static List<UCIWebSocket> engines = new ArrayList<UCIWebSocket>();

    public static void main(String[] args) {
        EngineModel[] enginesModels = readConfigFile("enginesConfig.json");
        createWebsocketsAndStartEngines(enginesModels);
        listen();
        stopEnginesAndCloseWebsockets();
    }

    private static EngineModel[] readConfigFile(String path) {
        File file = new File(path);
        final Gson GSON = new Gson();
        EngineModel models[] = null;
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
            byte data[] = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String string = new String(data, "UTF-8");
            models = GSON.getAdapter(EngineModel[].class).fromJson(string);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return models;
    }

    private static void createWebsocketsAndStartEngines(EngineModel[] enginesModels) {
        for (EngineModel engineModel : enginesModels) {
            engines.add(new UCIWebSocket(engineModel));
        }
    }

    private static void listen() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line = "";
        while(!line.equals("stop")) {
            try {
                line = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sendToAllEngines(line);
        }
    }

    private static void sendToAllEngines(String line) {
        for (UCIWebSocket engine : engines) {
            engine.send(line);
        }
    }

    private static void stopEnginesAndCloseWebsockets() {
        for (UCIWebSocket engine : engines) {
            engine.stop();
            engine.close();
        }
    }


}

