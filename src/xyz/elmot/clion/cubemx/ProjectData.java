package xyz.elmot.clion.cubemx;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ProjectData {
  @Nullable
  private String projectName;
  @Nullable
  private String linkerScript;
  @Nullable
  private String mcuFamily;
  @Nullable
  private String genericConfigName;
  @Nullable
  private String linkerFlags;
  @Nullable
  private String defines;
  @Nullable
  private String includes;
  @Nullable
  private String sources;
  @Nullable
  private String mcpu;
  @Nullable
  private String board;

  public void setProjectName(@NotNull String projectName) {
    this.projectName = projectName;
  }

  public void setLinkerFlags(@NotNull String linkerFlags) {
    this.linkerFlags = linkerFlags;
  }

  public void setLinkerScript(@NotNull String linkerScript) {
    this.linkerScript = linkerScript;
    genericConfigName = linkerScript.replace(".ld", "");
  }

  public void setMcuFamily(@NotNull String mcuFamily) {
    this.mcuFamily = mcuFamily;
    switch (mcuFamily.substring(0, 7).toUpperCase(Locale.ENGLISH)) {
      case "STM32F0":
      case "STM32L0":
      case "STM32G0":
        mcpu = "cortex-m0";
        break;
      case "STM32F1":
      case "STM32F2":
      case "STM32L1":
        mcpu = "cortex-m3";
        break;
      case "STM32F7":
      case "STM32H7":
        mcpu = "cortex-m7";
        break;
      case "STM32L5":
        mcpu = "cortex-m33";
        break;
      default:
        mcpu = "cortex-m4";
    }
  }

  @Nullable
  public String getProjectName() {
    return projectName;
  }

  @Nullable
  public String getLinkerScript() {
    return linkerScript;
  }

  @Nullable
  public String getMcuFamily() {
    return mcuFamily;
  }

  @Nullable
  public String getGenericConfigName() {
    return genericConfigName;
  }

  @Nullable
  public String getLinkerFlags() {
    return linkerFlags;
  }

  @Nullable
  public String getDefines() {
    return defines;
  }

  @Nullable
  public String getIncludes() {
    return includes;
  }

  @Nullable
  public String getSources() {
    return sources;
  }

  @Nullable
  public String getMcpu() {
    return mcpu;
  }

  @Nullable
  public String getBoard() {
    return board;
  }

  public void setDefines(@NotNull String defines) {
    this.defines = defines;
  }

  public void setIncludes(@NotNull String includes) {
    this.includes = includes;
  }

  public void setSources(@NotNull String sources) {
    this.sources = sources;
  }

  public void setBoard(@NotNull String board) {
    this.board = board;
  }

  @Override
  @NotNull
  public String toString() {
    return "projectName='" + projectName + '\'' +
           "\n linkerScript='" + linkerScript + '\'' +
           "\n mcuFamily='" + mcuFamily + '\'' +
           "\n genericConfigName='" + genericConfigName + '\'' +
           "\n linkerFlags='" + linkerFlags + '\'' +
           "\n defines='" + defines + '\'' +
           "\n includes='" + includes + '\'' +
           "\n sources='" + sources + '\'' +
           "\n mcpu='" + mcpu + '\'';
  }

  @NotNull
  public Map<String, String> getAsMap() {
    Map<String, String> map = new HashMap<>();
    map.put("projectName", projectName);
    map.put("linkerScript", linkerScript);
    map.put("mcuFamily", mcuFamily);
    map.put("genericConfigName", genericConfigName);
    map.put("linkerFlags", linkerFlags);
    map.put("defines", defines);
    map.put("includes", includes);
    map.put("sources", sources);
    map.put("mcpu", mcpu);
    map.put("templateWarning", "THIS FILE IS AUTO GENERATED FROM THE TEMPLATE! DO NOT CHANGE!");
    return map;
  }

  @NotNull
  public String shortHtml() {
    return String.format("<table>" +
                         "<tr><td>Chip</td><td><b>%s</b></td></tr>" +
                         "<tr><td>Detected CPU</td><td><b>%s</b></td></tr>" +
                         "</table>", mcuFamily, mcpu);
  }
}
