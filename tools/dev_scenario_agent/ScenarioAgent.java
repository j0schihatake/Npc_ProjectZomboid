import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dev JVM only: acknowledge the loading screen for the exact token-bound copy.
 * Native loading readiness checks remain in GameLoadingState.update(). */
public final class ScenarioAgent {
    public static void agentmain(String argument, Instrumentation instrumentation) {
        premain(argument,instrumentation);
    }
    public static void premain(String argument, Instrumentation instrumentation) {
        Thread thread=new Thread(() -> run(Path.of(argument),instrumentation),"PersonalKnox-dev-scenario");
        thread.setDaemon(true);
        thread.start();
    }
    private static void run(Path control, Instrumentation instrumentation) {
        try {
            List<String> initial=Files.readAllLines(control);
            if(initial.size()!=5 || !initial.get(0).equals("PersonalKnoxDevScenario1")
                || !initial.get(2).matches("[0-9a-f]{32}")
                || !initial.get(1).equals("PersonalKnox-scenario-"+initial.get(2))) return;
            long deadline=System.nanoTime()+1_800_000_000_000L;
            while(System.nanoTime()<deadline) {
                List<String> current=Files.readAllLines(control);
                if(current.size()!=5 || !current.get(2).equals(initial.get(2))) return;
                if(current.get(4).equals("OK") || current.get(4).equals("FAIL")) return;
                if(current.get(4).equals("loading")) {
                    Class<?> window=null;
                    for(Class<?> type:instrumentation.getAllLoadedClasses())
                        if(type.getName().equals("zombie.GameWindow")) {window=type;break;}
                    if(window!=null) {
                        Object machine=window.getField("states").get(null);
                        Object state=machine==null?null:machine.getClass().getField("current").get(machine);
                        if(state!=null && state.getClass().getName().equals("zombie.gameStates.GameLoadingState")) {
                            ClassLoader loader=window.getClassLoader();
                            Class<?> worldClass=Class.forName("zombie.iso.IsoWorld",false,loader);
                            Object world=worldClass.getField("instance").get(null);
                            String name=(String)worldClass.getMethod("getWorld").invoke(world);
                            if(!name.equals(initial.get(1))) return;
                            Field force=state.getClass().getDeclaredField("forceDone");
                            force.setAccessible(true);
                            force.setBoolean(state,true);
                        }
                        if(state!=null && state.getClass().getName().equals("zombie.gameStates.IngameState")) return;
                    }
                }
                Thread.sleep(500);
            }
        } catch(Exception error) {
            System.err.println("[PersonalKnox][dev-scenario-agent] "+error);
        }
    }
}
