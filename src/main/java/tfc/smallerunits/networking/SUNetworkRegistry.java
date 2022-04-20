package tfc.smallerunits.networking;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import tfc.smallerunits.networking.sync.*;

import java.util.ArrayList;

public class SUNetworkRegistry {
	public static final String networkingVersion = "1.0.0";
	protected static String serverVersion = "";
	public static final SimpleChannel NETWORK_INSTANCE = NetworkRegistry.newSimpleChannel(
			new ResourceLocation("smaller_units", "main"),
			() -> networkingVersion,
			(s) -> compareVersionsClient(networkingVersion, s),
			(s) -> compareVersionsServer(networkingVersion, s)
	);
	
	static {
//		NetworkEntry<?>[] entries = new NetworkEntry[]{
//		};
		ArrayList<NetworkEntry<?>> entries = new ArrayList<>();
		entries.add(new NetworkEntry<>(SyncPacketS2C.class, SyncPacketS2C::new));
		entries.add(new NetworkEntry<>(UpdateStatesS2C.class, UpdateStatesS2C::new));
		entries.add(new NetworkEntry<>(RemoveUnitPacket.class, RemoveUnitPacket::new));
		entries.add(new NetworkEntry<>(SpawningBlockEntitiesS2C.class, SpawningBlockEntitiesS2C::new));
		entries.add(new NetworkEntry<>(DeleteBlockEntityS2C.class, DeleteBlockEntityS2C::new));
		entries.add(new NetworkEntry<>(SpawnEntityPacketS2C.class, SpawnEntityPacketS2C::new));
		entries.add(new NetworkEntry<>(SyncEntityPacketS2C.class, SyncEntityPacketS2C::new));
		entries.add(new NetworkEntry<>(RemoveEntityPacketS2C.class, RemoveEntityPacketS2C::new));
		
		for (int i = 0; i < entries.size(); i++) entries.get(i).register(i, NETWORK_INSTANCE);
//		NETWORK_INSTANCE.registerMessage(
//				0,
//				SyncPacketS2C.class,
//				Packet::write,
//				SyncPacketS2C::new,
//				(pkt, ctx) -> pkt.handle(ctx.get())
//		);
//		NETWORK_INSTANCE.registerMessage(1, UpdateStatesS2C.class, Packet::write, UpdateStatesS2C::new, (pkt, ctx) -> pkt.handle(ctx.get()));
	}
	
	public static void init() {
		// nothing to do
	}
	
	public static boolean compareVersionsServer(String str0, String str1) {
		if (str1.contains("compat")) return true;
		str0 = str0.split("compat")[0];
		str1 = str0.split("compat")[0];
		String[] serverVer = parseVersion(str0);
		String[] clientVer = parseVersion(str1);
		serverVer = addPlaceholders(serverVer, clientVer);
		clientVer = addPlaceholders(clientVer, serverVer);
		serverVersion = str0;
		
		if (serverVer.length == 0 || clientVer.length == 0) return false;
		if (!serverVer[0].equals(clientVer[0])) return false;
		
		if (serverVer.length < 2 || clientVer.length < 2) return false;
		// server uses newer server sub than client
		// client is allowed
		if (Integer.parseInt(clientVer[1]) >= Integer.parseInt(serverVer[1])) return true;
		if (serverVer.length > 2 && clientVer.length > 2) {
			// server uses older client version than client
			// client is allowed
			if (Integer.parseInt(clientVer[2]) <= Integer.parseInt(serverVer[2])) return false;
		} else {
			// client does not have sub but server does
			// client uses older client networking version
			// client is allowed
			if (serverVer.length > clientVer.length) return false;
		}
		return false;
	}
	
	public static boolean compareVersionsClient(String str0, String str1) {
		if (str0.contains("compat")) return true;
		str0 = str0.split("compat")[0];
		str1 = str0.split("compat")[0];
		String[] clientVer = parseVersion(str0);
		String[] serverVer = parseVersion(str1);
		clientVer = addPlaceholders(clientVer, serverVer);
		serverVer = addPlaceholders(serverVer, clientVer);
		serverVersion = str1;
		
		if (clientVer.length == 0 || serverVer.length == 0) return false;
		if (!clientVer[0].equals(serverVer[0])) return false;
		
		if (clientVer.length < 2 || serverVer.length < 2) return false;
		if (Integer.parseInt(serverVer[1]) <= Integer.parseInt(clientVer[1])) return true;
		if (clientVer.length > 2 && serverVer.length > 2) {
			if (Integer.parseInt(serverVer[2]) >= Integer.parseInt(clientVer[2])) return false;
		} else {
			if (clientVer.length > serverVer.length) return false;
		}
		return false;
	}
	
	public static String[] parseVersion(String input) {
		if (input.contains(".")) {
			return input.split("\\.");
		}
		return new String[]{input};
	}
	
	public static String[] addPlaceholders(String[] ver0, String[] ver1) {
		int len = Math.max(ver0.length, ver1.length);
		String[] strs = new String[len];
		for (int i = 0; i < len; i++) {
			if (i < ver0.length) {
				strs[i] = ver0[i];
			} else {
				strs[i] = "0";
			}
		}
		return strs;
	}
}
