package xyz.elmot.clion.cubemx;

import java.util.HashMap;
import java.util.Map;

/**
 * (c) elmot on 28.9.2017.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
class ProjectData {
    private String projectName;
    private String linkerScript;
    private String mcuFamily;
    private String genericConfigName;
    private String linkerFlags;
    private String defines;
    private String includes;
    private String sources;
    private String mcpu;

    void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setLinkerFlags(String linkerFlags) {
        this.linkerFlags = linkerFlags;
    }

    void setLinkerScript(String linkerScript) {
        this.linkerScript = linkerScript;
        genericConfigName = linkerScript.replace(".ld", "");
    }

    void setMcuFamily(String mcuFamily) {
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

    void setDefines(String defines) {
        this.defines = defines;
    }

    void setIncludes(String includes) {
        this.includes = includes;
    }

    void setSources(String sources) {
        this.sources = sources;
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

    public String extraInfo() {
        return String.format("Project Name: %s\nConfig Name: %s\nLinker Flags: %s",
                projectName, genericConfigName, linkerFlags);
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
        return map;
    }

    public String shortHtml() {
        return String.format("<html><table>" +
                "<tr><td>Project name</td><td><b>%s</b></td></tr>" +
                "<tr><td>Chip</td><td><b>%s</b></td></tr>" +
                "<tr><td>Detected CPU</td><td><b>%s</b></td></tr>" +
                "</table></html>",projectName, mcuFamily, mcpu);
    }
}
