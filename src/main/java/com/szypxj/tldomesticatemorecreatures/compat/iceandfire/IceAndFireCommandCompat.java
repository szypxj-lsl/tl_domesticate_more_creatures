package com.szypxj.tldomesticatemorecreatures.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import com.szypxj.tldomesticatemorecreatures.command.pet.provider.PetCommandCompatibilityApi;
import net.minecraft.world.entity.Mob;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class IceAndFireCommandCompat {
    private static final int NEUTRAL_COMMAND = 0;
    private static final int FOLLOW_COMMAND = 2;
    private static final String STATE_KEY = "iceandfire:dragon_command";
    private static final String DRAGON_BASE_CLASS = "com.github.alexthe666.iceandfire.entity.EntityDragonBase";
    private static final Map<Class<?>, Accessor> ACCESSORS = new ConcurrentHashMap<>();
    private static final Set<Class<?>> UNSUPPORTED = ConcurrentHashMap.newKeySet();
    private static boolean registered;

    private IceAndFireCommandCompat() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        PetCommandCompatibilityApi.registerNativeStateProvider(new PetCommandCompatibilityApi.NativeStateProvider() {
            @Override
            public String id() {
                return STATE_KEY;
            }

            @Override
            public boolean supports(Mob mob, PetCommand command) {
                return IceAndFireCommandCompat.supports(mob) && switch (command) {
                    case FOLLOW, MOVE, RETREAT, LAND, DEFEND -> true;
                    default -> false;
                };
            }

            @Override
            public boolean enter(Mob mob, PetCommand command, PetCommandRuntimeState runtime) {
                return enterCommandState(mob, command, runtime);
            }

            @Override
            public boolean maintain(Mob mob, PetCommand command, PetCommandRuntimeState runtime) {
                return maintainCommandState(mob, command);
            }

            @Override
            public void restore(Mob mob, PetCommand command, PetCommandRuntimeState runtime) {
                restoreCommand(mob, command, runtime);
            }
        });
    }

    public static boolean supports(Mob mob) {
        return accessor(mob) != null;
    }

    private static boolean enterCommandState(Mob mob, PetCommand command, PetCommandRuntimeState runtime) {
        Accessor accessor = accessor(mob);
        if (accessor == null) {
            return false;
        }
        Integer current = accessor.getCommand(mob);
        if (current == null) {
            disable(mob.getClass());
            return false;
        }
        runtime.rememberNativeState(STATE_KEY, current);
        int desired = desiredCommand(command);
        return current == desired || accessor.setCommand(mob, desired);
    }

    private static boolean maintainCommandState(Mob mob, PetCommand command) {
        Accessor accessor = accessor(mob);
        if (accessor == null) {
            return false;
        }
        Integer current = accessor.getCommand(mob);
        if (current == null) {
            disable(mob.getClass());
            return false;
        }
        int desired = desiredCommand(command);
        if (current == desired) {
            return true;
        }
        if (!accessor.setCommand(mob, desired)) {
            disable(mob.getClass());
            return false;
        }
        return true;
    }

    private static void restoreCommand(Mob mob, PetCommand command, PetCommandRuntimeState runtime) {
        Accessor accessor = accessor(mob);
        if (accessor == null) {
            return;
        }
        runtime.previousNativeState(STATE_KEY).ifPresent(previous -> {
            int restore = command == PetCommand.FOLLOW && previous == FOLLOW_COMMAND
                    ? NEUTRAL_COMMAND
                    : previous;
            if (!accessor.setCommand(mob, restore)) {
                disable(mob.getClass());
            }
        });
    }

    private static int desiredCommand(PetCommand command) {
        return command == PetCommand.FOLLOW ? FOLLOW_COMMAND : NEUTRAL_COMMAND;
    }

    private static Accessor accessor(Mob mob) {
        if (mob == null || !isIceAndFireDragon(mob.getClass())) {
            return null;
        }
        Class<?> type = mob.getClass();
        if (UNSUPPORTED.contains(type)) {
            return null;
        }
        Accessor cached = ACCESSORS.get(type);
        if (cached != null) {
            return cached;
        }
        try {
            Accessor discovered = new Accessor(
                    type.getMethod("getCommand"),
                    type.getMethod("setCommand", int.class)
            );
            ACCESSORS.put(type, discovered);
            return discovered;
        } catch (ReflectiveOperationException ignored) {
            UNSUPPORTED.add(type);
            return null;
        }
    }

    private static boolean isIceAndFireDragon(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (DRAGON_BASE_CLASS.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void disable(Class<?> type) {
        ACCESSORS.remove(type);
        UNSUPPORTED.add(type);
    }

    private record Accessor(Method getCommandMethod, Method setCommandMethod) {
        private Integer getCommand(Mob mob) {
            try {
                Object value = getCommandMethod.invoke(mob);
                return value instanceof Number number ? number.intValue() : null;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }

        private boolean setCommand(Mob mob, int command) {
            try {
                setCommandMethod.invoke(mob, command);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
    }
}
