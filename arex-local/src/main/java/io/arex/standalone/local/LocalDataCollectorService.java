package io.arex.standalone.local;

import com.google.auto.service.AutoService;
import io.arex.agent.bootstrap.model.ArexMocker;
import io.arex.agent.bootstrap.model.Mocker;
import io.arex.foundation.config.ConfigManager;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.inst.runtime.service.DataCollector;
import io.arex.standalone.local.server.TelnetServer;
import io.arex.standalone.local.storage.H2StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(DataCollector.class)
public class LocalDataCollectorService implements DataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(H2StorageService.class);

    @Override
    public void start() {
        try {
            H2StorageService.INSTANCE.start();
            TelnetServer.INSTANCE.start();
            LOGGER.info("[arex] local data collector service start success");
        } catch (Exception e) {
            LOGGER.warn("[arex] local data collector service start error", e);
        }
    }

    @Override
    public void save(String json) {
        if (ConfigManager.INSTANCE.isEnableDebug()) {
            LOGGER.info("[arex] local save mocker: {}", json);
        }
        H2StorageService.INSTANCE.save(Serializer.deserialize(json, ArexMocker.class), json);
    }

    @Override
    public String query(String json) {
        Mocker mocker = Serializer.deserialize(json, ArexMocker.class);
        H2StorageService.INSTANCE.save(mocker, json);
        mocker.setReplayId(null);
        String result = H2StorageService.INSTANCE.queryJson(mocker);
        if (ConfigManager.INSTANCE.isEnableDebug()) {
            LOGGER.info("[arex] local query mocker: {}", result);
        }
        return result;
    }
}
