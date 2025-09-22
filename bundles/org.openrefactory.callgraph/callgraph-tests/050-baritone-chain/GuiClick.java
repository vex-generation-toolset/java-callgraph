public class GuiClick extends GuiScreen {
    private BlockPos clickStart;
    private BlockPos currentMouseOver;
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {/*<<<5,4,12,4,callee,Z:foo()V*/
        if (mouseButton == 0) {
            if (clickStart != null) {
                ((Baritone) BaritoneAPI.getProvider().getPrimaryBaritone()).getBaritoneProcess().clearArea(clickStart, currentMouseOver);
                clickStart = null;
            }
        }
    }
}