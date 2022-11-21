public enum RAMType {
    BRAM8192{
        public int size() {
            return 8192;
        }
        public int maxWidth() {
            return 32;
        }
        public int minWidth() {
            return 1;
        }
        public int lutRatio() {
            return 10;
        }
    }, 
    BRAM128k{
        public int size() {
            return 1024*128;
        }
        public int maxWidth() {
            return 128;
        }
        public int minWidth() {
            return 1;
        }
        public int lutRatio() {
            return 300;
        }
    }, 
    LUTRAM{
        public int size() {
            return 64*10;
        }
        public int maxWidth() {
            return 20;
        }
        public int minWidth() {
            return 10;
        }
        public int lutRatio() {
            return 2; // Half of the LUT can be used as LUTRAM
        }
        @Override
        public int lutImpl() {
            return 10;
        }
    };

    public abstract int size();
    public abstract int maxWidth();
    public abstract int minWidth();
    public abstract int lutRatio();
    public int lutImpl(){
        return 0;
    }
}
