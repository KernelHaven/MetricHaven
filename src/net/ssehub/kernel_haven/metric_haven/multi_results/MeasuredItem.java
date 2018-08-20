package net.ssehub.kernel_haven.metric_haven.multi_results;

import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * An item that is measured by metrics; represents the following tuple: (file, included file, line number, element),
 * while the included file is optional. This usually represents a C function.
 * 
 * @author El-Sharkawy
 * @author Adam
 */
public class MeasuredItem implements Comparable<MeasuredItem> {
    
    private @NonNull String mainFile;
    
    private boolean considerIncludedFile;
    
    private @Nullable String includedFile;
    
    private int lineNo;
    
    private @NonNull String element;
    
    /**
     * Creates a new {@link MeasuredItem}.
     * 
     * @param sourceFile The measured source file (e.g., a C-file).
     * @param includedFile Optional: an included file (e.g., a H-file). If this is not <code>null</code>, then
     *      considerIncludedFile is set to true (but only for this element! you have to do it for other elements
     *          in the list, too).
     * @param lineNo The line number of the measured item.
     * @param element The measured item (e.g., the name of a function).
     */
    public MeasuredItem(@NonNull String sourceFile, @Nullable String includedFile, int lineNo,
            @NonNull String element) {
        
        this.mainFile = sourceFile;
        this.includedFile = includedFile;
        this.lineNo = lineNo;
        this.element = element;
        this.considerIncludedFile = includedFile != null;
    }
    
    /**
     * Creates a new {@link MeasuredItem} without an includedFile.
     * 
     * @param sourceFile The measured source file (e.g., a C-file).
     * @param lineNo The line number of the measured item.
     * @param element The measured item (e.g., the name of a function).
     */
    public MeasuredItem(@NonNull String sourceFile, int lineNo, @NonNull String element) {
        
        this.mainFile = sourceFile;
        this.lineNo = lineNo;
        this.element = element;
        this.considerIncludedFile = false;
    }
    
    /**
     * Returns the measured source file (e.g., a C-file).
     * @return The mainFile (e.g., a C-file).
     */
    public @NonNull String getMainFile() {
        return mainFile;
    }

    /**
     * Returns the included file.
     * @return The included file, e.g., a H-file or <tt>null</tt>.
     */
    public @Nullable String getIncludedFile() {
        return includedFile;
    }

    /**
     * Whether {@link #getIncludedFile()} should be considered. This is important if a list of {@link MeasuredItem}s
     * sometimes have an included file and sometimes don't. If any element inside a list of {@link MeasuredItem} has
     * an included file, then this is true for all the items in that list.
     * 
     * @return Whether any element in the list of {@link MeasuredItem} has an include file.
     */
    public boolean isConsiderIncludedFile() {
        return considerIncludedFile;
    }
    
    /**
     * Whether {@link #getIncludedFile()} should be considered. This is important if a list of {@link MeasuredItem}s
     * sometimes have an included file and sometimes don't. If any element inside a list of {@link MeasuredItem} has
     * an included file, then this is true for all the items in that list.
     * 
     * @param considerIncludedFile Whether any element in the list of {@link MeasuredItem} has an include file.
     */
    public void setConsiderIncludedFile(boolean considerIncludedFile) {
        this.considerIncludedFile = considerIncludedFile;
    }
    
    /**
     * Returns the line number.
     * @return The line number of the measured element, maybe  &lt; 0 if the line number could not be determined.
     */
    public int getLineNo() {
        return lineNo;
    }

    /**
     * Returns the measured element.
     * @return The measured element, e.g., the name of the measured function.
     */
    public @NonNull String getElement() {
        return element;
    }

    /**
     * Compare method to create a nice ordering of measured elements.
     * @see <a href="https://stackoverflow.com/a/4258738">https://stackoverflow.com/a/4258738</a>
     */
    @Override
    public int compareTo(MeasuredItem other) {
        // First: Order by main file
        int result = this.mainFile.compareTo(other.mainFile);
        
        if (0 == result) {
            String thisIncludedFile = this.includedFile;
            String otherIncludedFile = other.includedFile;
            // Second: All included files at the bottom
            if (thisIncludedFile == null) {
                if (otherIncludedFile == null) {
                    result = 0;
                } else {
                    result = 1;
                }
            } else {
                if (otherIncludedFile == null) {
                    result = -1;
                } else {
                    result = thisIncludedFile.compareTo(otherIncludedFile);
                }
            }
        }
        
        // Third: Function names in alphabetical order 
        if (0 == result) {
            result = this.element.compareTo(other.element);
        }
        
        // 4th: Order by line numbers.
        if (0 == result) {
            /* Should not be possible
             * (would mean that two functions with same name are at different positions in the same file)
             */
            result = Integer.compare(this.lineNo, other.lineNo);
        }
        
        return result;
    }
    
    @Override
    public int hashCode() {
        return 83 * mainFile.hashCode() + 71 * Integer.hashCode(lineNo) + 19 * element.hashCode()
                + (includedFile != null ? includedFile.hashCode() : 41);
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equal = false;
        
        if (obj instanceof MeasuredItem) {
            MeasuredItem other = (MeasuredItem) obj;
            equal = this.mainFile.equals(other.mainFile)
                    && this.lineNo == other.lineNo
                    && this.element.equals(other.element);
            
            if (equal) {
                String includedFile = this.includedFile;
                if (includedFile == null) {
                    equal = other.includedFile == null;
                } else {
                    equal = includedFile.equals(other.includedFile);
                }
            }
        }
        
        return equal;
    }
    
    @Override
    public String toString() {
        return "(" + mainFile + ", " + (includedFile != null ? includedFile + ", " : "") + lineNo + ", " + element
                + ")";
    }
    
}
