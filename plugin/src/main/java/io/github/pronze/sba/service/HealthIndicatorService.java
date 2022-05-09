package io.github.pronze.sba.service;

import io.github.pronze.sba.SBA;
import io.github.pronze.sba.config.SBAConfig;
import io.github.pronze.sba.game.IArena;
import io.github.pronze.sba.game.ArenaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.screamingsandals.bedwars.api.events.BedwarsGameEndingEvent;
import org.screamingsandals.bedwars.api.events.BedwarsGameStartedEvent;
import org.screamingsandals.bedwars.api.events.BedwarsPlayerLeaveEvent;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.lib.healthindicator.HealthIndicator2;
import org.screamingsandals.lib.healthindicator.HealthIndicatorManager2;
import org.screamingsandals.lib.player.PlayerMapper;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.methods.OnPostEnable;
import org.screamingsandals.lib.utils.annotations.methods.OnPreDisable;
import org.screamingsandals.lib.visuals.Visual;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Service(dependsOn = {
        HealthIndicatorManager2.class
})
public class HealthIndicatorService implements Listener {
    private final Map<IArena, HealthIndicator2> healthIndicatorMap = new HashMap<>();

    private boolean tabEnabled;

    

    @OnPostEnable
    public void postEnabled() {
        this.tabEnabled = SBAConfig
                .getInstance()
                .node("show-health-in-tablist")
                .getBoolean();

        boolean tagEnabled = SBAConfig
                .getInstance()
                .node("show-health-under-player-name")
                .getBoolean();

        if (!tagEnabled) {
            return;
        }

        SBA.getInstance().registerListener(this);
    }

    @OnPreDisable
    public void onDestroy() {
        healthIndicatorMap.values().forEach(Visual::destroy);
        healthIndicatorMap.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameStart(BedwarsGameStartedEvent event) {
        final Game game = event.getGame();
        final var healthIndicator = HealthIndicator2.of()
                .symbol(Component.text("\u2665", NamedTextColor.RED))
                .showHealthInTabList(tabEnabled)
                .show()
                .startUpdateTask(2, TaskerTime.TICKS);

        game.getConnectedPlayers()
                .stream()
                .map(PlayerMapper::wrapPlayer)
                .forEach(healthIndicator::addViewer);

        game.getConnectedPlayers()
                .stream()
                .map(PlayerMapper::wrapPlayer)
                .forEach(healthIndicator::addTrackedPlayer);

        healthIndicatorMap.put(ArenaManager.getInstance().get(game.getName()).orElseThrow(), healthIndicator);
    }

    @EventHandler
    public void onPlayerLeave(BedwarsPlayerLeaveEvent event) {
        final var playerWrapper = PlayerMapper.wrapPlayer(event.getPlayer());
        final var healthIndicator = healthIndicatorMap.get(ArenaManager.getInstance().get(event.getGame().getName()).orElse(null));
        if (healthIndicator != null) {
            healthIndicator.removeViewer(playerWrapper);
            healthIndicator.removeTrackedPlayer(playerWrapper);
        }
    }

    @EventHandler
    public void onBedwarsGameEndingEvent(BedwarsGameEndingEvent event) {
        final var arena = ArenaManager.getInstance().get(event.getGame().getName()).orElseThrow();
        final var healthIndicator = healthIndicatorMap.get(arena);
        if (healthIndicator != null) {
            healthIndicator.destroy();
            healthIndicatorMap.remove(arena);
        }
    }

}
