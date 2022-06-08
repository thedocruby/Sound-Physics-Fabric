package dev.thedocruby.resounding.integration;

import dev.thedocruby.resounding.Engine;
import dev.thedocruby.resounding.openal.Context;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.Vec3d;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.OpenALSoundEvent;
import de.maxhenkel.voicechat.api.events.CreateOpenALContextEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import org.lwjgl.openal.EXTThreadLocalContext;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

public class SVC implements VoicechatPlugin {

    private Map<UUID, SVCchannel> channels;
	private Context context;


    public SVC() {
        context = new Context();
        channels = new HashMap<>();
    }

    @Override
    public String getPluginId() {
        return "resounding";
    }

    @Override
    public void initialize(VoicechatApi api) {
        channels.clear();
    }

    private void onCreateALContext(CreateOpenALContextEvent event) {
		context.bind(event.getContext(), "Simple Voice Chat");
		Engine.root.addChild(context);
    }

    private void onConnection(ClientVoicechatConnectionEvent event) {
        channels.values().removeIf(SVCchannel::canBeRemoved);
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientVoicechatConnectionEvent.class, this::onConnection);
        registration.registerEvent(OpenALSoundEvent.class, this::onOpenALSound);
        registration.registerEvent(CreateOpenALContextEvent.class, this::onCreateALContext);
    }


    private void onOpenALSound(OpenALSoundEvent event) {
        @Nullable
        final Position position = event.getPosition();
        @Nullable
        final UUID channelId = event.getChannelId();

        if (channelId == null) {
            return;
        }

        @Nullable
        SVCchannel channel = channels.get(channelId);

        if (channel == null) {
            channel = new SVCchannel(channelId);
            channels.put(channelId, channel);
        }

        channel.onSound(context, event.getSource(), position == null ? null : new Vec3d(position.getX(), position.getY(), position.getZ()));
    }

}
