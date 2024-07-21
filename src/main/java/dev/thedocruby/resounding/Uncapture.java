package dev.thedocruby.resounding;

import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Objects;
import java.util.function.*;

public class Uncapture {

    @FunctionalInterface
    public interface QuadriFunction<A,B,C,D,R> {

        R apply(A a, B b, C c, D d);

        default <V> QuadriFunction<A, B, C, D, V> andThen(
                Function<? super R, ? extends V> after) {
            Objects.requireNonNull(after);
            return (A a, B b, C c, D d) -> after.apply(apply(a, b, c, d));
        }
    }
    @FunctionalInterface
    public interface PentaFunction<A,B,C,D,E,R> {

        R apply(A a, B b, C c, D d, E e);

        default <V> PentaFunction<A, B, C, D, E, V> andThen(
                Function<? super R, ? extends V> after) {
            Objects.requireNonNull(after);
            return (A a, B b, C c, D d, E e) -> after.apply(apply(a, b, c, d, e));
        }
    }
    @FunctionalInterface
    public interface HexaFunction<A,B,C,D,E,F,R> {

        R apply(A a, B b, C c, D d, E e, F f);

        default <V> HexaFunction<A, B, C, D, E, F, V> andThen(
                Function<? super R, ? extends V> after) {
            Objects.requireNonNull(after);
            return (A a, B b, C c, D d, E e, F f) -> after.apply(apply(a, b, c, d, e, f));
        }
    }

    @FunctionalInterface
    public interface QuadriConsumer<A,B,C,D> {

        void accept(A a, B b, C c, D d);

        default QuadriConsumer<A, B, C, D> andThen(
                QuadriConsumer<? super A, ? super B, ? super C, ? super D> after) {
            Objects.requireNonNull(after);
            return after::accept;
        }
    }
    @FunctionalInterface
    public interface PentaConsumer<A,B,C,D,E> {

        void accept(A a, B b, C c, D d, E e);

        default PentaConsumer<A, B, C, D, E> andThen(
                PentaConsumer<? super A, ? super B, ? super C, ? super D, ? super E> after) {
            Objects.requireNonNull(after);
            return after::accept;
        }
    }
    @FunctionalInterface
    public interface HexaConsumer<A,B,C,D,E,F> {

        void accept(A a, B b, C c, D d, E e, F f);

        default HexaConsumer<A, B, C, D, E, F> andThen(
                HexaConsumer<? super A, ? super B, ? super C, ? super D, ? super E, ? super F> after) {
            Objects.requireNonNull(after);
            return after::accept;
        }
    }

    // TODO determine where Action FunctionalInterface is -> () -> { accepts nothing, does something stateful, returns void }

    // Uncapture external variables from large lambdas, resulting in faster execution with less resource usage.
    // A smaller, captured lambda is. If the passed lambda is smaller than the returned one. Do not use this function.
    // Turns: O(n) where n is the arbitrary complexity of the given lambda.
    // Into:  O(1) where 1 is this constant, small, captured lambda.
    public static <C1 , OUT> Supplier<OUT> supplier(C1 capture1, Function<C1,OUT> lambda) {
        return () -> lambda.apply(capture1);
    }  // 1 -> 0 -> 1
    public static <C1,C2 , OUT> Supplier<OUT> supplier(C1 capture1, C2 capture2, BiFunction<C1,C2,OUT> lambda) {
        return () -> lambda.apply(capture1, capture2);
    }  // 2 -> 0 -> 1
    public static <C1,C2,C3 , OUT> Supplier<OUT> supplier(C1 capture1, C2 capture2, C3 capture3, TriFunction<C1,C2,C3,OUT> lambda) {
        return () -> lambda.apply(capture1, capture2, capture3);
    }  // 3 -> 0 -> 1
    public static <C1,C2,C3,C4 , OUT> Supplier<OUT> supplier(C1 capture1, C2 capture2, C3 capture3, C4 capture4, QuadriFunction<C1,C2,C3,C4,OUT> lambda) {
        return () -> lambda.apply(capture1, capture2, capture3, capture4);
    }  // 4 -> 0 -> 1
    public static <C1,C2,C3,C4,C5 , OUT> Supplier<OUT> supplier(C1 capture1, C2 capture2, C3 capture3, C4 capture4, C5 capture5, PentaFunction<C1,C2,C3,C4,C5,OUT> lambda) {
        return () -> lambda.apply(capture1, capture2, capture3, capture4, capture5);
    }  // 5 -> 0 -> 1
    public static <C1,C2,C3,C4,C5,C6 , OUT> Supplier<OUT> supplier(C1 capture1, C2 capture2, C3 capture3, C4 capture4, C5 capture5, C6 capture6, HexaFunction<C1,C2,C3,C4,C5,C6,OUT> lambda) {
        return () -> lambda.apply(capture1, capture2, capture3, capture4, capture5, capture6);
    }  // 6 -> 0 -> 1

    public static <C1, I1, OUT> Function<I1,OUT> function(C1 capture1, BiFunction<C1,I1,OUT> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, internal1);
    }  // 1 -> 1 -> 1
    public static <C1,C2, I1, OUT> Function<I1,OUT> function(C1 capture1, C2 capture2, TriFunction<C1,C2,I1,OUT> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, capture2, internal1);
    }  // 2 -> 1 -> 1
    public static <C1,C2,C3, I1, OUT> Function<I1,OUT> function(C1 capture1, C2 capture2, C3 capture3, QuadriFunction<C1,C2,C3,I1,OUT> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, capture2, capture3, internal1);
    }  // 3 -> 1 -> 1
    public static <C1,C2,C3,C4, I1, OUT> Function<I1,OUT> function(C1 capture1, C2 capture2, C3 capture3, C4 capture4, PentaFunction<C1,C2,C3,C4, I1,OUT> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, capture2, capture3, capture4, internal1);
    }  // 4 -> 1 -> 1
    public static <C1,C2,C3,C4,C5, I1, OUT> Function<I1,OUT> function(C1 capture1, C2 capture2, C3 capture3, C4 capture4, C5 capture5, HexaFunction<C1,C2,C3,C4,C5, I1,OUT> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, capture2, capture3, capture4, capture5, internal1);
    }  // 5 -> 1 -> 1

    public static <C1, I1,I2, OUT> BiFunction<I1,I2,OUT> function(C1 capture1, TriFunction<C1,I1,I2,OUT> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.apply(capture1, internal1, internal2);
    }  // 1 -> 2 -> 1
    public static <C1,C2, I1,I2, OUT> BiFunction<I1,I2,OUT> function(C1 capture1, C2 capture2, QuadriFunction<C1,C2,I1,I2,OUT> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.apply(capture1, capture2, internal1, internal2);
    }  // 2 -> 2 -> 1
    public static <C1,C2,C3, I1,I2, OUT> BiFunction<I1,I2,OUT> function(C1 capture1, C2 capture2, C3 capture3, PentaFunction<C1,C2,C3,I1,I2,OUT> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.apply(capture1, capture2, capture3, internal1, internal2);
    }  // 3 -> 2 -> 1
    public static <C1,C2,C3,C4, I1,I2, OUT> BiFunction<I1,I2,OUT> function(C1 capture1, C2 capture2, C3 capture3, C4 capture4, HexaFunction<C1,C2,C3,C4,I1,I2,OUT> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.apply(capture1, capture2, capture3, capture4, internal1, internal2);
    }  // 4 -> 2 -> 1

    public static <C1, I1,I2,I3, OUT> TriFunction<I1,I2,I3,OUT> function(C1 capture1, QuadriFunction<C1,I1,I2,I3,OUT> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3) -> lambda.apply(capture1, internal1, internal2, internal3);
    }  // 1 -> 3 -> 1
    public static <C1,C2, I1,I2,I3, OUT> TriFunction<I1,I2,I3,OUT> function(C1 capture1, C2 capture2, PentaFunction<C1,C2,I1,I2,I3,OUT> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3) -> lambda.apply(capture1, capture2, internal1, internal2, internal3);
    }  // 2 -> 3 -> 1
    public static <C1,C2,C3, I1,I2,I3, OUT> TriFunction<I1,I2,I3,OUT> function(C1 capture1, C2 capture2, C3 capture3, HexaFunction<C1,C2,C3,I1,I2,I3,OUT> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3) -> lambda.apply(capture1, capture2, capture3, internal1, internal2, internal3);
    }  // 3 -> 3 -> 1

    public static <C1, I1,I2,I3,I4, OUT> QuadriFunction<I1,I2,I3,I4,OUT> function(C1 capture1, PentaFunction<C1,I1,I2,I3,I4,OUT> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3, I4 internal4) -> lambda.apply(capture1, internal1, internal2, internal3, internal4);
    }  // 1 -> 4 -> 1
    public static <C1,C2, I1,I2,I3,I4, OUT> QuadriFunction<I1,I2,I3,I4,OUT> function(C1 capture1, C2 capture2, HexaFunction<C1,C2,I1,I2,I3,I4,OUT> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3, I4 internal4) -> lambda.apply(capture1, capture2, internal1, internal2, internal3, internal4);
    }  // 2 -> 4 -> 1

    public static <C1, I1,I2,I3,I4,I5, OUT> PentaFunction<I1,I2,I3,I4,I5,OUT> function(C1 capture1, HexaFunction<C1,I1,I2,I3,I4,I5,OUT> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3, I4 internal4, I5 internal5) -> lambda.apply(capture1, internal1, internal2, internal3, internal4, internal5);
    }  // 1 -> 5 -> 1


    // same as above, but no longer returning values
    public static <C1, I1 > Consumer<I1> consumer(C1 capture1, BiConsumer<C1,I1> lambda) {
        return (I1 internal1) -> lambda.accept(capture1, internal1);
    }  // 1 -> 1 -> 0
    public static <C1,C2, I1 > Consumer<I1> consumer(C1 capture1, C2 capture2, TriConsumer<C1,C2,I1> lambda) {
        return (I1 internal1) -> lambda.accept(capture1, capture2, internal1);
    }  // 2 -> 1 -> 0
    public static <C1,C2,C3, I1 > Consumer<I1> consumer(C1 capture1, C2 capture2, C3 capture3, QuadriConsumer<C1,C2,C3,I1> lambda) {
        return (I1 internal1) -> lambda.accept(capture1, capture2, capture3, internal1);
    }  // 3 -> 1 -> 0
    public static <C1,C2,C3,C4, I1 > Consumer<I1> consumer(C1 capture1, C2 capture2, C3 capture3, C4 capture4, PentaConsumer<C1,C2,C3,C4,I1> lambda) {
        return (I1 internal1) -> lambda.accept(capture1, capture2, capture3, capture4, internal1);
    }  // 4 -> 1 -> 0
    public static <C1,C2,C3,C4,C5, I1 > Consumer<I1> consumer(C1 capture1, C2 capture2, C3 capture3, C4 capture4, C5 capture5, HexaConsumer<C1,C2,C3,C4,C5,I1> lambda) {
        return (I1 internal1) -> lambda.accept(capture1, capture2, capture3, capture4, capture5, internal1);
    }  // 5 -> 1 -> 0

    public static <C1, I1,I2 > BiConsumer<I1,I2> consumer(C1 capture1, TriConsumer<C1,I1,I2> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.accept(capture1, internal1, internal2);
    }  // 1 -> 2 -> 0
    public static <C1,C2, I1,I2 > BiConsumer<I1,I2> consumer(C1 capture1, C2 capture2, QuadriConsumer<C1,C2,I1,I2> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.accept(capture1, capture2, internal1, internal2);
    }  // 2 -> 2 -> 0
    public static <C1,C2,C3, I1,I2 > BiConsumer<I1,I2> consumer(C1 capture1, C2 capture2, C3 capture3, PentaConsumer<C1,C2,C3,I1,I2> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.accept(capture1, capture2, capture3, internal1, internal2);
    }  // 3 -> 2 -> 0
    public static <C1,C2,C3,C4, I1,I2 > BiConsumer<I1,I2> consumer(C1 capture1, C2 capture2, C3 capture3, C4 capture4, HexaConsumer<C1,C2,C3,C4,I1,I2> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.accept(capture1, capture2, capture3, capture4, internal1, internal2);
    }  // 4 -> 2 -> 0

    public static <C1, I1,I2,I3 > TriConsumer<I1,I2,I3> consumer(C1 capture1, QuadriConsumer<C1,I1,I2,I3> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3) -> lambda.accept(capture1, internal1, internal2, internal3);
    }  // 1 -> 3 -> 0
    public static <C1,C2, I1,I2,I3 > TriConsumer<I1,I2,I3> consumer(C1 capture1, C2 capture2, PentaConsumer<C1,C2,I1,I2,I3> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3) -> lambda.accept(capture1, capture2, internal1, internal2, internal3);
    }  // 2 -> 3 -> 0
    public static <C1,C2,C3, I1,I2,I3 > TriConsumer<I1,I2,I3> consumer(C1 capture1, C2 capture2, C3 capture3, HexaConsumer<C1,C2,C3,I1,I2,I3> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3) -> lambda.accept(capture1, capture2, capture3, internal1, internal2, internal3);
    }  // 3 -> 3 -> 0

    public static <C1, I1,I2,I3,I4 > QuadriConsumer<I1,I2,I3,I4> consumer(C1 capture1, PentaConsumer<C1,I1,I2,I3,I4> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3, I4 internal4) -> lambda.accept(capture1, internal1, internal2, internal3, internal4);
    }  // 1 -> 4 -> 0
    public static <C1,C2, I1,I2,I3,I4 > QuadriConsumer<I1,I2,I3,I4> consumer(C1 capture1, C2 capture2, HexaConsumer<C1,C2,I1,I2,I3,I4> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3, I4 internal4) -> lambda.accept(capture1, capture2, internal1, internal2, internal3, internal4);
    }  // 2 -> 4 -> 0

    public static <C1, I1,I2,I3,I4,I5 > PentaConsumer<I1,I2,I3,I4,I5> consumer(C1 capture1, HexaConsumer<C1,I1,I2,I3,I4,I5> lambda) {
        return (I1 internal1, I2 internal2, I3 internal3, I4 internal4, I5 internal5) -> lambda.accept(capture1, internal1, internal2, internal3, internal4, internal5);
    }  // 1 -> 5 -> 0


    // same as above, but for predicates (they have the same erasure, so the name needs to differ)
    public static <C1, I1 > Predicate<I1> predicate(C1 capture1, BiFunction<C1,I1,Boolean> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, internal1);
    }  // 1 -> 1 -> !
    public static <C1,C2, I1 > Predicate<I1> predicate(C1 capture1, C2 capture2, TriFunction<C1,C2,I1,Boolean> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, capture2, internal1);
    }  // 2 -> 1 -> !
    public static <C1,C2,C3, I1 > Predicate<I1> predicate(C1 capture1, C2 capture2, C3 capture3, QuadriFunction<C1,C2,C3,I1,Boolean> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, capture2, capture3, internal1);
    }  // 3 -> 1 -> !
    public static <C1,C2,C3,C4, I1 > Predicate<I1> predicate(C1 capture1, C2 capture2, C3 capture3, C4 capture4, PentaFunction<C1,C2,C3,C4,I1,Boolean> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, capture2, capture3, capture4, internal1);
    }  // 4 -> 1 -> !
    public static <C1,C2,C3,C4,C5, I1 > Predicate<I1> predicate(C1 capture1, C2 capture2, C3 capture3, C4 capture4, C5 capture5, HexaFunction<C1,C2,C3,C4,C5,I1,Boolean> lambda) {
        return (I1 internal1) -> lambda.apply(capture1, capture2, capture3, capture4, capture5, internal1);
    }  // 5 -> 1 -> !

    public static <C1, I1,I2 > BiPredicate<I1,I2> predicate(C1 capture1, TriFunction<C1,I1,I2,Boolean> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.apply(capture1, internal1, internal2);
    }  // 1 -> 2 -> !
    public static <C1,C2, I1,I2 > BiPredicate<I1,I2> predicate(C1 capture1, C2 capture2, QuadriFunction<C1,C2,I1,I2,Boolean> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.apply(capture1, capture2, internal1, internal2);
    }  // 2 -> 2 -> !
    public static <C1,C2,C3, I1,I2 > BiPredicate<I1,I2> predicate(C1 capture1, C2 capture2, C3 capture3, PentaFunction<C1,C2,C3,I1,I2,Boolean> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.apply(capture1, capture2, capture3, internal1, internal2);
    }  // 3 -> 2 -> !
    public static <C1,C2,C3,C4, I1,I2 > BiPredicate<I1,I2> predicate(C1 capture1, C2 capture2, C3 capture3, C4 capture4, HexaFunction<C1,C2,C3,C4,I1,I2,Boolean> lambda) {
        return (I1 internal1, I2 internal2) -> lambda.apply(capture1, capture2, capture3, capture4, internal1, internal2);
    }  // 4 -> 2 -> !
}
