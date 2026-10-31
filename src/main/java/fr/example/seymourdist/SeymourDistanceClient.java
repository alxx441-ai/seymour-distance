package fr.seymournorm;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.Set;

public class SeymourDistanceClient implements ClientModInitializer {

    /** IDs SkyBlock des pièces de Seymour. */
    private static final Set<String> SEYMOUR_IDS = Set.of(
            "VELVET_TOP_HAT", "CASHMERE_JACKET", "SATIN_TROUSERS", "OXFORD_SHOES");

    /** Couleur de référence (RGB), modifiable via /seymour. */
    private static volatile int[] ref = null;

    @Override
    public void onInitializeClient() {
        // /seymour <hex>  ou  /seymour <r> <g> <b>   (ex: /seymour 1F0030  |  /seymour 31 0 48)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("seymour")
                .then(ClientCommandManager.literal("clear").executes(ctx -> {
                    ref = null;
                    ctx.getSource().sendFeedback(Component.literal("Couleur de référence effacée."));
                    return 1;
                }))
                .then(ClientCommandManager.argument("couleur", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        int[] c = parse(StringArgumentType.getString(ctx, "couleur"));
                        if (c == null) {
                            ctx.getSource().sendError(Component.literal("Format invalide. Ex: /seymour 1F0030 ou /seymour 31 0 48"));
                            return 0;
                        }
                        ref = c;
                        ctx.getSource().sendFeedback(Component.literal(
                            String.format("Référence : #%02X%02X%02X (%d, %d, %d)", c[0], c[1], c[2], c[0], c[1], c[2])));
                        return 1;
                    }))));

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            int[] r = ref;
            if (r == null || !isSeymour(stack)) return;

            DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
            if (dyed == null) return;

            int rgb = dyed.rgb();
            int dr = ((rgb >> 16) & 0xFF) - r[0];
            int dg = ((rgb >> 8) & 0xFF) - r[1];
            int db = (rgb & 0xFF) - r[2];
            double norm = Math.sqrt((double) dr * dr + (double) dg * dg + (double) db * db);

            ChatFormatting fmt = norm <= 10 ? ChatFormatting.GREEN
                    : norm <= 30 ? ChatFormatting.YELLOW : ChatFormatting.RED;

            lines.add(Component.literal(String.format("Norme L2 : %.2f", norm)).withStyle(fmt));
        });
    }

    private static boolean isSeymour(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        return SEYMOUR_IDS.contains(cd.copyTag().getString("id").orElse(""));
    }

    private static int[] parse(String s) {
        try {
            s = s.trim().replace(",", " ");
            String[] p = s.split("\\s+");
            if (p.length == 3) {
                int[] c = { Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]) };
                for (int v : c) if (v < 0 || v > 255) return null;
                return c;
            }
            String h = s.startsWith("#") ? s.substring(1) : s;
            if (h.length() != 6) return null;
            int v = Integer.parseInt(h, 16);
            return new int[] { (v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
