// Issue 25
// Subclass of a Library Adapter or Abstract Convenience Class

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MyMouseAdapter extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("Mouse clicked");
        super.mouseClicked(e);  // calls MouseAdapter.mouseClicked() (no-op implementation)
    }
}

/*$$$$$ MyMouseAdapter#mouseClicked(MouseEvent), 1,
  		11,9, 1, java.awt.event.MouseAdapter#mouseClicked(MouseEvent),
*/
