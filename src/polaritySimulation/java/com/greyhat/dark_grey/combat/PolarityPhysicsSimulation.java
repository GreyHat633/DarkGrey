package com.greyhat.dark_grey.combat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.AxisAlignedBB;

/** Deterministic, no-AI verification for the production polarity math. */
public final class PolarityPhysicsSimulation {

    private static final double RANGE = 20.0D;
    private static final double MAX_PAIR_ACCELERATION = 0.20D;
    private static final double MAX_NET_DELTA = 0.40D;
    private static final double COLLISION_TOLERANCE = 0.20D;
    private static final double COLLISION_SPEED_THRESHOLD = 0.90D;
    private static final double ENTITY_WIDTH = 0.60D;
    private static final double HORIZONTAL_DRAG = 0.91D;

    private PolarityPhysicsSimulation() {}

    public static void main(String[] args) throws Exception {
        File output = new File(args.length == 0 ? "极性物理模拟.csv" : args[0]);
        File parent = output.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("无法创建输出目录：" + parent);
        }

        List<String> lines = new ArrayList<>();
        lines.add(
            "测试组,初始距离,Tick,距离,单对磁力,本Tick净磁力A,本Tick净磁力B,施力前速度A,施力前速度B,施力后预移动速度A,施力后预移动速度B,移动后motionA,移动后motionB,相对闭合速度,实际接触,扫掠接触,触发爆炸,备注");

        List<AttractionResult> attractionResults = new ArrayList<>();
        for (double distance : new double[] { 2.0D, 5.0D, 10.0D, 15.0D, 19.0D }) {
            attractionResults.add(runAttraction(distance, lines));
        }

        List<RepulsionResult> repulsionResults = new ArrayList<>();
        for (double distance : new double[] { 1.0D, 3.0D, 6.0D, 10.0D, 15.0D }) {
            repulsionResults.add(runRepulsion(distance, lines));
        }
        assertRepulsionOrdering(repulsionResults);

        MixedResult mixed = runMixedEntityTest(lines);
        runOverlapAndSweepTests(lines);
        runDamageFormulaTests(lines);

        lines.add("");
        lines.add("汇总组,初始距离,总Tick,碰撞前闭合速度,判定结果,补充指标");
        for (AttractionResult result : attractionResults) {
            lines.add(
                csv(
                    "异极吸引汇总",
                    result.initialDistance,
                    result.ticks,
                    result.closingSpeed,
                    result.triggered ? "触发爆炸" : "未触发",
                    result.swept ? "扫掠接触" : "实际接触"));
        }
        for (RepulsionResult result : repulsionResults) {
            lines.add(
                csv(
                    "同极排斥汇总",
                    result.initialDistance,
                    result.ticks,
                    result.maxSeparationSpeed,
                    "通过",
                    "最终距离=" + format(result.finalDistance) + "；最终剩余分离速度=" + format(result.finalSeparationSpeed)));
        }
        lines.add(
            csv(
                "十实体混合汇总",
                "-",
                mixed.iterations,
                "-",
                mixed.orderIndependent ? "顺序无关且限幅正确" : "失败",
                "单次十实体求解平均耗时微秒=" + format(mixed.averageMicros)));

        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }

        System.out.println("极性确定性模拟通过：" + output.getAbsolutePath());
        for (AttractionResult result : attractionResults) {
            System.out.println(
                String.format(
                    Locale.ROOT,
                    "异极 %.0f格：%d tick，碰撞前闭合速度 %.6f，%s，爆炸=%s",
                    result.initialDistance,
                    result.ticks,
                    result.closingSpeed,
                    result.swept ? "扫掠接触" : "实际接触",
                    result.triggered));
        }
        for (RepulsionResult result : repulsionResults) {
            System.out.println(
                String.format(
                    Locale.ROOT,
                    "同极 %.0f格：峰值分离速度 %.6f，2000 tick最终距离 %.6f，剩余速度 %.9f",
                    result.initialDistance,
                    result.maxSeparationSpeed,
                    result.finalDistance,
                    result.finalSeparationSpeed));
        }
        System.out.println(
            String.format(Locale.ROOT, "十实体混合：顺序无关=%s，平均 %.3f 微秒/次", mixed.orderIndependent, mixed.averageMicros));
    }

    private static AttractionResult runAttraction(double initialDistance, List<String> lines) {
        Body a = new Body(1, 0.0D, 1);
        Body b = new Body(2, initialDistance, -1);
        for (int tick = 1; tick <= 20000; tick++) {
            double distance = b.x - a.x;
            double acceleration = PolarityPhysicsMath
                .pairAcceleration(Math.abs(distance), RANGE, MAX_PAIR_ACCELERATION);
            double beforeA = a.velocity;
            double beforeB = b.velocity;
            double deltaA = acceleration;
            double deltaB = -acceleration;
            a.velocity += deltaA;
            b.velocity += deltaB;
            double preMoveA = a.velocity;
            double preMoveB = b.velocity;
            double closing = PolarityPhysicsMath
                .closingSpeed(preMoveA, 0.0D, 0.0D, preMoveB, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D);

            AxisAlignedBB startA = box(a.x);
            AxisAlignedBB startB = box(b.x);
            a.x += preMoveA;
            b.x += preMoveB;
            AxisAlignedBB endA = box(a.x);
            AxisAlignedBB endB = box(b.x);
            boolean actual = PolarityPhysicsMath.intersectsWithTolerance(endA, endB, COLLISION_TOLERANCE);
            double sweptTime = PolarityPhysicsMath
                .sweptContactTime(startA, startB, preMoveA, 0.0D, 0.0D, preMoveB, 0.0D, 0.0D, COLLISION_TOLERANCE);
            boolean swept = !actual && !Double.isNaN(sweptTime);
            boolean triggered = (actual || swept) && closing >= COLLISION_SPEED_THRESHOLD;

            a.velocity *= HORIZONTAL_DRAG;
            b.velocity *= HORIZONTAL_DRAG;
            lines.add(
                tickLine(
                    "异极吸引",
                    initialDistance,
                    tick,
                    Math.abs(distance),
                    acceleration,
                    deltaA,
                    deltaB,
                    beforeA,
                    beforeB,
                    preMoveA,
                    preMoveB,
                    a.velocity,
                    b.velocity,
                    closing,
                    actual,
                    swept,
                    triggered,
                    "无AI、初速0、水平阻力0.91"));
            if (actual || swept) {
                require(triggered, "异极 " + initialDistance + " 格发生接触时闭合速度未达到门槛");
                return new AttractionResult(initialDistance, tick, closing, swept, true);
            }
        }
        throw new AssertionError("异极 " + initialDistance + " 格在限定Tick内未接触");
    }

    private static RepulsionResult runRepulsion(double initialDistance, List<String> lines) {
        Body a = new Body(1, 0.0D, 1);
        Body b = new Body(2, initialDistance, 1);
        double maximumSeparationSpeed = 0.0D;
        int totalTicks = 2000;
        for (int tick = 1; tick <= totalTicks; tick++) {
            double distance = b.x - a.x;
            double acceleration = PolarityPhysicsMath
                .pairAcceleration(Math.abs(distance), RANGE, MAX_PAIR_ACCELERATION);
            double beforeA = a.velocity;
            double beforeB = b.velocity;
            double deltaA = -acceleration;
            double deltaB = acceleration;
            a.velocity += deltaA;
            b.velocity += deltaB;
            double preMoveA = a.velocity;
            double preMoveB = b.velocity;
            double separationSpeed = preMoveB - preMoveA;
            maximumSeparationSpeed = Math.max(maximumSeparationSpeed, separationSpeed);
            a.x += preMoveA;
            b.x += preMoveB;
            a.velocity *= HORIZONTAL_DRAG;
            b.velocity *= HORIZONTAL_DRAG;

            lines.add(
                tickLine(
                    "同极排斥",
                    initialDistance,
                    tick,
                    Math.abs(distance),
                    acceleration,
                    deltaA,
                    deltaB,
                    beforeA,
                    beforeB,
                    preMoveA,
                    preMoveB,
                    a.velocity,
                    b.velocity,
                    -separationSpeed,
                    false,
                    false,
                    false,
                    "无AI、初速0、水平阻力0.91"));
        }
        return new RepulsionResult(
            initialDistance,
            totalTicks,
            b.x - a.x,
            Math.max(0.0D, b.velocity - a.velocity),
            maximumSeparationSpeed);
    }

    private static void assertRepulsionOrdering(List<RepulsionResult> results) {
        for (int i = 1; i < results.size(); i++) {
            RepulsionResult closer = results.get(i - 1);
            RepulsionResult farther = results.get(i);
            require(
                closer.maxSeparationSpeed > farther.maxSeparationSpeed,
                "同极排斥不满足初始越近、峰值分离速度越高：" + closer.initialDistance + " / " + farther.initialDistance);
            require(
                closer.finalDistance > farther.finalDistance,
                "同极排斥不满足初始越近、固定窗口最终分离距离越大：" + closer.initialDistance + " / " + farther.initialDistance);
        }
    }

    private static MixedResult runMixedEntityTest(List<String> lines) {
        List<VecBody> bodies = Arrays.asList(
            new VecBody(1, -4.0D, 0.0D, 0.0D, 1),
            new VecBody(2, 4.0D, 0.0D, 0.0D, -1),
            new VecBody(3, 0.0D, -4.0D, 0.0D, 1),
            new VecBody(4, 0.0D, 4.0D, 0.0D, -1),
            new VecBody(5, 0.0D, 0.0D, -4.0D, 1),
            new VecBody(6, 0.0D, 0.0D, 4.0D, -1),
            new VecBody(7, -3.0D, -3.0D, 1.0D, -1),
            new VecBody(8, 3.0D, 3.0D, -1.0D, 1),
            new VecBody(9, -2.0D, 3.0D, 2.0D, 1),
            new VecBody(10, 2.0D, -3.0D, -2.0D, -1));

        List<VecBody> ascending = new ArrayList<>(bodies);
        List<VecBody> descending = new ArrayList<>(bodies);
        Collections.reverse(descending);
        double[][] forward = solveMixed(ascending);
        double[][] reverse = solveMixed(descending);
        boolean orderIndependent = true;
        for (int id = 1; id <= 10; id++) {
            for (int axis = 0; axis < 3; axis++) {
                orderIndependent &= Math.abs(forward[id][axis] - reverse[id][axis]) <= 1.0E-12D;
            }
            double magnitude = Math.sqrt(
                forward[id][0] * forward[id][0] + forward[id][1] * forward[id][1] + forward[id][2] * forward[id][2]);
            require(magnitude <= MAX_NET_DELTA + 1.0E-12D, "十实体净磁力增量超过0.40");
        }
        require(orderIndependent, "十实体混合磁力依赖遍历顺序");

        int iterations = 10000;
        long started = System.nanoTime();
        for (int i = 0; i < iterations; i++) solveMixed((i & 1) == 0 ? ascending : descending);
        double averageMicros = (System.nanoTime() - started) / 1000.0D / iterations;
        lines.add(
            csv(
                "十实体混合",
                "-",
                1,
                "-",
                "每对一次",
                "所有向量先累加后限幅",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                false,
                false,
                false,
                "顺序无关=" + orderIndependent + "；最大净增量<=0.40；平均微秒=" + format(averageMicros)));
        return new MixedResult(orderIndependent, iterations, averageMicros);
    }

    private static double[][] solveMixed(List<VecBody> input) {
        List<VecBody> bodies = new ArrayList<>(input);
        bodies.sort(Comparator.comparingInt(body -> body.id));
        double[][] deltas = new double[11][3];
        double[] direction = new double[3];
        for (int i = 0; i < bodies.size(); i++) {
            VecBody a = bodies.get(i);
            for (int j = i + 1; j < bodies.size(); j++) {
                VecBody b = bodies.get(j);
                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double dz = b.z - a.z;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance >= RANGE) continue;
                PolarityPhysicsMath.directionAtoB(dx, dy, dz, a.id, b.id, direction);
                double acceleration = PolarityPhysicsMath.pairAcceleration(distance, RANGE, MAX_PAIR_ACCELERATION);
                double sign = a.polarity == b.polarity ? -1.0D : 1.0D;
                for (int axis = 0; axis < 3; axis++) {
                    double contribution = direction[axis] * acceleration * sign;
                    deltas[a.id][axis] += contribution;
                    deltas[b.id][axis] -= contribution;
                }
            }
        }
        for (int id = 1; id <= 10; id++) PolarityPhysicsMath.limitMagnitude(deltas[id], MAX_NET_DELTA);
        return deltas;
    }

    private static void runOverlapAndSweepTests(List<String> lines) {
        double[] first = new double[3];
        double[] second = new double[3];
        PolarityPhysicsMath.directionAtoB(0.0D, 0.0D, 0.0D, 7, 9, first);
        PolarityPhysicsMath.directionAtoB(0.0D, 0.0D, 0.0D, 7, 9, second);
        require(Arrays.equals(first, second), "完全重叠时的方向不稳定");
        require(
            Double.isFinite(first[0]) && Double.isFinite(first[1]) && Double.isFinite(first[2]),
            "完全重叠时出现NaN或Infinity");
        require(
            Math.abs(PolarityPhysicsMath.pairAcceleration(0.0D, RANGE, MAX_PAIR_ACCELERATION) - 0.20D) < 1.0E-12D,
            "完全重叠时没有使用r趋近0的最大磁力极限");

        AxisAlignedBB a = box(0.0D);
        AxisAlignedBB b = box(5.0D);
        double time = PolarityPhysicsMath
            .sweptContactTime(a, b, 3.0D, 0.0D, 0.0D, -3.0D, 0.0D, 0.0D, COLLISION_TOLERANCE);
        require(!Double.isNaN(time) && time >= 0.0D && time <= 1.0D, "高速交叉未被扫掠检测捕获");
        lines.add(
            csv(
                "重叠与扫掠",
                0,
                1,
                0,
                0.20D,
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                6.0D,
                false,
                true,
                true,
                "稳定方向=" + Arrays.toString(first) + "；扫掠接触时刻=" + format(time)));
    }

    private static void runDamageFormulaTests(List<String> lines) {
        PolarityDamageResolver.DamageValues zero = PolarityDamageResolver.calculateDamageValues(0, 0, false);
        PolarityDamageResolver.DamageValues zeroSpecial = PolarityDamageResolver.calculateDamageValues(0, 0, true);
        PolarityDamageResolver.DamageValues iron = PolarityDamageResolver.calculateDamageValues(20, 20, false);
        PolarityDamageResolver.DamageValues ironSpecial = PolarityDamageResolver.calculateDamageValues(20, 20, true);
        require(
            zero.collisionDamage == 160 && zero.normalExplosionDamage == 50 && zero.appliedExplosionDamage == 50,
            "零护甲伤害公式错误");
        require(zeroSpecial.appliedExplosionDamage == 200, "零护甲施加者参与爆炸倍率错误");
        require(
            iron.collisionDamage == 460 && iron.normalExplosionDamage == 200 && iron.appliedExplosionDamage == 200,
            "双方20护甲伤害公式错误");
        require(ironSpecial.appliedExplosionDamage == 800, "双方20护甲施加者参与爆炸倍率错误");
        lines.add(
            csv(
                "伤害公式",
                "-",
                1,
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                false,
                false,
                false,
                "0+0护甲=160/50/200；20+20护甲=460/200/800"));
    }

    private static AxisAlignedBB box(double centerX) {
        double half = ENTITY_WIDTH * 0.5D;
        return AxisAlignedBB.getBoundingBox(centerX - half, 0.0D, -half, centerX + half, 1.8D, half);
    }

    private static String tickLine(String group, double initialDistance, int tick, double distance, double acceleration,
        double deltaA, double deltaB, double beforeA, double beforeB, double preMoveA, double preMoveB,
        double postMoveA, double postMoveB, double closing, boolean actual, boolean swept, boolean triggered,
        String note) {
        return csv(
            group,
            initialDistance,
            tick,
            distance,
            acceleration,
            deltaA,
            deltaB,
            beforeA,
            beforeB,
            preMoveA,
            preMoveB,
            postMoveA,
            postMoveB,
            closing,
            actual,
            swept,
            triggered,
            note);
    }

    private static String csv(Object... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(',');
            String value = values[i] instanceof Number ? format(((Number) values[i]).doubleValue())
                : String.valueOf(values[i]);
            builder.append('"')
                .append(value.replace("\"", "\"\""))
                .append('"');
        }
        return builder.toString();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.9f", value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Body {

        private final int id;
        private final int polarity;
        private double x;
        private double velocity;

        private Body(int id, double x, int polarity) {
            this.id = id;
            this.x = x;
            this.polarity = polarity;
        }
    }

    private static final class VecBody {

        private final int id;
        private final double x;
        private final double y;
        private final double z;
        private final int polarity;

        private VecBody(int id, double x, double y, double z, int polarity) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.polarity = polarity;
        }
    }

    private static final class AttractionResult {

        private final double initialDistance;
        private final int ticks;
        private final double closingSpeed;
        private final boolean swept;
        private final boolean triggered;

        private AttractionResult(double initialDistance, int ticks, double closingSpeed, boolean swept,
            boolean triggered) {
            this.initialDistance = initialDistance;
            this.ticks = ticks;
            this.closingSpeed = closingSpeed;
            this.swept = swept;
            this.triggered = triggered;
        }
    }

    private static final class RepulsionResult {

        private final double initialDistance;
        private final int ticks;
        private final double finalDistance;
        private final double finalSeparationSpeed;
        private final double maxSeparationSpeed;

        private RepulsionResult(double initialDistance, int ticks, double finalDistance, double finalSeparationSpeed,
            double maxSeparationSpeed) {
            this.initialDistance = initialDistance;
            this.ticks = ticks;
            this.finalDistance = finalDistance;
            this.finalSeparationSpeed = finalSeparationSpeed;
            this.maxSeparationSpeed = maxSeparationSpeed;
        }
    }

    private static final class MixedResult {

        private final boolean orderIndependent;
        private final int iterations;
        private final double averageMicros;

        private MixedResult(boolean orderIndependent, int iterations, double averageMicros) {
            this.orderIndependent = orderIndependent;
            this.iterations = iterations;
            this.averageMicros = averageMicros;
        }
    }
}
