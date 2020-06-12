package com.example.skillboxcryptochat;

import android.util.Log;
import android.util.Pair;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Server {
    //35.214.3.133:8881
    Map<Long, String> names = new ConcurrentHashMap<>();
    WebSocketClient client;
    private Consumer<Pair<String, String>> onMessageReceived;

    public Server(Consumer<Pair<String, String>> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void connect() {
        URI address;
        try {
//            address = new URI("ws://35.214.3.133:8881");
            address = new URI("ws://138.197.189.159:8881");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        client = new WebSocketClient(address) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i("SERVER", "Connection to server is open");
                String myName = Protocol.packName(new Protocol.UserName("Мишаня"));
                Log.i("SERVER", "Sending my name to server: " + myName);
                client.send(myName);

                Protocol.Message m = new Protocol.Message("Всем приветы");
                m.setReceiver(Protocol.GROUP_CHAT);
                String packedMessage = Protocol.packMessage(m);

                Log.i("SERVER", "Sending message: " + packedMessage);
                client.send(packedMessage);



                // 1 - статус пользователя (оффлайн или онлайн)
                // 2 - текстовое сообщение
                // 3 - имя пользователя

                //При подключении

            }

            @Override
            public void onMessage(String message) {

                //При поступлении "сообщения" с сервера
                Log.i("SERVER", "Got message from server: " + message);
                int type = Protocol.getType(message);

                if (type == Protocol.USER_STATUS){
                    // обработать факт подключения или отключения пользователя
                    userStatusChanged(message);
                }

                if (type == Protocol.MESSAGE){
                    // показать сообщение на экране
                    displayIncomingMessage(message);
                }

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {

                Log.i("SERVER", "Connection closed");

            }

            @Override
            public void onError(Exception ex) {

                Log.i("SERVER", "ERROR occured: " + ex.getMessage());

            }
        };

        client.connect();
    }



    private void displayIncomingMessage(String json){
        Protocol.Message m = Protocol.unpackMessage(json);
        String name = names.get(m.getSender());
        if (name == null){
            name = "Безымянный";
        }
        m.getEncodedText(); // текст
        m.getSender(); // кто отправитель
        onMessageReceived.accept(
                new Pair<String, String>(name, m.getEncodedText())
        );
    }

    private void userStatusChanged(String json) {
        Protocol.UserStatus s = Protocol.unpackStatus(json);
        Protocol.User user = s.getUser();
        if(s.isConnected()){
            names.put(user.getId(), user.getName());
        }else {
            names.remove(user.getId());

        }
    }

}
