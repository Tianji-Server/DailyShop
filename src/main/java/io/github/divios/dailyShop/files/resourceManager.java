package io.github.divios.dailyShop.files;

import io.github.divios.core_lib.scheduler.Schedulers;
import io.github.divios.dailyShop.utils.FileUtils;
import org.bukkit.configuration.file.YamlConfiguration;

public class resourceManager {

    private static boolean init = false;
    private final langYml langYml;
    private final settingsYml settingsYml;
    private shopsResource shopsResource;
    private final priceModifiersResource modifiersResource;

    public static resourceManager generate() {
        if (!init) {
            init = true;
            return new resourceManager();
        }
        throw new RuntimeException("You cannot create more than one instance of this class");
    }

    private resourceManager() {
        FileUtils.createParentDirectory();
        langYml = new langYml();
        settingsYml = new settingsYml();
        Schedulers.sync().run(() -> shopsResource = new shopsResource());
        FileUtils.createDatabaseFile();
        modifiersResource = new priceModifiersResource();
    }

    public synchronized YamlConfiguration getLangYml() {
        return langYml.getYaml();
    }

    public synchronized YamlConfiguration getSettingsYml() {
        return settingsYml.getYaml();
    }

    public synchronized void reload() {
        langYml.reload();
        settingsYml.reload();
        shopsResource.reload();
        modifiersResource.reload();
    }

    private static final class langYml extends resource {

        private langYml() {
            super("lang.yml");
        }

        @Override
        protected String getStartMessage() {
            return "Reading lang.yml...";
        }

        @Override
        protected String getCanceledMessage() {
            return "No changes were made on lang.yml, skipping...";
        }

        @Override
        protected String getFinishedMessage(long time) {
            return "Imported lang.yml in " + time + " ms";
        }

        @Override
        protected void init() {
        }

    }

    private static final class settingsYml extends resource {

        private settingsYml() {
            super("settings.yml");
        }

        @Override
        protected String getStartMessage() {
            return "Reading settings.yml... ";
        }

        @Override
        protected String getCanceledMessage() {
            return "No changes were made in settings.yml, skipping...";
        }

        @Override
        protected String getFinishedMessage(long time) {
            return "Imported settings.yml in " + time + " ms";
        }

        @Override
        protected void init() {
        }
    }

}
