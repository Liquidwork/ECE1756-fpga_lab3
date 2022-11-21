public enum RAMMode {
    ROM{
        public double widthUtilization(){
            return 1.;
        }
    }, 
    SINGLEPORT{
        public double widthUtilization(){
            return 1.;
        }
    }, 
    SIMPLEDUALPORT{
        public double widthUtilization(){
            return 1.;
        }
    }, 
    TRUEDUALPORT{
        public double widthUtilization(){
            return 0.5;
        }
    };

    public abstract double widthUtilization();

    public static RAMMode parseMode(String s){
        for(RAMMode mode : RAMMode.values()){
            if (s.equalsIgnoreCase(mode.toString())) return mode;
        }
        return null;
    }
}
