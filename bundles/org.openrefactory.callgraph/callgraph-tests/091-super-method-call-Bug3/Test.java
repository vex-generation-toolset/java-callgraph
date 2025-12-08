// Issue 3
// Extended call graph entry for a super method call in an inheritance hierarchy

package org.openrefactory.test;

class BaseInner {
    String display() { return "base inner"; }
}

class Outer {
    String display() { return "outer"; }
}

class SubOuter extends Outer {
    String display() { return "sub"; }

    class Inner extends BaseInner {
        String m;
        void show() {
            // This calls BaseInner.display(), NOT Outer or SubOuter
            m = super.display();
        }
    }

    public static void main(String[] args) {
        SubOuter.Inner inner = new SubOuter().new Inner();
        inner.show();
        System.out.println(inner.m); // prints "base inner"
    }
}

/*$$$$$ org.openrefactory.test.SubOuter.main(String[]), 2,
   		26,32, 2, org.openrefactory.test.SubOuter.<init>(), org.openrefactory.test.SubOuter.Inner.<init>(),
   		27,9, 1, org.openrefactory.test.SubOuter.Inner#show(),
*/

/*$$$$$ org.openrefactory.test.SubOuter.Inner#show(), 1,
   		21,17, 1, org.openrefactory.test.BaseInner#display(),
*/
