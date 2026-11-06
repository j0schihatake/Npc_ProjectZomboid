"""Run the actual dev agent against small fake engine classes: exact world and loading state only."""
from pathlib import Path
import subprocess
import tempfile
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp);(p/'zombie/gameStates').mkdir(parents=True);(p/'zombie/iso').mkdir()
 (p/'zombie/GameWindow.java').write_text('package zombie; public class GameWindow {public static Machine states=new Machine(); public static class Machine {public Object current;}}')
 (p/'zombie/iso/IsoWorld.java').write_text('package zombie.iso; public class IsoWorld {public static IsoWorld instance=new IsoWorld(); public static String world;public String getWorld(){return world;}}')
 (p/'zombie/gameStates/GameLoadingState.java').write_text('package zombie.gameStates; public class GameLoadingState {private boolean forceDone;public boolean clicked(){return forceDone;}}')
 (p/'AgentTest.java').write_text('''
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import zombie.*;
import zombie.iso.*;
import zombie.gameStates.*;
public class AgentTest {
 public static void main(String[] args)throws Exception {
  Path control=Path.of(args[0]);String nonce="a".repeat(32),world="PersonalKnox-scenario-"+nonce;
  Files.writeString(control,"PersonalKnoxDevScenario1\\n"+world+"\\n"+nonce+"\\n10\\nloading\\n");
  var instrumentation=(Instrumentation)Proxy.newProxyInstance(AgentTest.class.getClassLoader(),new Class[]{Instrumentation.class},(a,b,c)->new Class[]{GameWindow.class});
  var wrong=new GameLoadingState();GameWindow.states.current=wrong;IsoWorld.world="normal-world";
  ScenarioAgent.premain(control.toString(),instrumentation);Thread.sleep(700);
  if(wrong.clicked())throw new AssertionError("Ordinary world acknowledged");
  var correct=new GameLoadingState();GameWindow.states.current=correct;IsoWorld.world=world;
  ScenarioAgent.premain(control.toString(),instrumentation);Thread.sleep(700);
  if(!correct.clicked())throw new AssertionError("Bound loading screen was not acknowledged");
  Files.writeString(control,"PersonalKnoxDevScenario1\\n"+world+"\\n"+nonce+"\\n10\\nOK\\n");Thread.sleep(700);
  if(Thread.getAllStackTraces().keySet().stream().anyMatch(t->t.getName().equals("PersonalKnox-dev-scenario")&&t.isAlive()))throw new AssertionError("Agent did not stop");
  System.out.println("Agent checks passed: ordinary world untouched, exact loading copy acknowledged, worker stops on completion.");
 }
}''')
 subprocess.run(['javac','--release','17','-d',str(p),str(root/'tools/dev_scenario_agent/ScenarioAgent.java'),*[str(f) for f in p.rglob('*.java')]],check=True)
 subprocess.run(['java','-cp',str(p),'AgentTest',str(p/'control')],check=True)
