package net.hypixel.resourcepack.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hypixel.resourcepack.Converter;
import net.hypixel.resourcepack.Pack;
import net.hypixel.resourcepack.PackConverter;
import net.hypixel.resourcepack.Util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public class BlockStateConverter extends Converter {

    @Override
    public void rewrite(PackConverter main, Pack pack) throws IOException {
        Path states = pack.getPath().resolve("assets\\minecraft\\blockstates");
        if (!states.toFile().exists()) return;

        Files.list(states)
                .filter(state -> state.toString().endsWith(".json"))
                .forEach(state -> {
                    try {
                        JsonObject json = Util.readJson(state);

                        boolean anyChanges = false;
                        JsonObject variantsObject = json.getAsJsonObject("variants");
                        if (variantsObject != null) {
                            // change "normal" key to ""
                            JsonElement normal = variantsObject.get("normal");
                            if (normal instanceof JsonObject || normal instanceof JsonArray) {
                                variantsObject.add("", normal);
                                variantsObject.remove("normal");

                                anyChanges = true;
                            }

                            // update model paths to prepend block
                            for (Map.Entry<String, JsonElement> entry : variantsObject.entrySet()) {
                                if (entry.getValue() instanceof JsonObject) {
                                    JsonObject value = (JsonObject) entry.getValue();
                                    if (value.has("model")) {
                                        value.addProperty("model", "block/" + value.get("model").getAsString());
                                        anyChanges = true;
                                    }
                                } else if (entry.getValue() instanceof JsonArray) { // some states have arrays
                                    for (JsonElement jsonElement : ((JsonArray) entry.getValue())) {
                                        if (jsonElement instanceof JsonObject) {
                                            JsonObject value = (JsonObject) jsonElement;
                                            if (value.has("model")) {
                                                value.addProperty("model", "block/" + value.get("model").getAsString());
                                                anyChanges = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (anyChanges) {
                            Files.write(state, Collections.singleton(json.toString()), Charset.forName("UTF-8"));

                            if (PackConverter.DEBUG) System.out.println("      Converted " + state.getFileName());
                        }
                    } catch (IOException e) {
                        Util.propagate(e);
                    }
                });
    }
}