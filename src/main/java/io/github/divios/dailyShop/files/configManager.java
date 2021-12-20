package io.github.divios.dailyShop.files;

import io.github.divios.core_lib.scheduler.Schedulers;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.utils.FileUtils;

public class configManager {

    private static final DailyShop main = DailyShop.get();
    private final langResource langYml;
    private final settingsResource settingsYml;
    private shopsResource shopsResource;
    private final priceModifiersResource modifiersResource;

    public static configManager generate() {
        return new configManager();
    }

    private configManager() {

        FileUtils.createParentDirectory();
        langYml = new langResource();
        settingsYml = new settingsResource();
        Schedulers.sync().run(() -> shopsResource = new shopsResource());
        FileUtils.createDatabaseFile();
        modifiersResource = new priceModifiersResource();
    }

    public synchronized langResource getLangYml() {
        return langYml;
    }

    public synchronized settingsResource getSettingsYml() {
        return settingsYml;
    }

    public synchronized void reload() {
        langYml.reload();
        settingsYml.reload();
        shopsResource.reload();
        modifiersResource.reload();
    }
}
