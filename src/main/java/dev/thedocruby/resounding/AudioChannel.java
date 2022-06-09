package dev.thedocruby.resounding;

import net.minecraft.util.math.Vec3d;
import dev.thedocruby.resounding.openal.Context;
import dev.thedocruby.resounding.Engine;

import javax.annotation.Nullable;
import java.util.UUID;

public class AudioChannel {

    private final UUID channelId;
    private long lastUpdate;
    private Vec3d lastPos;

    public AudioChannel(UUID channelId) {
        this.channelId = channelId;
    }

    public void onSound(Context context, int source, @Nullable Vec3d pos) {
//      if (soundPos == null) {
//          SoundPhysics.setDefaultEnvironment(source);
//          return;
//      }
        if (pos == null) return;

        long time = System.currentTimeMillis();

        if (time - lastUpdate < 500 && (lastPos != null && lastPos.distanceTo(pos) < 1D)) {
            return;
        }

//      SoundPhysics.setLastSoundCategoryAndName(SoundSource.MASTER, "voicechat");
        Engine.svc_playSound(context, pos.getX(), pos.getY(), pos.getZ(), source, false);

        lastUpdate = time;
        lastPos = pos;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public boolean canBeRemoved() {
        return System.currentTimeMillis() - lastUpdate > 5_000L;
    }
}
