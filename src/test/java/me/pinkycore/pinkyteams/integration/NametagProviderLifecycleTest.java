package me.pinkycore.pinkyteams.integration;

import me.pinkycore.pinkyteams.PinkyTeams;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NametagProviderLifecycleTest {

    @ParameterizedTest
    @CsvSource({"false, unt", "false, tab", "true, unt", "true, internal", "true, tab"})
    void inactiveTabLifecycleDoesNotLoadMissingTabClasses(boolean enabled, String provider) throws Exception {
        PinkyTeams plugin = configuredPlugin(enabled, provider);
        PluginManager manager = mock(PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            WithoutTabClassLoader loader = new WithoutTabClassLoader();
            Runnable probe = (Runnable) loader.loadClass(LifecycleProbe.class.getName())
                    .getConstructor(PinkyTeams.class).newInstance(plugin);
            assertDoesNotThrow(probe::run);
            assertFalse(loader.tabClassRequested, "Inactive lifecycle must never resolve TAB API classes");
        }
    }

    @Test
    void selectedTabWithMissingApiDoesNotCrashPlugin() throws Exception {
        PinkyTeams plugin = configuredPlugin(true, "tab");
        PluginManager manager = mock(PluginManager.class);
        when(manager.isPluginEnabled("TAB")).thenReturn(true);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            WithoutTabClassLoader loader = new WithoutTabClassLoader();
            Runnable probe = (Runnable) loader.loadClass(LifecycleProbe.class.getName())
                    .getConstructor(PinkyTeams.class).newInstance(plugin);
            assertDoesNotThrow(probe::run);
            assertTrue(loader.tabClassRequested, "The selected provider should attempt to initialize its API");
            bukkit.verify(Bukkit::getScheduler, never());
        }
    }

    @ParameterizedTest
    @CsvSource({"false, unt", "false, tab", "true, unt", "true, internal", "true, tab"})
    void reloadWithoutTabKeepsTabHookUninitialized(boolean enabled, String provider) throws Exception {
        PinkyTeams plugin = configuredPlugin(enabled, provider);
        doCallRealMethod().when(plugin).reloadIntegrations();
        UnlimitedNametagHook unt = mock(UnlimitedNametagHook.class);
        field("unlimitedNametagHook").set(plugin, unt);
        PluginManager manager = mock(PluginManager.class);
        when(manager.isPluginEnabled("UnlimitedNameTags")).thenReturn(true);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            assertDoesNotThrow(plugin::reloadIntegrations);
            assertDoesNotThrow(plugin::reloadIntegrations);
            assertNull(field("tabHook").get(plugin));
            if (enabled && "unt".equals(provider)) {
                verify(unt, times(2)).start();
                verify(unt, never()).stop();
            } else {
                verify(unt, times(2)).stop();
                verify(unt, never()).start();
            }
            if (!enabled) {
                verifyNoInteractions(manager);
            }
        }
    }

    @Test
    void disablingPrivacyStopsPreviouslyActiveProviders() throws Exception {
        PinkyTeams plugin = configuredPlugin(false, "tab");
        doCallRealMethod().when(plugin).reloadIntegrations();
        TabHook tab = mock(TabHook.class);
        UnlimitedNametagHook unt = mock(UnlimitedNametagHook.class);
        field("tabHook").set(plugin, tab);
        field("unlimitedNametagHook").set(plugin, unt);
        plugin.reloadIntegrations();
        verify(tab).stop();
        verify(unt).stop();
        verify(tab, never()).start();
        verify(unt, never()).start();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void activeTabStopsItsTaskAndOnlyRestoresTagsWhileTabIsEnabled(boolean tabStillEnabled) {
        PinkyTeams plugin = configuredPlugin(true, "tab");
        PluginManager manager = mock(PluginManager.class);
        when(manager.isPluginEnabled("TAB")).thenReturn(true);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(task.getTaskId()).thenReturn(42);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(20L), eq(100L))).thenReturn(task);
        TabAPI api = mock(TabAPI.class);
        NameTagManager tags = mock(NameTagManager.class);
        TabPlayer player = mock(TabPlayer.class);
        when(api.getNameTagManager()).thenReturn(tags);
        when(api.getOnlinePlayers()).thenReturn(new TabPlayer[]{player});
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<TabAPI> tab = mockStatic(TabAPI.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            tab.when(TabAPI::getInstance).thenReturn(api);
            TabHook hook = new TabHook(plugin);
            hook.start();
            // Starting a fresh hook must not reset any tags owned by TAB.
            verifyNoInteractions(tags);
            when(manager.isPluginEnabled("TAB")).thenReturn(tabStillEnabled);
            hook.stop();
            hook.stop();
            verify(scheduler).cancelTask(42);
            if (tabStillEnabled) {
                verify(tags).showNameTag(player, player);
            } else {
                verifyNoInteractions(tags);
                tab.verify(TabAPI::getInstance, times(1));
            }
        }
    }

    private static PinkyTeams configuredPlugin(boolean enabled, String provider) {
        PinkyTeams plugin = mock(PinkyTeams.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("nametag-privacy.enabled", enabled);
        config.set("nametag-privacy.provider", provider);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("nametag-lifecycle-test"));
        return plugin;
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = PinkyTeams.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    // Load the real hook in an environment where TAB's classes cannot be found.
    // Direct calls from this probe avoid reflection resolving the hook's private API signatures.
    public static class LifecycleProbe implements Runnable {
        private final PinkyTeams plugin;

        public LifecycleProbe(PinkyTeams plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            TabHook hook = new TabHook(plugin);
            hook.stop();
            hook.start();
            hook.stop();
            hook.stop();
        }
    }

    private static class WithoutTabClassLoader extends ClassLoader {
        private boolean tabClassRequested;

        WithoutTabClassLoader() {
            super(NametagProviderLifecycleTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("me.neznamy.tab.")) {
                tabClassRequested = true;
                throw new ClassNotFoundException(name);
            }
            if (!name.equals(LifecycleProbe.class.getName()) && !name.startsWith(TabHook.class.getName())) {
                return super.loadClass(name, resolve);
            }
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                String resource = name.replace('.', '/') + ".class";
                try (InputStream input = getParent().getResourceAsStream(resource)) {
                    if (input == null) throw new ClassNotFoundException(name);
                    byte[] bytes = input.readAllBytes();
                    loaded = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }
}
