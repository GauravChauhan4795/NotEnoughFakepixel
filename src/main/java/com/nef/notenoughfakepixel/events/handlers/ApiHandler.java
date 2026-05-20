package com.nef.notenoughfakepixel.events.handlers;

import com.google.gson.Gson;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.Logger;
import lombok.Builder;
import lombok.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Handles NEF's lightweight analytics payload.
 *
 * Privacy notice:
 * When the player is in SkyBlock, this handler sends the Minecraft username, the connected
 * server IP, and the installed mod list to the NEF analytics endpoint. This data is used only
 * for basic usage analytics, server verification, version/support decisions, and update timing.
 * The full privacy policy is available here:
 * https://github.com/davidbelesp/NotEnoughFakepixel/blob/master/PRIVACY.md
 */
public class ApiHandler {

    private static final String SEED_URL = "https://raw.githubusercontent.com/davidbelesp/NotEnoughFakepixel/refs/heads/master/seed";
    private static final String PRIVACY_POLICY_URL = "https://github.com/davidbelesp/NotEnoughFakepixel/blob/master/PRIVACY.md";
    private static final String OPTIFINE_URL = "https://optifine.net/downloadx?f=OptiFine_1.8.9_HD_U_M5.jar&x=2368da25408fcffee40a4e9740e9d3eb";
    private static final Gson GSON = new Gson();

    private static String apiUrl = null;
    private static boolean hasReportedSession = false;
    private static boolean hasShownOptifineNotice = false;

    private ApiHandler() {
        // Utility class
    }

    public static void init() {
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;

        if (!hasShownOptifineNotice) {
            notifyMissingOptifine();
        }

        if (!hasReportedSession) {
            reportSession();
        }
    }

    private static void reportSession() {
        if (apiUrl == null) {
            apiUrl = fetchApiUrl();
        }
        if (apiUrl == null) return;

        sendRequest(createApiModel());
    }

    public static void sendRequest(ApiModel model) {
        if (model == null) return;

        try {
            String payload = GSON.toJson(new Payload(model));
            int responseCode = postJson(new URL(apiUrl), payload);

            if (responseCode >= 200 && responseCode < 300) {
                hasReportedSession = true;
            }
        } catch (Exception e) {
            Logger.logErrorConsole(e.getMessage());
        }
    }

    private static int postJson(URL url, String jsonPayload) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }

        return conn.getResponseCode();
    }

    private static ApiModel createApiModel() {
        Minecraft mc = Minecraft.getMinecraft();
        String username = mc.getSession().getUsername();
        String serverIp = mc.getCurrentServerData() != null
                ? mc.getCurrentServerData().serverIP
                : "localhost";

        return ApiModel.builder()
                .ip(serverIp)
                .username(username)
                .content(collectInstalledModNames())
                .build();
    }

    private static String fetchApiUrl() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(SEED_URL).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String encodedUrl = reader.readLine();
                if (encodedUrl != null && !encodedUrl.trim().isEmpty()) {
                    return new String(Base64.getDecoder().decode(encodedUrl.trim()), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            Logger.logConsole(e.getMessage());
        }

        return null;
    }

    private static void notifyMissingOptifine() {
        if (hasOptifine()) {
            hasShownOptifineNotice = true;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        ChatComponentText message = new ChatComponentText(
                EnumChatFormatting.RED + "You are not using Optifine! Consider adding it for better performance clicking here"
        );
        message.setChatStyle(new ChatStyle()
                .setColor(EnumChatFormatting.RED)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, OPTIFINE_URL))
                .setChatHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText(EnumChatFormatting.YELLOW + "Open Optifine download")
                )));

        mc.thePlayer.addChatMessage(message);
        hasShownOptifineNotice = true;
    }

    private static boolean hasOptifine() {
        for (ModContainer mod : Loader.instance().getModList()) {
            if (containsOptifine(mod.getModId()) || containsOptifine(mod.getName())) {
                return true;
            }
        }

        for (String modFile : collectInstalledModNames()) {
            if (containsOptifine(modFile)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsOptifine(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("optifine");
    }

    private static List<String> collectInstalledModNames() {
        List<String> installedMods = collectModsFromDirectory();

        if (installedMods.isEmpty()) {
            installedMods.addAll(collectLoadedForgeModIds());
        }

        return installedMods;
    }

    private static List<String> collectModsFromDirectory() {
        List<String> modFileNames = new ArrayList<>();
        File modsDirectory = new File(Minecraft.getMinecraft().mcDataDir, "mods");

        File[] modFiles = modsDirectory.listFiles(file ->
                file.isFile() && isModFileName(file.getName()));
        if (modFiles == null) {
            return modFileNames;
        }

        for (File modFile : modFiles) {
            String fileName = modFile.getName().replaceAll("\\s+", "").trim();
            if (!fileName.isEmpty()) {
                modFileNames.add(fileName);
            }
        }

        return modFileNames;
    }

    private static boolean isModFileName(String fileName) {
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        return lowerFileName.endsWith(".jar")
                || lowerFileName.endsWith(".zip")
                || lowerFileName.endsWith(".litemod");
    }

    private static List<String> collectLoadedForgeModIds() {
        List<String> modIds = new ArrayList<>();

        for (ModContainer mod : Loader.instance().getModList()) {
            modIds.add(mod.getModId().toLowerCase(Locale.ROOT));
        }

        return modIds;
    }

    public static class Payload {
        final ApiModel content;

        public Payload(ApiModel content) {
            this.content = content;
        }
    }

    @Data
    @Builder
    public static class ApiModel {
        private String ip;
        private String username;
        private List<String> content;

        public String getJson() {
            return GSON.toJson(new Payload(this));
        }
    }
}
