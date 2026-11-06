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
    public static void run() {
        try {
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
