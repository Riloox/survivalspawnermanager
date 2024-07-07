package org.possess.spawnerproject;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

// by riloox 07/07/24
//to-do customizable through config file
public final class SpawnerProject extends JavaPlugin implements Listener {

    private static final String SPAWNER_GUI_TITLE = "Select Mob to Spawn";
    private static final String EQUIPMENT_GUI_TITLE = "Select Equipment";
    private static final Map<Material, Integer> EQUIPMENT_COSTS = new HashMap<>();
    private static final Map<Block, Map<EquipmentSlot, ItemStack>> spawnerEquipments = new HashMap<>();

    static {
        EQUIPMENT_COSTS.put(Material.LEATHER_HELMET, 5);
        EQUIPMENT_COSTS.put(Material.LEATHER_CHESTPLATE, 10);
        EQUIPMENT_COSTS.put(Material.LEATHER_LEGGINGS, 8);
        EQUIPMENT_COSTS.put(Material.LEATHER_BOOTS, 5);
        EQUIPMENT_COSTS.put(Material.CHAINMAIL_HELMET, 10);
        EQUIPMENT_COSTS.put(Material.CHAINMAIL_CHESTPLATE, 20);
        EQUIPMENT_COSTS.put(Material.CHAINMAIL_LEGGINGS, 15);
        EQUIPMENT_COSTS.put(Material.CHAINMAIL_BOOTS, 10);
        EQUIPMENT_COSTS.put(Material.IRON_HELMET, 15);
        EQUIPMENT_COSTS.put(Material.IRON_CHESTPLATE, 30);
        EQUIPMENT_COSTS.put(Material.IRON_LEGGINGS, 20);
        EQUIPMENT_COSTS.put(Material.IRON_BOOTS, 15);
        EQUIPMENT_COSTS.put(Material.DIAMOND_HELMET, 30);
        EQUIPMENT_COSTS.put(Material.DIAMOND_CHESTPLATE, 50);
        EQUIPMENT_COSTS.put(Material.DIAMOND_LEGGINGS, 40);
        EQUIPMENT_COSTS.put(Material.DIAMOND_BOOTS, 30);
        EQUIPMENT_COSTS.put(Material.NETHERITE_HELMET, 50);
        EQUIPMENT_COSTS.put(Material.NETHERITE_CHESTPLATE, 80);
        EQUIPMENT_COSTS.put(Material.NETHERITE_LEGGINGS, 60);
        EQUIPMENT_COSTS.put(Material.NETHERITE_BOOTS, 50);
        EQUIPMENT_COSTS.put(Material.WOODEN_SWORD, 5);
        EQUIPMENT_COSTS.put(Material.STONE_SWORD, 10);
        EQUIPMENT_COSTS.put(Material.IRON_SWORD, 20);
        EQUIPMENT_COSTS.put(Material.DIAMOND_SWORD, 40);
        EQUIPMENT_COSTS.put(Material.NETHERITE_SWORD, 60);
    }

    private Block selectedSpawner;
    private EntityType selectedType;

    @Override
    public void onEnable() {
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() == Material.SPAWNER) {
            Player player = event.getPlayer();
            ItemStack[] equipment = player.getEquipment().getArmorContents();
            ItemStack handItem = player.getEquipment().getItemInMainHand();

            Map<EquipmentSlot, ItemStack> equipmentMap = new EnumMap<>(EquipmentSlot.class);
            equipmentMap.put(EquipmentSlot.HEAD, equipment[3]); // Helmet
            equipmentMap.put(EquipmentSlot.CHEST, equipment[2]); // Chestplate
            equipmentMap.put(EquipmentSlot.LEGS, equipment[1]); // Leggings
            equipmentMap.put(EquipmentSlot.FEET, equipment[0]); // Boots
            equipmentMap.put(EquipmentSlot.HAND, handItem); // Main hand item

            spawnerEquipments.put(block, equipmentMap);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.SPAWNER) {
            ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
            if (itemInHand != null && (itemInHand.getType() == Material.IRON_PICKAXE ||
                    itemInHand.getType() == Material.DIAMOND_PICKAXE ||
                    itemInHand.getType() == Material.NETHERITE_PICKAXE) &&
                    itemInHand.containsEnchantment(Enchantment.SILK_TOUCH)) {
                // Cancel the default block break action
                event.setCancelled(true);
                // Remove the spawner block
                event.getBlock().setType(Material.AIR);
                // Drop the spawner item
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.SPAWNER));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.SPAWNER) {
            ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.AIR) {
                event.setCancelled(true);
                selectedSpawner = block;
                openSpawnerGUI(event.getPlayer());
            } else {
                event.getPlayer().sendMessage("You must have an empty hand to interact with the spawner.");
            }
        }
    }

    private void openSpawnerGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, SPAWNER_GUI_TITLE);

        // Add hostile mobs except bosses and mini-bosses
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && type.isSpawnable() && !isBoss(type)) {
                Material spawnEgg = getSpawnEggMaterial(type);
                if (spawnEgg != null) {
                    ItemStack item = new ItemStack(spawnEgg, 1);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("§b" + toReadableName(type.name()));
                    item.setItemMeta(meta);
                    gui.addItem(item);
                }
            }
        }

        player.openInventory(gui);
    }

    private boolean isBoss(EntityType type) {
        return type == EntityType.WITHER || type == EntityType.ENDER_DRAGON;
    }

    private Material getSpawnEggMaterial(EntityType type) {
        switch (type) {
            case ZOMBIE:
                return Material.ZOMBIE_SPAWN_EGG;
            case SKELETON:
                return Material.SKELETON_SPAWN_EGG;
            case SPIDER:
                return Material.SPIDER_SPAWN_EGG;
            case CREEPER:
                return Material.CREEPER_SPAWN_EGG;
            case ENDERMAN:
                return Material.ENDERMAN_SPAWN_EGG;
            case WITCH:
                return Material.WITCH_SPAWN_EGG;
            case VINDICATOR:
                return Material.VINDICATOR_SPAWN_EGG;
            // Add other cases as needed
            default:
                return null;
        }
    }

    private void openEquipmentGUI(Player player, EntityType entityType) {
        if (!canEquipArmor(entityType)) {
            player.sendMessage("This mob type cannot equip armor or weapons.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, EQUIPMENT_GUI_TITLE);

        Material[] orderedEquipment = {
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
        };

        for (Material material : orderedEquipment) {
            if (EQUIPMENT_COSTS.containsKey(material)) {
                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§b" + toReadableName(material.name()) + " (" + EQUIPMENT_COSTS.get(material) + " levels)");
                item.setItemMeta(meta);
                gui.addItem(item);
            }
        }

        player.openInventory(gui);
    }

    private boolean canEquipArmor(EntityType entityType) {
        switch (entityType) {
            case ZOMBIE:
            case SKELETON:
            case VINDICATOR:
                return true;
            default:
                return false;
        }
    }

    private String toReadableName(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder readableName = new StringBuilder();
        for (String part : parts) {
            readableName.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return readableName.toString().trim();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(SPAWNER_GUI_TITLE)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                Player player = (Player) event.getWhoClicked();
                Material spawnEggMaterial = clickedItem.getType();

                selectedType = getEntityTypeFromSpawnEgg(spawnEggMaterial);
                if (selectedType != null) {
                    setSpawnerType(selectedSpawner, selectedType);
                    player.closeInventory();
                    openEquipmentGUI(player, selectedType);
                }
            }
        } else if (event.getView().getTitle().equals(EQUIPMENT_GUI_TITLE)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                Player player = (Player) event.getWhoClicked();
                int cost = EQUIPMENT_COSTS.getOrDefault(clickedItem.getType(), 0);

                if (player.getLevel() >= cost) {
                    player.setLevel(player.getLevel() - cost);

                    // Apply equipment to the spawner
                    Map<EquipmentSlot, ItemStack> equipmentMap = spawnerEquipments.get(selectedSpawner);
                    if (equipmentMap == null) {
                        equipmentMap = new EnumMap<>(EquipmentSlot.class);
                        spawnerEquipments.put(selectedSpawner, equipmentMap);
                    }
                    equipmentMap.put(getEquipmentSlot(clickedItem.getType()), clickedItem);

                    player.sendMessage("Equipped " + toReadableName(clickedItem.getType().name()) + " to the spawner.");
                } else {
                    player.sendMessage("You do not have enough levels to purchase this item.");
                }
            }
        }
    }

    private EntityType getEntityTypeFromSpawnEgg(Material spawnEggMaterial) {
        switch (spawnEggMaterial) {
            case ZOMBIE_SPAWN_EGG:
                return EntityType.ZOMBIE;
            case SKELETON_SPAWN_EGG:
                return EntityType.SKELETON;
            case SPIDER_SPAWN_EGG:
                return EntityType.SPIDER;
            case CREEPER_SPAWN_EGG:
                return EntityType.CREEPER;
            case ENDERMAN_SPAWN_EGG:
                return EntityType.ENDERMAN;
            case WITCH_SPAWN_EGG:
                return EntityType.WITCH;
            case VINDICATOR_SPAWN_EGG:
                return EntityType.VINDICATOR;
            // Add other cases as needed
            default:
                return null;
        }
    }

    private void setSpawnerType(Block spawnerBlock, EntityType type) {
        if (spawnerBlock.getType() == Material.SPAWNER) {
            CreatureSpawner spawner = (CreatureSpawner) spawnerBlock.getState();
            spawner.setSpawnedType(type);
            spawner.update();
        }
    }

    private EquipmentSlot getEquipmentSlot(Material material) {
        switch (material) {
            case LEATHER_HELMET:
            case CHAINMAIL_HELMET:
            case IRON_HELMET:
            case DIAMOND_HELMET:
            case NETHERITE_HELMET:
                return EquipmentSlot.HEAD;
            case LEATHER_CHESTPLATE:
            case CHAINMAIL_CHESTPLATE:
            case IRON_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
            case NETHERITE_CHESTPLATE:
                return EquipmentSlot.CHEST;
            case LEATHER_LEGGINGS:
            case CHAINMAIL_LEGGINGS:
            case IRON_LEGGINGS:
            case DIAMOND_LEGGINGS:
            case NETHERITE_LEGGINGS:
                return EquipmentSlot.LEGS;
            case LEATHER_BOOTS:
            case CHAINMAIL_BOOTS:
            case IRON_BOOTS:
            case DIAMOND_BOOTS:
            case NETHERITE_BOOTS:
                return EquipmentSlot.FEET;
            case WOODEN_SWORD:
            case STONE_SWORD:
            case IRON_SWORD:
            case DIAMOND_SWORD:
            case NETHERITE_SWORD:
                return EquipmentSlot.HAND;
            default:
                return null;
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG ||
                event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {

            // Find the nearest spawner block within a larger area
            Block spawnerBlock = null;
            Location location = event.getLocation();

            // Load nearby chunks to ensure the spawner block is loaded
            Chunk chunk = location.getChunk();
            chunk.load();

            int searchRadius = 5; // Increase the search radius to 2

            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int y = -searchRadius; y <= searchRadius; y++) {
                    for (int z = -searchRadius; z <= searchRadius; z++) {
                        Block block = location.clone().add(x, y, z).getBlock();
                        if (block.getType() == Material.SPAWNER) {
                            spawnerBlock = block;
                            break;
                        }
                    }
                    if (spawnerBlock != null) break;
                }
                if (spawnerBlock != null) break;
            }

            if (spawnerBlock != null) {
                Bukkit.getLogger().info("Spawner detected at " + spawnerBlock.getLocation().toString());

                if (spawnerEquipments.containsKey(spawnerBlock)) {
                    Map<EquipmentSlot, ItemStack> equipment = spawnerEquipments.get(spawnerBlock);



                    if (equipment != null) {
                        Entity entity = event.getEntity();
                        if (entity instanceof LivingEntity) {
                            LivingEntity livingEntity = (LivingEntity) entity;

                            for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
                                Bukkit.getLogger().info("Applying " + entry.getKey() + " equipment: " + entry.getValue());

                                switch (entry.getKey()) {
                                    case HEAD:
                                        livingEntity.getEquipment().setHelmet(entry.getValue());
                                        break;
                                    case CHEST:
                                        livingEntity.getEquipment().setChestplate(entry.getValue());
                                        break;
                                    case LEGS:
                                        livingEntity.getEquipment().setLeggings(entry.getValue());
                                        break;
                                    case FEET:
                                        livingEntity.getEquipment().setBoots(entry.getValue());
                                        break;
                                    case HAND:
                                        livingEntity.getEquipment().setItemInMainHand(entry.getValue());
                                        break;
                                    default:
                                        break;
                                }
                            }
                            Bukkit.getLogger().info("Applied all equipment to entity: " + livingEntity);
                        } else {
                            Bukkit.getLogger().warning("Entity spawned is not a LivingEntity.");
                        }
                    } else {
                        Bukkit.getLogger().warning("Equipment map is null for spawner at " + spawnerBlock.getLocation().toString());
                    }
                } else {
                    Bukkit.getLogger().warning("No equipment map found for spawner at " + spawnerBlock.getLocation().toString());
                }
            } else {
                Bukkit.getLogger().warning("No spawner block found in the vicinity of " + location.toString());
            }
        }
    }

}
