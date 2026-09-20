package UI.Components;

import java.awt.*;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

//Custom FlowLayout that automatically wraps components onto new rows when space runs out
public class WrapLayout extends FlowLayout {
    // Creates a wrap layout using default FlowLayout alignment and gaps
    public WrapLayout() {
        super();
    }

    // Creates a wrap layout with the specified alignment.
    public WrapLayout(int align) {
        super(align);
    }

    // Creates a wrap layout with the specified alignment and horizontal/vertical gaps.
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    // Returns the preferred layout size accounting for component wrapping.
    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    // Returns the minimum layout size accounting for component wrapping.
    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    // Computes the layout size by placing components into rows that wrap within the container width.
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {

            int targetWidth = target.getWidth();

            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);

                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    if (rowWidth + d.width > maxWidth) {
                        dim.width = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    rowWidth += d.width + hgap;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            dim.width = Math.max(dim.width, rowWidth);
            dim.height += rowHeight;

            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.bottom + insets.top + vgap * 2;

            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

            if (scrollPane != null) {
                dim.width -= (hgap + 1);
            }

            return dim;
        }
    }
}
