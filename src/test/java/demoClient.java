import com.r3944realms.dg_lab.future.manager.DGPBClientManager;
import com.r3944realms.dg_lab.future.websocket.PowerBoxWSClient;
import com.r3944realms.dg_lab.future.websocket.sharedData.ClientPowerBoxSharedData;
import com.r3944realms.dg_lab.websocket.message.role.WebSocketClientRole;

public class demoClient {
    public static void main(String[] args) {
        try {
            Class.forName("io.netty.handler.codec.http.HttpHeaders");
            System.out.println("HttpHeaders class is available");
        } catch (ClassNotFoundException e) {
            System.out.println("HttpHeaders class is not available");
            e.printStackTrace();
        }
//        WebSocketClient.init(DGLabApi.informSupplier, DGLabApi.noticeSupplier, DGLabApi.qrCodeProducer);
//        WebSocketClient.enableDemo();
//        WebSocketClient.Start();
        ClientPowerBoxSharedData sharedData = new ClientPowerBoxSharedData();
        PowerBoxWSClient powerBoxWSClient = new PowerBoxWSClient(sharedData, new WebSocketClientRole("Cl"));
        DGPBClientManager dgpbClientManager = new DGPBClientManager(powerBoxWSClient);
        dgpbClientManager.start();
    }

}
