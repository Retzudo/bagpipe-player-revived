package bagpipeplayer;

import java.io.File;

/**
 *
 * @author Christoph Willinger
 */
public class CustomFileFilter extends javax.swing.filechooser.FileFilter {
        @Override
        public boolean accept(File file) {
            // Allow only directories, or files with ".txt" extension
            return file.getAbsolutePath().endsWith(".abc") || file.getAbsolutePath().endsWith(".bww") || file.getAbsolutePath().endsWith(".bmw");
        }
        @Override
        public String getDescription() {
            // This description will be displayed in the dialog,
            // hard-coded = ugly, should be done via I18N
            return "Bagpipe Player files (*.abc, *.bww, *bmw)";
        }
        
        
}
