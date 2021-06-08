import java.util.Random;

public class AntColony {

    private double evaporation=0.5;
    private int epochNum=7;

    //number of ants (threads)
    private double antNum;

    private double a=1;
    private double b=5;

    public double alpha;
    public double beta;

    // new trail deposit coefficient;
    private double Q = 500;

    public int[] bestRoute= new int[7];
    public double bestValue;
    public static double[] tarifs= new double[] {2.686, 2.462, 2.379,3.074,4.718,4.752,3.835};
    public static double[] powerNeeded = new double[] {100,100,100,100,100,200,200};

    private double probs[] = null;
    private double graph[][] = null;
    private double trails[][] = null;

    private class Ant{
        public int tour[] = new int[epochNum];
        private int currentLocation=0;

        public void takeRoute(int choice){
            tour[currentLocation] = choice;
            currentLocation++;
        }

        public double totalCost(){
            double total=0;
            for (int i=0;i<epochNum;i++){
                total += graph[i][tour[i]];
            }
            return total;
        }

        public void reset(){
            currentLocation=0;
        }

        private void probTo() {

            double denom = 0.0;
            for (int l = 0; l < graph[0].length; l++)
                denom += pow(trails[currentLocation][l], a)* pow(1.0 / graph[currentLocation][l], b);


            for (int j = 0; j < graph[0].length; j++) {
                double numerator = pow(trails[currentLocation][j], a)* pow(1.0 / graph[currentLocation][j], b);
                probs[j] = numerator / denom;

            }
        }
    }
    // Important facts:
    // - >25 times faster
    // - Extreme cases can lead to error of 25% - but usually less.
    // - Does not harm results -- not surprising for a stochastic algorithm.
    public static double pow(final double a, final double b) {
        final int x = (int) (Double.doubleToLongBits(a) >> 32);
        final int y = (int) (b * (x - 1072632447) + 1072632447);
        return Double.longBitsToDouble(((long) y) << 32);
    }


}