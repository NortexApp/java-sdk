# sendingnetwork-java-sdk

A Java Client SDK for SendingNetwork.

## Usage
### Add dependency
```groovy
repositories {
    maven {
        url 'https://s01.oss.sonatype.org/content/repositories/releases/'
    }
}

dependencies {
    implementation 'io.github.sending-network:sdn-sdk-java:0.1.0'
}
```

### Prepare a configuration file
Provide server endpoint, wallet address and private key in config.yaml:
```yaml
endpoint: ""
wallet_address: ""
private_key: ""
```

### Create an instance of `Client`
After reading the configuration file, create an instance of `Client`. Register event listener and start syncing.
```java
import org.yaml.snakeyaml.Yaml;
import com.sending.sdk.Client;

Map<String, Object> config = new Yaml().load(new BufferedReader(new FileReader(configFile)));
Client client = new Client((String)config.get("endpoint"));
client.loginDID((String)config.get("wallet_address"), (String)config.get("private_key"));

client.registerRoomEventListener(roomEvents -> {
    // process room events
    roomEvents.forEach(System.out::println);
});
client.startSync();
```

### Call API functions
```java
// create new room
String roomId = client.createRoom(roomName, "", null, System.out::println);

// invite user to the room
String userId = ""
client.inviteUser(roomId, userId, null);

// send room message
String eventId = client.sendMessage(roomId, "hello");

// logout to invalidate access token
client.logout()
```

## Examples
See more use cases in `examples` module.

## License
Apache 2.0