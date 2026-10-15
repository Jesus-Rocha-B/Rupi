package pe.rupi.api.report;

import java.math.*;
import java.util.List;

/** Puntajes de Rupi, no valoración oficial de competencias. */
public final class GradeCalculator {
    private GradeCalculator() {}
    public record Score(BigDecimal earned, BigDecimal possible) {}
    public record Average(BigDecimal value, double sortValue, String letter) {}
    private record Fraction(BigInteger numerator, BigInteger denominator) {
        Fraction add(Fraction other) {
            var n=numerator.multiply(other.denominator).add(other.numerator.multiply(denominator));
            var d=denominator.multiply(other.denominator);var gcd=n.gcd(d);
            return new Fraction(n.divide(gcd),d.divide(gcd));
        }
    }
    public static boolean valid(Score score) {
        return score!=null && score.earned()!=null && score.possible()!=null && score.possible().signum()>0
            && score.earned().signum()>=0 && score.earned().compareTo(score.possible())<=0;
    }
    public static Average average(List<Score> scores) {
        if(scores.isEmpty()) return null;
        var sum=new Fraction(BigInteger.ZERO,BigInteger.ONE);
        for(var score:scores) {
            if(!valid(score)) throw new IllegalArgumentException("Puntaje fuera de rango");
            int scale=Math.max(score.earned().scale(),score.possible().scale());
            sum=sum.add(new Fraction(score.earned().setScale(scale).unscaledValue().multiply(BigInteger.valueOf(20)),
                score.possible().setScale(scale).unscaledValue()));
        }
        var denominator=sum.denominator().multiply(BigInteger.valueOf(scores.size()));
        String letter="C";
        for(int threshold:new int[]{18,14,11}) {
            if(sum.numerator().compareTo(denominator.multiply(BigInteger.valueOf(threshold)))>=0) {
                letter=threshold==18?"AD":threshold==14?"A":"B";break;
            }
        }
        var n=new BigDecimal(sum.numerator());var d=new BigDecimal(denominator);
        return new Average(n.divide(d,1,RoundingMode.HALF_UP),n.divide(d,MathContext.DECIMAL128).doubleValue(),letter);
    }
}
