package personal.knox.npc.own;

/** The engine setDirectionAngle(float) contract is degrees, not atan2 radians. */
public final class OwnNpcBodyAdapterTest {
    public static final class Body {
        float x,y,z,angle; boolean moving;
        public float getX(){return x;}
        public float getY(){return y;}
        public void setX(float v){x=v;}
        public void setY(float v){y=v;}
        public void setZ(float v){z=v;}
        public void setDirectionAngle(float v){angle=v;}
        public void setMoving(boolean v){moving=v;}
        public void setAnimated(boolean v){}
    }
    public static class EnginePlayer {
        final Object cell,desc; final int x,y,z;
        public EnginePlayer(Object cell,Object desc,int x,int y,int z) {
            this.cell=cell;this.desc=desc;this.x=x;this.y=y;this.z=z;
        }
    }
    public static void run() {
        try {
        Class<?> type=OwnNpcBodyType.forBase(EnginePlayer.class);
        assert type!=EnginePlayer.class && type.getSuperclass()==EnginePlayer.class
                : "B42 exact-player renderer filter must not discard the NPC";
        assert OwnNpcBodyType.forBase(EnginePlayer.class)==type : "Generate once per engine class";
        Object cell=new Object(),desc=new Object();
        var npc=(EnginePlayer)type.getConstructor(Object.class,Object.class,int.class,int.class,int.class)
                .newInstance(cell,desc,10,20,2);
        assert npc.cell==cell && npc.desc==desc && npc.x==10 && npc.y==20 && npc.z==2
                : "Delegate the engine constructor without replacing its behavior";
        var method=OwnNpcBodyAdapter.class.getDeclaredMethod("move",String.class,Object.class,
                double.class,double.class,double.class,double.class,double.class,double.class);
        method.setAccessible(true);
        var adapter=new OwnNpcBodyAdapter();var body=new Body();
        method.invoke(adapter,"test",body,0d,0d,0d,0d,10d,0d);
        assert Math.abs(body.angle-90)<0.001 && body.moving : "Northward movement uses 90 degrees";
        method.invoke(adapter,"test",body,0d,0d,0d,-10d,0d,0d);
        assert Math.abs(Math.abs(body.angle)-180)<0.001 : "Westward movement uses 180 degrees";
        method.invoke(adapter,"test",body,0d,0d,0d,0d,0d,0d);
        assert !body.moving && Math.abs(Math.abs(body.angle)-180)<0.001 : "Idle preserves facing";
        System.out.println("Body adapter movement: engine degrees and idle facing verified.");
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
}
