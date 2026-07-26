package com.foliagui.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

/**
 * {@link #toBase64}/{@link #fromBase64} use standard Java object serialization. {@link #toBase64Compact}/
 * {@link #fromBase64Compact} use Paper's NBT-based encoding instead, smaller and without the class-registry
 * overhead; prefer it unless you need compatibility with older tooling.
 */
public final class ItemStackSerializer {

    private ItemStackSerializer() {
    }

    /** Null entries (empty slots) round-trip back to null. */
    public static @NotNull String toBase64(@Nullable ItemStack @NotNull [] contents) {
        Objects.requireNonNull(contents, "contents cannot be null");
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataStream = new BukkitObjectOutputStream(byteStream)) {
            dataStream.writeInt(contents.length);
            for (ItemStack stack : contents) {
                dataStream.writeObject(stack);
            }
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialise ItemStack[] to base64", e);
        }
    }

    /** Expects a string produced by {@link #toBase64}; throws {@link IllegalStateException} otherwise. */
    public static @Nullable ItemStack @NotNull [] fromBase64(@NotNull String base64) {
        Objects.requireNonNull(base64, "base64 cannot be null");
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataStream = new BukkitObjectInputStream(byteStream)) {
            int length = dataStream.readInt();
            ItemStack[] contents = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                contents[i] = (ItemStack) dataStream.readObject();
            }
            return contents;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialise ItemStack[] from base64", e);
        }
    }

    /** Null entries (empty slots) round-trip back to null. */
    public static @NotNull String toBase64Compact(@Nullable ItemStack @NotNull [] contents) {
        Objects.requireNonNull(contents, "contents cannot be null");
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream dataStream = new DataOutputStream(byteStream)) {
            dataStream.writeInt(contents.length);
            for (ItemStack stack : contents) {
                if (stack == null) {
                    dataStream.writeInt(-1);
                } else {
                    byte[] bytes = stack.serializeAsBytes();
                    dataStream.writeInt(bytes.length);
                    dataStream.write(bytes);
                }
            }
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialise ItemStack[] to base64", e);
        }
    }

    /** Expects a string produced by {@link #toBase64Compact}; throws {@link IllegalStateException} otherwise. */
    public static @Nullable ItemStack @NotNull [] fromBase64Compact(@NotNull String base64) {
        Objects.requireNonNull(base64, "base64 cannot be null");
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             DataInputStream dataStream = new DataInputStream(byteStream)) {
            int length = dataStream.readInt();
            ItemStack[] contents = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                int size = dataStream.readInt();
                if (size < 0) {
                    contents[i] = null;
                } else {
                    byte[] bytes = new byte[size];
                    dataStream.readFully(bytes);
                    contents[i] = ItemStack.deserializeBytes(bytes);
                }
            }
            return contents;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialise ItemStack[] from base64", e);
        }
    }
}
