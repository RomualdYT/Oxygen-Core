package austeretony.oxygen.common.telemetry.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Queue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import austeretony.oxygen.common.api.OxygenHelperServer;
import austeretony.oxygen.common.api.OxygenTask;
import austeretony.oxygen.common.io.OxygenIOServer;
import austeretony.oxygen.common.main.OxygenMain;
import austeretony.oxygen.common.telemetry.ILogType;
import austeretony.oxygen.common.telemetry.TelemetryManager;
import austeretony.oxygen.common.telemetry.api.ILog;
import austeretony.oxygen.common.telemetry.api.LogType;
import austeretony.oxygen.common.telemetry.config.OxygenTelemetryConfig;
import austeretony.oxygen.common.util.JsonUtils;
import austeretony.oxygen.common.util.StreamUtils;
import austeretony.oxygen.common.util.TimeCounter;

public class TelemetryIO {

    private String telemetryFolder;

    private long lastRecordTime, lastAppendingTime;

    private final TimeCounter appendingCounter;

    private TelemetryIO() {
        this.appendingCounter = new TimeCounter(OxygenTelemetryConfig.CACHE_FILE_VOLUME.getIntValue() * 1000);
        this.loadLogFilesDelegated();
    }

    public static TelemetryIO create() {
        return new TelemetryIO();
    }

    public static TelemetryIO instance() {
        return OxygenMain.getTelemetryManager().getIO();
    }

    private void loadLogFilesDelegated() {
        OxygenHelperServer.addIOTaskServer(new OxygenTask() {

            @Override
            public void execute() {
                loadLogFiles();
                clearCacheFiles();
            }           
        });
    }

    private void loadLogFiles() {
        this.telemetryFolder = OxygenIOServer.getDataFolder() + "/server/telemetry";
        String folder = this.telemetryFolder + "/log_files.json";
        Path path = Paths.get(folder);     
        LogFiles.initLogFileChangingInterval();
        long currentTime = System.currentTimeMillis();
        if (Files.exists(path)) {
            try {      
                JsonArray jsonArray = JsonUtils.getExternalJsonData(folder).getAsJsonArray();
                JsonObject object;
                long time;
                ILogType logType;
                for (JsonElement element : jsonArray) {
                    object = element.getAsJsonObject();
                    logType = LogType.getType(object.get(EnumLogsDataFileKeys.TYPE.key).getAsInt());
                    if (logType == null) continue;
                    time = object.get(EnumLogsDataFileKeys.TIME.key).getAsLong();
                    logType.getLogFiles().createLogFile(logType.getLogFileName(), time);
                    logType.getLogFiles().createCacheFiles(logType.getCacheFileName(), OxygenTelemetryConfig.CACHE_FILES_AMOUNT.getIntValue());
                }
            } catch (IOException exception) {
                OxygenMain.TELEMETRY_LOGGER.error("Log file names loading failed.");
                exception.printStackTrace();
            }       
        } else {                
            try {               
                Files.createDirectories(path.getParent());
                JsonArray jsonArray = new JsonArray();
                JsonObject object;
                for (ILogType logType : LogType.getLogTypes()) {
                    object = new JsonObject();
                    object.add(EnumLogsDataFileKeys.TYPE.key, new JsonPrimitive(logType.getId()));
                    object.add(EnumLogsDataFileKeys.TIME.key, new JsonPrimitive(logType.getLogFiles().getLogFileTimeCreated()));
                    jsonArray.add(object);
                }
                JsonUtils.createExternalJsonFile(folder, jsonArray);
            } catch (IOException exception) {             
                OxygenMain.TELEMETRY_LOGGER.error("Log file names saving failed.");
                exception.printStackTrace();
            }                       
        }
    }

    private void clearCacheFiles() {
        String cacheFileFolder;
        for (ILogType logType : LogType.getLogTypes()) {
            for (int i = 0; i < OxygenTelemetryConfig.CACHE_FILES_AMOUNT.getIntValue(); i++) {
                cacheFileFolder = this.telemetryFolder + "/" + logType.getFolderName() + "/" + logType.getCacheFileName() + "_" + i + ".dat";
                try (OutputStream os = new FileOutputStream(cacheFileFolder)) { 
                    //just clearing file
                } catch (IOException exception) {
                    OxygenMain.TELEMETRY_LOGGER.error("Log cache file <{}> clearing failed.", logType.getCacheFileName() + "_" + i + ".dat");
                }  
            }
        }
    }

    public String getTelemetryDataFolder() {
        return this.telemetryFolder;
    }

    public TimeCounter getCacheAppendingCounter() {
        return this.appendingCounter;
    }

    public long getLastRecordTime() {
        return this.lastRecordTime;
    }

    public long getLastCacheAppendTime() {
        return this.lastAppendingTime;
    }

    public LogFiles getLogFiles(ILogType logType) {
        return logType.getLogFiles();
    }

    public void writeCachedLog(ILogType logType, Queue<ILog> cache) {
        if (!cache.isEmpty()) {
            String 
            prevCacheFileFolder = this.telemetryFolder + "/" + logType.getFolderName() + "/" + this.getLogFiles(logType).getLatestCacheFile() + ".dat",
            newCacheFileFolder = this.telemetryFolder + "/" + logType.getFolderName() + "/" + this.getLogFiles(logType).getCacheFile() + ".dat";
            Path path = Paths.get(newCacheFileFolder);            
            if (!Files.exists(path)) {
                try {                   
                    Files.createDirectories(path.getParent());              
                } catch (IOException exception) {     
                    exception.printStackTrace();
                }
            }
            ILog log;
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newCacheFileFolder))) {
                byte[] cachedBytes = null;
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(prevCacheFileFolder))) {
                    cachedBytes = new byte[bis.available()];
                    bis.read(cachedBytes);
                } catch (IOException exception) {
                    OxygenMain.TELEMETRY_LOGGER.error("Log cache previous cache file <{}> loading failed.", this.getLogFiles(logType).getLatestCacheFile());
                }  
                if (cachedBytes != null)
                    bos.write(cachedBytes);
                StreamUtils.write((byte) logType.getId(), bos);
                StreamUtils.write(cache.size(), bos);
                while ((log = cache.poll()) != null)
                    log.write(bos);
            } catch (IOException exception) {
                OxygenMain.TELEMETRY_LOGGER.error("Log cache data writing to {}.dat failed.", this.getLogFiles(logType).getLatestCacheFile());
                exception.printStackTrace();
            }     
            cache.clear();
            OxygenMain.TELEMETRY_LOGGER.info("Log cache data written to {}.dat", this.getLogFiles(logType).getLatestCacheFile());
        }
        this.lastRecordTime = System.currentTimeMillis();
    }

    public void appendCache() {
        if (this.appendingCounter.expired()) {
            for (ILogType logType : LogType.getLogTypes())
                this.appendCachedLog(logType);
            this.saveLogFileNames();
            this.lastAppendingTime = System.currentTimeMillis();
        }    
    }

    private void appendCachedLog(ILogType logType) {
        String 
        cacheFileFolder = this.telemetryFolder + "/" + logType.getFolderName() + "/" + this.getLogFiles(logType).getLatestCacheFile() + ".dat",
        logFileFolder = this.telemetryFolder + "/" + logType.getFolderName() + "/" + this.getLogFiles(logType).getLogFile() + ".dat";
        Path path = Paths.get(logFileFolder);            
        if (!Files.exists(path)) {
            try {                   
                Files.createDirectories(path.getParent());              
            } catch (IOException exception) {     
                exception.printStackTrace();
            }
        }
        if (this.getLogFiles(logType).isCacheFileUpdated()) {
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(logFileFolder, true))) {   
                byte[] cachedBytes = null;
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(cacheFileFolder))) {
                    cachedBytes = new byte[bis.available()];
                    bis.read(cachedBytes);       
                } catch (IOException exception) {
                    OxygenMain.TELEMETRY_LOGGER.error("Log cache file <{}> loading failed.", this.getLogFiles(logType).getLatestCacheFile());
                }  
                if (cachedBytes != null)
                    bos.write(cachedBytes);
                try (OutputStream os = new FileOutputStream(cacheFileFolder)) { 
                    //just clearing file
                } catch (IOException exception) {
                    OxygenMain.TELEMETRY_LOGGER.error("Log cache file <{}> clearing failed.", this.getLogFiles(logType).getLatestCacheFile());
                }  
            } catch (IOException exception) {
                OxygenMain.TELEMETRY_LOGGER.error("Log cache data appending to {}.dat failed.", this.getLogFiles(logType).getLatestLogFile());
                exception.printStackTrace();
            }     
            OxygenMain.TELEMETRY_LOGGER.info("Log cache data appended to {}.dat", this.getLogFiles(logType).getLatestLogFile());
        }
    }

    private void saveLogFileNames() {
        String folder = this.telemetryFolder + "/log_files.json";
        Path path = Paths.get(folder);     
        try {               
            Files.createDirectories(path.getParent());
            JsonArray jsonArray = new JsonArray();
            JsonObject object;
            for (ILogType type : LogType.getLogTypes()) {
                object = new JsonObject();
                object.add(EnumLogsDataFileKeys.TYPE.key, new JsonPrimitive(type.getId()));
                object.add(EnumLogsDataFileKeys.TIME.key, new JsonPrimitive(this.getLogFiles(type).getLogFileTimeCreated()));
                jsonArray.add(object);
            }
            JsonUtils.createExternalJsonFile(folder, jsonArray);
        } catch (IOException exception) {       
            OxygenMain.TELEMETRY_LOGGER.error("Log file names saving failed.");
            exception.printStackTrace();
        }   
    }

    public void forceSave() {
        this.appendingCounter.setExpired();
        TelemetryManager.instance().getProcessingThread().startRecord();
    }
}