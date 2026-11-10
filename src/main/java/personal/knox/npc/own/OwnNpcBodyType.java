package personal.knox.npc.own;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.matcher.ElementMatchers;

/** Engine-only subtype, generated once: B42 FBO skips exact IsoPlayer instances.
 * It inherits engine behavior and constructors; all decisions stay in our Java core.
 * Resolving the engine at runtime keeps the addon build independent of Java 25 classes.
 */
final class OwnNpcBodyType {
    private static final ClassValue<Class<?>> TYPES = new ClassValue<>() {
        @Override protected Class<?> computeValue(Class<?> base) {
            synchronized (OwnNpcBodyType.class) {
                String name="personal.knox.npc.own.generated.PersonalKnoxNpc_"+base.getSimpleName()+"_"+Integer.toHexString(base.getName().hashCode());
                try {
                    Class<?> existing=Class.forName(name,false,base.getClassLoader());
                    if(existing.getSuperclass()!=base) throw new IllegalStateException("Unexpected NPC body type: "+name);
                    return existing;
                } catch(ClassNotFoundException missing) {
                    return new ByteBuddy()
                            .subclass(base,ConstructorStrategy.Default.IMITATE_SUPER_CLASS_PUBLIC)
                            .name(name)
                            .method(ElementMatchers.named("updateLOS").or(ElementMatchers.named("updateAlpha")))
                            .intercept(StubMethod.INSTANCE)
                            .make()
                            .load(OwnNpcBodyType.class.getClassLoader(),ClassLoadingStrategy.Default.INJECTION)
                            .getLoaded();
                }
            }
        }
    };
    static Class<?> forBase(Class<?> base) { return TYPES.get(base); }
    private OwnNpcBodyType() { }
}
