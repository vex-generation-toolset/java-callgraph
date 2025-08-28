// Issue 415 
// Inspired from Baritone
// getBaritoneForPlayer is in an interface, hence not found

class Z {
    String aa;
}

final class FooAPI {

    private static final IBB provider;

    public static IBB getProvider() {
        return FooAPI.provider;
    }
}


interface IBB {

    Z getPrimaryBaritone();

    default Z getBaritoneForPlayer() {
        Z z = new Z();
        z.aa = "ss";
        return z;
    }
}

final class BB implements IBB {

    @Override
    public Z getPrimaryBaritone() {
        return new Z();
    }
}

public final class A {
    public void foo(BlockPos pos) {
        Z z = FooAPI.getProvider().getBaritoneForPlayer();/*<<<<<23,4,27,4,caller,A:foo(QBlockPos;)V*/
        String p = z.aa;
    }
}






