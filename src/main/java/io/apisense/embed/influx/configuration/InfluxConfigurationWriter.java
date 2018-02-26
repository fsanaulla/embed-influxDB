package io.apisense.embed.influx.configuration;

import com.moandjiezana.toml.TomlWriter;
import de.flapdoodle.embed.process.io.directories.PropertyOrPlatformTempDir;
import de.flapdoodle.embed.process.io.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of a {@link ConfigurationWriter}.
 */
public class InfluxConfigurationWriter implements ConfigurationWriter {
    private static final Logger logger = LoggerFactory.getLogger(InfluxConfigurationWriter.class.getName());
    // Sections
    public static final String META_SECTION = "meta";
    public static final String DATA_SECTION = "data";
    public static final String HTTP_SECTION = "http";
    public static final String UDP_SECTION = "udp";

    // Entries
    public static final String BIND_ADDRESS_ENTRY = "bind-address";
    public static final String ENABLED_ENTRY = "enabled";
    public static final String DIR_ENTRY = "dir";
    public static final String WAL_DIR_ENTRY = "wal-dir";

    private final Map<String, Object> configMap;
    private final TomlWriter tomlWriter;
    private File dataPath;

    public InfluxConfigurationWriter(int backupAndRestorePort, int httpPort, int udpPort) throws IOException {
        this(backupAndRestorePort, httpPort, udpPort, Files.createTempDir(new PropertyOrPlatformTempDir(), "embedded-influx-data"));
    }

    public InfluxConfigurationWriter(int backupAndRestorePort, int httpPort, int udpPort, File dataPath) {
        this(backupAndRestorePort, httpPort, udpPort, dataPath, new TomlWriter());
    }

    InfluxConfigurationWriter(int backupAndRestorePort, int httpPort, int udpPort, File dataPath, TomlWriter writer) {
        configMap = new HashMap<>();
        configMap.put(BIND_ADDRESS_ENTRY, ":" + backupAndRestorePort);
        configMap.put(HTTP_SECTION, defaultHttpSection(httpPort));
        configMap.put(UDP_SECTION, defaultUdpSection(udpPort));
        setDataPath(dataPath);
        tomlWriter = writer;
    }

    @Override
    public void setDataPath(File dataPath) {
        this.dataPath = dataPath;
        configMap.put(META_SECTION, defaultMetaSection(dataPath));
        configMap.put(DATA_SECTION, defaultDataSection(dataPath));
    }

    @Override
    public File getDataPath() {
        return dataPath;
    }

    private static Map<String, String> defaultHttpSection(int port) {
        HashMap<String, String> meta = new HashMap<>();
        meta.put(BIND_ADDRESS_ENTRY, ":" + port);
        return meta;
    }

    private static Map<String, String> defaultUdpSection(int port) {
        HashMap<String, String> meta = new HashMap<>();
        meta.put(ENABLED_ENTRY, "true");
        meta.put(BIND_ADDRESS_ENTRY, ":" + port);
        return meta;
    }

    private static Map<String, String> defaultMetaSection(File dataPath) {
        HashMap<String, String> meta = new HashMap<>();
        meta.put(DIR_ENTRY, dataPath.getAbsolutePath() + File.separator + "meta");
        return meta;
    }

    private static Map<String, String> defaultDataSection(File dataPath) {
        HashMap<String, String> meta = new HashMap<>();
        meta.put(DIR_ENTRY, dataPath.getAbsolutePath() + File.separator + "data");
        meta.put(WAL_DIR_ENTRY, dataPath.getAbsolutePath() + File.separator + "wal-dir");
        return meta;
    }

    @Override
    public void addStatements(Map<String, Object> config) {
        for (String key : config.keySet()) {
            this.configMap.put(key, config.get(key));
        }
    }

    @Override
    public File writeFile() throws IOException {
        File configFile = File.createTempFile("influxdb.config", Long.toString(System.nanoTime()));
        logger.debug("Writing configuration file into: " + configFile.getAbsolutePath());
        tomlWriter.write(configMap, configFile);
        return configFile;
    }
}
