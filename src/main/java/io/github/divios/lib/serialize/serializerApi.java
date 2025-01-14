package io.github.divios.lib.serialize;

import com.cryptomorin.xseries.ReflectionUtils;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.divios.core_lib.cache.Lazy;
import io.github.divios.core_lib.utils.Log;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.utils.DebugLog;
import io.github.divios.dailyShop.utils.FileUtils;
import io.github.divios.dailyShop.utils.Utils;
import io.github.divios.dailyShop.utils.cache.Cache;
import io.github.divios.dailyShop.utils.cache.CacheBuilder;
import io.github.divios.dailyShop.utils.cache.RemovalCause;
import io.github.divios.lib.dLib.shop.dShop;
import io.github.divios.lib.dLib.shop.dShopState;
import io.github.divios.lib.serialize.adapters.dShopStateAdapter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class serializerApi {

    private static final DailyShop plugin = DailyShop.get();
    private static final Lazy<File> shopsFolder = Lazy.suppliedBy(() -> new File(plugin.getDataFolder(), "shops"));

    protected static final ExecutorService asyncPool = Executors.newSingleThreadExecutor();

    protected static final Cache<dShop, Runnable> cache = new CacheBuilder<dShop, Runnable>()
            .expireAfter(2, TimeUnit.SECONDS)
            .removalListener(event -> {
                if (event.getCause() == RemovalCause.REPLACED) return;
                event.getValue().run();
            })
            .build();


    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(dShopState.class, new dShopStateAdapter())
            .create();

    public static void saveShopToFile(dShop shop) {
        try {
            File data = new File(shopsFolder.get(), shop.getName() + ".yml");
            //DebugLog.info("waaaa");
            gson.toJsonTree(shop.toState());
            FileUtils.dumpToYaml(gson.toJsonTree(shop.toState(), dShopState.class), data);
            //DebugLog.info("waaaa1");
        } catch (Exception e) {
            Log.info("There was a problem saving the shop " + shop.getName());
            // e.printStackTrace();
        }
        //Log.info("Converted all items correctly of shop " + shop.getName());
    }

    public static void saveShopToFileAsync(dShop shop) {
        DebugLog.info("Serialize shop");
        //if (ReflectionUtils.VER >= 12) cache.put(shop, () -> asyncPool.submit(() -> saveShopToFile(shop)));
        asyncPool.execute(() -> saveShopToFile(shop));
    }

    public static dShopState getShopFromFile(File data) {
        Objects.requireNonNull(data, "data cannot be null");
        Preconditions.checkArgument(data.exists(), "The file does not exist");
        return gson.fromJson(Utils.getJsonFromFile(data), dShopState.class);
    }

    public static Future<dShopState> getShopFromFileAsync(File data) {
        return asyncPool.submit(() -> getShopFromFile(data));
    }

    public static void deleteShop(String name) {
        File[] files = shopsFolder.get().listFiles((dir, name1) -> name1.endsWith(".yml"));
        if (files == null) throw new RuntimeException("shops directory does not exits");
        for (int i = 0; i < files.length; i++) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(files[i]);
            if (yaml.get("id").equals(name)) {
                files[i].delete();
                break;
            }
        }
    }

    public static void deleteShopAsync(String name) {
        asyncPool.submit(() -> deleteShop(name));
    }

    public static void stop() {
        cache.cleanUp();
        asyncPool.shutdown();
        try {
            asyncPool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
