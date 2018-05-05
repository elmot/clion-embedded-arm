package xyz.elmot.clion.cubemx;

import java.util.HashMap;
import java.util.Map;

/**
 * (c) elmot on 28.9.2017.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ProjectData {
    private String projectName;
    private String linkerScript;
    private String mcuFamily;
    private String genericConfigName;
    private String linkerFlags;
    private String defines;
    private String includes;
    private String sources;
    private String mcpu;
    private String board;

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setLinkerFlags(String linkerFlags) {
        this.linkerFlags = linkerFlags;
    }

    public void setLinkerScript(String linkerScript) {
        this.linkerScript = linkerScript;
        genericConfigName = linkerScript.replace(".ld", "");
    }

    public void setMcuFamily(String mcuFamily) {
        this.mcuFamily = mcuFamily;
        switch (mcuFamily.substring(0, 7).toUpperCase()) {
            case "STM32F0":
            case "STM32L0":
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
            default:
                mcpu = "cortex-m4";
        }
    }

    public String getProjectName() {
        return projectName;
    }

    public String getLinkerScript() {
        return linkerScript;
    }

    public String getMcuFamily() {
        return mcuFamily;
    }

    public String getGenericConfigName() {
        return genericConfigName;
    }

    public String getLinkerFlags() {
        return linkerFlags;
    }

    public String getDefines() {
        return defines;
    }

    public String getIncludes() {
        return includes;
    }

    public String getSources() {
        return sources;
    }

    public String getMcpu() {
        return mcpu;
    }

    public String getBoard() {
        return board;
    }

    public void setDefines(String defines) {
        this.defines = defines;
    }

    public void setIncludes(String includes) {
        this.includes = includes;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    @Override
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

    public String shortHtml() {
        return String.format("<table>" +
                "<tr><td>Chip</td><td><b>%s</b></td></tr>" +
                "<tr><td>Detected CPU</td><td><b>%s</b></td></tr>" +
                "</table>", mcuFamily, mcpu);
    }
}
