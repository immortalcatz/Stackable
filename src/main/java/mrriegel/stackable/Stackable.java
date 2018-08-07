package mrriegel.stackable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;

import com.google.common.collect.ImmutableBiMap;

import mrriegel.stackable.block.BlockAll;
import mrriegel.stackable.block.BlockIngots;
import mrriegel.stackable.client.ClientUtils;
import mrriegel.stackable.message.MessageConfigSync;
import mrriegel.stackable.message.MessagePlaceKey;
import mrriegel.stackable.tile.TileAll;
import mrriegel.stackable.tile.TileIngots;
import net.minecraft.block.Block;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Stackable.MODID, name = Stackable.NAME, version = Stackable.VERSION, acceptedMinecraftVersions = "[1.12,1.13)")
@EventBusSubscriber
public class Stackable {

	@Instance(Stackable.MODID)
	public static Stackable INSTANCE;

	public static final String VERSION = "1.0.0";
	public static final String NAME = "Stackable";
	public static final String MODID = "stackable";

	//config
	public static int itemsPerIngot, perX, perY, perZ, overlay, maxPileHeight, allSize = 4;
	public static boolean useBlockTexture, useCompressedTexture;
	public static Set<ResourceLocation> allowedIngots;

	public static SimpleNetworkWrapper snw;

	public static final Block ingots = new BlockIngots();
	public static final Block all = new BlockAll();

	public static final KeyBinding PLACE_KEY = new KeyBinding("key.stackable.place", KeyConflictContext.IN_GAME, Keyboard.KEY_P, MODID);

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		itemsPerIngot = config.getInt("itemsPerIngot", Configuration.CATEGORY_GENERAL, 4, 1, 64, "Items per visual ingot");
		String name = Configuration.CATEGORY_GENERAL + ".ingotsPerBlock";
		perX = config.getInt("x", name, 6, 1, 24, "");
		perY = config.getInt("y", name, 8, 1, 24, "");
		perZ = config.getInt("z", name, 2, 1, 16, "");
		useBlockTexture = config.getBoolean("useBlockTexture", Configuration.CATEGORY_CLIENT, true, "Use textures from blocks for ingots (e.g. iron block texture for iron ingot).");
		useCompressedTexture = config.getBoolean("useCompressedTexture", Configuration.CATEGORY_CLIENT, true, "Use compressed textures.");
		allowedIngots = Arrays.stream(config.getStringList("allowedIngots", Configuration.CATEGORY_GENERAL, new String[] {}, "Items that are allowed to be added to the ingot block as well. (Notation: MODID:ITEMNAME)")).map(ResourceLocation::new).collect(Collectors.toSet());
		overlay = config.getInt("overlay", Configuration.CATEGORY_CLIENT, 1, 0, 2, "0 - Overlay not visible" + Configuration.NEW_LINE + "1 - Overlay visible while sneaking" + Configuration.NEW_LINE + "2 - Overlay always visible");
		maxPileHeight = config.getInt("maxPileHeight", Configuration.CATEGORY_GENERAL, 30, 1, 512, "Maximum pile height.");
		if (config.hasChanged())
			config.save();
		generateConstants();
		snw = new SimpleNetworkWrapper(MODID);
		snw.registerMessage(MessageConfigSync.class, MessageConfigSync.class, 0, Side.CLIENT);
		snw.registerMessage(MessagePlaceKey.class, MessagePlaceKey.class, 1, Side.SERVER);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			ClientUtils.init();
		}
		//TODO
		//block lights if item lights
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

	@SubscribeEvent
	public static void register(@SuppressWarnings("rawtypes") RegistryEvent.Register event) {
		if (event.getGenericType() == Block.class) {
			event.getRegistry().register(ingots);
			GameRegistry.registerTileEntity(TileIngots.class, ingots.getRegistryName().toString());
			event.getRegistry().register(all);
			GameRegistry.registerTileEntity(TileAll.class, all.getRegistryName().toString());
		}
	}

	@SubscribeEvent
	public static void join(EntityJoinWorldEvent event) {
		if (event.getEntity() instanceof EntityPlayerMP /*&& event.getEntity().getServer().isDedicatedServer()*/) {
			MessageConfigSync p = new MessageConfigSync();
			p.nbt.setInteger("a", Stackable.itemsPerIngot);
			p.nbt.setInteger("x", Stackable.perX);
			p.nbt.setInteger("y", Stackable.perY);
			p.nbt.setInteger("z", Stackable.perZ);
			snw.sendTo(p, (EntityPlayerMP) event.getEntity());
		}
	}

	public static void generateConstants() {
		TileIngots.maxIngotAmount = Stackable.perX * Stackable.perY * Stackable.perZ;
		TileIngots.coordMap = ImmutableBiMap.<Integer, Vec3i> builder().putAll(Stream.of((Object) null).flatMap(n -> {
			List<Pair<Integer, Vec3i>> l = new ArrayList<>();
			int count = 0;
			for (int y = 0; y < Stackable.perY; y++) {
				for (int z = 0; z < Stackable.perZ; z++) {
					for (int x = 0; x < Stackable.perX; x++) {
						l.add(Pair.of(count, new Vec3i(x, y, z)));
						count++;
					}
				}
			}
			return l.stream();
		}).collect(Collectors.toList())).build();
		TileAll.maxItemAmount = Stackable.allSize * Stackable.allSize * Stackable.allSize;
		TileAll.coordMap = ImmutableBiMap.<Integer, Vec3i> builder().putAll(Stream.of((Object) null).flatMap(n -> {
			List<Pair<Integer, Vec3i>> l = new ArrayList<>();
			int count = 0;
			for (int y = 0; y < Stackable.allSize; y++) {
				for (int z = 0; z < Stackable.allSize; z++) {
					for (int x = 0; x < Stackable.allSize; x++) {
						l.add(Pair.of(count, new Vec3i(x, y, z)));
						count++;
					}
				}
			}
			return l.stream();
		}).collect(Collectors.toList())).build();
	}

	//TODO waila support (disable overlay when waila is loaded)
	//override getDrops

}
