package com.sending.example;

import com.sending.sdk.Client;
import com.sending.sdk.models.LoginData;
import com.sending.sdk.models.Member;
import com.sending.sdk.models.Room;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class Main {
    public static void main(String[] args) throws IOException {
        String configFile = "config.yaml";
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        Map<String, Object> config = yaml.load(new BufferedReader(new FileReader(configFile)));
        Client c = new Client((String)config.get("endpoint"));
        if(config.get("user_id") != null && config.get("access_token") != null && config.get("device_id") != null) {
            c.setLoginData(new LoginData(true,
                    (String)config.get("endpoint"), (String)config.get("user_id"),
                    (String)config.get("access_token"), (String)config.get("device_id")));
        } else {
            LoginData loginData = c.loginDID((String)config.get("wallet_address"), (String)config.get("private_key"));
            if (loginData.isSuccess()) {
                System.out.println("logging in success: " + loginData.getAccess_token());
                config.put("user_id", loginData.getUser_id());
                config.put("access_token", loginData.getAccess_token());
                config.put("device_id", loginData.getDevice_id());
                yaml.dump(config, new BufferedWriter(new FileWriter(configFile)));
            } else {
                System.err.println("error logging in");
            }
        }

        if(!c.isLoggedIn()) {
            return;
        }

        c.registerRoomEventListener(roomEvents -> {
            roomEvents.forEach(System.out::println);
        });
        c.startSync();

        String commands = "Commands:\n" +
                "room list -- List rooms\n" +
                "room create <name> -- Create new room\n" +
                "room invite <roomId> <userId> -- Invite user to room\n" +
                "room join <roomId> -- Join room by id\n" +
                "room kick <roomId> <userId> -- Kick user by userId\n" +
                "room leave <roomId> -- Leave room by id\n" +
                "room members <roomId> -- List room members\n" +
                "room send <roomId> <message text> -- Send a text message\n";
        Scanner sc = new Scanner(System.in);
        while(true) {
            System.out.print("[Enter command]: ");
            String line = sc.nextLine();
            execCommand(c, line.split("\\s"));
        }
    }

    private static boolean execCommand(Client client, String[] cmd) throws IOException {
        if(cmd.length < 2) {
            return false;
        }
        if(!cmd[0].equalsIgnoreCase("room")) {
            return false;
        }

        String action = cmd[1];
        switch (action) {
            case "list":
                List<Room> roomList = client.getRooms();
                if(roomList != null) {
                    roomList.forEach(System.out::println);
                }
                break;
            case "create":
                String roomName = cmd[2];
                String createRoomId = client.createRoom(roomName, "", null);
                System.out.println(createRoomId);
                break;
            case "invite":
                String inviteRoomId = cmd[2];
                String inviteUserId = cmd[3];
                client.inviteUser(inviteRoomId, inviteUserId, null);
                System.out.println("invite success");
                break;
            case "join":
                String joinRoomId = cmd[2];
                client.joinRoom(joinRoomId);
                System.out.println("join success");
                break;
            case "kick":
                String kickRoomId = cmd[2];
                String kickUserId = cmd[3];
                client.kickUser(kickRoomId, kickUserId, "");
                System.out.println("kick success");
                break;
            case "members":
                String roomId = cmd[2];
                List<Member> memberList = client.getRoomMembers(roomId);
                if(memberList != null) {
                    memberList.forEach(System.out::println);
                }
                break;
            case "send":
                String sendRoomId = cmd[2];
                String message = cmd[3];
                String eventId = client.sendMessage(sendRoomId, message);
                System.out.println(eventId);
                break;
            case "leave":
                String leaveRoomId = cmd[2];
                client.leaveRoom(leaveRoomId);
                System.out.println("leave success");
                break;
            default:
                System.out.println("no action");
        }
        return true;
    }
}

