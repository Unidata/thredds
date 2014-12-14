
////////////////////////////////////////////////
static double chosen = 30;
static double total = 200;
static double unchosen = total - chosen;

static double probChosen(int i) {
        return chosen / (total - i);
        }

static double probUnchosen(int i) {
        if (i < 0) return 1;
        return (unchosen - i) / (total - i);
        }

static double probUnchosenAccum(int i) {
        double accum = 1;
        for (int k=i; k>=0; k--) {
        accum *= probUnchosen(k);
        }
        return accum;
        }

public static void main(String[] args) {

        for (int k = 100; k < 200; k+=100) {
        total = k;
        unchosen = total - chosen;
        System.out.printf("chosen = %f total=%f  %n", chosen, total);

        double sum = 0;
        for (int i = 0; i < unchosen; i++) {
        double p = probChosen(i);
        double q = probUnchosenAccum(i-1);
        sum += i * p * q;
        System.out.printf("%d : p=%f q=%f (sum=%f)%n", i, p, q, i * p * q);
        }
        System.out.printf("result = %f%n", sum);
        }
        }