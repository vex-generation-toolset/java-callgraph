public class Baritone extends AbstractBaritone {
    @Override
    public BaritoneProcessor getBaritoneProcess() {
        return new BaritoneProcessor();
    }
}