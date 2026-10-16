package moze_intel.projecte.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import moze_intel.projecte.PECore;
import moze_intel.projecte.utils.text.PELang;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent.OpenUrl;

public class ThreadCheckUpdate extends Thread {
  private static final String CURSE_URL =
      "https://www.curseforge.com/minecraft/mc-mods/projecte/files";
  private static final URI RELEASES_URI =
      URI.create("https://api.github.com/repos/sinkillerj/ProjectE/releases/latest");
  private static final Pattern TAG_NAME =
      Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
  private static volatile Version target;
  private static volatile boolean hasSentMessage;
  private static boolean callbackRegistered;

  public ThreadCheckUpdate() {
    this.setName("ProjectE Update Checker Notifier");
    registerClientCallback();
  }

  public static synchronized void register() {
    registerClientCallback();
  }

  private static synchronized void registerClientCallback() {
    if (!callbackRegistered && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
      callbackRegistered = true;
      ClientPlayConnectionEvents.JOIN.register(
          (Join)
              (handler, sender, client) -> {
                if (client.player instanceof LocalPlayer player) {
                  worldLoad(player);
                }
              });
    }
  }

  @Override
  public void run() {
    Version current =
        FabricLoader.getInstance()
            .getModContainer("projecte")
            .map(container -> container.getMetadata().getVersion())
            .orElse(null);
    if (current == null) {
      PECore.LOGGER.warn(
          "Unable to determine the installed ProjectE version; update check skipped.");
    } else {
      Version latest = queryLatestVersion(current);
      if (latest != null && latest.compareTo(current) > 0) {
        target = latest;
      }
    }
  }

  private static Version queryLatestVersion(Version current) {
    HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10L))
            .followRedirects(Redirect.NORMAL)
            .build();
    HttpRequest request =
        HttpRequest.newBuilder(RELEASES_URI)
            .timeout(Duration.ofSeconds(10L))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ProjectE/" + current.getFriendlyString())
            .GET()
            .build();

    try {
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        PECore.LOGGER.warn("Update check failed with HTTP status {}.", response.statusCode());
        return null;
      }

      Matcher matcher = TAG_NAME.matcher(response.body());
      if (!matcher.find()) {
        PECore.LOGGER.warn("Update check response did not contain a release version.");
        return null;
      }

      String version = matcher.group(1).trim();

      while (version.startsWith("v") || version.startsWith("V")) {
        version = version.substring(1);
      }

      return Version.parse(version);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (IOException e) {
      PECore.LOGGER.warn("Update check failed.", e);
      return null;
    } catch (VersionParsingException | RuntimeException e) {
      PECore.LOGGER.warn("Update check failed.", e);
      return null;
    }
  }

  public static void worldLoad(LocalPlayer player) {
    Version latest = target;
    if (latest != null && !hasSentMessage) {
      hasSentMessage = true;
      player.sendSystemMessage(PELang.UPDATE_AVAILABLE.translate(latest.getFriendlyString()));
      player.sendSystemMessage(PELang.UPDATE_GET_IT.translate());
      player.sendSystemMessage(
          TextComponentUtil.build(
              new OpenUrl(
                  URI.create("https://www.curseforge.com/minecraft/mc-mods/projecte/files")),
              "https://www.curseforge.com/minecraft/mc-mods/projecte/files"));
    }
  }
}
