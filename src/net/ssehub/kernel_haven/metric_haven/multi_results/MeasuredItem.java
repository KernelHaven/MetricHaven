package net.ssehub.kernel_haven.metric_haven.multi_results;

/**
 * Part of {@link MetricsAggregator}, will be used to order the rows. A {@link MeasuredItem} represents the fowllowing
 * tuple: (file, included file, line number, element), while the included file is optional.
 * @author El-Sharkawy
 *
 */
class MeasuredItem implements Comparable<MeasuredItem> {
    
    private String mainFile;
    private String includedFile;
    private int lineNo;
    private String element;
    
    /**
     * Initializes the measured element (a row).
     * @param sourceFile The measured source file (e.g., a C-file).
     * @param includedFile Optional: an included file (e.g., a H-file).
     * @param lineNo The line number of the measured item.
     * @param element The measured item (e.g., the name of a function).
     */
    MeasuredItem(String sourceFile, String includedFile, int lineNo, String element) {
        this.mainFile = sourceFile;
        this.includedFile = includedFile;
        this.lineNo = lineNo;
        this.element = element;
    }
    
    /**
     * Returns the measured source file (e.g., a C-file).
     * @return The mainFile (e.g., a C-file).
     */
    public String getMainFile() {
        return mainFile;
    }

    /**
     * Returns the included file.
     * @return The included file, e.g., a H-file or <tt>null</tt>.
     */
    public String getIncludedFile() {
        return includedFile;
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
    public String getElement() {
        return element;
    }

    /**
     * Compare method to create a nice ordering of measured elements.
     * @see <a href="https://stackoverflow.com/a/4258738">https://stackoverflow.com/a/4258738</a>
     */
    @Override
    public int compareTo(MeasuredItem other) {
        int result = this.mainFile.compareTo(other.mainFile);
        
        // First: Order by main file
        if (0 == result) {
            // Second: All included files at the bottum
            if (null == this.includedFile && null != other.includedFile) {
                result = 1;
            } else if (null != this.includedFile && null == other.includedFile) {
                result = -1;
            } else if (null == this.includedFile && null == other.includedFile) {
                result = 0; 
            } else {
                result = this.includedFile.compareTo(other.includedFile);
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
}
