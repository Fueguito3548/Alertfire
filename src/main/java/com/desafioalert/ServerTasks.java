package com.desafioalert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Pequeno programador de tareas del servidor: ejecuta un Runnable pasados N ticks. */
@Mod.EventBusSubscriber(modid = DesafioAlert.MODID)
public final class ServerTasks {
    private static final class Task {
        int ticks;
        final Runnable action;

        Task(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private ServerTasks() {
    }

    public static void schedule(int ticks, Runnable action) {
        if (ticks <= 0) {
            action.run();
        } else {
            TASKS.add(new Task(ticks, action));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) {
            return;
        }
        List<Runnable> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (--task.ticks <= 0) {
                due.add(task.action);
                it.remove();
            }
        }
        for (Runnable action : due) {
            action.run();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }
}
