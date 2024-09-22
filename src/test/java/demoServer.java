
import com.r3944realms.dg_lab.future.websocket.PowerBoxWSServer;
import com.r3944realms.dg_lab.future.websocket.sharedData.ServerPowerBoxSharedData;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketServerRole;

public class demoServer {
    public static void main(String[] args) {
        ServerPowerBoxSharedData sharedData = new ServerPowerBoxSharedData();
        PowerBoxWSServer powerBoxWSServer = new PowerBoxWSServer(sharedData, new WebSocketServerRole("IWebsocketServer"));
        powerBoxWSServer.start();
    }
}
