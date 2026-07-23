public class TestAtan2 {
    public static void main(String[] args) {
        // Vanilla arrow calculation
        double[][] motions = {
            {0, 1},   // South (+Z)
            {-1, 0},  // West (-X)
            {0, -1},  // North (-Z)
            {1, 0}    // East (+X)
        };
        String[] dirs = {"South(+Z)", "West(-X)", "North(-Z)", "East(+X)"};
        
        for (int i=0; i<4; i++) {
            double motionX = motions[i][0];
            double motionZ = motions[i][1];
            float yaw = (float)(Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
            System.out.println(dirs[i] + " -> motionX=" + motionX + " motionZ=" + motionZ + " -> yaw=" + yaw);
        }
    }
}
