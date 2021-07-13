import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class AntColony {

    private double evaporation=0.9999;
    private int epochNum=7;

    //number of ants (threads)
    private double antNum;

    private double a=1;
    private double b=1;

    public double alpha;
    public double beta;

    // new trail deposit coefficient;
    private double Weight = 50;

    //critical section
    public int[] bestRoute= new int[7];
    public double bestValue=Double.POSITIVE_INFINITY;

    public int bestCounter=0;

    public static double[] tarifs= new double[] {2.686, 2.462, 2.379,3.074,4.718,4.752,3.835};
    public static double[] powerNeeded = new double[] {100,100,100,100,100,200,200};
    public static double Bmin=280;
    public static double Bmax=13720;
    public static double Qmin=-750;
    public static double Qmax=511.5;

    //Qi: cost/length of every road
    private double[] Qi = null;
    //Ti: pheromone coefficient on roads
    private double[][] Ti = null;//critical common data

    private Random rand = new Random();

    //ReaderWriter
    int numReaders = 0;
    Semaphore mutex = new Semaphore(1);
    Semaphore lock = new Semaphore(1);

    public void startRead() {
        try {
            mutex.acquire();
            numReaders++;
            if (numReaders == 1) lock.acquire();
            mutex.release();
        } catch (InterruptedException e) { e.printStackTrace(); }
    }
    public void endRead() {
        try {
            mutex.acquire();
            numReaders--;
            if (numReaders == 0) lock.release();
            mutex.release();
        } catch (InterruptedException e) { e.printStackTrace(); }
    }
    public void startWrite() {
        try {
            lock.acquire();
        } catch (InterruptedException e) { e.printStackTrace(); }
    }
    public void endWrite() {
        lock.release();
    }

    private void Instantiate(){
        //Linspace
        //
        int size=(int)(Qmax-Qmin)*2;
        size+=2;
        Qi= new double[size];
        Ti= new double[size][epochNum];
        double val = Qmin;
        for (int i=0;i<size;i++){
            Qi[i]=val;
            for (int j = 0; j<epochNum; j++){
                Ti[i][j]=1;
            }
            val+=0.5;
        }
    }

    private class Ant extends Thread{
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
            probs= new double[Qi.length];
            currentLocation=0;
            discharge=false;
            B=Bmin;
        }

        private void probCalc() {

            //reader
            startRead();
            double[][] TiCopy=Ti.clone();
            endRead();
            // end reader
            double denom = 0.0;
            double[] p=new double[Qi.length];
            double pNeed=0;
            //check how much power is still needed
            for(int i=currentLocation; i<epochNum;i++){
                pNeed+=powerNeeded[i];
            }
            for (int l = 0; l < Qi.length; l++){
                //conditions
                if(Qi[l]+B<Bmin || Qi[l]+B>Bmax ||(discharge && Qi[l]>0) ||(Qi[l]<0 && B-Bmin<pNeed)||(Qi[l]<-powerNeeded[currentLocation]/0.95))
                    p[l]=0;
                else p[l]= pow(TiCopy[l][currentLocation], a)* pow(1.0 / (Qi[l]-Qi[0]+1), b); //I remove the lowest value in order to remove negative values
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
            for (int i = 0; i < Qi.length; i++) {
                tot += probs[i];
                if (tot >= r)
                    return i;
            }

            throw new RuntimeException("Not supposed to get here.");
        }

        public void takeRoute(int choice){
            path[currentLocation] = choice;
            B+=Qi[choice];
            if(Qi[choice]<0 && !discharge)
                discharge=true;
            currentLocation++;
        }

        public void getOi(){
            for (int i=0;i<epochNum;i++)
                if (Qi[path[i]] < 0) {
                    Oi[i] = powerNeeded[i] + Qi[path[i]] * 0.95;
                    if (Oi[i] < 0)
                        Oi[i] = 0; //For safety
                } else Oi[i] = powerNeeded[i] + Qi[path[i]] / 0.93;
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
                for (int j = 0; j < Ti.length; j++)
                    Ti[j][i] *= evaporation;

            // ant contribution
            double contribution = Weight / getObjective();
            for (int i = 0; i < epochNum; i++)
                Ti[path[i]][i] += contribution;
        }

        private void updateBest() {

            if (getObjective() < bestValue) {
                bestValue = getObjective();
                bestRoute = path.clone();
                bestCounter=0;
                System.out.println("the best value is:" + bestValue);
                System.out.println(Qi[bestRoute[0]]+","+Qi[bestRoute[1]]+","+Qi[bestRoute[2]]+","+Qi[bestRoute[3]]+","+Qi[bestRoute[4]]+","+Qi[bestRoute[5]]+","+Qi[bestRoute[6]]);
            }else if(getObjective()==bestValue || getObjective()-bestValue<1){
                bestCounter++;
                System.out.println("+");
            }
        }

        public void run(){
            do {
                reset();
                for (int i = 0; i < epochNum; i++) {
                    takeRoute(selectNext());
                }
                //System.out.println(Qi[path[0]]+","+Qi[path[1]]+","+Qi[path[2]]+","+Qi[path[3]]+","+Qi[path[4]]+","+Qi[path[5]]+","+Qi[path[6]]);
                getOi();
                //critical section
                startWrite();

                updateTi();
                updateBest();

                endWrite();
                //end of critical section
            }while (bestCounter<5);
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

    public void MyAntColony(){
        antNum=5;
        Instantiate();

        alpha=1;
        beta=0;
        Ant a1;
        Ant a2,a3,a4,a5,a6,a7,a8,a9,a10;
        a1=new Ant();
        a2=new Ant();
        a3=new Ant();
        a4=new Ant();
        a5=new Ant();
        a6=new Ant();
        a7=new Ant();
        a8=new Ant();
        a9=new Ant();
        a10=new Ant();
        a1.start();
        a2.start();
        a3.start();
        a4.start();
        a5.start();
        a6.start();
        a7.start();
        a8.start();
        a9.start();
        a10.start();

        try {
            a1.join();
            a2.join();
            a3.join();
            a4.join();
            a5.join();
            a6.join();
            a7.join();
            a8.join();
            a9.join();
            a10.join();
        } catch ( InterruptedException e ) { }

        System.out.println(bestValue);
        System.out.println(Arrays.toString(bestRoute));
    }

    public static class Demo{
        public static void main(String[] args){
            AntColony myColony = new AntColony();
            myColony.MyAntColony();

        }
    }

}