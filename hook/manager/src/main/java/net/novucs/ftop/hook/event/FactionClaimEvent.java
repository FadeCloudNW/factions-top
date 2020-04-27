package net.novucs.ftop.hook.event;

import com.google.common.collect.Multimap;
import net.novucs.ftop.entity.ChunkPos;
import org.bukkit.event.HandlerList;

public class FactionClaimEvent extends FactionEvent {

    private static final HandlerList handlers = new HandlerList();
    private final Multimap<String, ChunkPos> claims;
    private boolean isClaiming = false;

    public FactionClaimEvent(String factionId, Multimap<String, ChunkPos> claims) {
        super(factionId);
        this.claims = claims;
    }

    public FactionClaimEvent(String factionId, Multimap<String, ChunkPos> claims, boolean isClaiming) {
        super(factionId);
        this.claims = claims;
        this.isClaiming = isClaiming;
    }

    public Multimap<String, ChunkPos> getClaims() {
        return claims;
    }

    public boolean isClaiming() {
        return isClaiming;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
