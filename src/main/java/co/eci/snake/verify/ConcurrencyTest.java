package co.eci.snake.verify;

import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Position;
import co.eci.snake.core.Snake;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyTest {
    public static void main(String[] args) throws InterruptedException {
        final Snake snake = Snake.of(10, 10, Direction.UP);
        final Board board = new Board(35, 28);
        final int iterations = 100000;
        final AtomicInteger errors = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Writer thread: advances the snake
        executor.submit(() -> {
            for (int i = 0; i < iterations; i++) {
                try {
                    snake.advance(new Position(i % 35, i % 28), i % 2 == 0);
                } catch (Exception e) {
                    System.err.println("Writer error: " + e);
                    errors.incrementAndGet();
                    break;
                }
            }
        });

        // Reader thread: takes snapshots
        executor.submit(() -> {
            for (int i = 0; i < iterations; i++) {
                try {
                    java.util.Deque<Position> snap = snake.snapshot();
                    // Basic consistency: size should at least be 1 (the head)
                    if (snap.isEmpty()) {
                        System.err.println("Error: Empty snake snapshot!");
                        errors.incrementAndGet();
                    }
                    // Without a getter for maxLength, we can't be perfect, 
                    // but we can check if size changes wildly or is null.
                } catch (Exception e) {
                    System.err.println("Reader error: " + e);
                    errors.incrementAndGet();
                    break;
                }
            }
        });

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        if (errors.get() == 0) {
            System.out.println(
                    "SUCCESS: No ConcurrentModificationException detected after " + iterations + " iterations.");
        } else {
            System.err.println("FAILURE: " + errors.get() + " errors detected.");
        }
    }
}
