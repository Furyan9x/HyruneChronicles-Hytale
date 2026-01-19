package dev.hytalemodding.origins.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate; // Import this!
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.hytalemodding.origins.classes.Classes;
import dev.hytalemodding.origins.level.LevelingService;

import java.util.UUID;

public class NameplateManager {

    public static void updateNameplate(UUID uuid, LevelingService service) {
        PlayerRef ref = Universe.get().getPlayer(uuid);
        if (ref == null) {
            System.out.println("[Origins-Debug] Failed: PlayerRef is null for " + uuid);
            return;
        }

        var entityRef = ref.getReference();
        if (entityRef == null) {
            System.out.println("[Origins-Debug] Failed: EntityRef is null for " + ref.getUsername());
            return;
        }

        int globalLvl = service.getAdventurerLevel(uuid);
        String classId = service.getActiveClassId(uuid);
        String username = ref.getUsername();

        StringBuilder sb = new StringBuilder();
        sb.append("&7[Lvl &e").append(globalLvl);

        if (classId != null) {
            Classes rpgClass = Classes.fromId(classId);
            String className = (rpgClass != null) ? rpgClass.getDisplayName() : "Hero";
            sb.append(" &6").append(className);
        }

        sb.append("&7] &f").append(username);

        String finalString = sb.toString();
        Message newMessage = Message.raw(finalString);
        var store = entityRef.getStore();
        System.out.println("[Origins-Debug] Updating Nameplate for " + username + " to: " + finalString);

        store.putComponent(entityRef, DisplayNameComponent.getComponentType(), new DisplayNameComponent(newMessage));

        var nameplateType = Nameplate.getComponentType();
        Nameplate np = store.getComponent(entityRef, nameplateType);


        if (np != null) {

            np.setText(finalString);

            store.putComponent(entityRef, nameplateType, np);
        }
            System.out.println("[Origins-Debug] Direct Nameplate component updated.");
        }
    }