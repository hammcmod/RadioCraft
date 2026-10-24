package com.arrl.radiocraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Patchouli guide book ({@code assets/radiocraft/patchouli_books/guide}) points only at things that exist: every
 * entry's category, every recipe on a crafting page, every radiocraft item used as an icon or spotlight, every
 * $(l:...) link, and every block and block state property in a build's multiblock, whose pattern must also be well
 * formed. Patchouli itself only finds these mistakes in game, one blank page at a time.
 */
class PatchouliBookTest {

    private static final Path PROJECT = projectRoot();
    private static final Path BOOK = PROJECT.resolve("src/main/resources/assets/radiocraft/patchouli_books/guide/en_us");
    private static final List<Path> RESOURCES = List.of(PROJECT.resolve("src/main/resources"), PROJECT.resolve("src/generated/resources"));
    private static final Pattern LINK = Pattern.compile("\\$\\(l:([a-z0-9_]+:[a-z0-9_/]+)(#[a-z0-9_]+)?\\)");
    private static final Pattern STATE = Pattern.compile("([a-z0-9_]+):([a-z0-9_]+)(?:\\[(.*)])?");

    @Test
    void bookPointsOnlyAtThingsThatExist() throws IOException {
        JsonObject book = read(PROJECT.resolve("src/main/resources/data/radiocraft/patchouli_books/guide/book.json"));
        List<String> problems = new ArrayList<>();
        if(!book.has("use_resource_pack") || !book.get("use_resource_pack").getAsBoolean())
            problems.add("book.json must set use_resource_pack: true (Patchouli 1.20+)");

        Set<String> categories = ids(BOOK.resolve("categories"));
        Set<String> entries = ids(BOOK.resolve("entries"));
        for(String category : categories)
            checkItem(read(BOOK.resolve("categories/" + category + ".json")).get("icon").getAsString(), "category " + category, problems);

        for(String id : entries) {
            JsonObject entry = read(BOOK.resolve("entries/" + id + ".json"));
            String where = "entry " + id;
            String category = entry.get("category").getAsString();
            if(!category.startsWith("radiocraft:") || !categories.contains(category.substring("radiocraft:".length())))
                problems.add(where + ": no category " + category);
            checkItem(entry.get("icon").getAsString(), where, problems);
            int pageNumber = 0;
            for(JsonElement element : entry.getAsJsonArray("pages")) {
                JsonObject page = element.getAsJsonObject();
                String at = where + " page " + pageNumber++;
                String type = page.get("type").getAsString();
                if(page.has("text"))
                    checkLinks(page.get("text").getAsString(), categories, entries, at, problems);
                switch(type) {
                    case "patchouli:crafting" -> {
                        checkRecipe(page.get("recipe").getAsString(), at, problems);
                        if(page.has("recipe2"))
                            checkRecipe(page.get("recipe2").getAsString(), at, problems);
                    }
                    case "patchouli:spotlight" -> checkItem(page.get("item").getAsString(), at, problems);
                    case "patchouli:multiblock" -> checkMultiblock(page.getAsJsonObject("multiblock"), at, problems);
                    case "patchouli:relations" -> {
                        for(JsonElement linked : page.getAsJsonArray("entries"))
                            if(!entries.contains(linked.getAsString().substring("radiocraft:".length())))
                                problems.add(at + ": relation to missing entry " + linked.getAsString());
                    }
                    case "patchouli:text" -> {}
                    default -> problems.add(at + ": page type " + type + " is not checked here");
                }
            }
        }
        checkLinks(book.get("landing_text").getAsString(), categories, entries, "landing text", problems);
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static void checkLinks(String text, Set<String> categories, Set<String> entries, String where, List<String> problems) {
        Matcher matcher = LINK.matcher(text);
        while(matcher.find()) {
            String id = matcher.group(1);
            String path = id.substring(id.indexOf(':') + 1);
            if(!id.startsWith("radiocraft:") || !(entries.contains(path) || categories.contains(path)))
                problems.add(where + ": link to missing " + id);
        }
    }

    private static void checkRecipe(String id, String where, List<String> problems) {
        if(id.startsWith("radiocraft:") && find("data/radiocraft/recipe/" + id.substring("radiocraft:".length()) + ".json") == null)
            problems.add(where + ": no recipe " + id);
    }

    /** A radiocraft item has an item model; vanilla ones are trusted. */
    private static void checkItem(String id, String where, List<String> problems) {
        if(id.startsWith("radiocraft:") && find("assets/radiocraft/models/item/" + id.substring("radiocraft:".length()) + ".json") == null)
            problems.add(where + ": no item " + id);
    }

    private static void checkMultiblock(JsonObject multiblock, String where, List<String> problems) {
        Map<Character, String> mapping = new HashMap<>();
        for(Map.Entry<String, JsonElement> e : multiblock.getAsJsonObject("mapping").entrySet()) {
            mapping.put(e.getKey().charAt(0), e.getValue().getAsString());
            checkState(e.getValue().getAsString(), where, problems);
        }
        JsonArray layers = multiblock.getAsJsonArray("pattern");
        int rows = -1, length = -1, zeros = 0;
        for(JsonElement layer : layers) {
            JsonArray strings = layer.getAsJsonArray();
            if(rows >= 0 && strings.size() != rows)
                problems.add(where + ": layers have different numbers of rows");
            rows = strings.size();
            for(JsonElement row : strings) {
                String s = row.getAsString();
                if(length >= 0 && s.length() != length)
                    problems.add(where + ": rows have different lengths");
                length = s.length();
                for(char c : s.toCharArray()) {
                    if(c == '0')
                        zeros++;
                    else if(c != ' ' && c != '_' && !mapping.containsKey(c))
                        problems.add(where + ": '" + c + "' is not mapped");
                }
            }
        }
        if(zeros != 1)
            problems.add(where + ": the pattern needs exactly one '0', has " + zeros);
    }

    /** A radiocraft block has a blockstate file, and every property named is one that file uses (if it lists any). */
    private static void checkState(String state, String where, List<String> problems) {
        Matcher matcher = STATE.matcher(state);
        if(!matcher.matches()) {
            problems.add(where + ": cannot read block state " + state);
            return;
        }
        if(!matcher.group(1).equals("radiocraft"))
            return;
        Path file = find("assets/radiocraft/blockstates/" + matcher.group(2) + ".json");
        if(file == null) {
            problems.add(where + ": no block " + matcher.group(1) + ":" + matcher.group(2));
            return;
        }
        if(matcher.group(3) == null)
            return;
        Set<String> known;
        try {
            known = properties(read(file));
        }
        catch(IOException e) {
            problems.add(where + ": cannot read " + file);
            return;
        }
        if(known.isEmpty())
            return; // One model for every state (wires draw their connections in code): nothing to check against.
        for(String pair : matcher.group(3).split(",")) {
            String property = pair.split("=")[0];
            if(!known.contains(property))
                problems.add(where + ": " + matcher.group(2) + " has no property " + property);
        }
    }

    private static Set<String> properties(JsonObject blockstate) {
        Set<String> names = new HashSet<>();
        if(blockstate.has("variants")) {
            for(String key : blockstate.getAsJsonObject("variants").keySet())
                for(String pair : key.split(","))
                    if(pair.contains("="))
                        names.add(pair.split("=")[0]);
        }
        if(blockstate.has("multipart")) {
            for(JsonElement part : blockstate.getAsJsonArray("multipart")) {
                JsonObject object = part.getAsJsonObject();
                if(object.has("when") && object.get("when").isJsonObject())
                    names.addAll(object.getAsJsonObject("when").keySet());
            }
            names.removeAll(Set.of("OR", "AND"));
        }
        return names;
    }

    private static Set<String> ids(Path dir) throws IOException {
        Set<String> ids = new HashSet<>();
        if(!Files.isDirectory(dir))
            return ids;
        try(Stream<Path> files = Files.walk(dir)) {
            files.filter(file -> file.toString().endsWith(".json"))
                    .forEach(file -> ids.add(dir.relativize(file).toString().replace('\\', '/').replaceAll("\\.json$", "")));
        }
        return ids;
    }

    private static Path find(String relative) {
        for(Path root : RESOURCES)
            if(Files.exists(root.resolve(relative)))
                return root.resolve(relative);
        return null;
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    /** The project folder: the working folder or the nearest one above it with the mod's resources. */
    private static Path projectRoot() {
        for(Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent())
            if(Files.isDirectory(dir.resolve("src/main/resources/assets/radiocraft")))
                return dir;
        return Path.of("");
    }
}
