package com.arrl.radiocraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every sound in sounds.json is mono. Minecraft (OpenAL) places only mono sounds in the world: a stereo one plays at
 * full volume wherever the listener is, so the redstone RFI noise was heard from radios far off (2026-10-30).
 */
class SoundFilesTest {

    private static final Path ASSETS = projectRoot().resolve("src/main/resources/assets/radiocraft");

    @Test
    void soundsAreMono() throws IOException {
        Path root = ASSETS;
        JsonObject sounds = JsonParser.parseString(Files.readString(root.resolve("sounds.json"))).getAsJsonObject();
        List<String> stereo = new ArrayList<>();
        for(Map.Entry<String, JsonElement> event : sounds.entrySet())
            for(JsonElement entry : event.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                String name = entry.isJsonObject() ? entry.getAsJsonObject().get("name").getAsString() : entry.getAsString();
                Path file = root.resolve("sounds").resolve(name.substring(name.indexOf(':') + 1) + ".ogg");
                if(channels(Files.readAllBytes(file)) != 1)
                    stereo.add(file.getFileName().toString());
            }
        assertTrue(stereo.isEmpty(), "Stereo sounds (mix them down to mono): " + stereo);
    }

    private static Path projectRoot() {
        for(Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent())
            if(Files.isDirectory(dir.resolve("src/main/resources/assets/radiocraft")))
                return dir;
        return Path.of("");
    }

    /** The channel count from an Ogg Vorbis file's identification header ("\1vorbis", version, channels). */
    private static int channels(byte[] ogg) {
        byte[] magic = "\1vorbis".getBytes(StandardCharsets.ISO_8859_1);
        outer:
        for(int i = 0; i + magic.length + 5 < ogg.length; i++) {
            for(int k = 0; k < magic.length; k++)
                if(ogg[i + k] != magic[k])
                    continue outer;
            return ogg[i + magic.length + 4] & 0xFF;
        }
        throw new IllegalStateException("No Vorbis header");
    }
}
