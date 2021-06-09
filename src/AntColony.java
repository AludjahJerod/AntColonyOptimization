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
    private double Weight = 100;

    public int[] bestRoute= new int[7];
    public double bestValue;
    public static double[] tarifs= new double[] {2.686, 2.462, 2.379,3.074,4.718,4.752,3.835};
    public static double[] powerNeeded = new double[] {100,100,100,100,100,200,200};
    public static double Bmin=280;
    public static double Bmax=13720;
    public static double Qmin=-750;
    public static double Qmax=511.5;

    //Qi: cost/length of every road
    private double[] Qi = null;
    //trails: pheromone coefficient
    private double[][] Ti = null;
    private Random rand = new Random();

    private void Instantiate(int order){
        //
        //
        //
    }

    private class Ant{
        //path taken
        public int[] path = new int[epochNum];
        //Current epoch
        private int currentLocation=0;
        //probabilities of next epoch
        private double[] probs = null;
        //Has entered the discharge phase?
        private boolean discharge=false;
        //Oi Elements
        private double[] Oi= new double[epochNum];
        //Intermediate Variable Bi
        private double B=Bmin;

        public void reset(){
            currentLocation=0;
            discharge=true;
            B=Bmin;
        }

        private void probCalc() {

            double[][] TiCopy=Ti.clone();
            double denom = 0.0;
            double[] p=new double[Qi.length];
            for (int l = 0; l < Qi.length; l++){
                //conditions
                if(Qi[l]+B<Bmin || Qi[l]+B>Bmax ||(discharge && Qi[l]>0))
                    p[l]=0;
                else p[l]= pow(TiCopy[currentLocation][l], a)* pow(1.0 / (Qi[l]-Qi[0]+1), b); //I remove the lowest value in order to remove negative values
                denom+=p[l];
            }

            for (int j = 0; j < Qi.length; j++) {
                probs[j] = p[j] / denom;
            }
        }

        private int selectNext(){
            // calculate probabilities for each choice (stored in probs)
            probCalc();
            // randomly select according to probs
            double r = rand.nextDouble();
            double tot = 0;
            for (int i = 0; i < epochNum; i++) {
                tot += probs[i];
                if (tot >= r)
                    return i;
            }

            throw new RuntimeException("Not supposed to get here.");
        }

        public void takeRoute(int choice){
            path[currentLocation] = choice;
            B+=Qi[choice];
            if(Qi[choice]<0)
                discharge=true;
            currentLocation++;
        }

        public void getOi(){
            for (int i=0;i<epochNum;i++)
                if (Qi[path[i]] < 0) {
                    Oi[i] = powerNeeded[i] + Qi[path[i]] * 0.95;
                    if (Oi[i] < 0)
                        Oi[i] = 0;
                } else Oi[i] = powerNeeded[i] + Qi[path[i]] * 0.93;
        }

        public double getObjective(){
            double cost=0;
            double peak=0;

            for(int cnt=0; cnt<epochNum;cnt++){
                if(Oi[cnt]>peak)
                    peak=Oi[cnt];
                cost+=Oi[cnt]*tarifs[cnt];
            }

            return cost*alpha+peak*beta;

        }

        private void updateTi(){
            // evaporation
            for (int i = 0; i < epochNum; i++)
                for (int j = 0; j < Ti[0].length; j++)
                    Ti[i][j] *= evaporation;

            // ant contribution
            double contribution = Weight / getObjective();
            for (int i = 0; i < epochNum; i++)
                Ti[i][path[i]] += contribution;
        }

        private void updateBest() {

            if (bestRoute == null) {
                bestRoute = path;
                bestValue = getObjective();
            }
            if (getObjective() < bestValue) {
                bestValue = getObjective();
                bestRoute = path.clone();
            }
        }

        public void launchAnt(){
            reset();
            for(int i=0;i<epochNum;i++){
                takeRoute(selectNext());
            }
            getOi();
            //critical section
            updateTi();
            updateBest();
            //end of critical section
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