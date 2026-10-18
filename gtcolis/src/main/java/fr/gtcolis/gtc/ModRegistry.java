package fr.gtcolis.gtc;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GtColis.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GtColis.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GtColis.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GtColis.MODID);

    // ---- Entites
    public static final RegistryObject<EntityType<DouanierEntity>> DOUANIER = ENTITIES.register("douanier",
            () -> EntityType.Builder.<DouanierEntity>of(DouanierEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(10).build("douanier"));
    public static final RegistryObject<EntityType<ContactEntity>> CONTACT = ENTITIES.register("contact",
            () -> EntityType.Builder.<ContactEntity>of(ContactEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("contact"));
    public static final RegistryObject<EntityType<MerchantEntity>> MARCHAND = ENTITIES.register("marchand",
            () -> EntityType.Builder.<MerchantEntity>of(MerchantEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("marchand"));
    public static final RegistryObject<EntityType<VoitureEntity>> VOITURE = ENTITIES.register("voiture",
            () -> EntityType.Builder.<VoitureEntity>of(VoitureEntity::new, MobCategory.MISC).sized(1.9F, 1.5F).clientTrackingRange(10).build("voiture"));

    // ---- Blocs
    private static RegistryObject<Block> blk(String n, Supplier<Block> s) {
        RegistryObject<Block> b = BLOCKS.register(n, s);
        ITEMS.register(n, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }

    public static final RegistryObject<Block> ASPHALTE = blk("asphalte", () -> new Block(BlockBehaviour.Properties.of().strength(2F, 8F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> TROTTOIR = blk("trottoir", () -> new Block(BlockBehaviour.Properties.of().strength(2F, 8F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BAT_ROSE = blk("bat_rose", () -> new Block(BlockBehaviour.Properties.of().strength(3F, 12F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BAT_VIOLET = blk("bat_violet", () -> new Block(BlockBehaviour.Properties.of().strength(3F, 12F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BAT_CYAN = blk("bat_cyan", () -> new Block(BlockBehaviour.Properties.of().strength(3F, 12F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BAT_ORANGE = blk("bat_orange", () -> new Block(BlockBehaviour.Properties.of().strength(3F, 12F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BAT_LIME = blk("bat_lime", () -> new Block(BlockBehaviour.Properties.of().strength(3F, 12F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> POINT_RELAIS = blk("point_relais", () -> new PointRelaisBlock(BlockBehaviour.Properties.of().strength(4F, 20F).lightLevel(s -> 10)));
    public static final RegistryObject<Block> PISTE_DANSE = blk("piste_danse", () -> new Block(BlockBehaviour.Properties.of().strength(2F, 8F).lightLevel(s -> 13)));

    public static Block[] facades() {
        return new Block[]{BAT_ROSE.get(), BAT_VIOLET.get(), BAT_CYAN.get(), BAT_ORANGE.get(), BAT_LIME.get()};
    }

    // ---- Objets
    public static final RegistryObject<Item> COLIS = ITEMS.register("colis", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MUNITIONS = ITEMS.register("munitions", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PORTEFEUILLE = ITEMS.register("portefeuille", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TELEPHONE = ITEMS.register("telephone", () -> new PhoneItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CLE_VOITURE = ITEMS.register("cle_voiture", () -> new CarKeyItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BATTE = ITEMS.register("batte", () -> new BatItem(new Item.Properties().stacksTo(1).durability(500)));
    public static final RegistryObject<Item> PISTOLET = ITEMS.register("pistolet", () -> new GunItem(new Item.Properties().stacksTo(1), 7F, 60, 8, 1, 0.01F, false, 1.7F));
    public static final RegistryObject<Item> FUSIL = ITEMS.register("fusil", () -> new GunItem(new Item.Properties().stacksTo(1), 4F, 22, 24, 6, 0.07F, false, 0.8F));
    public static final RegistryObject<Item> MITRAILLETTE = ITEMS.register("mitraillette", () -> new GunItem(new Item.Properties().stacksTo(1), 3F, 48, 3, 1, 0.03F, true, 2.0F));

    public static final RegistryObject<Item> EGG_DOUANIER = ITEMS.register("douanier_spawn_egg", () -> new ForgeSpawnEggItem(DOUANIER, 0x1F4FBF, 0xDCC83C, new Item.Properties()));
    public static final RegistryObject<Item> EGG_CONTACT = ITEMS.register("contact_spawn_egg", () -> new ForgeSpawnEggItem(CONTACT, 0x783C96, 0xFFDC00, new Item.Properties()));
    public static final RegistryObject<Item> EGG_MARCHAND = ITEMS.register("marchand_spawn_egg", () -> new ForgeSpawnEggItem(MARCHAND, 0x3C7846, 0xF0F0F0, new Item.Properties()));
    public static final RegistryObject<Item> EGG_VOITURE = ITEMS.register("voiture_spawn_egg", () -> new ForgeSpawnEggItem(VOITURE, 0xC82828, 0x1E2837, new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.gtcolis"))
            .icon(() -> new ItemStack(COLIS.get()))
            .displayItems((params, out) -> ITEMS.getEntries().forEach(i -> out.accept(i.get())))
            .build());

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
