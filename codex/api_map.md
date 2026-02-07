# HytaleServer Decompiled Index (Curated)

Source: libs/HytaleServer/com (IntelliJ decompiler output; class files only)
Scope: RPG overhaul modding focus (entities/combat, stats, items, NPC/AI, world, spawning, worldgen)

## Top-Level Package Map (com.hypixel.hytale.*)
- com.hypixel.hytale.assetstore: asset store integration and asset pack handling
- com.hypixel.hytale.builtin: built-in server features and defaults
- com.hypixel.hytale.codec: codecs and serialization helpers (Codec, KeyedCodec, RawJsonCodec)
- com.hypixel.hytale.common: shared utilities (collections, semver, threading, tuples)
- com.hypixel.hytale.component: ECS-style components, systems, queries, resources
- com.hypixel.hytale.event: event bus and event registry system
- com.hypixel.hytale.function: generic functional helpers
- com.hypixel.hytale.logger: logging utilities
- com.hypixel.hytale.math: math primitives/utilities
- com.hypixel.hytale.metrics: metrics registry and JVM metrics
- com.hypixel.hytale.plugin: early plugin loading and class transformation
- com.hypixel.hytale.procedurallib: procedural logic, conditions, random, properties
- com.hypixel.hytale.protocol: networking IO + packet definitions
- com.hypixel.hytale.registry: base Registry/Registration types
- com.hypixel.hytale.server: server runtime packages (core, npc, spawning, worldgen, etc.)
- com.hypixel.hytale.sneakythrow: exception utilities
- com.hypixel.hytale.storage: indexed storage file formats
- com.hypixel.hytale.unsafe: unsafe helpers

## Core Systems (RPG-Relevant)

### ECS / Systems
Note: Entity is only a unique ID number. Behaviors come from Components attached to the ID. 
- Package: com.hypixel.hytale.component
- Purpose: ECS-style model (Component, Resource, SystemGroup, queries, registries)
- Common classes: Component, ComponentType, Resource, SystemGroup, ReadWriteQuery, Store
- Key interfaces: IComponentRegistry, IResourceStorage

### Events / Extension Hooks
- Package: com.hypixel.hytale.event
- Purpose: event bus, dispatch, registration, priorities
- Common classes: EventBus, EventRegistry, EventRegistration, EventPriority
- Key interfaces: IEvent, IEventBus, IEventRegistry, IEventDispatcher, ICancellable

### Server Core (Foundation)
- Package: com.hypixel.hytale.server.core
- Purpose: server-side gameplay systems and utilities
- Common classes: ServerManager, PluginManager, PermissionsModule, TaskRegistry

#### Entities / Combat / Stats
  Note: Entity is the container; logic should reside in System classes acting on Component data found in the EntityStore.
- Package: com.hypixel.hytale.server.core.entity
- Purpose: entity base, interactions, stats, combat
- Common classes: Entity, LivingEntity, InteractionManager, InteractionContext, StatModifiersManager, ItemUtils
- Subpackages:
  - damage: DamageDataComponent, DamageDataSetupSystem
  - effect: ActiveEntityEffect, EffectControllerComponent
  - entities: Player, BlockEntity, ProjectileComponent
  - movement: MovementStatesComponent, MovementStatesSystems
  - knockback: KnockbackComponent, KnockbackSystems
  - nameplate: Nameplate, NameplateSystems

#### Inventory / Items / Transactions
- Package: com.hypixel.hytale.server.core.inventory
- Purpose: item stacks, containers, inventory math, transactions
- Common classes: Inventory, ItemStack, ItemContainer, SimpleItemContainer
- Subpackages:
  - container: ItemContainer, ItemStackItemContainer, ItemContainerUtil, SortType
  - transaction: Transaction, ItemStackTransaction, MoveTransaction, SlotTransaction

#### World / Universe
- Package: com.hypixel.hytale.server.core.universe
- Purpose: universe and world lifecycle
- Common classes: Universe, World
- World package: com.hypixel.hytale.server.core.universe.world
  - Common classes: World, WorldConfig, WorldProvider, WorldMapTracker
  - Interfaces: IWorldChunks, IWorldChunksAsync
  - Utilities: SpawnUtil, ParticleUtil, SoundUtil
- World storage: com.hypixel.hytale.server.core.universe.world.storage
  - Interfaces: IChunkLoader, IChunkSaver
  - Common classes: ChunkStore, EntityStore
- Worldgen (universe): com.hypixel.hytale.server.core.universe.world.worldgen
  - Interfaces: IWorldGen, IWorldGenProvider, IWorldGenBenchmark
  - Common classes: GeneratedChunk, ValidatableWorldGen

#### Prefabs / Assets / Cosmetics
- Package: com.hypixel.hytale.server.core.prefab
  - Common classes: PrefabStore, PrefabEntry, PrefabRotation
- Package: com.hypixel.hytale.server.core.asset
  - Common classes: AssetModule, AssetRegistryLoader, HytaleAssetStore
- Package: com.hypixel.hytale.server.core.cosmetics
  - Common classes: CosmeticRegistry, PlayerSkin, Emote

#### Commands / Plugins / Permissions
- Package: com.hypixel.hytale.server.core.command
  - Player commands: DamageCommand, KillCommand, GameModeCommand
- Package: com.hypixel.hytale.server.core.plugin
  - Common classes: PluginManager, JavaPlugin, PluginClassLoader
- Package: com.hypixel.hytale.server.core.permissions
  - Common classes: PermissionsModule, PermissionHolder

#### Metadata / Data
- Package: com.hypixel.hytale.server.core.meta
  - Common classes: MetaRegistry, MetaKey, IMetaStore

### NPC / AI (RPG NPCs)
- Package: com.hypixel.hytale.server.npc
- Purpose: AI, roles, navigation, blackboard, state evaluation
- Common classes and areas:
  - entities: NPCEntity
  - blackboard: Blackboard
  - decisionmaker: Evaluator, EvaluationContext, StateEvaluator
  - navigation: AStarBase, AStarNode, PathFollower, IWaypoint
  - movement: MovementState, Steering
  - interactions: UseNPCInteraction, SpawnNPCInteraction
  - role: Role, RoleUtils
  - systems: NPCSystems, RoleSystems, NPCDamageSystems, NPCDeathSystems

### Spawning
- Package: com.hypixel.hytale.server.spawning
- Purpose: spawn management, beacons, markers, controllers
- Common classes:
  - managers: SpawnManager, BeaconSpawnManager
  - systems: BeaconSpatialSystem, SpawnMarkerSpatialSystem
  - world: WorldEnvironmentSpawnData

### Worldgen (Server)
- Package: com.hypixel.hytale.server.worldgen
- Purpose: biome + zone generation, chunk generation, prefabs
- Common classes:
  - biome: Biome, BiomePatternGenerator, CustomBiome
  - chunk: ChunkGenerator, BlockPriorityChunk
  - zone: Zone, ZonePatternGenerator
  - prefab: PrefabLoadingCache, PrefabPatternGenerator

## Key Interfaces / Extension Points (Quick Jump)
- com.hypixel.hytale.component.IComponentRegistry
- com.hypixel.hytale.component.IResourceStorage
- com.hypixel.hytale.event.IEvent, IEventBus, IEventRegistry, ICancellable
- com.hypixel.hytale.server.core.universe.world.IWorldChunks, IWorldChunksAsync
- com.hypixel.hytale.server.core.universe.world.storage.IChunkLoader, IChunkSaver
- com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen, IWorldGenProvider
- com.hypixel.hytale.server.npc.navigation.IWaypoint
- com.hypixel.hytale.server.core.meta.IMetaStore, IMetaRegistry
- com.hypixel.hytale.server.core.auth.IAuthCredentialStore

## Commonly Used Classes (RPG Overhaul)
- Entities/Combat: Entity, LivingEntity, InteractionManager, DamageDataComponent, ActiveEntityEffect
- Items/Inventory: ItemStack, ItemContainer, Transaction, MoveTransaction
- NPC/AI: NPCEntity, Blackboard, Evaluator, StateEvaluator, AStarBase, PathFollower
- World/Chunks: World, WorldConfig, WorldChunk, ChunkStore, EntityStore
- Worldgen: ChunkGenerator, Biome, Zone
- Plugins/Events: JavaPlugin, PluginManager, EventBus, EventRegistry

## Third-Party / External Packages
- com.google.*, com.github.*, com.nimbusds.*: external libraries bundled by the server
- com.hypixel.fastutil: fastutil-like collections
